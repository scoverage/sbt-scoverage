name := "sbt-scoverage"

organization := "org.scoverage"

version := "1.0.5-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

resolvers <++= isSnapshot (
  if (_)
    Seq(Resolver.mavenLocal, Resolver.sonatypeRepo("snapshots"))
  else
    Seq()
)

libraryDependencies ++= Seq(
  "org.scoverage" %% "scalac-scoverage-plugin" % "1.0.5-SNAPSHOT"
)

publishTo <<= isSnapshot {
  if (_)
    Some(Resolver.sbtPluginRepo("snapshots"))
  else
    Some(Resolver.sbtPluginRepo("releases"))
}

publishMavenStyle := false

publishArtifact in Test := false

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}
