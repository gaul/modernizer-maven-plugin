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

import java.util.List;

import org.apache.maven.plugin.logging.Log;

public final class LoggerOutputer implements Outputer {
    private final Log log;
    private final String level;

    public LoggerOutputer(Log log, String level) {
        this.log = log;
        this.level = level;
    }

    @Override
    public void output(List<OutputEntry> entries) {
        for (OutputEntry entry : entries) {
            String message = entry.getFileName() + ":" +
                    entry.getOccurrence().getLineNumber() + ": " +
                    entry.getOccurrence().getViolation().getComment();
            if (level.equals("error")) {
                log.error(message);
            } else if (level.equals("warn")) {
                log.warn(message);
            } else if (level.equals("info")) {
                log.info(message);
            } else if (level.equals("debug")) {
                log.debug(message);
            } else {
                throw new IllegalStateException(
                        "unexpected log level, was: " + level);
            }
        }
    }
}
