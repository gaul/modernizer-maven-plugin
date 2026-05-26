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

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.gaul.modernizer_maven_plugin.Utils.checkArgument;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.gaul.modernizer_maven_plugin.output.CodeClimateOutputer;
import org.gaul.modernizer_maven_plugin.output.LogLevel;
import org.gaul.modernizer_maven_plugin.output.LoggerOutputer;
import org.gaul.modernizer_maven_plugin.output.OutputEntry;
import org.gaul.modernizer_maven_plugin.output.OutputFormat;
import org.gaul.modernizer_maven_plugin.output.Outputer;
import org.xml.sax.SAXException;

@Mojo(name = "modernizer", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        threadSafe = true)
public final class ModernizerMojo extends AbstractMojo {

    private static final String CLASSPATH_PREFIX = "classpath:";

    /** The maven project (effective pom). */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /** The output directory into which to find the source code. */
    @Parameter(property = "project.build.sourceDirectory")
    // TODO: cannnot convert to Path:
    // https://lists.apache.org/thread/b4gz60c5v5oz7khczs4nozywbkfbmhjf
    private File sourceDirectory;

    /** The output directory into which to find the test source code. */
    @Parameter(property = "project.build.testSourceDirectory")
    private File testSourceDirectory;

    /** The output directory into which to find the resources. */
    @Parameter(property = "project.build.outputDirectory")
    private File outputDirectory;

    /** The output directory into which to find the test resources. */
    @Parameter(property = "project.build.testOutputDirectory")
    private File testOutputDirectory;

    /**
     * Enables violations based on target Java version, e.g., 1.8. For example,
     * Modernizer will detect uses of Vector as violations when targeting Java
     * 1.2 but not when targeting Java 1.1.
     */
    @Parameter(property = "modernizer.javaVersion")
    private String javaVersion;

    /** Fail phase if Modernizer detects any violations. */
    @Parameter(defaultValue = "true", property = "modernizer.failOnViolations")
    private boolean failOnViolations = true;

    /** Run Modernizer on test classes. */
    @Parameter(defaultValue = "true",
               property = "modernizer.includeTestClasses")
    private boolean includeTestClasses = true;

    /**
     * User-specified violation file. Also disables standard violation checks.
     * Can point to files from classpath using an absolute path, e.g.:
     *
     * classpath:/modernizer.xml
     *
     * for the default violations file.
     */
    @Parameter(property = "modernizer.violationsFile")
    private String violationsFile = "classpath:/modernizer.xml";

    /**
     * User-specified violation files. The violations loaded from
     * violationsFiles override the ones specified in violationsFile (or the
     * default violations file if no violationsFile is given). Violations from
     * the latter files override violations from the former files.
     *
     * Can point to files from classpath using an absolute path, e.g.:
     *
     * classpath:/modernizer.xml
     *
     * for the default violations file.
     */
    @Parameter(property = "modernizer.violationsFiles")
    private List<String> violationsFiles = emptyList();

    /**
     * Disables user-specified violations. This is a text file with one
     * exclusion per line in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Parameter(property = "modernizer.exclusionsFile")
    private String exclusionsFile;

    /**
     * Format to output violations in.
     */
    @Parameter(defaultValue = "CONSOLE", property = "modernizer.outputFormat")
    private OutputFormat outputFormat;

    /**
     * Path to the file to output violations to.
     * Ignored if {@code modernizer.outputFormat} is {@code CONSOLE}.
     */
    @Parameter(property = "modernizer.outputFile")
    private File outputFile;

    /**
     * Severity of modernizer violations for CodeClimate.
     * Ignored if {@code modernizer.outputFormat} is not {@code CODECLIMATE}.
     */
    @Parameter(defaultValue = "MINOR",
            property = "modernizer.codeclimateSeverity")
    private CodeClimateOutputer.Severity codeClimateSeverity;

    /**
     * Log level to emit violations at, e.g., error, warn, info, debug.
     * Ignored if {@code modernizer.outputFormat} is not {@code CONSOLE}.
     */
    @Parameter(defaultValue = "error",
               property = "modernizer.violationLogLevel")
    private String violationLogLevel;

    /**
     * Classes annotated with {@code @Generated} will be excluded from
     * scanning, as will individual methods and constructors annotated with
     * {@code @Generated}, e.g., members generated by Lombok.
     * */
    @Parameter(defaultValue = "true",
               property = "modernizer.ignoreGeneratedClasses")
    private boolean ignoreGeneratedClasses;

    /**
     * Violations to disable. Each exclusion should be in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Parameter
    private Set<String> exclusions = new HashSet<>();

    /**
     * Violation patterns to disable. Each exclusion should be a
     * regular expression that matches the javap format:
     *
     * java/lang/.*
     */
    @Parameter
    private Set<String> exclusionPatterns = new HashSet<>();

