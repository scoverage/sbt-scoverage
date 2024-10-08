version := "0.1"

scalaVersion := "2.13.15"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageMinimumBranchTotal := 80

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
