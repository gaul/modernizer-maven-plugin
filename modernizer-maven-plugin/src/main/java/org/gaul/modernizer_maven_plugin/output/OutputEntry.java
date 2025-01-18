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

package org.gaul.modernizer_maven_plugin.output;

import java.util.Objects;

import org.gaul.modernizer_maven_plugin.ViolationOccurrence;

public final class OutputEntry {
    private final String fileName;
    private final ViolationOccurrence occurrence;

    public OutputEntry(String fileName, ViolationOccurrence occurrence) {
        this.fileName = fileName;
        this.occurrence = occurrence;
    }

    public String getFileName() {
        return fileName;
    }

    public ViolationOccurrence getOccurrence() {
        return occurrence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutputEntry)) {
            return false;
        }
        OutputEntry that = (OutputEntry) o;
        return Objects.equals(fileName, that.fileName) &&
                Objects.equals(occurrence, that.occurrence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, occurrence);
    }

    @Override
    public String toString() {
        return "OutputEntry{" +
                "fileName='" + fileName + '\'' +
                ", occurrence=" + occurrence +
                '}';
    }
}
