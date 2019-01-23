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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.gaul.modernizer_maven_plugin
    .SuppressModernizerTestClasses.SuppressedOnClass;
import org.gaul.modernizer_maven_plugin
    .SuppressModernizerTestClasses.SuppressedOnMembers;
import org.junit.Test;

public final class SuppressModernizerAnnotationDetectorTest {

    @Test
    public void itDetectsSuppressModernizerAnnotation() throws IOException {
        Set<String> actual = SuppressModernizerAnnotationDetector.detect(
            SuppressedOnClass.class,
            SuppressedOnClass.InnerClass.class,
            SuppressedOnMembers.class,
            SuppressedOnMembers.InnerClass.class
        );

        assertThat(new TreeSet<String>(actual)).containsExactly(
            SuppressedOnClass.class.getName().replace('.', '/'),
            SuppressedOnClass.InnerClass.class.getName().replace('.', '/'),
            SuppressedOnMembers.InnerClass.class.getName().replace('.', '/')
        );
    }
}
