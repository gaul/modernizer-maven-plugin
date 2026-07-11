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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.xml.bind.DatatypeConverter;

import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.Atomics;
import com.google.inject.Provider;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.input.NullReader;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.gaul.modernizer_maven_plugin.SuppressModernizerTestClasses.SuppressedOnClass;
import org.gaul.modernizer_maven_plugin.SuppressModernizerTestClasses.SuppressedOnMembers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("deprecation")
public final class ModernizerTest {
    private Map<String, Collection<Violation>> violations;
    private static final Collection<String> NO_EXCLUSIONS =
            Collections.<String>emptySet();
    private static final Collection<Pattern> NO_EXCLUSION_PATTERNS =
            Collections.<Pattern>emptySet();
    private static final Collection<String> NO_IGNORED_PACKAGES =
            Collections.<String>emptySet();
    private static final Set<String> NO_IGNORED_CLASS_NAMES =
        Collections.<String>emptySet();

    @BeforeEach
    public void setUp() throws Exception {
        try (InputStream is = Modernizer.class.getResourceAsStream(
                "/modernizer.xml")) {
            violations = Modernizer.parseFromXml(is);
        }
    }

    @Test
    public void readsOldJavaVersionFormat() throws Exception {
        try (InputStream is = Modernizer.class.getResourceAsStream(
                "/modernizer-old-versions.xml")) {
            Map<String, Collection<Violation>> old = Modernizer.parseFromXml(is);
            assertThat(old).hasSize(1);
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
                NO_EXCLUSION_PATTERNS, true
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
                NO_EXCLUSION_PATTERNS, true
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
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS,
                true
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
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS,
                true
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
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS,
                true
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
                ignorePackages, NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS,
                true
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
                ignoreClassNamePatterns, true
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
        Map<String, Collection<Violation>> testViolations = new HashMap<>();
        testViolations.put(name,
                Collections.singleton(new Violation(name, 5, OptionalInt.empty(), "")));
        Modernizer modernizer = new Modernizer("1.5", testViolations,
                NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS, NO_IGNORED_PACKAGES,
                NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS, true);
        ClassReader cr = new ClassReader(AnnotatedMethod.class.getName());
        Collection<ViolationOccurrence> occurences =
                modernizer.check(cr);
        assertThat(occurences).hasSize(1);
        assertThat(occurences.iterator().next().getViolation().getName())
                .isEqualTo(name);
    }

    @Test
    public void testClassAnnotationViolation() throws Exception {
        String name = TestAnnotation.class.getName().replace('.', '/');
        Map<String, Collection<Violation>> testViolations = new HashMap<>();
        testViolations.put(name,
                Collections.singleton(new Violation(name, 5, OptionalInt.empty(), "")));
        Modernizer modernizer = new Modernizer("1.5", testViolations,
                NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS, NO_IGNORED_PACKAGES,
                NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS, true);
        ClassReader cr = new ClassReader(AnnotatedClass.class.getName());
        Collection<ViolationOccurrence> occurences =
                modernizer.check(cr);
        assertThat(occurences).hasSize(1);
        assertThat(occurences.iterator().next().getViolation().getName())
                .isEqualTo(name);
    }

    @Test
    public void testIgnoreGeneratedClassesOnMembers() throws Exception {
        String name = TestAnnotation.class.getName().replace('.', '/');
        Map<String, Collection<Violation>> testViolations = new HashMap<>();
        testViolations.put(name, Collections.singleton(
                new Violation(name, 5, OptionalInt.empty(), "")));
        String className = GeneratedMembers.class.getName();

        // @Generated methods and constructors are suppressed, but the plain
        // method still reports a violation.
        Collection<ViolationOccurrence> ignored = new Modernizer(
                "1.5", testViolations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                NO_IGNORED_PACKAGES, NO_IGNORED_CLASS_NAMES,
                NO_EXCLUSION_PATTERNS, /* ignoreGeneratedClasses */ true
        ).check(new ClassReader(className));
        assertThat(ignored).hasSize(1);

        // Without the option every annotated member reports a violation.
        Collection<ViolationOccurrence> notIgnored = new Modernizer(
                "1.5", testViolations, NO_EXCLUSIONS, NO_EXCLUSION_PATTERNS,
                NO_IGNORED_PACKAGES, NO_IGNORED_CLASS_NAMES,
                NO_EXCLUSION_PATTERNS, /* ignoreGeneratedClasses */ false
        ).check(new ClassReader(className));
        assertThat(notIgnored).hasSize(3);
    }

    @Test
    public void testUntil() throws Exception {
        ClassReader cr = new ClassReader(UntilTest.class.getName());
        Collection<ViolationOccurrence> occurrences = createModernizer("10").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getComment())
                .isEqualTo("Prefer java.nio.file.Files.newInputStream(Paths.get(String))");

        occurrences = createModernizer("11").check(cr);
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.iterator().next().getViolation().getComment())
                .isEqualTo("Prefer java.nio.file.Files.newInputStream(Path.of(String))");
    }

    @Test
    public void testAllViolations() throws Exception {
        int maxVersion = 26;
        Modernizer modernizer = createModernizer(String.valueOf(maxVersion));
        List<Class<?>> fixtures = List.of(
                Java2Violations.class,
                Java4Violations.class,
                Java5Violations.class,
                Java6Violations.class,
                Java7Violations.class,
                Java8Violations.class,
                Java9Violations.class,
                Java10Violations.class,
                Java11Violations.class,
                Java12Violations.class,
                Java15Violations.class,
                Java16Violations.class,
                Java17Violations.class,
                Java18Violations.class,
                Java19Violations.class,
                Java20Violations.class,
                Java21Violations.class,
                Java22Violations.class,
                Java23Violations.class,
                Java24Violations.class,
                Java25Violations.class,
                Java26Violations.class,
                // inner classes must be visited manually
                DictionaryTestClass.class,
                EnumerationTestClass.class,
                VoidFunction.class,
                VoidPredicate.class,
                VoidSupplier.class,
                VoidCollectionsPredicate.class,
                VoidTransformer.class,
                VoidFactory.class,
                VoidClosure.class,
                AutoValueClass.class,
                AutowiredMethod.class,
                ObjectProvider.class);
        Collection<ViolationOccurrence> occurrences = new ArrayList<>();
        for (Class<?> fixture : fixtures) {
            occurrences.addAll(modernizer.check(
                    new ClassReader(fixture.getName())));
        }

        Collection<Violation> actualViolations = new ArrayList<>();
        for (ViolationOccurrence occurrence : occurrences) {
            actualViolations.add(occurrence.getViolation());
        }

        // Not testing these since modern JDK have removed them.
        violations.remove(
                "sun/misc/BASE64Decoder.decodeBuffer:(Ljava/lang/String;)[B");
        violations.remove(
                "sun/misc/BASE64Encoder.encode:([B)Ljava/lang/String;");

        Collection<Violation> expectedViolations = violations.values().stream()
                .flatMap(Collection::stream)
                .filter(violation -> !violation.getUntil().isPresent() ||
                        violation.getUntil().getAsInt() > maxVersion)
                .collect(Collectors.toList());

        assertThat(actualViolations).containsAll(expectedViolations);
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
            NO_EXCLUSION_PATTERNS,
            true
        );

        Set<Class<?>> classes = ImmutableSet.of(
            SuppressedOnClass.class,
            SuppressedOnClass.InnerClass.class,
            SuppressedOnMembers.class,
            SuppressedOnMembers.InnerClass.class
        );

        Collection<ViolationOccurrence> occurrences = new ArrayList<>();
        for (Class<?> clazz : classes) {
            occurrences.addAll(
                modernizer.check(new ClassReader(clazz.getName())));
        }

        assertThat(occurrences).isEmpty();
    }

    @Test
    public void testSuppressModernizerFromAnyPackage() throws Exception {
        Set<String> ignoreClassNames = ImmutableSet.of(
            SuppressModernizerSimpleNameTestClasses.SuppressedOnClass.class
                .getName().replace('.', '/')
        );
        Modernizer modernizer = new Modernizer(
            "1.10",
            violations,
            NO_EXCLUSIONS,
            NO_EXCLUSION_PATTERNS,
            NO_IGNORED_PACKAGES,
            ignoreClassNames,
            NO_EXCLUSION_PATTERNS,
            true
        );

        Set<Class<?>> classes = ImmutableSet.of(
            SuppressModernizerSimpleNameTestClasses.SuppressedOnClass.class,
            SuppressModernizerSimpleNameTestClasses.SuppressedOnMembers.class
        );

        Collection<ViolationOccurrence> occurrences = new ArrayList<>();
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
                NO_IGNORED_CLASS_NAMES, NO_EXCLUSION_PATTERNS, true);
    }

    @SuppressModernizer
    private static class CharsetsTestClass {
        private final Object object = Charsets.UTF_8;
    }

    private static class StandardCharsetsTestClass {
        private final Object object = StandardCharsets.UTF_8;
    }

    private static class ArrayListTestClass {
        private final Object object = new ArrayList<Object>();
    }

    @SuppressModernizer
    private static class StackTestClass {
        @SuppressWarnings("JdkObsolete")
        private final Object object = new Stack<Object>();
    }

    @SuppressModernizer
    private static class VectorTestClass {
        @SuppressWarnings("JdkObsolete")
        private final Object object = new Vector<Object>();
    }

    @SuppressModernizer
    private static class StringGetBytesString {
        private static void method() throws Exception {
            "".getBytes("UTF-8");
        }
    }

    private static class StringGetBytesCharset {
        private final Object object = "".getBytes(StandardCharsets.UTF_8);
    }

    @SuppressModernizer
    private static class UntilTest {
        public static void method() throws Exception {
            new FileInputStream("");
        }
    }

    @SuppressModernizer
    @SuppressWarnings("JdkObsolete")
    private static class DictionaryTestClass extends Dictionary<Object, Object> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Enumeration<Object> keys() {
            return null;
        }

        @Override
        public Enumeration<Object> elements() {
            return null;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }
    }

    @SuppressModernizer
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

    @SuppressModernizer
    private static class VoidFunction implements Function<Void, Void> {
        @Override
        public Void apply(Void input) {
            return null;
        }
    }

    @SuppressModernizer
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

    @SuppressModernizer
    private static class VoidSupplier implements Supplier<Void> {
        @Override
        public Void get() {
            return null;
        }
    }

    @SuppressModernizer
    private static class VoidCollectionsPredicate
            implements org.apache.commons.collections4.Predicate<Void> {
        @Override
        public boolean evaluate(Void input) {
            return true;
        }
    }

    @SuppressModernizer
    private static class VoidTransformer
            implements org.apache.commons.collections4.Transformer<Void, Void> {
        @Override
        public Void transform(Void input) {
            return null;
        }
    }

    @SuppressModernizer
    private static class VoidFactory
            implements org.apache.commons.collections4.Factory<Void> {
        @Override
        public Void create() {
            return null;
        }
    }

    @SuppressModernizer
    private static class VoidClosure
            implements org.apache.commons.collections4.Closure<Void> {
        @Override
        public void execute(Void input) {
            // Nothing
        }
    }

    @SuppressModernizer
    @AutoValue
    abstract static class AutoValueClass {
        abstract String value();
    }

    private @interface TestAnnotation {
        // Nothing
    }

    @SuppressModernizer
    private static class AnnotatedMethod {
        @TestAnnotation
        public void annotatedMethod() {
            // Nothing
        }
    }

    @SuppressModernizer
    @TestAnnotation
    private static class AnnotatedClass {
        // Nothing
    }

    // Marker annotation whose simple name matches the @Generated detection
    // used by ignoreGeneratedClasses, mimicking Lombok's @lombok.Generated.
    private @interface Generated {
        // Nothing
    }

    @SuppressModernizer
    private static class GeneratedMembers {
        @Generated
        @TestAnnotation
        GeneratedMembers() {
            // Nothing
        }

        @TestAnnotation
        public void plainMethod() {
            // Nothing
        }

        @Generated
        @TestAnnotation
        public void generatedMethod() {
            // Nothing
        }
    }

    @SuppressModernizer
    private static class AutowiredMethod {
        @Autowired
        AutowiredMethod() {
            // Nothing
        }
    }

    @SuppressModernizer
    private static class ObjectProvider implements Provider<Object> {
        @Override
        public Object get() {
            return new Object();
        }
    }

    @SuppressModernizer
    private static class Java2Violations {
        @SuppressWarnings({"deprecation", "JdkObsolete"})
        private static void method() throws Exception {
            new Hashtable<Object, Object>(0, 0.0F);
            new Hashtable<Object, Object>(0);
            new Hashtable<Object, Object>();
            new Hashtable<Object, Object>((Map<Object, Object>) null);
            new Vector<Object>();
            new Vector<Object>(1);
            new Vector<Object>(0, 0);
            new Vector<Object>((Collection<Object>) null);
            new Properties().save((OutputStream) null, "");
        }
    }

    @SuppressModernizer
    private static class Java4Violations {
        @SuppressWarnings({"BoxedPrimitiveConstructor", "deprecation"})
        private static void method() throws Exception {
            new InputStreamReader((InputStream) null, "");
            new OutputStreamWriter((OutputStream) null, "");
            new Boolean(true);
            URLDecoder.decode("");
            URLEncoder.encode("");
            new URL("");
            new URL("", "", "");
            new URL("", "", 0, "");
            new URL((URL) null, "");
            Doubles.compare(0.0, 0.0);
            Floats.compare(0.0F, 0.0F);
            org.apache.commons.lang.math.NumberUtils.compare(0.0, 0.0);
            org.apache.commons.lang.math.NumberUtils.compare(0.0F, 0.0F);
            new StringTokenizer("");
            new StringTokenizer("", "");
            new StringTokenizer("", "", true);
        }
    }

    @SuppressModernizer
    private static class Java5Violations {
        @SuppressWarnings({"BoxedPrimitiveConstructor", "JdkObsolete"})
        private static void method() throws Exception {
            Object object;
            new Byte((byte) 0);
            new Character((char) 0);
            new Double(0.0);
            new Float(0.0);
            new Float(0.0F);
            new Integer(0);
            new Long(0L);
            new Short((short) 0);
            new StringBuffer();
            new StringBuffer(0);
            new StringBuffer("");
            new StringBuffer((CharSequence) "");
            object = Collections.EMPTY_LIST;
            object = Collections.EMPTY_MAP;
            object = Collections.EMPTY_SET;
        }
    }

    @SuppressModernizer
    private static class Java6Violations {
        @SuppressWarnings({"CheckReturnValue", "JdkObsolete"})
        private static void method() throws Exception {
            "".getBytes("UTF-8");
            new String((byte[]) null, 0, 0, "");
            new String((byte[]) null, "");
            new Stack<Object>();
            Sets.newSetFromMap((Map<Object, Boolean>) null);
        }
    }

    @SuppressModernizer
    private static class Java7Violations {
        @SuppressWarnings({"CheckReturnValue", "deprecation", "JdkObsolete"})
        private static void method() throws Exception {
            Object object;
            object = Logger.global;
            object = Charsets.ISO_8859_1;
            object = Charsets.US_ASCII;
            object = Charsets.UTF_8;
            object = Charsets.UTF_16BE;
            object = Charsets.UTF_16LE;
            object = Charsets.UTF_16;
            Objects.equal(null, null);
            Objects.hashCode(null, null);
            Lists.newArrayList();
            Lists.newArrayList(new Object[0]);
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
            Sets.newCopyOnWriteArraySet();
            Sets.newHashSet();
            Sets.newLinkedHashSet();
            Sets.newTreeSet();
            Sets.newTreeSet((Comparator<Object>) null);
            Queues.newArrayDeque();
            Queues.newArrayBlockingQueue(0);
            Queues.newConcurrentLinkedQueue();
            Queues.newLinkedBlockingDeque();
            Queues.newLinkedBlockingDeque(0);
            Queues.newLinkedBlockingQueue();
            Queues.newLinkedBlockingQueue(0);
            Queues.newPriorityBlockingQueue();
            Queues.newPriorityQueue();
            Queues.newSynchronousQueue();
            Files.toByteArray((File) null);
            Files.write(new byte[0], (File) null);
            Files.newReader((File) null, (Charset) null);
            Files.newWriter((File) null, (Charset) null);
            Files.copy((File) null, (File) null);
            Files.createParentDirs((File) null);
            Files.createTempDir();
            Files.move((File) null, (File) null);
            Files.readLines((File) null, (Charset) null);
            File.createTempFile("", "");
            File.createTempFile("", "", (File) null);
            Chars.compare((char) 0, (char) 0);
            Ints.compare(0, 0);
            Longs.compare(0L, 0L);
            Shorts.compare((short) 0, (short) 0);
            Booleans.compare(false, false);
            SignedBytes.compare((byte) 0, (byte) 0);
            NumberUtils.compare((byte) 0, (byte) 0);
            NumberUtils.compare(0, 0);
            NumberUtils.compare(0L, 0L);
            NumberUtils.compare((short) 0, (short) 0);
            CharUtils.compare((char) 0, (char) 0);
            Atomics.newReference();
            Atomics.newReference((Object) null);
            Atomics.newReferenceArray(0);
            Atomics.newReferenceArray((Object[]) null);
            ((HttpURLConnection) null).setFixedLengthStreamingMode(0);
            ((URLConnection) null).getContentLength();
            ((HttpURLConnection) null).getContentLength();
            object = org.apache.commons.io.Charsets.ISO_8859_1;
            object = org.apache.commons.io.Charsets.US_ASCII;
            object = org.apache.commons.io.Charsets.UTF_8;
            object = org.apache.commons.io.Charsets.UTF_16BE;
            object = org.apache.commons.io.Charsets.UTF_16LE;
            object = org.apache.commons.io.Charsets.UTF_16;
            object = IOUtils.LINE_SEPARATOR;
            object = SystemUtils.LINE_SEPARATOR;
            object = org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
            object = org.apache.commons.codec.Charsets.ISO_8859_1;
            object = org.apache.commons.codec.Charsets.US_ASCII;
            object = org.apache.commons.codec.Charsets.UTF_8;
            object = org.apache.commons.codec.Charsets.UTF_16BE;
            object = org.apache.commons.codec.Charsets.UTF_16LE;
            object = org.apache.commons.codec.Charsets.UTF_16;
            FileUtils.readFileToByteArray((File) null);
            FileUtils.writeByteArrayToFile((File) null, new byte[0]);
            FileUtils.writeByteArrayToFile((File) null, new byte[0], true);
            FileUtils.copyFile((File) null, (File) null);
            FileUtils.readLines((File) null);
            FileUtils.readLines((File) null, (Charset) null);
            FileUtils.readLines((File) null, "");
            FileUtils.writeLines((File) null, (Collection<?>) null);
            FileUtils.writeLines((File) null, (Collection<?>) null, true);
            FileUtils.writeLines((File) null, "", (Collection<?>) null);
            FileUtils.writeLines((File) null, "", (Collection<?>) null, true);
            FileUtils.openInputStream((File) null);
            FileUtils.openOutputStream((File) null);
            FileUtils.openOutputStream((File) null, true);
            FileUtils.forceMkdir((File) null);
            FileUtils.moveFile((File) null, (File) null);
            FileUtils.moveFile((File) null, (File) null, (CopyOption[]) null);
            Preconditions.checkNotNull(new Object());
            Validate.notNull(new Object());
            Validate.notNull(new Object(), "", new Object[0]);
            ObjectUtils.toString(new Object(), "");
            org.apache.commons.lang.ObjectUtils.toString(new Object(), "");
            org.apache.commons.lang.Validate.notNull(new Object());
            org.apache.commons.lang.Validate.notNull(new Object(), "");
            IteratorUtils.emptyIterator();
        }
    }

    @SuppressModernizer
    private static class Java8Violations {
        @SuppressWarnings({"CheckReturnValue", "deprecation", "JdkObsolete"})
        private static void method() throws Exception {
            com.google.common.base.Optional.absent();
            com.google.common.base.Optional.of((Object) null);
            com.google.common.base.Optional.fromNullable(null);
            Maps.unmodifiableNavigableMap(new TreeMap<Object, Object>());
            Maps.synchronizedNavigableMap(new TreeMap<Object, Object>());
            Sets.unmodifiableNavigableSet(new TreeSet<Object>());
            Sets.synchronizedNavigableSet(new TreeSet<Object>());
            Sets.newConcurrentHashSet();
            Ordering.natural();
            Ordering.usingToString();
            BaseEncoding.base64();
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
            Booleans.hashCode(false);
            Bytes.hashCode((byte) 0);
            Chars.hashCode((char) 0);
            Doubles.hashCode(0.0);
            Floats.hashCode(0.0F);
            Ints.hashCode(0);
            Longs.hashCode(0L);
            Shorts.hashCode((short) 0);
            Doubles.isFinite(0.0);
            Floats.isFinite(0.0F);
            IntMath.mod(0, 1);
            LongMath.mod(0L, 1L);
            UnsignedBytes.toInt((byte) 0);
            UnsignedInts.toLong(0);
            UnsignedInts.toString(0);
            UnsignedLongs.toString(0L);
            Preconditions.checkNotNull(new Object(), new Object());
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
            Joiner.on(',').join((Iterable<?>) null);
            Joiner.on(',').join((Iterator<?>) null);
            Joiner.on(',').join((Object[]) null);
            Joiner.on(',').join((Object) null, (Object) null, (Object) null);
            StringUtils.join((Iterable<String>) null, ',');
            StringUtils.join((Iterable<String>) null, null);
            StringUtils.join((Iterator<String>) null, ',');
            StringUtils.join((Iterator<String>) null, (String) null);
            StringUtils.join((Object[]) null, ',');
            StringUtils.join((Object[]) null, (String) null);
            org.apache.commons.lang.StringUtils.join((Collection<?>) null, ',');
            org.apache.commons.lang.StringUtils.join((Collection<?>) null, "");
            org.apache.commons.lang.StringUtils.join((Iterator<?>) null, ',');
            org.apache.commons.lang.StringUtils.join((Iterator<?>) null, "");
            org.apache.commons.lang.StringUtils.join((Object[]) null, ',');
            org.apache.commons.lang.StringUtils.join((Object[]) null, "");
            FastDateFormat.getInstance("");
            FastDateFormat.getInstance("", (Locale) null);
            FastDateFormat.getInstance("", (TimeZone) null);
            FastDateFormat.getInstance("", (TimeZone) null, (Locale) null);
            org.apache.commons.lang.time.FastDateFormat.getInstance("");
            org.apache.commons.lang.time.FastDateFormat
                    .getInstance("", (Locale) null);
            org.apache.commons.lang.time.FastDateFormat
                    .getInstance("", (TimeZone) null);
            org.apache.commons.lang.time.FastDateFormat
                    .getInstance("", (TimeZone) null, (Locale) null);
            Base64.decodeBase64("");
            Base64.decodeBase64((byte[]) null);
            Base64.encodeBase64String((byte[]) null);
            Base64.encodeBase64((byte[]) null);
            Base64.encodeBase64URLSafe((byte[]) null);
            Base64.encodeBase64URLSafeString((byte[]) null);
            Base64.encodeBase64Chunked((byte[]) null);
            DatatypeConverter.parseBase64Binary("");
            DatatypeConverter.printBase64Binary(new byte[]{});
            org.bouncycastle.util.encoders.Base64.toBase64String(null);
            org.bouncycastle.util.encoders.Base64.encode(null);
            org.bouncycastle.util.encoders.Base64.decode("");
            org.bouncycastle.util.encoders.Base64.decode((byte[]) null);
            Collections.sort(new ArrayList<String>());
            Collections.sort(new ArrayList<String>(), Comparator.naturalOrder());
            FileUtils.lineIterator((File) null);
            FileUtils.lineIterator((File) null, "");
            Object object = ComparatorUtils.NATURAL_COMPARATOR;
            ComparatorUtils.naturalComparator();
            ComparatorUtils.reversedComparator((Comparator<Object>) null);
            ComparatorUtils.chainedComparator(
                    (Collection<Comparator<Object>>) null);
            ComparatorUtils.chainedComparator((Comparator<Object>[]) null);
            ComparatorUtils.nullHighComparator((Comparator<Object>) null);
            ComparatorUtils.nullLowComparator((Comparator<Object>) null);
            ComparatorUtils.transformedComparator((Comparator<Object>) null,
                    null);
        }
    }

    @SuppressModernizer
    private static class Java9Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            ByteStreams.toByteArray((InputStream) null);
            ByteStreams.copy((InputStream) null, (OutputStream) null);
            IOUtils.copy((InputStream) null, (OutputStream) null);
            IOUtils.copyLarge((InputStream) null, (OutputStream) null);
            IOUtils.copy((Reader) null, (Writer) null);
            IOUtils.copyLarge((Reader) null, (Writer) null);
            IOUtils.toByteArray((InputStream) null);
            IOUtils.toString((InputStream) null, (Charset) null);
            IOUtils.toString((InputStream) null);
            IOUtils.toString((InputStream) null, "");
            Object.class.newInstance();
            Matcher m = Pattern.compile("").matcher("");
            m.appendReplacement(new StringBuffer(), "");
            m.appendTail(new StringBuffer());
            new Boolean((String) null);
            new Byte((String) null);
            new Double((String) null);
            new Short((String) null);
            new Float((String) null);
            new Integer((String) null);
            new Long((String) null);
            Collections.emptyList();
            Collections.emptyMap();
            Collections.emptySet();
            Collections.singletonList(null);
            Collections.singleton(null);
            Collections.singletonMap(null, null);
            Preconditions.checkElementIndex(0, 0);
            Preconditions.checkPositionIndexes(0, 0, 0);
            Streams.stream(Optional.empty());
            Streams.stream(OptionalInt.empty());
            Streams.stream(OptionalLong.empty());
            Streams.stream(OptionalDouble.empty());
            Iterators.forEnumeration(NetworkInterface.getNetworkInterfaces());
            MoreObjects.firstNonNull("", "");
            Maps.immutableEntry(null, null);
            ObjectUtils.firstNonNull("", "");
            ObjectUtils.defaultIfNull("", "");
            org.apache.commons.lang.ObjectUtils.defaultIfNull("", "");
            BigDecimal bd = BigDecimal.ZERO;
            bd.divide(bd, 0);
            bd.divide(bd, 0, 0);
            bd.setScale(0, 0);
        }
    }

    @SuppressModernizer
    private static class Java10Violations {
        private static void method() throws Exception {
            CharStreams.copy((Readable) null, (Appendable) null);
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
            OptionalInt.empty().getAsInt();
            OptionalLong.empty().getAsLong();
            OptionalDouble.empty().getAsDouble();
            new File(new File(""), "");
            new File("");
            new File("", "");
            new File(URI.create("file://name"));
            new FileInputStream("");
            new FileInputStream(new File(""));
            new FileOutputStream("");
            new FileOutputStream("", true);
            new FileOutputStream(new File(""));
            new FileOutputStream(new File(""), true);
            new FileReader((File) null);
            new FileReader((String) null);
            new FileWriter((File) null);
            new FileWriter((File) null, true);
            new FileWriter("");
            new FileWriter("", true);
        }
    }

    @SuppressModernizer
    private static class Java11Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            Object object;
            ByteStreams.nullOutputStream();
            CharStreams.nullWriter();
            object = NullOutputStream.NULL_OUTPUT_STREAM;
            new NullOutputStream();
            object = NullWriter.NULL_WRITER;
            new NullWriter();
            new NullInputStream();
            new NullReader();
            StringUtils.repeat("", 0);
            org.apache.commons.lang.StringUtils.repeat("", 0);
            Files.toString((File) null, (Charset) null);
            Files.write("", (File) null, (Charset) null);
            Files.append("", (File) null, (Charset) null);
            FileUtils.readFileToString((File) null);
            FileUtils.readFileToString((File) null, (Charset) null);
            FileUtils.readFileToString((File) null, (String) null);
            FileUtils.writeStringToFile((File) null, "");
            FileUtils.writeStringToFile((File) null, "", (Charset) null);
            FileUtils.writeStringToFile((File) null, "", (String) null);
            FileUtils.writeStringToFile((File) null, "", (Charset) null, true);
            FileUtils.writeStringToFile((File) null, "", (String) null, true);
            new FileReader((File) null, (Charset) null);
            new FileReader((String) null, (Charset) null);
            new FileWriter((File) null, (Charset) null);
            new FileWriter((File) null, (Charset) null, true);
            new FileWriter("", (Charset) null);
            new FileWriter("", (Charset) null, true);
            Paths.get(URI.create("file://name"));
            Paths.get("", "");
            Strings.repeat("", 0);
        }
    }

    @SuppressModernizer
    private static class Java12Violations {
        private static void method() throws Exception {
            ByteStreams.skipFully(null, 0L);
            IOUtils.skipFully((InputStream) null, 0L);
            FileUtils.contentEquals((File) null, (File) null);
        }
    }

    @SuppressModernizer
    private static class Java15Violations {
        private static void method() throws Exception {
            String.format("%s", "");
        }
    }

    @SuppressModernizer
    private static class Java16Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            Collectors.toUnmodifiableList();
            LogRecord record = new LogRecord(Level.INFO, "");
            record.getThreadID();
            record.setThreadID(0);
        }
    }

    @SuppressModernizer
    private static class Java17Violations {
        private static void method() throws Exception {
            Hex.encodeHexString(new byte[]{});
            Hex.encodeHexString(new byte[]{}, true);
            Hex.encodeHex(new byte[]{});
            Hex.encodeHex(new byte[]{}, true);
            Hex.decodeHex("");
            Hex.decodeHex(new char[]{});
            DatatypeConverter.printHexBinary(new byte[]{});
            DatatypeConverter.parseHexBinary("");
            org.bouncycastle.util.encoders.Hex.toHexString(new byte[]{});
            org.bouncycastle.util.encoders.Hex.toHexString(new byte[]{}, 0, 0);
            org.bouncycastle.util.encoders.Hex.encode(new byte[]{});
            org.bouncycastle.util.encoders.Hex.decode("");
            org.bouncycastle.util.encoders.Hex.decode(new byte[]{});
            org.bouncycastle.util.encoders.Hex.decodeStrict("");
        }
    }

    @SuppressModernizer
    private static class Java18Violations {
        private static void method() throws Exception {
            Runtime.getRuntime().exec("");
            Runtime.getRuntime().exec("", new String[]{});
            Runtime.getRuntime().exec("", new String[]{}, new File(""));
        }
    }

    @SuppressModernizer
    private static class Java19Violations {
        private static void method() throws Exception {
            new Locale("");
            new Locale("", "");
            new Locale("", "", "");
            new Thread().getId();
            Maps.newHashMapWithExpectedSize(0);
            Maps.newLinkedHashMapWithExpectedSize(0);
            Sets.newHashSetWithExpectedSize(0);
            Sets.newLinkedHashSetWithExpectedSize(0);
        }
    }

    @SuppressModernizer
    private static class Java20Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            new URL("", "", 0, "", (URLStreamHandler) null);
            new URL((URL) null, "", (URLStreamHandler) null);
        }
    }

    @SuppressModernizer
    private static class Java21Violations {
        private static void method() throws Exception {
            Lists.reverse(List.of());
            Doubles.constrainToRange(0.0, 0.0, 0.0);
            Floats.constrainToRange(0.0F, 0.0F, 0.0F);
            Ints.constrainToRange(0, 0, 0);
            Longs.constrainToRange(0L, 0L, 0L);
        }
    }

    @SuppressModernizer
    private static class Java22Violations {
        private static void method() throws Exception {
            InetAddresses.forString("127.0.0.1");
        }
    }

    @SuppressModernizer
    private static class Java23Violations {
        @SuppressWarnings("deprecation")
        private static void method() throws Exception {
            Deflater deflater = new Deflater();
            deflater.getTotalIn();
            deflater.getTotalOut();
            Inflater inflater = new Inflater();
            inflater.getTotalIn();
            inflater.getTotalOut();
        }
    }

    @SuppressModernizer
    private static class Java24Violations {
        private static void method() throws Exception {
            new StringReader("");
            new CharSequenceReader("");
        }
    }

    @SuppressModernizer
    private static class Java25Violations {
        private static void method() throws Exception {
            Reader reader = Reader.nullReader();
            IOUtils.toString(reader);
            IOUtils.readLines(reader);
            CharStreams.toString(reader);
            CharStreams.readLines(reader);
        }
    }

    @SuppressModernizer
    private static class Java26Violations {
        private static void method() throws Exception {
            "".equalsIgnoreCase("");
            "".compareToIgnoreCase("");
            Object object = String.CASE_INSENSITIVE_ORDER;
            Comparators.max("", "");
            Comparators.min("", "");
            Comparators.max("", "", Comparator.naturalOrder());
            Comparators.min("", "", Comparator.naturalOrder());
            Ordering.from(Comparator.<String>naturalOrder()).max("", "");
            Ordering.from(Comparator.<String>naturalOrder()).min("", "");
        }
    }

    private enum EnumClass {
    }
}
