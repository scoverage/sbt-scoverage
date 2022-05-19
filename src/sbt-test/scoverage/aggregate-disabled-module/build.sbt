inThisBuild(
  List(
    organization := "org.scoverage",
    scalaVersion := "2.13.6",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
)

lazy val a = project
lazy val b = project
lazy val c = project.disablePlugins(ScoverageSbtPlugin)

ThisBuild / resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
