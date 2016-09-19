/*
  The projects test aggregation of coverage reports from two sub-projects.
  The sub-projects are in the directories partA and partB.
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
lazy val partB = module("B").settings(
  coverageSkip := true
)

lazy val root = (project in file("."))
  .settings(commonSettings:_*)
  .settings(
    name := "root",
    test := { }
  ).aggregate(
    partA,
    partB
  )

resolvers in ThisBuild ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
