Modernizer Maven Plugin
=======================

[![Maven Central](https://img.shields.io/maven-central/v/org.gaul/modernizer-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22modernizer-maven-plugin%22)

Modernizer Maven Plugin detects uses of legacy APIs which modern Java versions
supersede.
These modern APIs are often more performant, safer, and idiomatic than the
legacy equivalents.
For example, Modernizer can detect uses of `Vector` instead of `ArrayList`,
`String.getBytes(String)` instead of `String.getBytes(Charset)`, and
Guava `Objects.equal` instead of Java 7 `Objects.equals`.
The default configuration detects
[over 200 legacy APIs](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml),
including third-party libraries like
[Apache Commons](https://commons.apache.org/),
[Guava](https://github.com/google/guava),
and [Joda-Time](https://www.joda.org/joda-time/).

Configuration
-------------

To run Modernizer, add the following to the `<plugins>` stanza in your pom.xml
then invoke `mvn modernizer:modernizer`:

```xml
<plugin>
  <groupId>org.gaul</groupId>
  <artifactId>modernizer-maven-plugin</artifactId>
  <version>2.4.0</version>
  <configuration>
    <javaVersion>8</javaVersion>
  </configuration>
</plugin>
```

The `<configuration>` stanza can contain several elements:

* `<javaVersion>` enables violations based on target Java version, e.g., 8.  For example, Modernizer will detect uses of `Vector` as violations when targeting Java 1.2 but not when targeting Java 1.1.  Required parameter.
* `<failOnViolations>` fail phase if Modernizer detects any violations.  Defaults to true.
* `<includeTestClasses>` run Modernizer on test classes.  Defaults to true.
* `<violationsFile>` user-specified violation file.  Also disables standard violation checks. Can point to classpath using absolute paths, e.g. `classpath:/your/file.xml`.
* `<violationsFiles>` user-specified violations file.  The latter files override violations from the former ones, including `violationsFile` and the default violations. Can point to classpath using absolute paths, e.g. `classpath:/your/file.xml`.
* `<exclusionsFile>` disables user-specified violations.  This is a text file with one exclusion per line in the javap format: `java/lang/String.getBytes:(Ljava/lang/String;)[B`.  Empty lines and lines starting with `#` are ignored.
* `<exclusions>` violations to disable. Each exclusion should be in the javap format: `java/lang/String.getBytes:(Ljava/lang/String;)[B`.
* `<exclusionPatterns>` violation patterns to disable, specified using `<exclusionPattern>` child elements. Each exclusion should be a regular expression that matches the javap format: `java/lang/.*` of a violation.
* `<ignorePackages>` package prefixes to ignore, specified using `<ignorePackage>` child elements. Specifying `foo.bar` subsequently ignores `foo.bar.*`, `foo.bar.baz.*` and so on.
* `<ignoreClassNamePatterns>` full qualified class names (incl. package) to ignore, specified using `<ignoreClassNamePattern>` child elements. Each exclusion should be a regular expression that matches a package and/or class; the package will be / not . separated (ASM's format).
* `<ignoreGeneratedClasses>` classes annotated with an annotation whose retention policy is <code>runtime</code> or <code>class</code> and whose simple name contain "Generated" will be ignored. (Note: both [javax.annotation.Generated](https://docs.oracle.com/javase/8/docs/api/javax/annotation/Generated.html) and [javax.annotation.processing.Generated](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/annotation/processing/Generated.html) have [retention policy](https://docs.oracle.com/javase/8/docs/api/index.html?java/lang/annotation/RetentionPolicy.html) SOURCE (aka discarded by compiler).)

To run Modernizer during the verify phase of your build, add the following to
the modernizer `<plugin>` stanza in your pom.xml:

```xml
<executions>
  <execution>
    <id>modernizer</id>
    <phase>verify</phase>
    <goals>
      <goal>modernizer</goal>
    </goals>
  </execution>
</executions>
```

Command-line flags can override Modernizer configuration and
[ModernizerMojo](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/java/org/gaul/modernizer_maven_plugin/ModernizerMojo.java) 
documents all of these.  The most commonly used flags:

* `-Dmodernizer.failOnViolations` - fail phase if violations detected, defaults to true
* `-Dmodernizer.skip` - skip plugin execution, defaults to false

Ignoring elements
-----------------

Code can suppress violations within a class or method via an annotation.  First
add the following dependency to your `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>org.gaul</groupId>
    <artifactId>modernizer-maven-annotations</artifactId>
    <version>2.4.0</version>
  </dependency>
</dependencies>
```

Then add `@SuppressModernizer` to the element to ignore:

```java
import org.gaul.modernizer_maven_annotations.SuppressModernizer;

public class Example {
    @SuppressModernizer
    public static void method() { ... }
}
```

References
----------

* [ASM](https://asm.ow2.org/) provides Java bytecode introspection which enables Modernizer's checks
* [Checkstyle](https://checkstyle.org/) IllegalInstantiation and Regexp checks can mimic some of Modernizer's functionality
* [Google Error Prone](https://errorprone.info/) JdkObsolete can mimic some of Modernizer's functionality
* [Gradle Modernizer Plugin](https://github.com/andygoossens/gradle-modernizer-plugin) provides a Gradle interface to Modernizer
* `javac -Xlint:deprecated` detects uses of interfaces with @Deprecated annotations
* [Overstock.com library-detectors](https://github.com/overstock/library-detectors) detects uses of interfaces with @Beta annotations
* [Policeman's Forbidden API Checker](https://github.com/policeman-tools/forbidden-apis) provides similar functionality to Modernizer

License
-------
Copyright (C) 2014-2022 Andrew Gaul

Licensed under the Apache License, Version 2.0
