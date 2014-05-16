name := "sbt-scoverage"

organization := "org.scoverage"

version := "0.99.2.2"

scalaVersion := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "org.scoverage" %% "scalac-scoverage-plugin" % "0.99.2"
)

publishTo := Some(Resolver.url("sbt-plugin-releases",
  new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

publishArtifact in Test := false

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
