organization := "org.scalescc"

name := "sbt-scales"

version := "0.11.0-SNAPSHOT"

scalaVersion := "2.10.3"

sbtPlugin := true

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

libraryDependencies ++= Seq(
  "org.scalescc" %% "scalac-scales-plugin" % "0.11.0-SNAPSHOT"
)

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