version := "0.1"

scalaVersion := "2.13.14"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageMinimumStmtTotal := 80

coverageFailOnMinimum := true

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}
