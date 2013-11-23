organization := "org.scalescc"

name := "sbt-scales"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.10.3")

sbtPlugin := true

//crossBuildingSettings

//CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

//resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/snapshots"

//resolvers += Resolver.url("local-ivy", new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

libraryDependencies += "org.scalescc" %% "scalac-scales-plugin" % "0.1.0-SNAPSHOT"

publishTo <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := {
  x => false
}

pomExtra := {
  <url>https://github.com/scala-scales/sbt-scales</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:scala-scales/sbt-scales.git</url>
      <connection>scm:git@github.com:scala-scales/sbt-scales.git</connection>
    </scm>
    <developers>
      <developer>
        <id>sksamuel</id>
        <name>Stephen Samuel</name>
        <url>http://github.com/sksamuel</url>
      </developer>
    </developers>
}

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  System.getenv("SONATYPE_USER"),
  System.getenv("SONATYPE_PASS")
)