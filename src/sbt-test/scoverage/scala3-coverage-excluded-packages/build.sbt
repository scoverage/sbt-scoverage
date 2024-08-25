version := "0.1"

scalaVersion := "3.5.0"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageExcludedPackages := "two\\..*;three\\..*"

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}
