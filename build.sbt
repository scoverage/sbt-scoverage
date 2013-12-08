name := "sbt-scoverage"

organization := "org.scala-sbt.plugins"

version := "0.95.0"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.sksamuel.scoverage" %% "scalac-scoverage-plugin" % "0.95.0"
)

publishMavenStyle := false

publishArtifact in Test := false

publishTo := {
  Some(
    Resolver.url(
      "sbt-plugin-releases",
      new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/")
    )(Resolver.ivyStylePatterns)
  )
}
