version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageMinimumStmtPerFile := 90

coverageFailOnMinimum := true

resolvers ++= {
  if (
    sys.props
      .get("plugin.version")
      .map(_.endsWith("-SNAPSHOT"))
      .getOrElse(false)
  ) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
