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

package org.gaul.modernizer_annotation_processor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;

@SupportedAnnotationTypes("java.lang.SuppressWarnings")
@AutoService(Processor.class)
public class ModernizerAnnotationProcessor extends AbstractProcessor {

    @Override
    public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private class AnnotatedElements {
        private List<String> annotatedClasses;
        private List<String> annotatedMethods;

        AnnotatedElements(
            List<String> annotatedClasses,
            List<String> annotatedMethods
        ) {
            this.annotatedClasses = annotatedClasses;
            this.annotatedMethods = annotatedMethods;
        }

        public List<String> getAnnotatedClasses() {
            return annotatedClasses;
        }

        public List<String> getAnnotatedMethods() {
            return annotatedMethods;
        }
    }

    @Override
    public final boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv
    ) {
        for (TypeElement annotation : annotations) {
            AnnotatedElements annotatedElements =
                getAnnotatedElements(roundEnv, annotation);
            if (!(annotatedElements.getAnnotatedClasses().isEmpty() &&
                annotatedElements.getAnnotatedMethods().isEmpty())
            ) {
                File outputDir = getOutputDirectory();
                outputDir.mkdirs();
                makeFile(new File(outputDir,
                    ModernizerAnnotationUtils.IGNORE_CLASSES_FILE_NAME),
                    annotatedElements.getAnnotatedClasses());
                makeFile(new File(outputDir,
                    ModernizerAnnotationUtils.IGNORE_METHODS_FILE_NAME),
                    annotatedElements.getAnnotatedMethods());
            }
        }
        return true;
    }

    private AnnotatedElements getAnnotatedElements(
        RoundEnvironment roundEnv,
        TypeElement annotation
    ) {
        List<String> annotatedClasses = new ArrayList<String>();
        List<String> annotatedMethods = new ArrayList<String>();
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            List<String> warnings = Arrays.asList(
                    element.getAnnotation(SuppressWarnings.class).value());
            if (warnings.contains("modernizer")) {
                if (element.getKind().isClass()) {
                    annotatedClasses.add(
                        getFullClassNameRegex(element));
                } else if (element.getKind().equals(ElementKind.METHOD) ||
                    element.getKind().equals(ElementKind.CONSTRUCTOR)) {
                    annotatedMethods.add(getMethodRepresentation(element));
                }
            }
        }
        return new AnnotatedElements(annotatedClasses, annotatedMethods);
    }


    private void makeFile(
        File file,
        List<String> annotatedElements
    ) {
        if (annotatedElements.isEmpty()) {
            return;
        }
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (String element : annotatedElements) {
                writer.write(element + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private File getOutputDirectory() {
        try {
            FileObject fileObjectToGetPath =
                processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "dummy-file.txt");
            File outputDir =
                new File(fileObjectToGetPath.getName()).getParentFile();
            return ModernizerAnnotationUtils.getOutputDir(outputDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a formatted version of the fully qualified class name
     * regular expression of the element to perform the necessary
     * checks in the plugin against the ASM parsed class name.
     *
     * Example:
     * Output: "org/gaul/mypackage/ExampleClass\$TestClass(\$.+)?"
     */
    private String getFullClassNameRegex(Element classElement) {
        return getFullClassName(classElement)
            .replace('.', '/')
            .replace("$", "\\$") + "(\\$.+)?";
    }

    /**
     * Returns the fully-qualified class name of the provided element.
     *
     * Example:
     * Output: "org.gaul.mypackage.ExampleClass$TestClass"
     */
    private String getFullClassName(Element classElement) {
        String packageName = processingEnv
            .getElementUtils()
            .getPackageOf(classElement)
            .getQualifiedName()
            .toString();
        String packagePrefix = !packageName.isEmpty() ? packageName + "." : "";
        String classHeader = packagePrefix + getClassName(classElement);
        return classHeader;
    }

    /**
     * Returns the package-less class name of the provided element.
     *
     * Example:
     * Output: "ExampleClass$TestClass"
     */
    private String getClassName(Element classElement) {
        List<String> parentClasses = new ArrayList<String>();
        Element enclosingElement = classElement.getEnclosingElement();
        while (enclosingElement != null &&
            enclosingElement.getKind().isClass()) {
            parentClasses.add(enclosingElement.getSimpleName().toString());
            enclosingElement = enclosingElement.getEnclosingElement();
        }
        StringBuilder className = new StringBuilder();
        for (int index = parentClasses.size() - 1; index >= 0; index--) {
            className.append(parentClasses.get(index));
            className.append("$");
        }
        return className.toString() + classElement.getSimpleName();
    }

    private String getMethodRepresentation(Element methodElement) {
        ExecutableType method = getExecutableType(methodElement);
        return ModernizerAnnotationUtils.getMethodRep(
        getFullClassName(methodElement.getEnclosingElement()),
        methodElement.getSimpleName().toString(),
        getRepresentation(method.getReturnType()),
        getParamsRepresentation(method.getParameterTypes(), methodElement));
    }

    private ExecutableType getExecutableType(Element methodElement) {
        TypeVisitor<ExecutableType, Void> executableTypeVisitor =
            new SimpleTypeVisitor6<ExecutableType, Void>() {
                @Override
                public ExecutableType visitExecutable(
                    ExecutableType executableType, Void obj
                ) {
                    return executableType;
                }
            };
        return methodElement.asType().accept(executableTypeVisitor, null);
    }

    /**
     * Returns the method parameters in the format ASM expects.
     *
     * Example:
     * Input: methodParams {@code {int[][], String, List<Integer>}}
     * Output: {@code {"int[][]", "String", "java.util.List"}}
     */
    private List<String> getParamsRepresentation(
        List<? extends TypeMirror> methodParams,
        Element methodElement
    ) {
        List<String> methodParamReps = new ArrayList<String>();
        /*
         * Add the containing class as a parameter to non-static
         * inner class constructors
         *
         * Example:
         * fullClassName = "org.gaul.example_package.OuterClass$InnerClass"
         * methodParamReps.add("org.gaul.example_package.OuterClass")
         */
        if (methodElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
            String fullClassName =
                getFullClassName(methodElement.getEnclosingElement());
            Set<Modifier> modifiers =
                methodElement.getEnclosingElement().getModifiers();
            if (!modifiers.contains(Modifier.STATIC)) {
                int index = fullClassName.lastIndexOf("$");
                if (index != -1) {
                    methodParamReps.add(fullClassName.substring(0, index));
                }
            }
        }
        for (TypeMirror param : methodParams) {
            methodParamReps.add(getRepresentation(param));
        }
        return methodParamReps;
    }

    /**
     * Returns an ASM-parsable string representation of the element.
     *
     * Example:
     *
     * Generic type
     * Input: {@code java.lang.List<String>}
     * Output: {@code "java.lang.List"}
     *
     * Type Variable
     * Input: {@code E}
     * Output: {@code "java.lang.Object"}
     *
     * Primitive type
     * Input: {@code int}
     * Output: {@code "int"}
     *
     * Array of type variables
     * Input: {@code E[]}
     * Output: {@code "java.lang.Object[]"}
     *
     * Array of primitive type
     * Input: {@code int[]}
     * Output: {@code "int[]"}
     */
    public final String getRepresentation(TypeMirror param) {
        switch (param.getKind()) {
        case ARRAY:
            int arrayLength = 0;
            TypeMirror paramType = param;
            while (paramType.getKind().equals(TypeKind.ARRAY)) {
                paramType = ((ArrayType) paramType).getComponentType();
                arrayLength++;
            }
            return getRepresentation(paramType) +
                Strings.repeat("[]", arrayLength);
        case TYPEVAR:
            return Object.class.getName();
        default:
            String paramString = param.toString();
            int index = paramString.indexOf("<");
            return index == -1 ? paramString : paramString.substring(0, index);
        }
    }
}
