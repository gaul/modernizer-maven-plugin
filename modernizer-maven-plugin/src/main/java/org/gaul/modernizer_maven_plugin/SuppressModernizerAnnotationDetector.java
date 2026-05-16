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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public final class SuppressModernizerAnnotationDetector {
    private SuppressModernizerAnnotationDetector() { }

    public static Set<String> detect(Path path) throws IOException {
        return newDetector().detect(path);
    }

    // For testing
    static Set<String> detect(Class<?>... classes) throws IOException {
        return newDetector().detect(classes);
    }

    private static AnnotationDetector newDetector() {
        return new AnnotationDetector(
                SuppressModernizerAnnotationDetector::isSuppressModernizerAnnotation,
                /*scanMethodBodies=*/ true);
    }

    static boolean isSuppressModernizerAnnotation(String desc) {
        String simpleName = desc.substring(Math.max(desc.lastIndexOf('/'),
                desc.lastIndexOf('$')) + 1);
        return simpleName.equals("SuppressModernizer;");
    }
}
