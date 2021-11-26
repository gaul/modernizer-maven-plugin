# Modernizer Maven Plugin

Modernizer Maven Plugin detects uses of legacy APIs which modern Java versions supersede. These modern APIs are often
more performant, safer, and idiomatic than the legacy equivalents. For example, Modernizer can detect uses of `Vector`
instead of `ArrayList`,
`String.getBytes(String)` instead of `String.getBytes(Charset)`, and Guava `Objects.equal` instead of Java
7 `Objects.equals`. The default configuration detects
[over 200 legacy APIs](https://github.com/gaul/modernizer-maven-plugin/blob/master/modernizer-maven-plugin/src/main/resources/modernizer.xml)
, including third-party libraries like
[Apache Commons](https://commons.apache.org/),
[Guava](https://github.com/google/guava), and [Joda-Time](https://www.joda.org/joda-time/).