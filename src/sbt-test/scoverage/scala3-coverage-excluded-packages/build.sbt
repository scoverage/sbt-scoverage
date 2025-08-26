version := "0.1"

scalaVersion := "3.5.0"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageExcludedPackages := "two\\..*;three\\..*"
