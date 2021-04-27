import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

lazy val root = (project in file(".")).aggregate(crossJS, crossJVM)

lazy val cross = CrossProject("sjstest", file("sjstest"))(JVMPlatform, JSPlatform)
  .settings(
    scalaVersion := "2.13.5",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.8" % "test"
    )
  )

lazy val crossJS = cross.js
lazy val crossJVM = cross.jvm

resolvers in ThisBuild ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
