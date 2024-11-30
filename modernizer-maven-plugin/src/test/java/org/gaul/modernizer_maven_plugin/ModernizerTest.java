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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.UnsignedInts;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.Atomics;
import com.google.inject.Provider;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gaul.modernizer_maven_plugin
    .SuppressModernizerTestClasses.SuppressedOnClass;
import org.gaul.modernizer_maven_plugin
    .SuppressModernizerTestClasses.SuppressedOnMembers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.springframework.beans.factory.annotation.Autowired;

public final class ModernizerTest {
    private Map<String, Violation> violations;
    private static final Collection<String> NO_EXCLUSIONS =
            Collections.<String>emptySet();
    private static final Collection<Pattern> NO_EXCLUSION_PATTERNS =
            Collections.<Pattern>emptySet();
    private static final Collection<String> NO_IGNORED_PACKAGES =
            Collections.<String>emptySet();
    private static final Set<String> NO_IGNORED_CLASS_NAMES =
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
    public void readsOldJavaVersionFormat() throws Exception {
        InputStream is = Modernizer.class.getResourceAsStream(
            "/modernizer-old-versions.xml");
        try {
            Map<String, Violation> old = Modernizer.parseFromXml(is);
            assertThat(old).hasSize(1);
        } finally {
            Utils.closeQuietly(is);
        }
    }

