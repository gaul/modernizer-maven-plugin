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
[over 300 legacy APIs](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml),
including third-party libraries like
[Apache Commons](https://commons.apache.org/),
[Guava](https://github.com/google/guava),
and [Joda-Time](https://www.joda.org/joda-time/).

Requirements
------------

* Maven 3.6.3 or newer
* JDK 8 or newer to run the plugin
* JDK 17 or newer to build the plugin from source

Configuration
-------------

To run Modernizer, add the following to the `<plugins>` stanza in your pom.xml
then invoke `mvn modernizer:modernizer`:

```xml
<plugin>
  <groupId>org.gaul</groupId>
  <artifactId>modernizer-maven-plugin</artifactId>
  <version>3.4.0</version>
  <configuration>
    <javaVersion>${maven.compiler.release}</javaVersion>
  </configuration>
</plugin>
```

The `<configuration>` stanza can contain several elements:

* `<javaVersion>` target Java version, e.g., `8` or `1.8` — both forms are accepted for any release.  Modernizer reports a violation only when its `<version>` is at or below this value, so targeting Java 1.2 flags `Vector` but targeting Java 1.1 does not.  Required parameter; binding it to `${maven.compiler.release}` (or `${maven.compiler.target}`) keeps it in sync with the rest of the build.
* `<failOnViolations>` fail phase if Modernizer detects any violations.  Defaults to true.
* `<includeTestClasses>` run Modernizer on test classes.  Defaults to true.
* `<violationsFile>` user-specified violation file.  Also disables standard violation checks.  See [Custom violations](#custom-violations) below.
* `<violationsFiles>` user-specified violation files.  Later files override violations from earlier ones, including `<violationsFile>` and the default violations.
* `<exclusionsFile>` disables user-specified violations.  This is a text file with one exclusion per line in the javap format: `java/lang/String.getBytes:(Ljava/lang/String;)[B`.  Empty lines and lines starting with `#` are ignored.
* `<exclusions>` violations to disable. Each exclusion should be in the javap format: `java/lang/String.getBytes:(Ljava/lang/String;)[B`.
* `<exclusionPatterns>` violation patterns to disable, specified using `<exclusionPattern>` child elements. Each exclusion should be a regular expression that matches the javap format: `java/lang/.*` of a violation.
* `<ignorePackages>` package prefixes to ignore, specified using `<ignorePackage>` child elements. Specifying `foo.bar` subsequently ignores `foo.bar.*`, `foo.bar.baz.*` and so on.
* `<ignoreClassNamePatterns>` full qualified class names (incl. package) to ignore, specified using `<ignoreClassNamePattern>` child elements. Each exclusion should be a regular expression that matches a package and/or class; the package will be / not . separated (ASM's format).
* `<ignoreGeneratedClasses>` classes annotated with an annotation whose retention policy is <code>runtime</code> or <code>class</code> and whose simple name contain "Generated" will be ignored. (Note: both [javax.annotation.Generated](https://docs.oracle.com/javase/8/docs/api/javax/annotation/Generated.html) and [javax.annotation.processing.Generated](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/annotation/processing/Generated.html) have [retention policy](https://docs.oracle.com/javase/8/docs/api/index.html?java/lang/annotation/RetentionPolicy.html) SOURCE (aka discarded by compiler).)

Invoking `mvn modernizer:modernizer` runs the goal once.  For automatic
checks during normal builds, bind it to a lifecycle phase via an
`<execution>`.  For example, to run Modernizer during the verify phase, add
the following to the modernizer `<plugin>` stanza in your pom.xml:

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

### Output Formats

For each violation Modernizer emits one line containing the source location
and the recommended replacement.  Using the default `CONSOLE` format, output
looks like:

```
[ERROR] src/main/java/org/example/Foo.java:42: Prefer java.util.ArrayList<>()
[ERROR] src/main/java/org/example/Foo.java:57: Prefer java.nio.charset.StandardCharsets.UTF_8
```

When `<failOnViolations>` is `true` (the default), the build fails after the
listing with the total violation count.

The plugin can output Modernizer violations in one of many formats which can be configured with the `<configuration>`
stanza using `<outputFormat>`.

The currently supported formats and their respective configuration options are outlined below:
* `CONSOLE` List each violation using Maven's logger. This is the **default** format.
  * `<violationLogLevel>` Specify the log level of the logger: `ERROR`, `WARN`, `INFO` or `DEBUG`.
Default is `ERROR`.
* `CODE_CLIMATE` Write the violations according to [Code Climate's Spec](https://github.com/codeclimate/platform/blob/master/spec/analyzers/SPEC.md). 
GitLab uses this format for its code quality as shown [here](https://docs.gitlab.com/ee/ci/testing/code_quality.html#implement-a-custom-tool).
  * `<outputFile>` The full path the file to output to. Default is `${project.build.directory}/code-quality.json`
  * `<codeClimateSeverity>` Severity of Modernizer violations for CodeClimate: `INFO`, `MINOR`, `MAJOR`, `CRITICAL` or `BLOCKER`.
Default is `MINOR`.

### Custom violations

Modernizer reads its rules from an XML file in the same format as the
[bundled `modernizer.xml`](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml).
Point `<violationsFile>` at your own file to replace the defaults, or list
additional files in `<violationsFiles>` to layer rules on top (later files
override earlier ones).  Either path can be prefixed with `classpath:/` to
load from the classpath.

Each `<violation>` element accepts:

* `<name>` API in javap format: `java/util/Vector` for a type, or
  `java/lang/String.getBytes:(Ljava/lang/String;)[B` for a specific method
* `<version>` lowest target Java version at which the API becomes legacy
* `<comment>` recommended replacement, shown in violation output
* `<until>` (optional) target Java version, exclusive, at which the rule
  stops applying — useful when a newer replacement supersedes an older one

```xml
<?xml version="1.0"?>
<modernizer>
  <violation>
    <name>com/example/legacy/OldThing</name>
    <version>11</version>
    <comment>Use com.example.modern.Thing instead</comment>
  </violation>
</modernizer>
```

Ignoring elements
-----------------

Code can suppress violations on a class (or other type declaration),
constructor, method, or type use via the `@SuppressModernizer` annotation.
First add the following dependency to your `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>org.gaul</groupId>
    <artifactId>modernizer-maven-annotations</artifactId>
    <version>3.4.0</version>
  </dependency>
</dependencies>
```

Then annotate the element to ignore:

```java
import org.gaul.modernizer_maven_annotations.SuppressModernizer;

@SuppressModernizer
public class Example {
    @SuppressModernizer
    public Example() { ... }

    @SuppressModernizer
    public static void method() { ... }
}
```

Modernizer matches the annotation by simple name, so any `@SuppressModernizer`
in any package will suppress violations — the `modernizer-maven-annotations`
dependency above is a convenient canonical copy but is not required.  Fields
and packages cannot be suppressed because the annotation's `@Target` does not
include them; use `<exclusions>` or `<ignoreClassNamePatterns>` instead.

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
Copyright (C) 2014-2026 Andrew Gaul

Licensed under the Apache License, Version 2.0
