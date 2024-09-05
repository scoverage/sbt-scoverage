version := "0.1"

scalaVersion := "2.13.14"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageMinimumStmtPerFile := 90

coverageFailOnMinimum := true

resolvers ++= {
  if (
    sys.props
      .get("plugin.version")
      .map(_.endsWith("-SNAPSHOT"))
      .getOrElse(false)
  ) Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}
