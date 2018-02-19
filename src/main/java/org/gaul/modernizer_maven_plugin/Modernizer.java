/*
 * Copyright 2014-2018 Andrew Gaul <andrew@gaul.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gaul.modernizer_maven_plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class Modernizer {
    private final long javaVersion;
    private final Map<String, Violation> violations;
    private final Collection<String> exclusions;
    private final Collection<Pattern> exclusionPatterns;
    private final Collection<String> ignorePackages;
    private final Collection<Pattern> ignoreFullClassNamePatterns;

    Modernizer(String javaVersion, Map<String, Violation> violations,
            Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Collection<Pattern> ignoreClassNamePatterns) {
        long version;
        if (javaVersion.startsWith("1.")) {
            version = Long.parseLong(javaVersion.substring(2));
        } else {
            version = Long.parseLong(javaVersion);
        }
        Utils.checkArgument(version >= 0);
        this.javaVersion = version;
        this.violations = Utils.createImmutableMap(violations);
        this.exclusions = Utils.createImmutableSet(exclusions);
        this.exclusionPatterns = Utils.createImmutableSet(exclusionPatterns);
        this.ignorePackages = Utils.createImmutableSet(ignorePackages);
        this.ignoreFullClassNamePatterns
            = Utils.createImmutableSet(ignoreClassNamePatterns);
    }

    Collection<ViolationOccurrence> check(ClassReader classReader)
            throws IOException {
        ModernizerClassVisitor classVisitor = new ModernizerClassVisitor(
                javaVersion, violations, exclusions, exclusionPatterns,
                ignorePackages, ignoreFullClassNamePatterns);
        classReader.accept(classVisitor, 0);
        return classVisitor.getOccurrences();
    }

    Collection<ViolationOccurrence> check(InputStream is) throws IOException {
        return check(new ClassReader(is));
    }

    static Map<String, Violation> parseFromXml(InputStream is)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Violation> map =
                new HashMap<String, Violation>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("violation");
        for (int temp = 0; temp < nList.getLength(); ++temp) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) nNode;
            String version = element.getElementsByTagName("version").item(0)
                    .getTextContent();
            if (!version.startsWith("1.")) {
                throw new IllegalArgumentException(
                        "Invalid version, must have the form 1.6");
            }
            Violation violation = new Violation(
                    element.getElementsByTagName("name").item(0)
                            .getTextContent(),
                    Integer.parseInt(version.substring(2)),
                    element.getElementsByTagName("comment").item(0)
                            .getTextContent());
            map.put(violation.getName(), violation);
        }

        return map;
    }
}

final class ModernizerClassVisitor extends ClassVisitor {
    private final long javaVersion;
    private final Map<String, Violation> violations;
    private final Collection<String> exclusions;
    private final Collection<Pattern> exclusionPatterns;
    private final Collection<String> ignorePackages;
    private final Collection<Pattern> ignoreFullClassNamePatterns;
    private final Collection<ViolationOccurrence> occurrences =
            new ArrayList<ViolationOccurrence>();
    private String packageName;
    private String className;

    ModernizerClassVisitor(long javaVersion,
            Map<String, Violation> violations, Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Collection<Pattern> ignoreFullClassNamePatterns) {
        super(Opcodes.ASM5);
        Utils.checkArgument(javaVersion >= 0);
        this.javaVersion = javaVersion;
        this.violations = Utils.checkNotNull(violations);
        this.exclusions = Utils.checkNotNull(exclusions);
        this.exclusionPatterns = Utils.checkNotNull(exclusionPatterns);
        this.ignorePackages = Utils.checkNotNull(ignorePackages);
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
        MethodVisitor origVisitor = new MethodVisitor(Opcodes.ASM5, base) {
        };
        InstructionAdapter adapter = new InstructionAdapter(Opcodes.ASM5,
                origVisitor) {
            private int lineNumber = -1;

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
        };
        return adapter;
    }

    private void checkToken(String token, Violation violation, String name,
            int lineNumber) {
        if (violation != null && !exclusions.contains(token) &&
                javaVersion >= violation.getVersion() &&
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
