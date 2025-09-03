version := "0.1"

scalaVersion := "2.13.13"

allowUnsafeScalaLibUpgrade := true

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageExcludedFiles := ".*\\/two\\/GoodCoverage;.*\\/three\\/.*"
