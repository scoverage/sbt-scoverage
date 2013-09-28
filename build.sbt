organization := "reaktor"

name := "sbt-scct"

version := "0.2-SNAPSHOT"

scalaVersion := "2.10.2"

crossScalaVersions := Seq("2.10.2", "2.9.2", "2.9.1-1", "2.9.1", "2.9.0-1", "2.9.0")

sbtPlugin := true

crossBuildingSettings

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

// Load scct from remote:
resolvers += "scct-github-repository" at "http://scct.github.com/scct/maven-repo"

libraryDependencies += "reaktor" %% "scct" % "0.2-SNAPSHOT"

publishTo := Some(Resolver.file("file",  new File("../gh-pages/maven-repo")))

