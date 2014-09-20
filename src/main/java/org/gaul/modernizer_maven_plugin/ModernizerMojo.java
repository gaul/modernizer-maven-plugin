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

@Mojo(name = "modernizer", defaultPhase = LifecyclePhase.COMPILE)
public final class ModernizerMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFilesDirectory;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    private File testClassFilesDirectory;

    @Parameter(defaultValue = "${javaVersion}", required = true)
    private String javaVersion;

    @Parameter(defaultValue = "${failOnViolations}")
    private boolean failOnViolations = true;

    @Parameter(defaultValue = "${includeTestClasses}")
    private boolean includeTestClasses = true;

    @Parameter(defaultValue = "${violationsFile}")
    private String violationsFile;

    @Parameter(defaultValue = "${exclusionsFile}")
    private String exclusionsFile;

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
            File file = new File(exclusionsFile);
            try {
                exclusions.addAll(Utils.readAllLines(file));
            } catch (IOException ioe) {
                throw new MojoExecutionException(
                        "Error reading exclusion file: " + file, ioe);
            }
        }

        modernizer = new Modernizer(javaVersion, violations, exclusions);

        try {
            long count = recurseFiles(classFilesDirectory);
            if (includeTestClasses) {
                count += recurseFiles(testClassFilesDirectory);
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
                    // TODO: map back to source file and column number?
                    getLog().error(file.toString() + ":" +
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
