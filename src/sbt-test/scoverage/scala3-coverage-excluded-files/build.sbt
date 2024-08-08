version := "0.1"

scalaVersion := "3.4.2"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

coverageExcludedFiles := ".*/two/GoodCoverage;.*/three/GoodCoverage"

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
