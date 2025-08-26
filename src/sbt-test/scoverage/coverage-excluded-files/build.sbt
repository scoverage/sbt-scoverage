version := "0.1"

scalaVersion := "2.13.13"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageExcludedFiles := ".*\\/two\\/GoodCoverage;.*\\/three\\/.*"
