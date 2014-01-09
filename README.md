sbt-scoverage
========

sbt-scoverage is a plugin for SBT that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Build Status](https://travis-ci.org/scoverage/sbt-scoverage.png?branch=master)](https://travis-ci.org/scoverage/sbt-scoverage)

## How to use

Add the plugin to your build with the following in project/build.sbt:
```scala
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-scoverage" % "0.95.4")
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

## Coveralls

If you have an open source project then you can add code coverage metrics with the excellent website http://coveralls.io. Scoverage will integrate with coveralls using the [sbt-coveralls](https://github.com/scoverage/sbt-coveralls) plugin.

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
