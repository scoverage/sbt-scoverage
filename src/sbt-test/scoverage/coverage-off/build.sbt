version := "0.1"

scalaVersion := "2.10.4"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

ScoverageSbtPlugin.ScoverageKeys.coverageMinimum := 80

ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := true
