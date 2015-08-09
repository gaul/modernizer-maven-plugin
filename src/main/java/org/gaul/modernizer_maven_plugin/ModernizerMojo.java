/*
 * Copyright 2014-2015 Andrew Gaul <andrew@gaul.org>
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

@Mojo(name = "modernizer", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        threadSafe = true)
public final class ModernizerMojo extends AbstractMojo {
    /** The maven project (effective pom). */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /** The output directory into which to find the source code. */
    @Parameter(property = "project.build.sourceDirectory")
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
    @Parameter(required = true, property = "modernizer.javaVersion")
    private String javaVersion = null;

    /** Fail phase if Modernizer detects any violations. */
    @Parameter(defaultValue = "true", property = "modernizer.failOnViolations")
    private boolean failOnViolations = true;

    /** Run Modernizer on test classes. */
    @Parameter(defaultValue = "true",
               property = "modernizer.includeTestClasses")
    private boolean includeTestClasses = true;

    /**
     * User-specified violation file. Also disables standard violation checks.
     */
    @Parameter(property = "modernizer.violationsFile")
    private String violationsFile = null;

    /**
     * Disables user-specified violations. This is a text file with one
     * exclusion per line in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Parameter(property = "modernizer.exclusionsFile")
    private String exclusionsFile = null;

    /**
     * Violations to disable. Each exclusion should be in the javap format:
     *
     * java/lang/String.getBytes:(Ljava/lang/String;)[B.
     */
    @Parameter
    private Set<String> exclusions = new HashSet<String>();

    /**
     * Package prefixes to ignore, specified using &lt;ignorePackage&gt; child
     * elements. Specifying foo.bar subsequently ignores foo.bar.*,
     * foo.bar.baz.* and so on.
     */
    @Parameter
    private Set<String> ignorePackages = new HashSet<String>();

    private Modernizer modernizer;

    /**
     * Skips the plugin execution.
     *
     * @since 1.4.0
     */
    @Parameter(defaultValue = "false", property = "modernizer.skip")
    protected boolean skip = false;

    @Override
    public void execute() throws MojoExecutionException {
        Map<String, Violation> violations;
        InputStream is;
        if (skip) {
            getLog().info("Skipping modernizer execution!");
            return;
        }

        if (violationsFile == null) {
            is = Modernizer.class.getResourceAsStream("/modernizer.xml");
        } else {
            File file = new File(violationsFile);
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException fnfe) {
                throw new MojoExecutionException(
                        "Error opening violation file: " + file, fnfe);
            }
        }
        try {
            violations = Modernizer.parseFromXml(is);
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

        Set<String> allExclusions = new HashSet<String>();
        allExclusions.addAll(exclusions);
        if (exclusionsFile != null) {
            is = null;
            try {
                File file = new File(exclusionsFile);
                if (file.exists()) {
                    is = new FileInputStream(file);
                } else {
                    is = this.getClass().getClassLoader().getResourceAsStream(
                            exclusionsFile);
                }
                if (is == null) {
                    throw new MojoExecutionException(
                            "Could not find exclusion file: " + exclusionsFile);
                }

                allExclusions.addAll(Utils.readAllLines(is));
            } catch (IOException ioe) {
                throw new MojoExecutionException(
                        "Error reading exclusion file: " + exclusionsFile, ioe);
            } finally {
                Utils.closeQuietly(is);
            }
        }

        modernizer = new Modernizer(javaVersion, violations, allExclusions,
                ignorePackages);

        try {
            long count = recurseFiles(outputDirectory);
            if (includeTestClasses) {
                count += recurseFiles(testOutputDirectory);
            }
            if (failOnViolations && count != 0) {
                throw new MojoExecutionException("Found " + count +
                        " violations");
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Error reading Java classes", ioe);
        }
    }

    private long recurseFiles(File file) throws IOException {
        long count = 0;
        if (!file.exists()) {
            return count;
        }
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children != null) {
                for (String child : children) {
                    count += recurseFiles(new File(file, child));
                }
            }
        } else if (file.getPath().endsWith(".class")) {
            InputStream is = new FileInputStream(file);
            try {
                Collection<ViolationOccurrence> occurrences =
                        modernizer.check(is);
                for (ViolationOccurrence occurrence : occurrences) {
                    String name = file.getPath();
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
                    getLog().error(name + ":" +
                            occurrence.getLineNumber() + ": " +
                            occurrence.getViolation().getComment());
                    ++count;
                }
            } finally {
                Utils.closeQuietly(is);
            }
        }
        return count;
    }
}
