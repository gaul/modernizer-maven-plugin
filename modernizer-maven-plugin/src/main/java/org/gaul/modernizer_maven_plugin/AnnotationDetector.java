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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

/**
 * Walks .class files looking for classes annotated with a marker annotation.
 * Inner classes whose enclosing class is annotated are also reported.
 */
final class AnnotationDetector {
    private final Predicate<String> isMarker;
    private final boolean scanMethodBodies;
    private final Set<String> annotatedClassNames = new HashSet<>();
    private final Set<String> allClassNames = new HashSet<>();

    AnnotationDetector(Predicate<String> isMarker, boolean scanMethodBodies) {
        this.isMarker = isMarker;
        this.scanMethodBodies = scanMethodBodies;
    }

    Set<String> detect(Path path) throws IOException {
        scan(path);
        return computeSuppressedClassNames();
    }

    Set<String> detect(Class<?>... classes) throws IOException {
        for (Class<?> clazz : classes) {
            scan(new ClassReader(clazz.getName()));
        }
        return computeSuppressedClassNames();
    }

    private void scan(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    try (InputStream is = Files.newInputStream(file)) {
                        scan(new ClassReader(is));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scan(ClassReader classReader) {
        classReader.accept(new Visitor(), 0);
    }

    private Set<String> computeSuppressedClassNames() {
        Set<String> suppressedClassNames = new HashSet<>(annotatedClassNames);
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
                if (annotatedClassNames.contains(
                        className.substring(0, index))) {
                    suppressedClassNames.add(className);
                    break;
                }
                fromIndex = index + 1;
            }
        }
        return suppressedClassNames;
    }

    private final class Visitor extends ClassVisitor {
        private String className;

        Visitor() {
            super(ASM_API);
        }

        @Override
        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
            this.className = name;
            allClassNames.add(className);
            super.visit(version, access, name, signature, superName,
                    interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (isMarker.test(desc)) {
                annotatedClassNames.add(className);
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor,
                    signature, exceptions);
            if (!scanMethodBodies) {
                return mv;
            }
            return new MethodVisitor(ASM_API, mv) {
                @Override
                public AnnotationVisitor visitLocalVariableAnnotation(
                        int typeRef, TypePath typePath, Label[] start,
                        Label[] end, int[] index, String desc,
                        boolean visible) {
                    if (isMarker.test(desc)) {
                        annotatedClassNames.add(className);
                    }
                    return super.visitLocalVariableAnnotation(typeRef,
                            typePath, start, end, index, desc, visible);
                }
            };
        }
    }
}
