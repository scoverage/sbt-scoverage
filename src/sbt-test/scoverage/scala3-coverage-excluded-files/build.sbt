version := "0.1"

scalaVersion := "3.3.4"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

coverageExcludedFiles := ".*\\/two\\/GoodCoverage;.*\\/three\\/.*"

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}
