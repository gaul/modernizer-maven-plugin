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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public final class ModernizerMojoTest {

    private static final Path OUTPUT = Paths.get("target", "classes");
    private static final Path SOURCE = Paths.get("src", "main", "java");

    @Test
    public void mapToSourceForPlainClass() {
        Path classFile = OUTPUT.resolve("com/example/Foo.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve("com/example/Foo.java").toString());
    }

    @Test
    public void mapToSourceForAnonymousInnerClass() {
        // SimpleKVDatabase$1.class belongs to SimpleKVDatabase.java (#217)
        Path classFile = OUTPUT.resolve(
                "io/permazen/kv/simple/SimpleKVDatabase$1.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve(
                        "io/permazen/kv/simple/SimpleKVDatabase.java")
                        .toString());
    }

    @Test
    public void mapToSourceForNamedInnerClass() {
        Path classFile = OUTPUT.resolve("com/example/Foo$Bar.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve("com/example/Foo.java").toString());
    }

    @Test
    public void mapToSourceForDeeplyNestedClass() {
        Path classFile = OUTPUT.resolve("com/example/Foo$Bar$1.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve("com/example/Foo.java").toString());
    }

    @Test
    public void mapToSourceForPackageInfo() {
        Path classFile = OUTPUT.resolve("com/example/package-info.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve(
                        "com/example/package-info.java").toString());
    }
}
