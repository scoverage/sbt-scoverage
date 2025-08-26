version := "0.1"

scalaVersion := "3.5.0"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageMinimumStmtTotal := 100
coverageMinimumBranchTotal := 100
coverageMinimumStmtPerPackage := 100
coverageMinimumBranchPerPackage := 100
coverageMinimumStmtPerFile := 100
coverageMinimumBranchPerFile := 100

coverageFailOnMinimum := true
