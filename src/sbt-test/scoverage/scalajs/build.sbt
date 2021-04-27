import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

lazy val root = (project in file(".")).aggregate(crossJS, crossJVM)

lazy val cross = CrossProject("sjstest", file("sjstest"))(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .settings(
    scalaVersion := "2.12.13",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.8" % "test" cross(CrossVersion.binary)
    )
  )

lazy val crossJS = cross.js
lazy val crossJVM = cross.jvm

ThisBuild / resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT"))) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
