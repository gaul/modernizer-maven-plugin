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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;

@SuppressWarnings("deprecation")
public class SuppressModernizerSimpleNameTestClasses {

    @Retention(CLASS)
    @Target({TYPE, METHOD, CONSTRUCTOR})
    public @interface SuppressModernizer { }

    @SuppressModernizer
    public static final class SuppressedOnClass {
        public Charset getCharset() {
            return Charsets.UTF_8;
        }
    }

    public static final class SuppressedOnMembers {
        @SuppressModernizer
        public Charset getCharset() {
            return Charsets.UTF_8;
        }
    }
}
