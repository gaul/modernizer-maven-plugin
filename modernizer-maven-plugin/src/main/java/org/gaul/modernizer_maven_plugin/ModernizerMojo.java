/*
 * Copyright 2014-2025 Andrew Gaul <andrew@gaul.org>
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.gaul.modernizer_maven_plugin.output.CodeClimateOutputer;
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
    protected boolean failOnViolations = true;

    /** Run Modernizer on test classes. */
    @Parameter(defaultValue = "true",
               property = "modernizer.includeTestClasses")
    protected boolean includeTestClasses = true;

    /**
     * User-specified violation file. Also disables standard violation checks.
     * Can point to files from classpath using an absolute path, e.g.:
     *
     * classpath:/modernizer.xml
     *
     * for the default violations file.
     */
    @Parameter(property = "modernizer.violationsFile")
    protected String violationsFile = "classpath:/modernizer.xml";

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
    protected List<String> violationsFiles = emptyList();

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
    private String outputFile;

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
     * scanning.
     * */
    @Parameter(defaultValue = "true",
               property = "modernizer.ignoreGeneratedClasses")
    private Boolean ignoreGeneratedClasses;

    /**
     * Violations to disable. Each exclusion should be in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Parameter
    protected Set<String> exclusions = new HashSet<String>();

    /**
     * Violation patterns to disable. Each exclusion should be a
     * regular expression that matches the javap format:
     *
     * java/lang/.*
     */
    @Parameter
    protected Set<String> exclusionPatterns = new HashSet<String>();

    /**
     * Package prefixes to ignore, specified using &lt;ignorePackage&gt; child
     * elements. Specifying foo.bar subsequently ignores foo.bar.*,
     * foo.bar.baz.* and so on.
     */
    @Parameter
    protected Set<String> ignorePackages = new HashSet<String>();

    /**
     * Fully qualified class names (incl. package) to ignore by regular
     * expression, specified using &lt;ignoreClassNamePattern&gt; child
     * elements.  Specifying .*.bar.* ignores foo.bar.*, foo.bar.baz.* but
     * also bar.* and so on; or .*Immutable ignores all class with names
     * ending in Immutable in all packages.
     */
    @Parameter
    protected Set<String> ignoreClassNamePatterns = new HashSet<String>();

    private Modernizer modernizer;

    /**
     * Skips the plugin execution.
     *
     * @since 1.4.0
     */
    @Parameter(defaultValue = "false", property = "modernizer.skip")
    protected boolean skip = false;

    private List<OutputEntry> outputEntries = new ArrayList<>();

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

        Map<String, Violation> allViolations = parseViolations(violationsFile);
        for (String violationsFilePath : violationsFiles) {
            allViolations.putAll(parseViolations(violationsFilePath));
        }

        Set<String> allExclusions = new HashSet<String>();
        allExclusions.addAll(exclusions);
        if (exclusionsFile != null) {
            allExclusions.addAll(readExclusionsFile(exclusionsFile));
        }

        Set<Pattern> allExclusionPatterns = new HashSet<Pattern>();
        for (String pattern : exclusionPatterns) {
            try {
                allExclusionPatterns.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException pse) {
                throw new MojoExecutionException(
                        "Invalid exclusion pattern", pse);
            }
        }

        Set<Pattern> allIgnoreFullClassNamePatterns = new HashSet<Pattern>();
        for (String pattern : ignoreClassNamePatterns) {
            try {
                allIgnoreFullClassNamePatterns.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException pse) {
                throw new MojoExecutionException(
                        "Invalid exclusion pattern", pse);
            }
        }

        Set<String> ignoreClassNames = new HashSet<String>();
        try {
            ignoreClassNames.addAll(
                SuppressModernizerAnnotationDetector.detect(
                    outputDirectory.toPath()));
            if (ignoreGeneratedClasses) {
                Set<String> ignore =
                    SuppressGeneratedAnnotationDetector.detect(
                        outputDirectory.toPath());
                if (getLog().isDebugEnabled()) {
                    getLog().debug(
                        "The following generated classes will be ignored");
                    for (String s : ignore) {
                        getLog().debug(s);
                    }
                }
                ignoreClassNames.addAll(ignore);
            }
            if (includeTestClasses) {
                ignoreClassNames.addAll(
                    SuppressModernizerAnnotationDetector.detect(
                        testOutputDirectory.toPath()));
                if (ignoreGeneratedClasses) {
                    Set<String> ignore =
                        SuppressGeneratedAnnotationDetector.detect(
                            testOutputDirectory.toPath());
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(
                            "The following generated test classes " +
                            "will be ignored");
                        for (String s : ignore) {
                            getLog().debug(s);
                        }
                    }
                    ignoreClassNames.addAll(ignore);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading suppressions", e);
        }

        modernizer = new Modernizer(javaVersion, allViolations, allExclusions,
                allExclusionPatterns, ignorePackages,
                ignoreClassNames, allIgnoreFullClassNamePatterns);

        long count;
        try {
            count = recurseFiles(outputDirectory.toPath());
            if (includeTestClasses) {
                count += recurseFiles(testOutputDirectory.toPath());
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Error reading Java classes", ioe);
        }

        try {
            buildOutputer().output(outputEntries);
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Error outputting violations", ioe);
        }

        if (failOnViolations && count != 0) {
            throw new MojoExecutionException("Found " + count +
                    " violations");
        }
    }

    private static Map<String, Violation> parseViolations(
            String violationsFilePath) throws MojoExecutionException {
        InputStream is;
        if (violationsFilePath.startsWith(CLASSPATH_PREFIX)) {
            String classpath =
                    violationsFilePath.substring(CLASSPATH_PREFIX.length());
            checkArgument(classpath.startsWith("/"), format(
                    "Only absolute classpath references are allowed, got [%s]",
                    classpath));
            is = Modernizer.class.getResourceAsStream(classpath);
        } else {
            Path path = FileSystems.getDefault().getPath(violationsFilePath);
            try {
                is = Files.newInputStream(path);
            } catch (IOException fnfe) {
                throw new MojoExecutionException(
                        "Error opening violation file: " + path, fnfe);
            }
        }
        try {
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
        } finally {
            Utils.closeQuietly(is);
        }
    }

    private Collection<String> readExclusionsFile(String exclusionsFilePath)
            throws MojoExecutionException {
        InputStream is = null;
        try {
            Path path = FileSystems.getDefault().getPath(exclusionsFilePath);
            if (Files.exists(path)) {
                is = Files.newInputStream(path);
            } else {
                is = this.getClass().getClassLoader().getResourceAsStream(
                        exclusionsFilePath);
            }
            if (is == null) {
                throw new MojoExecutionException(
                        "Could not find exclusion file: " +
                        exclusionsFilePath);
            }

            return Utils.filterCommentLines(Utils.readAllLines(is));
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Error reading exclusion file: " +
                    exclusionsFilePath, ioe);
        } finally {
            Utils.closeQuietly(is);
        }
    }

    private long recurseFiles(Path path) throws IOException {
        long count = 0;
        if (!Files.exists(path)) {
            return count;
        }
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                Iterable<Path> children = stream::iterator;
                for (Path child : children) {
                    count += recurseFiles(path.resolve(child));
                }
            }
        } else if (path.toString().endsWith(".class")) {
            try (InputStream is = Files.newInputStream(path)) {
                Collection<ViolationOccurrence> occurrences =
                        modernizer.check(is);
                for (ViolationOccurrence occurrence : occurrences) {
                    String name = path.toString();
                    if (name.startsWith(outputDirectory.getPath())) {
                        name = sourceDirectory.getPath() + name.substring(
                                outputDirectory.getPath().length());
                        name = name.substring(0,
                                name.length() - ".class".length()) + ".java";
                    } else if (name.startsWith(testOutputDirectory.getPath())) {
                        name = testSourceDirectory.getPath() + name.substring(
                                testOutputDirectory.getPath().length());
                        name = name.substring(0,
                                name.length() - ".class".length()) + ".java";
                    }
                    outputEntries.add(new OutputEntry(name, occurrence));
                    ++count;
                }
            }
        }
        return count;
    }

    private Outputer buildOutputer() throws MojoExecutionException {
        String baseDir = project.getBuild().getDirectory();
        if (Objects.requireNonNull(outputFormat) == OutputFormat.CONSOLE) {
            return new LoggerOutputer(getLog(), violationLogLevel);
        } else if (outputFormat == OutputFormat.CODE_CLIMATE) {
            return new CodeClimateOutputer(
                    baseDir + "/" + CodeClimateOutputer.DEFAULT_FILENAME,
                    codeClimateSeverity);
        }
        throw new MojoExecutionException(
                "Invalid output format: " + outputFormat);
    }
}