    @Test
    public void testConstructorCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(ArrayListTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.2").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testStackConstructorLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(StackTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.0").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testStackConstructorLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(StackTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.6").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/util/Stack.\"<init>\":()V");
    }

    @Test
    public void testVectorConstructorLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(VectorTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.0").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testVectorConstructorLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(VectorTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.2").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/util/Vector.\"<init>\":()V");
    }

    @Test
    public void testFieldCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(
                StandardCharsetsTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.0").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testFieldLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(CharsetsTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.0").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testFieldLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(CharsetsTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.7").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("com/google/common/base/Charsets.UTF_8" +
                        ":Ljava/nio/charset/Charset;");
    }

    @Test
    public void testInterfaceLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(VoidSupplier.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.7").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testInterfaceLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(VoidSupplier.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.8").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("com/google/common/base/Supplier");
    }

    @Test
    public void testMethodCurrentApi() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesCharset.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.0").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiLegacyJava() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.0").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJava() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.6").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/lang/String.getBytes:(Ljava/lang/String;)[B");
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithExclusion() throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<String> exclusions = Collections.singleton(
                "java/lang/String.getBytes:(Ljava/lang/String;)[B");
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, exclusions, NO_EXCLUSION_PATTERNS,
                NO_IGNORED_PACKAGES, NO_IGNORED_CLASS_NAMES,
                NO_EXCLUSION_PATTERNS
        ).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithExclusionPattern()
            throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<Pattern> exclusionPatterns = Collections.singleton(
                Pattern.compile("java/lang/.*"));
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, NO_EXCLUSIONS, exclusionPatterns,
                NO_IGNORED_PACKAGES, NO_IGNORED_CLASS_NAMES,
                NO_EXCLUSION_PATTERNS
        ).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithIgnorePackages()
            throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<String> ignorePackages = Collections.singleton(
                StringGetBytesString.class.getPackage().getName());
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS
        ).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithIgnorePackagesPrefix()
            throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<String> ignorePackages = Collections.singleton("org.gaul");
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS
        ).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithIgnorePackagesSubprefix()
            throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<String> ignorePackages = Collections.singleton("org");
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS
        ).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithIgnorePackagesPartialPrefix()
            throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<String> ignorePackages = Collections.singleton("org.gau");
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS
        ).check(cr);
        assertThat(occurrences).hasSize(1);
    }

    @Test
    public void testMethodLegacyApiCurrentJavaWithIgnoreClassNamePatterns()
            throws Exception {
        ClassReader cr = new ClassReader(StringGetBytesString.class.getName());
        Collection<Pattern> ignoreClassNamePatterns = Collections.singleton(
                Pattern.compile(".*StringGetBytesString"));
        Collection<ViolationOccurrence> occurrences = new Modernizer(
                "1.6", violations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                NO_IGNORED_PACKAGES, NO_IGNORED_CLASS_NAMES,
                ignoreClassNamePatterns
        ).check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testHandlingOfDefaultPackageClass() throws Exception {
        ClassReader cr = new ClassReader(
                Class.forName("DefaultPackageClass").getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("1.6").check(cr);
        assertThat(occurrences).hasSize(0);
    }

    @Test
    public void testStackConstructorLegacyApiCurrentJavaWithVersionShorthand()
            throws Exception {
        ClassReader cr = new ClassReader(StackTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("6").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/util/Stack.\"<init>\":()V");
    }

    @Test
    public void testVectorConstructorLegacyApiCurrentJavaWithVersionShorthand()
            throws Exception {
        ClassReader cr = new ClassReader(VectorTestClass.class.getName());
        Collection<ViolationOccurrence> occurrences =
                createModernizer("2").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getName())
                .isEqualTo("java/util/Vector.\"<init>\":()V");
    }

    @Test
    public void testAnnotationViolation() throws Exception {
        String name = TestAnnotation.class.getName().replace('.', '/');
        Map<String, Violation> testViolations = Maps.newHashMap();
        testViolations.put(name,
                new Violation(name, 5, ""));
        Modernizer modernizer = new Modernizer("1.5", testViolations,
                NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS, NO_IGNORED_PACKAGES,
                NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS);
        ClassReader cr = new ClassReader(AnnotatedMethod.class.getName());
        Collection<ViolationOccurrence> occurences =
                modernizer.check(cr);
        assertThat(occurences).hasSize(1);
        assertThat(occurences.iterator().next().getViolation().getName())
                .isEqualTo(name);
    }

    @Test
    public void testAllViolations() throws Exception {
        Modernizer modernizer = createModernizer("24");
        Collection<ViolationOccurrence> occurrences = modernizer.check(
                new ClassReader(AllViolations.class.getName()));
        occurrences.addAll(modernizer.check(
                new ClassReader(Java8Violations.class.getName())));
        occurrences.addAll(modernizer.check(
            new ClassReader(Java9Violations.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(Java10Violations.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(Java11Violations.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(Java12Violations.class.getName())));
        occurrences.addAll(modernizer.check(
            new ClassReader(Java17Violations.class.getName())));
        occurrences.addAll(modernizer.check(
            new ClassReader(Java18Violations.class.getName())));
        occurrences.addAll(modernizer.check(
            new ClassReader(Java19Violations.class.getName())));
        occurrences.addAll(modernizer.check(
            new ClassReader(Java24Violations.class.getName())));
        // must visit inner classes manually
        occurrences.addAll(modernizer.check(
                new ClassReader(EnumerationTestClass.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(VoidFunction.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(VoidPredicate.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(VoidSupplier.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(AutowiredMethod.class.getName())));
        occurrences.addAll(modernizer.check(
                new ClassReader(ObjectProvider.class.getName())));

        Collection<Violation> actualViolations = Lists.newArrayList();
        for (ViolationOccurrence occurrence : occurrences) {
            actualViolations.add(occurrence.getViolation());
        }

        // Not testing these since modern JDK have removed them.
        violations.remove(
                "sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B");
        violations.remove(
                "sun/misc/BASE64Encoder.encode:([B)Ljava/lang/String;");

        assertThat(actualViolations).containsAll(violations.values());
    }

    @Test
    public void testSuppressModernizer() throws Exception {
        Set<String> ignoreClassNames = ImmutableSet.of(
            SuppressedOnClass.class.getName().replace('.', '/'),
            SuppressedOnClass.InnerClass.class.getName().replace('.', '/'),
            SuppressedOnMembers.InnerClass.class.getName().replace('.', '/')
        );
        Modernizer modernizer = new Modernizer(
            "1.10",
            violations,
            NO_EXCLUSIONS,
            NO_EXCLUSION_PATTERNS,
            NO_IGNORED_PACKAGES,
            ignoreClassNames,
            NO_EXCLUSION_PATTERNS
        );

        Set<Class<?>> classes = ImmutableSet.of(
            SuppressedOnClass.class,
            SuppressedOnClass.InnerClass.class,
            SuppressedOnMembers.class,
            SuppressedOnMembers.InnerClass.class
        );

        Collection<ViolationOccurrence> occurrences =
            new ArrayList<ViolationOccurrence>();
        for (Class<?> clazz : classes) {
            occurrences.addAll(
                modernizer.check(new ClassReader(clazz.getName())));
        }

        assertThat(occurrences).isEmpty();
    }

    /** Helper to create Modernizer object with default parameters. */
    private Modernizer createModernizer(String javaVersion) {
        return new Modernizer(javaVersion, violations, NO_EXCLUSIONS,
                NO_EXCLUSION_PATTERNS, NO_IGNORED_PACKAGES,
                NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS);
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

    private static class StackTestClass {
        @SuppressWarnings("JdkObsolete")
        private final Object object = new Stack<Object>();
    }

    private static class VectorTestClass {
        @SuppressWarnings("JdkObsolete")
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

    private static class EnumerationTestClass implements Enumeration<Object> {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            return null;
        }
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

        @Override
        public boolean test(Void input) {
            return true;
        }
    }

    private static class VoidSupplier implements Supplier<Void> {
        @Override
        public Void get() {
            return null;
        }
    }

    private @interface TestAnnotation {
        // Nothing
    }

    private static class AnnotatedMethod {
        @TestAnnotation
        public void annotatedMethod() {
            // Nothing
        }
    }

    private static class AutowiredMethod {
        @Autowired
        AutowiredMethod() {
            // Nothing
        }
    }

    private static class ObjectProvider implements Provider<Object> {
        @Override
        public Object get() {
            return new Object();
        }
    }

    private static class AllViolations {
        @SuppressWarnings(value = {
            "BoxedPrimitiveConstructor",
            "CheckReturnValue",
            "deprecation",
            "JdkObsolete"})
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
            com.google.common.base.Optional.absent();
            com.google.common.base.Optional.of((Object) null);
            com.google.common.base.Optional.fromNullable(null);
            Lists.newArrayList();
            Lists.newArrayListWithCapacity(0);
            Lists.newCopyOnWriteArrayList();
            Lists.newLinkedList();
            Maps.newConcurrentMap();
            Maps.newEnumMap((Class<EnumClass>) null);
            Maps.newEnumMap((Map<EnumClass, Object>) null);
            Maps.newHashMap();
            Maps.newHashMap((Map<Object, Object>) null);
            Maps.newIdentityHashMap();
            Maps.newLinkedHashMap();
            Maps.newLinkedHashMap((Map<Object, Object>) null);
            Maps.newTreeMap();
            Maps.newTreeMap((Comparator<Object>) null);
            Maps.newTreeMap((SortedMap<Object, Object>) null);
            Maps.unmodifiableNavigableMap(new TreeMap<Object, Object>());
            Maps.synchronizedNavigableMap(new TreeMap<Object, Object>());
            Sets.newCopyOnWriteArraySet();
            Sets.newHashSet();
            Sets.newLinkedHashSet();
            Sets.newSetFromMap((Map<Object, Boolean>) null);
            Sets.newTreeSet();
            Sets.newTreeSet((Comparator<Object>) null);
            Sets.unmodifiableNavigableSet(new TreeSet<Object>());
            Sets.synchronizedNavigableSet(new TreeSet<Object>());
            BaseEncoding.base64();
            ByteStreams.copy((InputStream) null, (OutputStream) null);
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
            Ints.checkedCast(0);
            IntMath.checkedAdd(0, 0);
            IntMath.checkedSubtract(0, 0);
            IntMath.checkedMultiply(0, 0);
            LongMath.checkedAdd(0, 0);
            LongMath.checkedSubtract(0, 0);
            LongMath.checkedMultiply(0, 0);
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
            new Stack<Object>();
            new Vector<Object>();
            new Vector<Object>(1);
            new Vector<Object>(0, 0);
            new Vector<Object>((Collection<Object>) null);
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
            Preconditions.checkNotNull(new Object());
            Preconditions.checkNotNull(new Object(), new Object());
            Preconditions.checkElementIndex(0, 0);
            Preconditions.checkPositionIndexes(0, 0, 0);
            DateTime.now();
            DateTime.parse("");
            new DateTime(50L);
            LocalDate.now();
            LocalDate.parse("");
            new LocalDate(50L);
            LocalTime.now();
            LocalTime.parse("");
            new LocalTime(50L);
            LocalDateTime.now();
            LocalDateTime.parse("");
            new LocalDateTime(50L);
            Instant.now();
            Instant.parse("");
            new Instant(50L);
            DateTimeZone.forID("Europe/Oslo");
            Duration.millis(50L);
            new Duration(DateTime.now(), DateTime.now());
            Period.parse("");
            new Period(DateTime.now(), DateTime.now());
            DateTimeFormat.forPattern("");
            object = Collections.EMPTY_LIST;
            object = Collections.EMPTY_MAP;
            object = Collections.EMPTY_SET;
            Streams.stream(Optional.empty());
            Streams.stream(OptionalInt.empty());
            Streams.stream(OptionalLong.empty());
            Streams.stream(OptionalDouble.empty());
            Iterators.forEnumeration(NetworkInterface.getNetworkInterfaces());
            MoreObjects.firstNonNull("", "");
        }
    }

    // TODO: move more methods from AllViolations to here
    private static class Java8Violations {
        private static void method() throws Exception {
            Iterables.getOnlyElement(null);
            Iterables.getOnlyElement(null, null);
            Iterables.frequency(null, null);
            Iterables.<Object>cycle((Iterable<Object>) null);
            Iterables.cycle();
            Iterables.concat(null, null);
            Iterables.concat(null, null, null);
            Iterables.concat(null, null, null, null);
            Iterables.concat((Iterable<Object>[]) null);
            Iterables.concat((Iterable<Iterable<Object>>) null);
            Iterables.filter(null, (Predicate<Object>) null);
            Iterables.filter(null, (Class<Object>) null);
            Iterables.any(null, null);
            Iterables.all(null, null);
            Iterables.find(null, null);
            Iterables.find(null, null, null);
            Iterables.tryFind(null, null);
            Iterables.transform(null, null);
            Iterables.get(null, 0);
            Iterables.get(null, 0, null);
            Iterables.getFirst(null, null);
            Iterables.getLast(null);
            Iterables.getLast(null, null);
            Iterables.skip(null, 0);
            Iterables.limit(null, 0);
            Iterables.isEmpty(null);
            StringUtils.join((Iterable<String>) null, ',');
            StringUtils.join((Iterable<String>) null, null);
            Base64.decodeBase64("");
            Base64.decodeBase64((byte[]) null);
            Base64.encodeBase64String((byte[]) null);
            Base64.encodeBase64((byte[]) null);
        }
    }

    private static class Java9Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            ByteStreams.toByteArray((InputStream) null);
            new Byte((String) null);
            new Double((String) null);
            new Short((String) null);
            new Float((String) null);
            new Integer((String) null);
            new Long((String) null);
        }
    }

    private static class Java10Violations {
        private static void method() throws Exception {
            new PrintStream((File) null, "");
            new PrintStream("", "");
            new PrintWriter((File) null, "");
            new PrintWriter("", "");
            new ByteArrayOutputStream().toString("");
            URLDecoder.decode("", "");
            URLEncoder.encode("", "");
            new Formatter((File) null, "");
            new Formatter((File) null, "", (Locale) null);
            new Formatter((OutputStream) null, "");
            new Formatter((OutputStream) null, "", (Locale) null);
            new Formatter("", "");
            new Formatter("", "", (Locale) null);
            new Scanner((File) null, "");
            new Scanner((InputStream) null, "");
            new Scanner((Path) null, "");
            new Scanner((ReadableByteChannel) null, "");
            Optional.empty().get();
        }
    }

    private static class Java11Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            ByteStreams.nullOutputStream();
            CharStreams.nullWriter();
            Files.toString((File) null, (Charset) null);
            Files.write("", (File) null, (Charset) null);
            new FileReader((File) null);
            new FileReader((String) null);
            new FileWriter((File) null);
            new FileWriter((File) null, true);
            new FileWriter("");
            new FileWriter("", true);
        }
    }

    private static class Java12Violations {
        private static void method() throws Exception {
            ByteStreams.skipFully(null, 0L);
        }
    }

    private static class Java17Violations {
        private static void method() throws Exception {
            Hex.encodeHexString(new byte[]{});
        }
    }

    private static class Java18Violations {
        private static void method() throws Exception {
            Runtime.getRuntime().exec("");
            Runtime.getRuntime().exec("", new String[]{});
            Runtime.getRuntime().exec("", new String[]{}, new File(""));
        }
    }

    private static class Java19Violations {
        private static void method() throws Exception {
            new Locale("");
            new Locale("", "");
            new Locale("", "", "");
            new Thread().getId();
        }
    }

    private static class Java24Violations {
        private static void method() throws Exception {
            new StringReader("");
        }
    }

    private enum EnumClass {
    }
}
