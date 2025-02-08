/*
 * Copyright 2014-2025 Andrew Gaul <andrew@gaul.org>
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

public final class SuppressModernizerAnnotationDetector {
    private final Set<String> annotatedClassNames =
        new HashSet<String>();
    private final Set<String> allClassNames =
        new HashSet<String>();

    private SuppressModernizerAnnotationDetector() { }

    public static Set<String> detect(Path path) throws IOException {
        SuppressModernizerAnnotationDetector detector =
            new SuppressModernizerAnnotationDetector();
        detector.detectInternal(path);
        return detector.computeSuppressedClassNames();
    }

    // For testing
    static Set<String> detect(Class<?>... classes) throws IOException {
        SuppressModernizerAnnotationDetector detector =
            new SuppressModernizerAnnotationDetector();
        for (Class<?> clazz : classes) {
            ClassReader classReader = new ClassReader(clazz.getName());
            detector.detectInternal(classReader);
        }
        return detector.computeSuppressedClassNames();
    }

    private Set<String> computeSuppressedClassNames() {
        Set<String> suppressedClassNames =
            new HashSet<String>(annotatedClassNames);
        for (String className : allClassNames) {
            if (suppressedClassNames.contains(className)) {
                continue;
            }
            int fromIndex = 0;
            while (true) {
                int index = className.indexOf('$', fromIndex);
                if (index == -1) {
                    break;
                }
                boolean outerSuppressed =
                    annotatedClassNames.contains(className.substring(0, index));
                if (outerSuppressed) {
                    suppressedClassNames.add(className);
                    break;
                }
                fromIndex = index + 1;
            }
        }
        return suppressedClassNames;
    }

    private void detectInternal(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        } else if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                Iterable<Path> children = stream::iterator;
                for (Path child : children) {
                    detectInternal(path.resolve(child));
                }
            }
        } else if (path.toString().endsWith(".class")) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                detectInternal(new ClassReader(inputStream));
            }
        }
    }

    private void detectInternal(ClassReader classReader) {
        classReader.accept(new Visitor(), 0);
    }

    private final class Visitor extends ClassVisitor {
        private String className;

        Visitor() {
            super(ASM_API);
        }

        @Override
        public void visit(int version, int access, String name,
                          String signature, String superName,
                          String[] interfaces) {
            this.className = name;
            allClassNames.add(className);
            super.visit(version, access, name,
                        signature, superName,
                        interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            boolean isSuppressModernizer = Type.getType(desc).getClassName()
                .equals(SuppressModernizer.class.getName());
            if (isSuppressModernizer) {
                annotatedClassNames.add(className);
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodvisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new SimpleMethodVisitor(ASM_API, methodvisitor, className);
        }
    }

    private final class SimpleMethodVisitor extends MethodVisitor {
        private final String className;

        SimpleMethodVisitor(int api, MethodVisitor methodVisitor, String className) {
            super(api, methodVisitor);
            this.className = className;
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            boolean isSuppressModernizer = Type.getType(descriptor).getClassName().equals(SuppressModernizer.class.getName());
            if (isSuppressModernizer) {
                annotatedClassNames.add(className);
            }
            return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
        }
    }
}
