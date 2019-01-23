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

import static org.gaul.modernizer_maven_plugin.Utils.ASM_API;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

public final class SuppressModernizerAnnotationDetector {
    private final Set<String> annotatedClassNames =
        new HashSet<String>();
    private final Set<String> allClassNames =
        new HashSet<String>();

    private SuppressModernizerAnnotationDetector() { }

    public static Set<String> detect(File file) throws IOException {
        SuppressModernizerAnnotationDetector detector =
            new SuppressModernizerAnnotationDetector();
        detector.detectInternal(file);
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

    private void detectInternal(File file) throws IOException {
        if (!file.exists()) {
            return;
        } else if (file.isDirectory()) {
            String[] children = file.list();
            if (children != null) {
                for (String child : children) {
                    detectInternal(new File(file, child));
                }
            }
        } else if (file.getPath().endsWith(".class")) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                detectInternal(new ClassReader(inputStream));
            } finally {
                Utils.closeQuietly(inputStream);
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
    }
}
