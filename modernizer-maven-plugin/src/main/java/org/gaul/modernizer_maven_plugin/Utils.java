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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.Opcodes;

final class Utils {
    static final int ASM_API = Opcodes.ASM9;

    static void checkArgument(boolean expression) {
        checkArgument(expression, null);
    }

    static void checkArgument(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    static <T> Set<T> createImmutableSet(Collection<T> collection) {
        return Collections.unmodifiableSet(new HashSet<>(
                Objects.requireNonNull(collection)));
    }

    static <T, U> Map<T, U> createImmutableMap(Map<T, U> map) {
        return Collections.unmodifiableMap(new HashMap<>(
                Objects.requireNonNull(map)));
    }

    private Utils() {
        throw new AssertionError("Intentionally not implemented");
    }
}
