organization := "org.scalescc"

name := "sbt-scales"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.10.3"

sbtPlugin := true

libraryDependencies += "org.scalescc" %% "scalac-scales-plugin" % "0.1.0-SNAPSHOT"

publishTo <<= isSnapshot {
  snapshot =>
    if (snapshot) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases)
}

publishMavenStyle := false

publishArtifact in Test := false

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  System.getenv("SONATYPE_USER"),
  System.getenv("SONATYPE_PASS")
)