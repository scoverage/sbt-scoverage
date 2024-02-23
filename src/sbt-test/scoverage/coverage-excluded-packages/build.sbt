version := "0.1"

scalaVersion := "2.13.13"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageExcludedPackages := "two\\..*"

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
