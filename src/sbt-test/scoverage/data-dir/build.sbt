version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.12.10" % "test"

coverageDataDir := target.value / "custom-test"

coverageMinimum := 80

coverageFailOnMinimum := true

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
