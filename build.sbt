name := "sbt-scales"

organization := "com.sksamuel.scala-scales"

version := "0.90.1"

scalaVersion := "2.10.3"

sbtPlugin := true

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

libraryDependencies ++= Seq(
  "com.sksamuel.scala-scales" %% "scalac-scales-plugin" % "0.90.0"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

publishTo := {
  val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
  val (name, url) = if (version.toString.contains("-SNAPSHOT"))
    ("sbt-plugin-snapshots", scalasbt + "sbt-plugin-snapshots")
  else ("sbt-plugin-releases", scalasbt + "sbt-plugin-releases")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle := false

publishArtifact in Test := false