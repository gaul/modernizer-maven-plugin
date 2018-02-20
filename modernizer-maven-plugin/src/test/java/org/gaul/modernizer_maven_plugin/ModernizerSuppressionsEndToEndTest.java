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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreConstructorTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreGenericClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreGenericClassConstructorTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodInGenericClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodReturningArrayClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodReturningArrayPrimitiveTypeClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodReturningDeclaredTypeClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodReturningGenericTypeClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodReturningPrimitiveTypeClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithArrayTypeParametersTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithDeclaredTypeParametersTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithEmptyParametersTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithGenericTypeParametersTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithPrimitiveAndGenericTypeParametersTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithPrimitiveTypeParametersTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreMethodWithVoidParameterTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreOverloadedMethodInGenericClassTest;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreOverloadedMethodTestClass;
import org.gaul.modernizer_maven_plugin.ModernizerSuppressionsEndToEndTestClass
    .IgnoreStaticOuterClassConstructor.IgnoreStaticInnerClassConstructor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

public final class ModernizerSuppressionsEndToEndTest {
    private static Map<String, Violation> violations;
    private static final Collection<String> NO_EXCLUSIONS =
        Collections.<String>emptySet();
    private static final Collection<Pattern> NO_EXCLUSION_PATTERNS =
        Collections.<Pattern>emptySet();
    private static final Collection<String> NO_IGNORED_PACKAGES =
        Collections.<String>emptySet();
    private static final Collection<String> NO_IGNORED_METHODS =
        Collections.<String>emptySet();
    private static List<Pattern> ignoreClasses;
    private static Set<String> ignoreMethods;

    @BeforeClass
    public static void setUp() throws Exception {
        violations = ModernizerTestUtils.readViolations();
        ignoreClasses = new ArrayList<Pattern>();
        ignoreMethods = new HashSet<String>();
        String currentDirectory = System.getProperty("user.dir");
        String ignoreClassesFilePath = currentDirectory +
            "/target/modernizer/test/ignore-annotated-classes.txt";
        String ignoreMethodsFilePath = currentDirectory +
            "/target/modernizer/test/ignore-annotated-methods.txt";
        for (String ignoreClass : readExclusionsFile(ignoreClassesFilePath)) {
            ignoreClasses.add(Pattern.compile(ignoreClass));
        }
        ignoreMethods.addAll(readExclusionsFile(ignoreMethodsFilePath));
    }

    public static Collection<String> readExclusionsFile(String filePath) {
        InputStream is = null;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                is = new FileInputStream(filePath);
            } else {
                is = ModernizerSuppressionsEndToEndTest.class.getClassLoader()
                    .getResourceAsStream(filePath);
            }
            if (is == null) {
                throw new RuntimeException(
                    "Could not find exclusion file: " +
                        filePath);
            }

            return Utils.readAllLines(is);
        } catch (IOException ioe) {
            throw new RuntimeException(
                "Error reading exclusion file: " +
                    filePath, ioe);
        } finally {
            Utils.closeQuietly(is);
        }
    }

    private static Collection<ViolationOccurrence> checkViolationsInMethods(
        Class classToVisit
    ) throws Exception {
        ClassReader cr = new ClassReader(classToVisit.getName());
        Modernizer modernizer = new Modernizer(
            "1.6", violations, NO_EXCLUSIONS,
            NO_EXCLUSION_PATTERNS, NO_IGNORED_PACKAGES,
            NO_EXCLUSION_PATTERNS, ignoreMethods);
        return modernizer.check(cr);
    }

    private static Collection<ViolationOccurrence> checkViolationsInClasses(
        Class classToVisit
    ) throws Exception {
        ClassReader cr = new ClassReader(classToVisit.getName());
        Modernizer modernizer = new Modernizer(
            "1.6", violations, NO_EXCLUSIONS,
            NO_EXCLUSION_PATTERNS, NO_IGNORED_PACKAGES,
            ignoreClasses, NO_IGNORED_METHODS);
        return modernizer.check(cr);
    }

    @Test
    public void checkIgnoreMethodWithEmptyParameters() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithEmptyParametersTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodWithVoidParameter() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithVoidParameterTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodWithPrimitiveTypeParameters()
    throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithPrimitiveTypeParametersTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreOverloadedConstructor() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreConstructorTestClass.class)
        ).hasSize(1);
    }

    @Test
    public void checkIgnoreMethodWithGenericTypeParameters() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithGenericTypeParametersTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodWithDeclaredTypeParameter() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithDeclaredTypeParametersTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodWithPrimitiveAndGenericParameters()
    throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithPrimitiveAndGenericTypeParametersTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreOverloadedMethod() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreOverloadedMethodTestClass.class)
        ).hasSize(1);
    }

    @Test
    public void checkIgnoreMethodWithArrayTypeParameters() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodWithArrayTypeParametersTestClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreGenericClassConstructor() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreGenericClassConstructorTestClass.class)
        ).hasSize(1);
    }

    @Test
    public void checkIgnoreMethodInGenericClass() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodInGenericClassTest.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreOverloadedMethodInGenericClass() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreOverloadedMethodInGenericClassTest.class)
        ).hasSize(1);
    }

    @Test
    public void checkIgnoreGenericClass() throws Exception {
        assertThat(checkViolationsInClasses(
            IgnoreGenericClass.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodReturningArrayOfDeclaredType()
    throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodReturningArrayClassTest.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodReturningArrayOfPrimitiveType()
    throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodReturningArrayPrimitiveTypeClassTest.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodReturningPrimitiveType() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodReturningPrimitiveTypeClassTest.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodReturningDeclaredType() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodReturningDeclaredTypeClassTest.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreMethodReturningGenericType() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreMethodReturningGenericTypeClassTest.class)
        ).hasSize(0);
    }

    @Test
    public void checkIgnoreStaticInnerClassConstructor() throws Exception {
        assertThat(checkViolationsInMethods(
            IgnoreStaticInnerClassConstructor.class)
        ).hasSize(0);
    }
}
