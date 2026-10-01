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

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.gaul.modernizer_maven_plugin.output.OutputFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class ModernizerMojoTest {

    private static final Path OUTPUT = Paths.get("target", "classes");
    private static final Path SOURCE = Paths.get("src", "main", "java");
    private static final String BUNDLED_VECTOR_COMMENT =
            "Prefer java.util.ArrayList<>(); note that Vector is " +
            "synchronized while ArrayList is not";
    private static final String BUNDLED_GET_BYTES_COMMENT =
            "Prefer java.lang.String.getBytes(Charset)";
    private static final String VECTOR_NAME =
            "java/util/Vector.\"&lt;init&gt;\":()V";
    private static final String CLOCK_NAME =
            "java/lang/System.currentTimeMillis:()J";
    private static final String JAVA_VERSION_REQUIRED =
            "javaVersion is not set but is required for execution.";

    @TempDir
    private Path tempDir;

    private Path sourceRoot;
    private Path classesDir;

    @Test
    public void mapToSourceForPlainClass() {
        Path classFile = OUTPUT.resolve("com/example/Foo.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve("com/example/Foo.java").toString());
    }

    @Test
    public void mapToSourceForAnonymousInnerClass() {
        // SimpleKVDatabase$1.class belongs to SimpleKVDatabase.java (#217)
        Path classFile = OUTPUT.resolve(
                "io/permazen/kv/simple/SimpleKVDatabase$1.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve(
                        "io/permazen/kv/simple/SimpleKVDatabase.java")
                        .toString());
    }

    @Test
    public void mapToSourceForNamedInnerClass() {
        Path classFile = OUTPUT.resolve("com/example/Foo$Bar.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve("com/example/Foo.java").toString());
    }

    @Test
    public void mapToSourceForDeeplyNestedClass() {
        Path classFile = OUTPUT.resolve("com/example/Foo$Bar$1.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve("com/example/Foo.java").toString());
    }

    @Test
    public void mapToSourceForPackageInfo() {
        Path classFile = OUTPUT.resolve("com/example/package-info.class");
        assertThat(ModernizerMojo.mapToSource(classFile, OUTPUT, SOURCE))
                .isEqualTo(SOURCE.resolve(
                        "com/example/package-info.java").toString());
    }

    @BeforeEach
    public void compileFixture() throws Exception {
        sourceRoot = tempDir.resolve("src");
        classesDir = tempDir.resolve("classes");
        Path source = sourceRoot.resolve("fixture/LegacyUser.java");
        Files.createDirectories(source.getParent());
        Files.write(source, List.of(
                "package fixture;",
                "",
                "import java.util.Vector;",
                "",
                "public final class LegacyUser {",
                "    public void legacy() throws Exception {",
                "        System.currentTimeMillis();",
                "        \"\".getBytes(\"UTF-8\");",
                "        new Vector<Object>();",
                "    }",
                "}",
                ""), StandardCharsets.UTF_8);
        Files.createDirectories(classesDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("system Java compiler").isNotNull();
        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<>();
        Boolean compiled;
        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(null, null, null)) {
            compiled = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("--release", "8", "-g", "-d",
                            classesDir.toString()),
                    null,
                    fileManager.getJavaFileObjects(source.toFile())).call();
        }
        assertThat(compiled).as("fixture diagnostics: %s", diagnostics
                .getDiagnostics()).isTrue();
    }

    @Test
    public void omittedIncludeDefaultViolationsReportsBundledViolation()
            throws Exception {
        RunResult omitted = execute(null, null, null, "8", false);
        RunResult enabled = execute(Boolean.TRUE, null, null, "8", false);
        assertViolations(omitted, BUNDLED_VECTOR_COMMENT,
                BUNDLED_GET_BYTES_COMMENT);
        assertViolations(enabled, BUNDLED_VECTOR_COMMENT,
                BUNDLED_GET_BYTES_COMMENT);
        assertThat(commentsOf(enabled.errors))
                .isEqualTo(commentsOf(omitted.errors));
    }

    @Test
    public void includeDefaultViolationsFalseWithNoFilesSucceeds()
            throws Exception {
        RunResult result = execute(Boolean.FALSE, null, null, "8", false);
        assertThat(result.failure).isNull();
        assertThat(result.errors).isEmpty();
    }

    @Test
    public void customViolationsFileAppliesWhenDefaultsDisabled()
            throws Exception {
        Path rules = writeRules("custom.xml", CLOCK_NAME, "custom clock");
        RunResult result = execute(Boolean.FALSE, rules.toString(), null,
                "8", false);
        assertViolations(result, "custom clock");
    }

    @Test
    public void customViolationsFileReplacesBundledRules() throws Exception {
        Path rules = writeRules("custom.xml", CLOCK_NAME, "custom clock");
        RunResult result = execute(Boolean.TRUE, rules.toString(), null,
                "8", false);
        assertViolations(result, "custom clock");
    }

    @Test
    public void violationsFilesOnEmptyBaseLaterFileOverrides()
            throws Exception {
        Path earlier = writeRules("earlier.xml",
                CLOCK_NAME, "first clock",
                VECTOR_NAME, "layered vector");
        Path later = writeRules("later.xml", CLOCK_NAME, "second clock");
        RunResult result = execute(Boolean.FALSE, null,
                List.of(earlier.toString(), later.toString()), "8", false);
        assertViolations(result, "second clock", "layered vector");
    }

    @Test
    public void violationsFilesLayerOnDefaultRules() throws Exception {
        Path earlier = writeRules("earlier.xml",
                CLOCK_NAME, "first clock",
                VECTOR_NAME, "layered vector");
        Path later = writeRules("later.xml", CLOCK_NAME, "second clock");
        RunResult result = execute(Boolean.TRUE, null,
                List.of(earlier.toString(), later.toString()), "8", false);
        assertViolations(result, "second clock", "layered vector",
                BUNDLED_GET_BYTES_COMMENT);
    }

    @Test
    public void explicitClasspathModernizerXmlLoadsBundledRules()
            throws Exception {
        RunResult result = execute(Boolean.FALSE,
                "classpath:/modernizer.xml", null, "8", false);
        assertViolations(result, BUNDLED_VECTOR_COMMENT,
                BUNDLED_GET_BYTES_COMMENT);
    }

    @Test
    public void missingConfiguredViolationsFileFails() throws Exception {
        Path missing = tempDir.resolve("missing-violations.xml");
        RunResult result = execute(Boolean.FALSE, missing.toString(), null,
                "8", false);
        assertThat(result.failure).isNotNull();
        assertThat(result.failure).hasMessageContaining(
                "Error opening violation file");
        assertThat(result.errors).isEmpty();
    }

    @Test
    public void malformedConfiguredViolationsFileFails() throws Exception {
        Path malformed = tempDir.resolve("malformed-violations.xml");
        Files.writeString(malformed, "this is not xml");
        RunResult result = execute(Boolean.TRUE, malformed.toString(), null,
                "8", false);
        assertThat(result.failure).isNotNull();
        assertThat(result.failure).hasMessageContaining(
                "Error parsing violation data");
        assertThat(result.errors).isEmpty();
    }

    @Test
    public void missingConfiguredViolationsFilesEntryFails() throws Exception {
        Path missing = tempDir.resolve("missing-layer.xml");
        RunResult result = execute(Boolean.FALSE, null,
                List.of(missing.toString()), "8", false);
        assertThat(result.failure).isNotNull();
        assertThat(result.failure).hasMessageContaining(
                "Error opening violation file");
        assertThat(result.errors).isEmpty();
    }

    @Test
    public void malformedConfiguredViolationsFilesEntryFails()
            throws Exception {
        Path malformed = tempDir.resolve("malformed-layer.xml");
        Files.writeString(malformed, "<modernizer>");
        RunResult result = execute(Boolean.TRUE, null,
                List.of(malformed.toString()), "8", false);
        assertThat(result.failure).isNotNull();
        assertThat(result.failure).hasMessageContaining(
                "Error parsing violation data");
        assertThat(result.errors).isEmpty();
    }

    @Test
    public void skipBypassesExecution() throws Exception {
        RunResult result = execute(null, null, null, null, true);
        assertThat(result.failure).isNull();
        assertThat(result.errors).isEmpty();
        assertThat(result.infos).contains("Skipping modernizer execution!");
    }

    @Test
    public void emptyPolicyStillRequiresJavaVersion() throws Exception {
        RunResult missing = execute(Boolean.FALSE, null, null, null, false);
        assertThat(missing.failure).isNotNull();
        assertThat(missing.failure).hasMessage(JAVA_VERSION_REQUIRED);
        assertThat(missing.errors).isEmpty();

        RunResult empty = execute(Boolean.FALSE, null, null, "", false);
        assertThat(empty.failure).isNotNull();
        assertThat(empty.failure).hasMessage(JAVA_VERSION_REQUIRED);
        assertThat(empty.errors).isEmpty();
    }

    private RunResult execute(Boolean includeDefaults, String violationsFile,
            List<String> violationsFiles, String javaVersion, boolean skip)
            throws Exception {
        Path testSource = tempDir.resolve("test-src");
        Path testClasses = tempDir.resolve("test-classes");
        Path buildDir = tempDir.resolve("build");
        Files.createDirectories(testSource);
        Files.createDirectories(testClasses);
        Files.createDirectories(buildDir);

        ModernizerMojo mojo = new ModernizerMojo();
        RecordingLog log = new RecordingLog();
        mojo.setLog(log);

        MavenProject project = new MavenProject();
        project.getBuild().setDirectory(buildDir.toString());
        setField(mojo, "project", project);
        setField(mojo, "sourceDirectory", sourceRoot.toFile());
        setField(mojo, "testSourceDirectory", testSource.toFile());
        setField(mojo, "outputDirectory", classesDir.toFile());
        setField(mojo, "testOutputDirectory", testClasses.toFile());
        setField(mojo, "outputFormat", OutputFormat.CONSOLE);
        setField(mojo, "violationLogLevel", "error");
        setField(mojo, "javaVersion", javaVersion);
        if (includeDefaults != null) {
            setField(mojo, "includeDefaultViolations", includeDefaults);
        }
        if (violationsFile != null) {
            setField(mojo, "violationsFile", violationsFile);
        }
        if (violationsFiles != null) {
            setField(mojo, "violationsFiles", violationsFiles);
        }
        if (skip) {
            setField(mojo, "skip", Boolean.TRUE);
        }

        MojoExecutionException failure = null;
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            failure = e;
        }
        return new RunResult(new ArrayList<>(log.errors),
                new ArrayList<>(log.infos), failure);
    }

    private Path writeRules(String fileName, String... nameAndComment)
            throws IOException {
        Path path = tempDir.resolve(fileName);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n<modernizer>\n");
        for (int i = 0; i < nameAndComment.length; i += 2) {
            xml.append("  <violation>\n    <name>")
                    .append(nameAndComment[i])
                    .append("</name>\n    <version>8</version>\n    <comment>")
                    .append(nameAndComment[i + 1])
                    .append("</comment>\n  </violation>\n");
        }
        xml.append("</modernizer>\n");
        Files.writeString(path, xml.toString());
        return path;
    }

    private static void assertViolations(RunResult result,
            String... expected) {
        assertThat(result.failure).as("reported: %s", result.errors)
                .isNotNull();
        assertThat(result.failure.getMessage())
                .as("reported: %s", result.errors)
                .isEqualTo("Found " + expected.length + " violations");
        assertThat(commentsOf(result.errors))
                .containsExactlyInAnyOrder(expected);
    }

    private static List<String> commentsOf(List<String> errors) {
        List<String> comments = new ArrayList<>();
        for (String error : errors) {
            int separator = error.lastIndexOf(": ");
            assertThat(separator).as("violation line %s", error)
                    .isGreaterThanOrEqualTo(0);
            comments.add(error.substring(separator + 2));
        }
        return comments;
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingLog extends SystemStreamLog {
        private final List<String> errors = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();

        @Override
        public void error(CharSequence content) {
            errors.add(content.toString());
        }

        @Override
        public void info(CharSequence content) {
            infos.add(content.toString());
        }
    }

    private static final class RunResult {
        private final List<String> errors;
        private final List<String> infos;
        private final MojoExecutionException failure;

        private RunResult(List<String> errors, List<String> infos,
                MojoExecutionException failure) {
            this.errors = errors;
            this.infos = infos;
            this.failure = failure;
        }
    }
}
