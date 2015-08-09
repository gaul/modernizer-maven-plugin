Modernizer Maven Plugin
=======================
Modernizer Maven Plugin detects uses of legacy APIs which modern Java versions
supersede.
These modern APIs are often more performant, safer, and idiomatic than the
legacy equivalents.
For example, Modernizer can detect uses of `Vector` instead of `ArrayList`,
`String.getBytes(String)` instead of `String.getBytes(Charset)`, and
Guava `Objects.equal` instead of Java 7 `Objects.equals`.
The default configuration detects
[over 100 legacy APIs](https://github.com/andrewgaul/modernizer-maven-plugin/blob/master/src/main/resources/modernizer.xml),
including third-party libraries like
[Guava](https://code.google.com/p/guava-libraries/).

Configuration
-------------
To run Modernizer, add the following to the `<plugins>` stanza in your pom.xml
then invoke `mvn modernizer:modernizer`:

```xml
<plugin>
  <groupId>org.gaul</groupId>
  <artifactId>modernizer-maven-plugin</artifactId>
  <version>1.4.0</version>
  <configuration>
    <javaVersion>1.8</javaVersion>
  </configuration>
</plugin>
```

The `<configuration>` stanza can contain several elements:

* `<javaVersion>` enables violations based on target Java version, e.g., 1.8.  For example, Modernizer will detect uses of `Vector` as violations when targeting Java 1.2 but not when targeting Java 1.1.  Required parameter.
* `<failOnViolations>` fail phase if Modernizer detects any violations.  Defaults to true.
* `<includeTestClasses>` run Modernizer on test classes.  Defaults to true.
* `<violationsFile>` user-specified violation file.  Also disables standard violation checks.
* `<exclusionsFile>` disables user-specified violations.  This is a text file with one exclusion per line in the javap format: `java/lang/String.getBytes:(Ljava/lang/String;)[B`.
* `<exclusions>` violations to disable. Each exclusion should be in the javap format: `java/lang/String.getBytes:(Ljava/lang/String;)[B`.
* `<ignorePackages>` package prefixes to ignore, specified using `<ignorePackage>` child elements. Specifying `foo.bar` subsequently ignores `foo.bar.*`, `foo.bar.baz.*` and so on.

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

References
----------
* [ASM](http://asm.ow2.org/) provides Java bytecode introspection which enables Modernizer's checks
* `javac -Xlint:deprecated` detects uses of interfaces with @Deprecated annotations
* [Overstock.com library-detectors](https://github.com/overstock/library-detectors) detects uses of interfaces with @Beta annotations
* [Checkstyle](http://checkstyle.sourceforge.net/) IllegalInstantiation and Regexp checks can mimic some of Modernizer's functionality

License
-------
Copyright (C) 2014-2015 Andrew Gaul

Licensed under the Apache License, Version 2.0
