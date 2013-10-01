organization := "com.github.scct"

name := "sbt-scct"

version := "0.2"

scalaVersion := "2.10.2"

crossScalaVersions := Seq("2.10.2", "2.9.2", "2.9.1-1", "2.9.1", "2.9.0")

sbtPlugin := true

crossBuildingSettings

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

libraryDependencies += "com.github.scct" %% "scct" % "0.2"

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := <url>http://scct.github.io/scct/</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:SCCT/sbt-scct.git</url>
    <connection>scm:git:git@github.com:SCCT/sbt-scct.git</connection>
  </scm>
  <developers>
    <developer>
      <id>mtkopone</id>
      <name>Mikko Koponen</name>
      <url>http://mtkopone.github.com</url>
    </developer>
  </developers>
