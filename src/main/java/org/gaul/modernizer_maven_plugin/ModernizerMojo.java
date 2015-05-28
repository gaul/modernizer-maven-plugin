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
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "project.build.sourceDirectory")
    private File sourceDirectory;

    @Parameter(property = "project.build.testSourceDirectory")
    private File testSourceDirectory;

    @Parameter(property = "project.build.outputDirectory")
    private File outputDirectory;

    @Parameter(property = "project.build.testOutputDirectory")
    private File testOutputDirectory;

    @Parameter(required = true, property = "modernizer.javaVersion")
    private String javaVersion = null;

    @Parameter(defaultValue = "true", property = "modernizer.failOnViolations")
    private boolean failOnViolations = true;

    @Parameter(defaultValue = "true",
               property = "modernizer.includeTestClasses")
    private boolean includeTestClasses = true;

    @Parameter(property = "modernizer.violationsFile")
    private String violationsFile = null;

    @Parameter(property = "modernizer.exclusionsFile")
    private String exclusionsFile = null;

    @Parameter
    private Set<String> ignorePackages = new HashSet<String>();

    private Modernizer modernizer;

    @Override
    public void execute() throws MojoExecutionException {
        Map<String, Violation> violations;
        InputStream is;
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

        Set<String> exclusions = new HashSet<String>();
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

                exclusions.addAll(Utils.readAllLines(is));
            } catch (IOException ioe) {
                throw new MojoExecutionException(
                        "Error reading exclusion file: " + exclusionsFile, ioe);
            } finally {
                Utils.closeQuietly(is);
            }
        }

        modernizer = new Modernizer(javaVersion, violations, exclusions,
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
