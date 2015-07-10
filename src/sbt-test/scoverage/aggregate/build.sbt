/*
  The projects test aggregation of coverage reports from two sub-projects.
  The sub-projects are in the irectories partA and partB.
  The tests are against the sources of ScoverageSbtPlugin in the parent directory.
  It might be possible to test other versions of ScoverageSbtPlugin.
*/

lazy val commonSettings = Seq(
  organization := "org.scoverage",
  version := "0.1.0",
  scalaVersion := "2.10.4"
)

lazy val specs2Lib = "org.specs2" %% "specs2" % "2.3.13" % "test"

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



