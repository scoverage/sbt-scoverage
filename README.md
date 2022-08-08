# sbt-scoverage

[![Gitter](https://img.shields.io/gitter/room/scoverage/scoverage.svg)](https://gitter.im/scoverage/scoverage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scoverage/sbt-scoverage/badge.svg?kill_cache=1)](https://search.maven.org/artifact/org.scoverage/sbt-scoverage/)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

sbt-scoverage is an sbt plugin that offers support for Scala code coverage using
[scoverage](https://github.com/scoverage/scalac-scoverage-plugin). This plugin
supports Scala 2.12, 2.13, and 3.

**NOTE**: that ScalaJS and Scala Native support is limited to Scala 2.
**NOTE**: that Scala 3 support starts with 3.2.x.


## Setup

**Requirements**: Requires sbt 1.2.8 or above

In `project/plugins.sbt`:
```scala
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "x.x.x")
```

If you are in an enterprise environment, and the above does not work, try:
```scala
libraryDependencies += "org.scoverage" % "sbt-scoverage_2.12_1.0" % "x.x.x"
```

## Usage

Run the tests with enabled coverage:
```
$ sbt clean coverage test
```
or if you have integration tests as well
```
$ sbt clean coverage it:test
```

To enable coverage directly in your build, use:
```scala
coverageEnabled := true
```

To generate the coverage reports run
```
$ sbt coverageReport
```

Coverage reports will be in your `target/scala-<scala-version>/scoverage-report`
directory.  There are HTML and XML reports. The XML is useful if you need to
programatically use the results, or if you're writing a tool.

**NOTE**: If you're running the coverage reports from within an sbt console
session (as opposed to one command per sbt launch), then the `coverage` command
is sticky.  To turn it back off when you're done running reports, use the
`coverageOff` command or reset `coverageEnabled` with `set coverageEnabled :=
false`.

### Multi project reports

By default, scoverage will generate reports for each project separately. You can
merge them into an aggregated report by using the following:

```
$ sbt coverageAggregate
```

**NOTE**: You do not need to run `coverageReport` before `coverageAggregate`; it
aggregates over the sub-projects' coverage data directly, not the report xml.

### Exclude classes and packages and files

You can exclude classes from being considered for coverage measurement by
providing semicolon-separated list of regular expressions.

```scala
coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;models\\.data\\..*"
```

The regular expressions are matched against the fully qualified class name, and
must match the entire string to take effect.  Any matched classes will not be
instrumented or included in the coverage report.

You can also exclude files and file paths.

```scala
coverageExcludedFiles := ".*\\/two\\/GoodCoverage;.*\\/three\\/.*"
```

Note: The `.scala` file extension needs to be omitted from the filename, if one is given.

Note: These two options only work for Scala2. Right now Scala3 does not support
a way to exclude packages or files from being instrumented.

You can also mark sections of code with comments like:

```scala
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  ...
  // $COVERAGE-ON$
```

Any code between two such comments will not be instrumented or included in the
coverage report.

### Minimum coverage

Based on minimum coverage, you can fail the build with the following keys:

```scala
coverageFailOnMinimum := true
coverageMinimumStmtTotal := 90
coverageMinimumBranchTotal := 90
coverageMinimumStmtPerPackage := 90
coverageMinimumBranchPerPackage := 85
coverageMinimumStmtPerFile := 85
coverageMinimumBranchPerFile := 80
```

These settings will be enforced when the reports are generated.  If you generate
an aggregate report using `coverageAggregate` then these settings will apply to
that report.

### Override Location for Coverage Data And Report

If desired, one could override the default location for generating the sbt report and data through setting `coverageDataDir`:

Example in data-dir test:
```scala
coverageDataDir := target.value / "custom-test"
```

Can also be set through the sbt set directive
```scala
set coverageDataDir := file("/tmp")
```

## Trouble-shooting failing tests

scoverage does a lot of file writing behind the scenes in order to track which
statements have been executed.  If you are running into a scenario where your
tests normally pass, but fail when scoverage is enabled, then the culprit can be
one of the following:

* timing issues on futures and other async operations, try upping the timeouts by an order of magnitude.
* tests are run in a sandbox mode (such as with `java.security.PrivilegedAction<T>`), try running the tests outside of the sandbox.

## Example project

[the scoverage samples project](https://github.com/scoverage/sbt-scoverage-samples).

## Integrations

### Codacy

[Codacy](https://www.codacy.com) integrates with your favorite coverage tool to
provide an in-depth overlook of your project status. scoverage information can
be integrated into Codacy through the
[codacy-coverage-reporter](https://github.com/codacy/codacy-coverage-reporter).

### Coveralls

If you have an open source project then you can add code coverage metrics with
the [Coveralls](https://coveralls.io/). scoverage will integrate with coveralls
using the [sbt-coveralls](https://github.com/scoverage/sbt-coveralls) plugin.

### Codecov

You can integrate with [Codecov](https://about.codecov.io/) easily sending your
reports there via your CI. You can see an example of this here in
[codecov/example-scala](https://github.com/codecov/example-scala).

### Plugin for SonarQube

If you want to visually browse statement coverage reports then use this [plugin
for SonarQube](https://github.com/RadoBuransky/sonar-scoverage-plugin).  It
allows you to review overall project statement coverage as well as dig deeper
into sub-modules, directories and source code files to see uncovered statements.
Statement coverage measurement can become an integral part of your team's
continuous integration process and a required quality standard.

## Release Notes

For any information on releases and upgrading, please refer to the [release
page](https://github.com/scoverage/sbt-scoverage/releases).
