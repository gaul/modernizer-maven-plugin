/*
 * Copyright 2014-2026 Andrew Gaul <andrew@gaul.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gaul.modernizer_maven_plugin;

import static org.gaul.modernizer_maven_plugin.Utils.ASM_API;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

final class ModernizerClassVisitor extends ClassVisitor {
    private final long javaVersion;
    private final Map<String, Violation> violations;
    private final Collection<String> exclusions;
    private final Collection<Pattern> exclusionPatterns;
    private final Collection<String> ignorePackages;
    private final Set<String> ignoreClassNames;
    private final Collection<Pattern> ignoreFullClassNamePatterns;
    private final Collection<ViolationOccurrence> occurrences =
            new ArrayList<ViolationOccurrence>();
    private String packageName;
    private String className;
    private boolean classIgnored;

    ModernizerClassVisitor(long javaVersion,
            Map<String, Violation> violations, Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Set<String> ignoreClassNames,
            Collection<Pattern> ignoreFullClassNamePatterns) {
        super(ASM_API);
        Utils.checkArgument(javaVersion >= 0);
        this.javaVersion = javaVersion;
        this.violations = Objects.requireNonNull(violations);
        this.exclusions = Objects.requireNonNull(exclusions);
        this.exclusionPatterns = Objects.requireNonNull(exclusionPatterns);
        this.ignorePackages = Objects.requireNonNull(ignorePackages);
        this.ignoreClassNames = Objects.requireNonNull(ignoreClassNames);
        this.ignoreFullClassNamePatterns =
                Objects.requireNonNull(ignoreFullClassNamePatterns);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        className = name;
        if (name.contains("/")) {
            packageName = name.substring(0, name.lastIndexOf('/'))
                    .replace('/', '.');
        } else {
            packageName = "";
        }
        classIgnored = computeClassIgnored();
        if (classIgnored) {
            return;
        }
        for (String itr : interfaces) {
            Violation violation = violations.get(itr);
            checkToken(itr, violation, itr, /*lineNumber=*/ -1);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName,
            String methodDescriptor, String methodSignature,
            String[] exceptions) {
        if (classIgnored) {
            return null;
        }
        return new MethodVisitor(ASM_API) {
            private int lineNumber = -1;
            private boolean methodSuppressed = false;
            private final List<ViolationOccurrence> pending = new ArrayList<>();

            @Override
            public void visitFieldInsn(int opcode, String owner, String name,
                    String desc) {
                visitFieldOrMethod(owner, name, desc);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                    String desc, boolean isInterface) {
                if (name.equals("<init>")) {
                    name = "\"<init>\"";
                }
                visitFieldOrMethod(owner, name, desc);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc,
                    boolean visible) {
                if (Type.getType(desc).getClassName()
                        .equals(SuppressModernizer.class.getName())) {
                    methodSuppressed = true;
                } else {
                    String name = Type.getType(desc).getInternalName();
                    Violation violation = violations.get(name);
                    queueCheck(name, violation, name, lineNumber);
                }
                return null;
            }

            private void visitFieldOrMethod(String owner, String name,
                    String desc) {
                String token = owner + "." + name + ":" + desc;
                Violation violation = violations.get(token);
                queueCheck(token, violation, name, lineNumber);
            }

            @Override
            public void visitLineNumber(int lineNumber, Label start) {
                this.lineNumber = lineNumber;
            }

            @Override
            public void visitEnd() {
                if (!methodSuppressed) {
                    occurrences.addAll(pending);
                }
                super.visitEnd();
            }

            private void queueCheck(String token, Violation violation,
                    String name, int lineNumber) {
                ViolationOccurrence occ =
                        ModernizerClassVisitor.this.evaluate(token, violation,
                                name, lineNumber);
                if (occ != null) {
                    pending.add(occ);
                }
            }
        };
    }

    private void checkToken(String token, Violation violation, String name,
            int lineNumber) {
        ViolationOccurrence occ = evaluate(token, violation, name, lineNumber);
        if (occ != null) {
            occurrences.add(occ);
        }
    }

    private ViolationOccurrence evaluate(String token, Violation violation,
            String name, int lineNumber) {
        // classIgnored is checked at visit()/visitMethod() so we never reach
        // this path on an ignored class.
        if (violation == null || exclusions.contains(token) ||
                javaVersion < violation.getVersion() ||
                ignorePackages.contains(packageName)) {
            return null;
        }
        for (Pattern pattern : exclusionPatterns) {
            if (pattern.matcher(token).matches()) {
                return null;
            }
        }
        for (String prefix : ignorePackages) {
            if (packageName.startsWith(prefix + ".")) {
                return null;
            }
        }
        return new ViolationOccurrence(name, lineNumber, violation);
    }

    private boolean computeClassIgnored() {
        if (ignoreClassNames.contains(className)) {
            return true;
        }
        for (Pattern pattern : ignoreFullClassNamePatterns) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    Collection<ViolationOccurrence> getOccurrences() {
        return occurrences;
    }
}
