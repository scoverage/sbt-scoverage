version := "0.1"
scalaVersion := "3.8.4"
libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test
Test / fork := true
coverageMinimumStmtTotal := 100
coverageFailOnMinimum := true

// coverageReport only logs "No coverage data" when the instrumentation file is
// missing, so assert on it explicitly. A command rather than a task: sbt 2
// caches tasks, and a cached check would not look at the file again.
commands += Command.command("checkCoverageData") { state =>
  val dir = Project.extract(state).get(coverageDataDir)
  val f = dir / "scoverage-data" / "scoverage.coverage"
  if (!f.exists) sys.error(s"missing $f")
  state
}
