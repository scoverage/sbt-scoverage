version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.25" % Test

coverageMinimum := 80

coverageFailOnMinimum := true

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
