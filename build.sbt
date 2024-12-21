name := "sbt-scoverage"

import sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts

lazy val scoverageVersion = "2.2.1"

inThisBuild(
  List(
    organization := "org.scoverage",
    homepage := Some(url("http://scoverage.org/")),
    developers := List(
      Developer(
        "sksamuel",
        "Stephen Samuel",
        "sam@sksamuel.com",
        url("https://github.com/sksamuel")
      ),
      Developer(
        "gslowikowski",
        "Grzegorz Slowikowski",
        "gslowikowski@gmail.com",
        url("https://github.com/gslowikowski")
      )
    ),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/license/LICENSE-2.0")
    ),
    scalaVersion := "2.12.19"
  )
)

lazy val root = Project("sbt-scoverage", file("."))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    crossScalaVersions += "3.6.2",
    libraryDependencies ++= Seq(
      "org.scoverage" %% "scalac-scoverage-reporter" % scoverageVersion
    ),
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          (pluginCrossBuild / sbtVersion).value
        case _ =>
          "2.0.0-M3"
      }
    },
    buildInfoKeys := Seq[BuildInfoKey]("scoverageVersion" -> scoverageVersion),
    buildInfoPackage := "scoverage",
    Test / fork := false,
    Test / publishArtifact := false,
    Test / parallelExecution := false,
    scalacOptions := Seq(
      "-language:implicitConversions",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding",
      "utf8"
    ),
    resolvers ++= {
      if (isSnapshot.value) Resolver.sonatypeOssRepos("snapshots") else Nil
    },
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    )
  )
