/*
 * Copyright 2014 Andrew Gaul <andrew@gaul.org>
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.gaul.modernizer_maven_plugin.Modernizer;
import org.gaul.modernizer_maven_plugin.Utils;
import org.gaul.modernizer_maven_plugin.Violation;
import org.gaul.modernizer_maven_plugin.ViolationOccurrence;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

public final class ModernizerDefaultPackageTest {
    private Map<String, Violation> violations;
    private static final Collection<String> NO_EXCLUSIONS =
            Collections.<String>emptySet();

    @Before
    public void setUp() throws Exception {
        InputStream is = Modernizer.class.getResourceAsStream(
                "/modernizer.xml");
        try {
            violations = Modernizer.parseFromXml(is);
        } finally {
            Utils.closeQuietly(is);
        }
    }

    @Test
    public void testSuccessfullHandlingOfDefaultPackageClass()
            throws Exception {
        ClassReader cr = new ClassReader(ModernizerDefaultPackageTest.class
                .getName());
        Collection<String> ignorePackages = Collections.singleton("org.gau");
        Collection<ViolationOccurrence> occurrences = new Modernizer(
            "1.6", violations, NO_EXCLUSIONS, ignorePackages).check(cr);
        assertThat(occurrences).hasSize(0);
    }
}
