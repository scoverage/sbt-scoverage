inThisBuild(
  List(
    organization := "org.scoverage",
    scalaVersion := "2.13.16",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.4" % Test
  )
)

lazy val a = project
lazy val b = project
lazy val c = project.disablePlugins(ScoverageSbtPlugin)

ThisBuild / resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}
