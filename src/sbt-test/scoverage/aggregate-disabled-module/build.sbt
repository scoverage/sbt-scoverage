inThisBuild(
  List(
    organization := "org.scoverage",
    scalaVersion := "2.13.17",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.4" % Test
  )
)

lazy val a = project
lazy val b = project
lazy val c = project.disablePlugins(ScoverageSbtPlugin)
