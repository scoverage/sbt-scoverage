version := "0.1"

scalaVersion := "3.2.0-RC1-bin-20220517-e5abec0-NIGHTLY" // TODO: Should be updated to stable version on 3.2.0-RC1 release

libraryDependencies += "org.scalameta" %% "munit" % "0.7.25" % Test

coverageMinimum := 80

coverageFailOnMinimum := true

coverageEnabled := true

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
