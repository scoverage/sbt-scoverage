organization := "org.scalescc"

name := "sbt-scales"

version := "0.11.0-SNAPSHOT"

scalaVersion := "2.10.3"

sbtPlugin := true

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

libraryDependencies ++= Seq(
  "org.scalescc" %% "scalac-scales-plugin" % "0.11.0-SNAPSHOT"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

publishTo := {
  val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
  val (name, url) = if (version.toString.contains("-SNAPSHOT"))
    ("sbt-plugin-snapshots", scalasbt + "sbt-plugin-snapshots")
  else
    ("sbt-plugin-releases", scalasbt + "sbt-plugin-releases")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle := false

publishArtifact in Test := false

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  System.getenv("SONATYPE_USER"),
  System.getenv("SONATYPE_PASS")
)