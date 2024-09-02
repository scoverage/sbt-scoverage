version := "0.1"

scalaVersion := "3.5.0"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageExcludedFiles := ".*\\/two\\/GoodCoverage;.*\\/three\\/.*"

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}