    /**
     * Package prefixes to ignore, specified using &lt;ignorePackage&gt; child
     * elements. Specifying foo.bar subsequently ignores foo.bar.*,
     * foo.bar.baz.* and so on.
     */
    @Parameter
    private Set<String> ignorePackages = new HashSet<>();

    /**
     * Fully qualified class names (incl. package) to ignore by regular
     * expression, specified using &lt;ignoreClassNamePattern&gt; child
     * elements.  Specifying .*.bar.* ignores foo.bar.*, foo.bar.baz.* but
     * also bar.* and so on; or .*Immutable ignores all class with names
     * ending in Immutable in all packages.
     */
    @Parameter
    private Set<String> ignoreClassNamePatterns = new HashSet<>();

    private Modernizer modernizer;

    /**
     * Skips the plugin execution.
     *
     * @since 1.4.0
     */
    @Parameter(defaultValue = "false", property = "modernizer.skip")
    private boolean skip = false;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping modernizer execution!");
            return;
        }

        if (javaVersion == null || javaVersion.isEmpty()) {
            throw new MojoExecutionException(
                    "javaVersion is not set but is required for execution.");
        }

        LogLevel logLevel = parseLogLevel(violationLogLevel);

        Map<String, Collection<Violation>> allViolations =
                parseViolations(violationsFile);
        for (String violationsFilePath : violationsFiles) {
            allViolations.putAll(parseViolations(violationsFilePath));
        }

        Set<String> allExclusions = new HashSet<>();
        allExclusions.addAll(exclusions);
        if (exclusionsFile != null) {
            allExclusions.addAll(readExclusionsFile(exclusionsFile));
        }

        Set<Pattern> allExclusionPatterns =
                compilePatterns(exclusionPatterns, "exclusion pattern");
        Set<Pattern> allIgnoreFullClassNamePatterns =
                compilePatterns(ignoreClassNamePatterns,
                        "ignoreClassNamePattern");

        Set<String> ignoreClassNames = new HashSet<>();
        try {
            collectIgnoredClassNames(outputDirectory.toPath(),
                    "generated classes", ignoreClassNames);
            if (includeTestClasses) {
                collectIgnoredClassNames(testOutputDirectory.toPath(),
                        "generated test classes", ignoreClassNames);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading suppressions", e);
        }

        modernizer = new Modernizer(javaVersion, allViolations, allExclusions,
                allExclusionPatterns, ignorePackages,
                ignoreClassNames, allIgnoreFullClassNamePatterns,
                ignoreGeneratedClasses);

        List<OutputEntry> outputEntries = new ArrayList<>();
        try {
            recurseFiles(outputDirectory.toPath(), outputEntries);
            if (includeTestClasses) {
                recurseFiles(testOutputDirectory.toPath(), outputEntries);
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Error reading Java classes", ioe);
        }

        try {
            buildOutputer(logLevel).output(outputEntries);
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Error outputting violations", ioe);
        }

        if (failOnViolations && !outputEntries.isEmpty()) {
            throw new MojoExecutionException("Found " + outputEntries.size() +
                    " violations");
        }
    }

    private static Map<String, Collection<Violation>> parseViolations(
            String violationsFilePath) throws MojoExecutionException {
        try (InputStream is = openViolations(violationsFilePath)) {
            return Modernizer.parseFromXml(is);
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Error reading violation data", ioe);
        } catch (ParserConfigurationException pce) {
            throw new MojoExecutionException(
                    "Error parsing violation data", pce);
        } catch (SAXException saxe) {
            throw new MojoExecutionException(
                    "Error parsing violation data", saxe);
        }
    }

    private static InputStream openViolations(String violationsFilePath)
            throws MojoExecutionException {
        if (violationsFilePath.startsWith(CLASSPATH_PREFIX)) {
            String classpath =
                    violationsFilePath.substring(CLASSPATH_PREFIX.length());
            checkArgument(classpath.startsWith("/"), format(
                    "Only absolute classpath references are allowed, got [%s]",
                    classpath));
            InputStream is = Modernizer.class.getResourceAsStream(classpath);
            if (is == null) {
                throw new MojoExecutionException(
                        "Error opening violation file: " + classpath);
            }
            return is;
        }
        Path path = FileSystems.getDefault().getPath(violationsFilePath);
        try {
            return Files.newInputStream(path);
        } catch (IOException fnfe) {
            throw new MojoExecutionException(
                    "Error opening violation file: " + path, fnfe);
        }
    }

    private Collection<String> readExclusionsFile(String exclusionsFilePath)
            throws MojoExecutionException {
        InputStream is;
        Path path = FileSystems.getDefault().getPath(exclusionsFilePath);
        try {
            if (Files.exists(path)) {
                is = Files.newInputStream(path);
            } else {
                is = this.getClass().getClassLoader().getResourceAsStream(
                        exclusionsFilePath);
                if (is == null) {
                    throw new MojoExecutionException(
                            "Could not find exclusion file: " +
                            exclusionsFilePath);
                }
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Error opening exclusion file: " +
                    exclusionsFilePath, ioe);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            Collection<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                lines.add(line);
            }
            return lines;
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Error reading exclusion file: " +
                    exclusionsFilePath, ioe);
        }
    }

    private static Set<Pattern> compilePatterns(Collection<String> patterns,
            String label) throws MojoExecutionException {
        Set<Pattern> compiled = new HashSet<>();
        for (String pattern : patterns) {
            try {
                compiled.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException pse) {
                throw new MojoExecutionException(
                        "Invalid " + label + ": " + pattern, pse);
            }
        }
        return compiled;
    }

    private void collectIgnoredClassNames(Path classRoot,
            String generatedLabel, Set<String> sink) throws IOException {
        sink.addAll(SuppressModernizerAnnotationDetector.detect(classRoot));
        if (ignoreGeneratedClasses) {
            Set<String> generated =
                    SuppressGeneratedAnnotationDetector.detect(classRoot);
            if (getLog().isDebugEnabled()) {
                getLog().debug("The following " + generatedLabel +
                        " will be ignored");
                for (String s : generated) {
                    getLog().debug(s);
                }
            }
            sink.addAll(generated);
        }
    }

    static String mapToSource(Path classFile, Path outputRoot,
            Path sourceRoot) {
        Path relative = outputRoot.relativize(classFile);
        Path sourceFile = sourceRoot.resolve(relative);
        String fileName = sourceFile.getFileName().toString();
        fileName = fileName.substring(0,
                fileName.length() - ".class".length());
        // Anonymous and nested classes (Foo$1.class, Foo$Bar.class) live
        // in the outer class's source file; strip from the first $.
        int dollar = fileName.indexOf('$');
        if (dollar >= 0) {
            fileName = fileName.substring(0, dollar);
        }
        return sourceFile.resolveSibling(fileName + ".java").toString();
    }

    private void recurseFiles(Path path, List<OutputEntry> outputEntries)
            throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    visitClassFile(file, outputEntries);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void visitClassFile(Path path, List<OutputEntry> outputEntries)
            throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            Collection<ViolationOccurrence> occurrences =
                    modernizer.check(is);
            Path outputPath = outputDirectory.toPath();
            Path testOutputPath = testOutputDirectory.toPath();
            Path sourcePath = sourceDirectory.toPath();
            Path testSourcePath = testSourceDirectory.toPath();
            // When one output directory is nested inside the other,
            // match the more specific (longer) one first.
            boolean testFirst = testOutputPath.startsWith(outputPath) &&
                    !outputPath.startsWith(testOutputPath);
            for (ViolationOccurrence occurrence : occurrences) {
                String name = path.toString();
                if (testFirst && path.startsWith(testOutputPath)) {
                    name = mapToSource(path, testOutputPath, testSourcePath);
                } else if (path.startsWith(outputPath)) {
                    name = mapToSource(path, outputPath, sourcePath);
                } else if (path.startsWith(testOutputPath)) {
                    name = mapToSource(path, testOutputPath, testSourcePath);
                }
                outputEntries.add(new OutputEntry(name, occurrence));
            }
        }
    }

    private static LogLevel parseLogLevel(String value)
            throws MojoExecutionException {
        for (LogLevel l : LogLevel.values()) {
            if (l.name().equalsIgnoreCase(value)) {
                return l;
            }
        }
        throw new MojoExecutionException("Unknown violationLogLevel: '" +
                value + "', must be one of " +
                Arrays.asList(LogLevel.values()));
    }

    private Outputer buildOutputer(LogLevel logLevel)
            throws MojoExecutionException {
        Path baseDir = Paths.get(project.getBuild().getDirectory());
        if (Objects.requireNonNull(outputFormat) == OutputFormat.CONSOLE) {
            return new LoggerOutputer(getLog(), logLevel);
        } else if (outputFormat == OutputFormat.CODE_CLIMATE) {
            // make sure the output directory exists
            if (!Files.exists(baseDir)) {
                getLog().debug("Create the missing target directory: " + baseDir);
                try {
                    Files.createDirectories(baseDir);
                } catch (IOException ioe) {
                    throw new MojoExecutionException(
                            "Create missing output directory failed: " + ioe.getMessage());
                }
            }

            Path destination = outputFile != null ?
                    outputFile.toPath() :
                    baseDir.resolve(CodeClimateOutputer.DEFAULT_FILENAME);
            return new CodeClimateOutputer(destination, codeClimateSeverity);
        }
        throw new MojoExecutionException(
                "Invalid output format: " + outputFormat);
    }
}
