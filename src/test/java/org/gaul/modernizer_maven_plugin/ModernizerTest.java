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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.Vector;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.UnsignedInts;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.Atomics;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

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
        ClassReader cr = new ClassReader(ArrayListTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.2", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testConstructorLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(VectorTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testConstructorLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(VectorTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.2", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/util/Vector.\"<init>\":()V");
    }

    @Test
    public void testFieldCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(
                StandardCharsetsTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testFieldLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(CharsetsTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testFieldLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(CharsetsTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.7", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("com/google/common/base/Charsets.UTF_8" +
                        ":Ljava/nio/charset/Charset;");
    }

    @Test
    public void testInterfaceLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(VoidSupplier.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.7", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testInterfaceLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(VoidSupplier.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.8", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("com/google/common/base/Supplier");
    }

    @Test
    public void testMethodCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesCharset.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.0", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.6", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/lang/String.getBytes:(Ljava/lang/String;)[B");
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithExclusion() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        exclusions = Collections.singleton(
                "java/lang/String.getBytes:(Ljava/lang/String;)[B");
        Collection<ViolationOccurrence> occurrences =
                new Modernizer("1.6", violations, exclusions).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testAllViolations() throws Exception {
        Modernizer modernizer = new Modernizer("1.8", violations, exclusions);
        Collection<ViolationOccurrence> occurrences = modernizer.check(
                new ClassReader(AllViolations.class.getName()));
        // must visit inner classes manually
        occurrences.addAll(modernizer.check(
                new ClassReader(VoidFunction.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(VoidPredicate.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(VoidSupplier.class.getName())));

        Collection<Violation> actualViolations = Lists.newArrayList();
        for (ViolationOccurrence occurrence : occurrences) {
            actualViolations.add(occurrence.getViolation());
        }
        assertThat(actualViolations).containsAll(violations.values());
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

    private static class VoidFunction implements Function<Void, Void> {
        @Override
        public Void apply(Void input) {
            return null;
        }
    }

    private static class VoidPredicate implements Predicate<Void> {
        @Override
        public boolean apply(Void input) {
            return true;
        }
    };

    private static class VoidSupplier implements Supplier<Void> {
        @Override
        public Void get() {
            return null;
        }
    }

    private static class AllViolations {
        private static void method() throws Exception {
            Object object;
            object = Charsets.ISO_8859_1;
            object = Charsets.US_ASCII;
            object = Charsets.UTF_8;
            object = Charsets.UTF_16BE;
            object = Charsets.UTF_16LE;
            object = Charsets.UTF_16;
            Objects.equal(null, null);
            Objects.hashCode(null, null);
            Optional.absent();
            Optional.of((Object) null);
            Optional.fromNullable(null);
            Lists.newArrayList();
            Lists.newArrayListWithCapacity(0);
            Lists.newCopyOnWriteArrayList();
            Lists.newLinkedList();
            Maps.newConcurrentMap();
            Maps.newEnumMap((Class<EnumClass>) null);
            Maps.newEnumMap((Map<EnumClass, Object>) null);
            Maps.newHashMap();
            Maps.newHashMap((Map<Object, Object>) null);
            Maps.newHashMapWithExpectedSize(0);
            Maps.newIdentityHashMap();
            Maps.newLinkedHashMap();
            Maps.newLinkedHashMap((Map<Object, Object>) null);
            Maps.newTreeMap();
            Maps.newTreeMap((Comparator<Object>) null);
            Maps.newTreeMap((SortedMap<Object, Object>) null);
            Sets.newCopyOnWriteArraySet();
            Sets.newHashSet();
            Sets.newHashSetWithExpectedSize(0);
            Sets.newLinkedHashSet();
            Sets.newLinkedHashSetWithExpectedSize(0);
            Sets.newSetFromMap((Map<Object, Boolean>) null);
            Sets.newTreeSet();
            Sets.newTreeSet((Comparator<Object>) null);
            BaseEncoding.base64();
            Files.toByteArray((File) null);
            Chars.compare((char) 0, (char) 0);
            Ints.compare(0, 0);
            Longs.compare(0L, 0L);
            Shorts.compare((short) 0, (short) 0);
            UnsignedInts.compare(0, 0);
            UnsignedInts.divide(0, 0);
            UnsignedInts.parseUnsignedInt("0");
            UnsignedInts.parseUnsignedInt("0", 10);
            UnsignedInts.remainder(0, 0);
            UnsignedLongs.compare(0, 0);
            UnsignedLongs.divide(0, 0);
            UnsignedLongs.parseUnsignedLong("0");
            UnsignedLongs.parseUnsignedLong("0", 10);
            UnsignedLongs.remainder(0, 0);
            Atomics.newReference();
            Atomics.newReference((Object) null);
            Atomics.newReferenceArray(0);
            Atomics.newReferenceArray((Object[]) null);
            new InputStreamReader((InputStream) null, "");
            new OutputStreamWriter((OutputStream) null, "");
            new Byte((byte) 0);
            new Character((char) 0);
            new Double(0.0);
            new Float(0.0);
            new Float(0.0F);
            new Integer(0);
            new Long(0L);
            new Short((short) 0);
            "".getBytes("UTF-8");
            new String((byte[]) null, 0, 0, "");
            new String((byte[]) null, "");
            new StringBuffer();
            new StringBuffer(0);
            new StringBuffer("");
            new StringBuffer((CharSequence) "");
            ((HttpURLConnection) null).setFixedLengthStreamingMode(0);
            new Hashtable<Object, Object>(0, 0.0F);
            new Hashtable<Object, Object>(0);
            new Hashtable<Object, Object>();
            new Hashtable<Object, Object>((Map<Object, Object>) null);
            new Vector<Object>();
            new Vector<Object>(1);
            new Vector<Object>(0, 0);
            new Vector<Object>((Collection<Object>) null);
            Base64.decodeBase64("");
            Base64.encodeBase64String((byte[]) null);
            object = org.apache.commons.io.Charsets.ISO_8859_1;
            object = org.apache.commons.io.Charsets.US_ASCII;
            object = org.apache.commons.io.Charsets.UTF_8;
            object = org.apache.commons.io.Charsets.UTF_16BE;
            object = org.apache.commons.io.Charsets.UTF_16LE;
            object = org.apache.commons.io.Charsets.UTF_16;
            FileUtils.readFileToByteArray((File) null);
            FileUtils.readLines((File) null);
            FileUtils.readLines((File) null, (Charset) null);
            FileUtils.readLines((File) null, "");
            new BASE64Decoder().decodeBuffer("");
            new BASE64Encoder().encode((byte[]) null);
        }
    }

    private enum EnumClass {
    }
}
