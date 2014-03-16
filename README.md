sbt-scoverage
========

sbt-scoverage is a plugin for SBT that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Build Status](https://travis-ci.org/scoverage/sbt-scoverage.png?branch=master)](https://travis-ci.org/scoverage/sbt-scoverage)

## How to use

Add the plugin to your build with the following in project/build.sbt:
```scala
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-scoverage" % "0.95.7")
```

Add the plugin settings to your project somewhere in build.sbt:
```scala
ScoverageSbtPlugin.instrumentSettings
```

Then run the your tests with coverage enabled by entering:
```
$ sbt clean scoverage:test
```

After the tests have finished you should find the coverage reports inside `target/scoverage-report`.

If you want to see a project that is already setup to use scoverage in both sbt and maven, then clone [the scoverage samples project](https://github.com/scoverage/scoverage-samples).

## Exclude classes and packages

You can exclude classes from being considered for coverage measurement by providing semicolon-separated list of
regular expressions.

Example:
```scala
ScoverageSbtPlugin.ScoverageKeys.excludedPackages in ScoverageSbtPlugin.scoverage :=
  "<empty>;Reverse.*;.*AuthService.*;models.data.*"
```

## Disable parallel test execution

It is possible to disable the parallel execution for tests:

```scala
parallelExecution in ScoverageSbtPlugin.scoverageTest := false,
```

## Coveralls

If you have an open source project then you can add code coverage metrics with the excellent website http://coveralls.io. Scoverage will integrate with coveralls using the [sbt-coveralls](https://github.com/scoverage/sbt-coveralls) plugin.

## Plugin for SonarQube

If you want to visually browse statement coverage reports then use this [plugin for SonarQube](https://github.com/RadoBuransky/sonar-scoverage-plugin).
It allows you to review overall project statement coverage as well as dig deeper into sub-modules, directories and
source code files to see uncovered statements. Statement coverage measurement can become an integral part of your
team's continuous integration process and a required quality standard.

## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Stephen Samuel

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
