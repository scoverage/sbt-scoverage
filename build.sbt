import bintray.Keys._

name := "sbt-scoverage"

organization := "com.sksamuel.scoverage"

version := "0.95.0"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.sksamuel.scoverage" %% "scalac-scoverage-plugin" % "0.95.0"
)

publishMavenStyle := false

publishArtifact in Test := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := None
