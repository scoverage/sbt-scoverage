/*
  The projects test aggregation of coverage reports from two sub-projects.
  The sub-projects are in the directories partA and partB.
*/

lazy val commonSettings = Seq(
  organization := "org.scoverage",
  version := "0.1.0",
  scalaVersion := "2.12.13"
)

lazy val specs2Lib = "org.specs2" %% "specs2" % "2.5" % "test"

def module(name: String) = {
  val id = s"part$name"
  Project(id = id, base = file(id))
    .settings(commonSettings: _*)
    .settings(
      Keys.name := name,
      libraryDependencies += specs2Lib
    )
}

lazy val partA = module("A")
lazy val partB = module("B")

lazy val root = (project in file("."))
  .settings(commonSettings:_*)
  .settings(
    name := "root",
    test := { }
  ).aggregate(
    partA,
    partB
  )

ThisBuild / resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT"))) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
