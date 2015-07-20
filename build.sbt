name := "sbt-scoverage"

organization := "org.scoverage"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

sbtPlugin := true

resolvers ++= {
  if(isSnapshot.value) Seq(Resolver.mavenLocal, Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}

libraryDependencies += "org.scoverage" %% "scalac-scoverage-plugin" % "1.1.0"

publishTo := Some(
  if (isSnapshot.value) Resolver.sbtPluginRepo("snapshots")
  else Resolver.sbtPluginRepo("releases")
)

publishArtifact in Test := false

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx1024M", "-XX:MaxPermSize=256M",
  "-Dplugin.version=" + version.value
)

releaseSettings

ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
