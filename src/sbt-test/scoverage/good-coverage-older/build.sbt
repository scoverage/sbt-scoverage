version := "0.1"

// Set older version to make sure fallbacks work correctly
scalaVersion := "2.13.11"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.0-M10" % Test

coverageMinimumStmtTotal := 100
coverageMinimumBranchTotal := 100
coverageMinimumStmtPerPackage := 100
coverageMinimumBranchPerPackage := 100
coverageMinimumStmtPerFile := 100
coverageMinimumBranchPerFile := 100

coverageFailOnMinimum := true

allowUnsafeScalaLibUpgrade := true