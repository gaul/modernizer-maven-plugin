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

package org.gaul.modernizer_maven_plugin.output;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.gaul.modernizer_maven_plugin.Violation;

public final class CodeClimateOutputer implements Outputer {
    public static final String DEFAULT_FILENAME = "code-quality.json";

    private final String outputFile;
    private final Severity severity;

    public CodeClimateOutputer(String outputFile, Severity severity) {
        this.outputFile = outputFile;
        this.severity = severity;
    }

    @Override
    public void output(List<OutputEntry> entries) throws IOException {
        List<Entry> toOutput = new ArrayList<>(entries.size());
        for (OutputEntry entry : entries) {
            Location location = new Location(
                    entry.getFileName(),
                    new Location.Lines(entry.getOccurrence().getLineNumber()));
            Violation violation = entry.getOccurrence().getViolation();
            toOutput.add(new Entry(
                    violation.getComment(),
                    violation.getName(),
                    Integer.toString(entry.hashCode()),
                    severity,
                    location));
        }
        TypeToken<List<Entry>> type = new TypeToken<List<Entry>>() {
        };
        OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(outputFile)),
                StandardCharsets.UTF_8);
        new Gson().toJson(toOutput, type.getType(), writer);
        writer.close();
    }

    public enum Severity {
        @SerializedName("info")
        INFO,

        @SerializedName("minor")
        MINOR,

        @SerializedName("major")
        MAJOR,

        @SerializedName("critical")
        CRITICAL,

        @SerializedName("blocker")
        BLOCKER;
    }

    private static final class Entry {
        private final String description;
        private final String checkName;
        private final String fingerprint;
        private final Severity severity;
        private final Location location;

        private Entry(String description,
                      String checkName,
                      String fingerprint,
                      Severity severity,
                      Location location) {
            this.description = description;
            this.checkName = checkName;
            this.fingerprint = fingerprint;
            this.severity = severity;
            this.location = location;
        }

        public String getDescription() {
            return description;
        }

        public String getCheckName() {
            return checkName;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public Severity getSeverity() {
            return severity;
        }

        public Location getLocation() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry that = (Entry) o;
            return Objects.equals(description, that.description) &&
                    Objects.equals(checkName, that.checkName) &&
                    Objects.equals(fingerprint, that.fingerprint) &&
                    severity == that.severity &&
                    Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(description,
                    checkName,
                    fingerprint,
                    severity,
                    location);
        }

        @Override
        public String toString() {
            return "CodeClimateEntry{" +
                    "description='" + description + '\'' +
                    ", checkName='" + checkName + '\'' +
                    ", fingerprint='" + fingerprint + '\'' +
                    ", severity=" + severity +
                    ", location=" + location +
                    '}';
        }
    }

    private static final class Location {
        private final String path;
        private final Lines lines;

        private Location(String path, Lines lines) {
            this.path = path;
            this.lines = lines;
        }

        public String getPath() {
            return path;
        }

        public Lines getLines() {
            return lines;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Location)) {
                return false;
            }
            Location location = (Location) o;
            return Objects.equals(path, location.path) &&
                    Objects.equals(lines, location.lines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, lines);
        }

        @Override
        public String toString() {
            return "Location{" +
                    "path='" + path + '\'' +
                    ", lines=" + lines +
                    '}';
        }

        private static final class Lines {
            private final int begin;

            private Lines(int begin) {
                this.begin = begin;
            }

            public int getBegin() {
                return begin;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof Lines)) {
                    return false;
                }
                Lines lines = (Lines) o;
                return begin == lines.begin;
            }

            @Override
            public int hashCode() {
                return Objects.hash(begin);
            }

            @Override
            public String toString() {
                return "Lines{" +
                        "begin=" + begin +
                        '}';
            }
        }
    }

}
