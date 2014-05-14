name := "sbt-scoverage"

organization := "org.scoverage"

version := "0.98.4"

scalaVersion := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

// If you change this, remember to also change the same value in ScoverageSbtPlugin.scala
libraryDependencies ++= Seq(
  "org.scoverage" %% "scalac-scoverage-plugin" % "0.98.4"
)

publishTo := Some(Resolver.url("sbt-plugin-releases",
  new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

publishArtifact in Test := false

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
