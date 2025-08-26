version := "0.1"

scalaVersion := "3.3.4"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageExcludedPackages := "two\\..*;three\\..*"
