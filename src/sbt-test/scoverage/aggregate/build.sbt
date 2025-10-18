/*
  The projects test aggregation of coverage reports from two sub-projects.
  The sub-projects are in the directories partA and partB.
 */

lazy val commonSettings = Seq(
  organization := "org.scoverage",
  allowUnsafeScalaLibUpgrade := true,
  version := "0.1.0",
  scalaVersion := "2.13.17"
)

def module(name: String) = {
  val id = s"part$name"
  Project(id = id, base = file(id))
    .settings(commonSettings: _*)
    .settings(
      Keys.name := name,
      libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test
    )
}

lazy val partA = module("A")
lazy val partB = module("B")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "root",
    test := {}
  )
  .aggregate(
    partA,
    partB
  )
