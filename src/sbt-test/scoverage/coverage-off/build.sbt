version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageMinimumStmtTotal := 100
coverageMinimumBranchTotal := 100
coverageMinimumStmtPerPackage := 100
coverageMinimumBranchPerPackage := 100
coverageMinimumStmtPerFile := 100
coverageMinimumBranchPerFile := 100

coverageFailOnMinimum := true

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
