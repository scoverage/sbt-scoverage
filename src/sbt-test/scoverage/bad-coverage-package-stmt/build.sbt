version := "0.1"

scalaVersion := "2.13.13"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageMinimumStmtPerPackage := 80

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
