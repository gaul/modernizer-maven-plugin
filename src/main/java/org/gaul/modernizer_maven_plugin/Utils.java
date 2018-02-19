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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Utils {
    static final Charset UTF_8 = Charset.forName("UTF-8");

    static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    static void checkArgument(boolean expression) {
        checkArgument(expression, null);
    }

    static void checkArgument(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    static <T> Set<T> createImmutableSet(Collection<T> collection) {
        return Collections.unmodifiableSet(new HashSet<T>(
                Utils.checkNotNull(collection)));
    }

    static <T, U> Map<T, U> createImmutableMap(Map<T, U> map) {
        return Collections.unmodifiableMap(new HashMap<T, U>(
                Utils.checkNotNull(map)));
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ioe) {
            // swallow exception
        }
    }

    static Collection<String> readAllLines(InputStream is) throws IOException {
        Collection<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is,
                UTF_8));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            lines.add(line);
        }
        return lines;
    }

    private Utils() {
        throw new AssertionError("Intentionally not implemented");
    }
}
