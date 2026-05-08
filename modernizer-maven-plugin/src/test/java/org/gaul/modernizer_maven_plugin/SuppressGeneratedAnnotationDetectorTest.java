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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.gaul.modernizer_maven_plugin.SuppressGeneratedTestClasses.GeneratedClass;
import org.gaul.modernizer_maven_plugin.SuppressGeneratedTestClasses.NotGeneratedClass;
import org.gaul.modernizer_maven_plugin.SuppressGeneratedTestClasses.PlainClass;
import org.junit.jupiter.api.Test;

public final class SuppressGeneratedAnnotationDetectorTest {

    @Test
    public void itDetectsGeneratedAnnotation() throws IOException {
        Set<String> actual = SuppressGeneratedAnnotationDetector.detect(
            GeneratedClass.class,
            GeneratedClass.InnerClass.class,
            NotGeneratedClass.class,
            NotGeneratedClass.InnerClass.class,
            PlainClass.class
        );

        assertThat(new TreeSet<>(actual)).containsExactly(
            internalName(GeneratedClass.class),
            internalName(GeneratedClass.InnerClass.class)
        );
    }

    private static String internalName(Class<?> clazz) {
        return clazz.getName().replace('.', '/');
    }
}
