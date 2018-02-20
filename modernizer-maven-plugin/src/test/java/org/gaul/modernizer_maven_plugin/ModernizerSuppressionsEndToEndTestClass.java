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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModernizerSuppressionsEndToEndTestClass {

    protected static class IgnoreMethodWithEmptyParametersTestClass {
        @SuppressWarnings("modernizer")
        private static void testMethodEmptyParameters() throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodWithVoidParameterTestClass {
        @SuppressWarnings("modernizer")
        private static void testMethodVoidParameter(Void var) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodWithPrimitiveTypeParametersTestClass {
        @SuppressWarnings("modernizer")
        private static void testMethodPrimitiveTypeParameters(
            int intVar,
            boolean boolVar,
            byte byteVar,
            char charVar,
            short shortVar,
            long longVar,
            float floatVar,
            double doubleVar
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreConstructorTestClass {
        @SuppressWarnings("modernizer")
        IgnoreConstructorTestClass() throws Exception {
            "".getBytes("UTF-8");
        }

        IgnoreConstructorTestClass(String string) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodWithGenericTypeParametersTestClass {
        @SuppressWarnings("modernizer")
        private static void testMethodMultipleGenericTypeParameters(
            List<String> stringList,
            Set<Integer> integerSet
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodWithDeclaredTypeParametersTestClass {
        @SuppressWarnings("modernizer")
        private static void testMethodDeclaredTypeParameter(
            IgnoreConstructorTestClass obj
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class
        IgnoreMethodWithPrimitiveAndGenericTypeParametersTestClass {
        @SuppressWarnings("modernizer")
        private static void testMethodPrimitiveAndGenericTypeParameters(
            List<String> list,
            boolean bool
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreOverloadedMethodTestClass {
        @SuppressWarnings("modernizer")
        private static void testOverloadedMethod() throws Exception {
            "".getBytes("UTF-8");
        }

        private static void testOverloadedMethod(
            String string
        ) throws Exception {
            string.getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodWithArrayTypeParametersTestClass {
        @SuppressWarnings("modernizer")
        private static void testArrayParameters(
            int[] array,
            String[] stringArray,
            float[][] arrayOfArrays,
            List<String> [] listOfArrayOfStrings,
            IgnoreOverloadedMethodTestClass[] obj
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreGenericClassConstructorTestClass<E> {
        @SuppressWarnings("modernizer")
        IgnoreGenericClassConstructorTestClass() throws Exception {
            "".getBytes("UTF-8");
        }
        IgnoreGenericClassConstructorTestClass(
            IgnoreOverloadedMethodTestClass ignore
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodInGenericClassTest<E> {
        @SuppressWarnings("modernizer")
        public final void testGenericMethod(E obj) throws Exception {
            "".getBytes("UTF-8");
        }

    }

    protected static class IgnoreOverloadedMethodInGenericClassTest<E> {
        public final void testGenericOverloadedMethod(
            E firstObj,
            E secondObj
        ) throws Exception {
            "".getBytes("UTF-8");
        }

        @SuppressWarnings("modernizer")
        public final void testGenericOverloadedMethod(
            int var,
            E genericVar,
            List<E> list,
            List<E>[] lists
        ) throws Exception {
            "".getBytes("UTF-8");
        }
    }

    @SuppressWarnings("modernizer")
    protected static class IgnoreGenericClass<E> {
        public final void testMethodInGenericClass() throws Exception {
            "".getBytes("UTF-8");
        }
    }

    protected static class IgnoreMethodReturningArrayClassTest {
        @SuppressWarnings("modernizer")
        public final IgnoreOverloadedMethodTestClass[]
            testMethodReturningArray() throws UnsupportedEncodingException {
            IgnoreOverloadedMethodTestClass[] ex =
                new IgnoreOverloadedMethodTestClass[1];
            "".getBytes("UTF-8");
            return ex;
        }
    }

    protected static class IgnoreMethodReturningArrayPrimitiveTypeClassTest {
        @SuppressWarnings("modernizer")
        public final int[] testMethodReturningArrayPrimitveType()
            throws UnsupportedEncodingException {
            int[] intArray = new int[1];
            "".getBytes("UTF-8");
            return intArray;
        }
    }

    protected static class IgnoreMethodReturningPrimitiveTypeClassTest {
        @SuppressWarnings("modernizer")
        public final char testMethodReturningPrimitiveType()
            throws UnsupportedEncodingException {
            "".getBytes("UTF-8");
            return ' ';
        }
    }

    protected static class IgnoreMethodReturningDeclaredTypeClassTest {
        @SuppressWarnings("modernizer")
        public final IgnoreConstructorTestClass
            testMethodReturningDeclaredType()
            throws Exception {
            "".getBytes("UTF-8");
            return new IgnoreConstructorTestClass();
        }
    }

    protected static class IgnoreMethodReturningGenericTypeClassTest {
        @SuppressWarnings("modernizer")
        public final List<List<String>> testMethodReturningGenericType()
            throws Exception {
            "".getBytes("UTF-8");
            return new ArrayList<List<String>>();
        }
    }

    protected static class IgnoreStaticOuterClassConstructor {
        protected static class IgnoreStaticInnerClassConstructor {
            @SuppressWarnings("modernizer")
            IgnoreStaticInnerClassConstructor() throws Exception {
                "".getBytes("UTF-8");
            }
        }
    }
}
