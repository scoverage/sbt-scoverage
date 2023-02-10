import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

lazy val root = (project in file(".")).aggregate(crossJS, crossJVM)

lazy val cross =
  CrossProject("sjstest", file("sjstest"))(JVMPlatform, JSPlatform)
    .crossType(CrossType.Full)
    .settings(
      scalaVersion := "2.13.10",
      libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
    )

lazy val crossJS = cross.js
lazy val crossJVM = cross.jvm

ThisBuild / resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
