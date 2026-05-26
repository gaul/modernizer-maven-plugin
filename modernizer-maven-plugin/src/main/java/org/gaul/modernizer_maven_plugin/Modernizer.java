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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.objectweb.asm.ClassReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class Modernizer {
    private final long javaVersion;
    private final Map<String, Collection<Violation>> violations;
    private final Collection<String> exclusions;
    private final Collection<Pattern> exclusionPatterns;
    private final Collection<String> ignorePackages;
    private final Set<String> ignoreClassNames;
    private final Collection<Pattern> ignoreFullClassNamePatterns;
    private final boolean ignoreGeneratedClasses;

    public Modernizer(String javaVersion, Map<String, Collection<Violation>> violations,
            Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Set<String> ignoreClassNames,
            Collection<Pattern> ignoreClassNamePatterns,
            boolean ignoreGeneratedClasses) {
        long version;
        if (javaVersion.startsWith("1.")) {
            version = Long.parseLong(javaVersion.substring(2));
        } else {
            version = Long.parseLong(javaVersion);
        }
        Utils.checkArgument(version >= 0);
        this.javaVersion = version;
        this.violations = Utils.createImmutableMap(violations);
        this.exclusions = Utils.createImmutableSet(exclusions);
        this.exclusionPatterns = Utils.createImmutableSet(exclusionPatterns);
        this.ignorePackages = Utils.createImmutableSet(ignorePackages);
        this.ignoreClassNames =  Utils.createImmutableSet(ignoreClassNames);
        this.ignoreFullClassNamePatterns
            = Utils.createImmutableSet(ignoreClassNamePatterns);
        this.ignoreGeneratedClasses = ignoreGeneratedClasses;
    }

    /**
     * Equivalent to the primary constructor with {@code ignoreGeneratedClasses}
     * disabled, preserving the behavior of releases before method- and
     * constructor-level {@code @Generated} suppression existed. Retained for
     * backwards compatibility with external callers such as the Gradle
     * Modernizer plugin.
     *
     * @deprecated use the constructor that accepts {@code ignoreGeneratedClasses}
     */
    @Deprecated
    // Not @InlineMe: callers should migrate deliberately and choose their own
    // ignoreGeneratedClasses value rather than inline the false default.
    @SuppressWarnings("InlineMeSuggester")
    public Modernizer(String javaVersion, Map<String, Collection<Violation>> violations,
            Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Set<String> ignoreClassNames,
            Collection<Pattern> ignoreClassNamePatterns) {
        this(javaVersion, violations, exclusions, exclusionPatterns,
                ignorePackages, ignoreClassNames, ignoreClassNamePatterns,
                /*ignoreGeneratedClasses=*/ false);
    }

    public Collection<ViolationOccurrence> check(ClassReader classReader) {
        ModernizerClassVisitor classVisitor = new ModernizerClassVisitor(
                javaVersion, violations, exclusions, exclusionPatterns,
                ignorePackages, ignoreClassNames, ignoreFullClassNamePatterns,
                ignoreGeneratedClasses);
        classReader.accept(classVisitor, 0);
        return classVisitor.getOccurrences();
    }

    public Collection<ViolationOccurrence> check(InputStream is)
            throws IOException {
        return check(new ClassReader(is));
    }

    public static Map<String, Collection<Violation>> parseFromXml(InputStream is)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Collection<Violation>> map = new HashMap<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbFactory.setNamespaceAware(true);
        SchemaFactory sFactory = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream xsd = Modernizer.class.getResourceAsStream(
                "/modernizer.xsd")) {
            dbFactory.setSchema(sFactory.newSchema(new StreamSource(xsd)));
        }
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        dBuilder.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) { }
            @Override
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }
            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("violation");
        for (int temp = 0; temp < nList.getLength(); ++temp) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) nNode;
            String name = element.getElementsByTagName("name").item(0)
                    .getTextContent();
            String version = element.getElementsByTagName("version").item(0)
                    .getTextContent();
            String comment = element.getElementsByTagName("comment").item(0)
                    .getTextContent();
            int versionNum = parseVersion(version);
            Node until = element.getElementsByTagName("until").item(0);
            OptionalInt untilNum = until == null ? OptionalInt.empty()
                    : OptionalInt.of(parseVersion(until.getTextContent()));
            Violation violation = new Violation(
                    name, versionNum, untilNum, comment);
            Collection<Violation> existing = map.computeIfAbsent(
                    violation.getName(), k -> new ArrayList<>());
            for (Violation other : existing) {
                if (other.getVersion() == violation.getVersion()) {
                    throw new SAXException(
                            "Duplicate <violation> with name " +
                            violation.getName() + " and version " +
                            violation.getVersion());
                }
            }
            existing.add(violation);
        }

        return map;
    }

    private static int parseVersion(String version) {
        return Integer.parseInt(
                version.startsWith("1.") ? version.substring(2) : version);
    }
}
