name := "sbt-scoverage"
organization := "org.scoverage"

import sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val root = Project("sbt-scoverage", file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % Compile
    ),
    libraryDependencies += "org.scoverage" %% "scalac-scoverage-plugin" % "1.4.3" cross(CrossVersion.full),
    scalaVersion := "2.12.13",
    publishMavenStyle := true,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    Test / fork := false,
    Test / publishArtifact := false,
    Test / parallelExecution := false,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    publishTo := {
      if (isSnapshot.value)
        Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      else
        Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },
    resolvers ++= {
      if (isSnapshot.value) Seq(Resolver.sonatypeRepo("snapshots")) else Nil
    },
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    pomExtra := {
      <url>https://github.com/scoverage/sbt-scoverage</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:scoverage/sbt-scoverage.git</url>
          <connection>scm:git@github.com:scoverage/sbt-scoverage.git</connection>
        </scm>
        <developers>
          <developer>
            <id>sksamuel</id>
            <name>sksamuel</name>
            <url>http://github.com/sksamuel</url>
          </developer>
          <developer>
            <id>gslowikowski</id>
            <name>Grzegorz Slowikowski</name>
            <url>http://github.com/gslowikowski</url>
          </developer>
        </developers>
    }
  )

scalariformAutoformat := false
