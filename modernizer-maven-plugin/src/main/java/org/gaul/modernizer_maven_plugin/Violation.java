/*
 * Copyright 2014-2022 Andrew Gaul <andrew@gaul.org>
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

import java.util.Optional;
import java.util.OptionalInt;

public final class Violation {
    private final String name;
    private final int version;
    private final OptionalInt until; // ignoring violation since this version
    private final String comment;

    @Deprecated
    Violation(String name, int version, String comment) {
        this(name, version, OptionalInt.empty(), comment);
    }

    Violation(String name, int version, OptionalInt until, String comment) {
        this.name = Utils.checkNotNull(name);
        Utils.checkArgument(version >= 0);
        this.version = version;
        this.until = Utils.checkNotNull(until);
        this.comment = Utils.checkNotNull(comment);
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public OptionalInt getUntil() {
        return until;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return name + " " + version + " " + comment;
    }
}
