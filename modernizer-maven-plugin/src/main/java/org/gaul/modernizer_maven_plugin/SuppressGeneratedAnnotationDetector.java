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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public final class SuppressGeneratedAnnotationDetector {
    private final Set<String> annotatedClassNames =
        new HashSet<String>();
    private final Set<String> allClassNames =
        new HashSet<String>();

    private SuppressGeneratedAnnotationDetector() { }

    public static Set<String> detect(Path path) throws IOException {
        SuppressGeneratedAnnotationDetector detector =
            new SuppressGeneratedAnnotationDetector();
        detector.detectInternal(path);
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
            final String name = desc.substring(Math.max(desc.lastIndexOf('/'),
                    desc.lastIndexOf('$')) + 1);
            boolean isGenerated = name.contains("Generated");
            if (isGenerated) {
                annotatedClassNames.add(className);
            }
            return super.visitAnnotation(desc, visible);
        }
    }
}
