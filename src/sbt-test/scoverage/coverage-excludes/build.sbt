version := "0.1"

scalaVersion := "2.10.4"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

coverageMinimum := 90

coverageFailOnMinimum := true

coverageExcludedPackages := "exclude.*"
coverageExcludedSymbols := "scala\\.Unit"

resolvers ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
