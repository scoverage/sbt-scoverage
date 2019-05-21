version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.specs2" %% "specs2" % "2.5" % "test"

coverageMinimum := 80

coverageFailOnMinimum := true

resolvers ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
