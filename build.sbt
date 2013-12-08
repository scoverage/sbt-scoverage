name := "sbt-scoverage"

organization := "com.sksamuel.scoverage"

version := "0.94.0"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.sksamuel.scoverage" %% "scalac-scoverage-plugin" % "0.94.0"
)

publishMavenStyle := false

publishArtifact in Test := false

publishTo <<= version {
  (v: String) =>
    val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
    val (name, url) = if (v.trim.endsWith("SNAPSHOT")) ("sbt-plugin-snapshots", scalasbt + "sbt-plugin-snapshots")
    else ("sbt-plugin-releases", scalasbt + "sbt-plugin-releases")
    Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}



