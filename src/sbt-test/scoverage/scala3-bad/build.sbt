version := "0.1"

scalaVersion := "3.2.0-RC1"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageMinimumStmtTotal := 80

coverageFailOnMinimum := true

coverageEnabled := true

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
