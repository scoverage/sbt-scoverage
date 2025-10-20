version := "0.1"

scalaVersion := "2.13.17"

allowUnsafeScalaLibUpgrade := true

libraryDependencies += "org.specs2" %% "specs2-core" % "4.12.10" % "test"

coverageDataDir := target.value / "custom-test"

coverageMinimumStmtTotal := 80

coverageFailOnMinimum := true
