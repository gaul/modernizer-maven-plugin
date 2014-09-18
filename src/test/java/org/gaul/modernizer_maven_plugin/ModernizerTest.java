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

package org.gaul.modernizer_maven_plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import com.google.common.base.Charsets;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

public final class ModernizerTest {
    private Map<String, Violation> violations;
    private Collection<String> exclusions;

    @Before
    public void setUp() throws Exception {
        InputStream is = Modernizer.class.getResourceAsStream(
                "/modernizer.xml");
        try {
            violations = Modernizer.parseFromXml(is);
        } finally {
            Utils.closeQuietly(is);
        }
        exclusions = Collections.<String>emptySet();
    }

    @Test
    public void testConstructorCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(
                new ArrayListTestClass().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.2", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testConstructorLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(
                new VectorTestClass().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testConstructorLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(
                new VectorTestClass().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.2", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
    }

    @Test
    public void testFieldCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(
                new StandardCharsetsTestClass().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testFieldLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(
                new CharsetsTestClass().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testFieldLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(
                new CharsetsTestClass().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.7", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
    }

    @Test
    public void testMethodCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(
                new StringGetBytesCharset().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(
                new StringGetBytesString().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(
                new StringGetBytesString().getClass().getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.6", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithExclusion() throws Exception {
        ClassReader cr = new ClassReader(
                new StringGetBytesString().getClass().getName());
        exclusions = Collections.singleton(
                "java/lang/String.getBytes:(Ljava/lang/String;)[B");
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.6", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    private static class CharsetsTestClass {
        private final Object object = Charsets.UTF_8;
    }

    private static class StandardCharsetsTestClass {
        private final Object object = StandardCharsets.UTF_8;
    }

    private static class ArrayListTestClass {
        private final Object object = new ArrayList<Object>();
    }

    private static class VectorTestClass {
        private final Object object = new Vector<Object>();
    }

    private static class StringGetBytesString {
        private static void method() throws Exception {
            "".getBytes("UTF-8");
        }
    }

    private static class StringGetBytesCharset {
        private final Object object = "".getBytes(StandardCharsets.UTF_8);
    }
}
