/*
 * Copyright 2014-2022 Andrew Gaul <andrew@gaul.org>
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

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

    ModernizerClassVisitor(long javaVersion,
            Map<String, Violation> violations, Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Set<String> ignoreClassNames,
            Collection<Pattern> ignoreFullClassNamePatterns) {
        super(ASM_API);
        Utils.checkArgument(javaVersion >= 0);
        this.javaVersion = javaVersion;
        this.violations = Utils.checkNotNull(violations);
        this.exclusions = Utils.checkNotNull(exclusions);
        this.exclusionPatterns = Utils.checkNotNull(exclusionPatterns);
        this.ignorePackages = Utils.checkNotNull(ignorePackages);
        this.ignoreClassNames = Utils.checkNotNull(ignoreClassNames);
        this.ignoreFullClassNamePatterns =
                Utils.checkNotNull(ignoreFullClassNamePatterns);
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
        if (ignoreClass()) {
            return;
        }
        for (String itr : interfaces) {
            Violation violation = violations.get(itr);
            checkToken(itr, violation, name, /*lineNumber=*/ -1);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, final String methodName,
            final String methodDescriptor, final String methodSignature,
            String[] exceptions) {
        MethodVisitor base = super.visitMethod(access, methodName,
                methodDescriptor, methodSignature, exceptions);
        MethodVisitor origVisitor = new MethodVisitor(ASM_API, base) {
        };
        InstructionAdapter adapter = new InstructionAdapter(ASM_API,
                origVisitor) {
            private int lineNumber = -1;
            private boolean methodSuppressed = false;

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
                methodSuppressed |= Type.getType(desc).getClassName()
                    .equals(SuppressModernizer.class.getName());

                String name = Type.getType(desc).getInternalName();
                Violation violation = violations.get(name);
                checkToken(name, violation, name, lineNumber);

                return super.visitAnnotation(desc, visible);
            }

            private void visitFieldOrMethod(String owner, String name,
                    String desc) {
                String token = owner + "." + name + ":" + desc;
                Violation violation = violations.get(token);
                checkToken(token, violation, name, lineNumber);
            }

            @Override
            public void visitLineNumber(int lineNumber, Label start) {
                this.lineNumber = lineNumber;
            }

            private void checkToken(
                String token,
                Violation violation,
                String name,
                int lineNumber
            ) {
                if (methodSuppressed) {
                    return;
                } else {
                    ModernizerClassVisitor.this
                        .checkToken(token, violation, name, lineNumber);
                }

            }
        };
        return adapter;
    }

    private void checkToken(String token, Violation violation, String name,
            int lineNumber) {
        if (violation != null && !exclusions.contains(token) &&
                javaVersion >= violation.getVersion() &&
                (!violation.getUntil().isPresent() ||
                        javaVersion < violation.getUntil().getAsInt()) &&
                !ignorePackages.contains(packageName)) {
            if (ignoreClass()) {
                return;
            }
            for (Pattern pattern : exclusionPatterns) {
                if (pattern.matcher(token).matches()) {
                    return;
                }
            }
            for (String prefix : ignorePackages) {
                if (packageName.startsWith(prefix + ".")) {
                    return;
                }
            }
            occurrences.add(new ViolationOccurrence(name, lineNumber,
                    violation));
        }
    }

    private boolean ignoreClass() {
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
