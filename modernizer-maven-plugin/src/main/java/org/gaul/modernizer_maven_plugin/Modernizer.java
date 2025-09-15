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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.objectweb.asm.ClassReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class Modernizer {
    private final long javaVersion;
    private final Map<String, Violation> violations;
    private final Collection<String> exclusions;
    private final Collection<Pattern> exclusionPatterns;
    private final Collection<String> ignorePackages;
    private final Set<String> ignoreClassNames;
    private final Collection<Pattern> ignoreFullClassNamePatterns;

    public Modernizer(String javaVersion, Map<String, Violation> violations,
            Collection<String> exclusions,
            Collection<Pattern> exclusionPatterns,
            Collection<String> ignorePackages,
            Set<String> ignoreClassNames,
            Collection<Pattern> ignoreClassNamePatterns) {
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
    }

    public Collection<ViolationOccurrence> check(ClassReader classReader)
            throws IOException {
        ModernizerClassVisitor classVisitor = new ModernizerClassVisitor(
                javaVersion, violations, exclusions, exclusionPatterns,
                ignorePackages, ignoreClassNames, ignoreFullClassNamePatterns);
        classReader.accept(classVisitor, 0);
        return classVisitor.getOccurrences();
    }

    public Collection<ViolationOccurrence> check(InputStream is)
            throws IOException {
        return check(new ClassReader(is));
    }

    public static Map<String, Violation> parseFromXml(InputStream is)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Violation> map =
                new HashMap<String, Violation>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("violation");
        for (int temp = 0; temp < nList.getLength(); ++temp) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) nNode;
            String version = element.getElementsByTagName("version").item(0)
                    .getTextContent();
            int versionNum = parseVersion(version);
            Optional<String> versionLimit = Optional.ofNullable(
                    element.getElementsByTagName("until").item(0))
                    .map(Node::getTextContent);
            OptionalInt versionLimitNum = mapToInt(versionLimit,
                    Modernizer::parseVersion);
            Violation violation = new Violation(
                    element.getElementsByTagName("name").item(0)
                            .getTextContent(),
                    versionNum,
                    versionLimitNum,
                    element.getElementsByTagName("comment").item(0)
                            .getTextContent());
            map.put(violation.getName(), violation);
        }

        return map;
    }

    private static int parseVersion(final String version) {
        return Integer.parseInt(
                version.startsWith("1.") ? version.substring(2) : version);
    }

    private static <T> OptionalInt mapToInt(final Optional<T> optional,
            final ToIntFunction<T> mapper) {
        return optional.isPresent() ?
                OptionalInt.of(mapper.applyAsInt(optional.get())) :
                OptionalInt.empty();
    }
}
