import sbt.complete.DefaultParsers._

version := "0.1"

scalaVersion := "2.12.13"

crossScalaVersions := Seq("2.10.6", "2.12.13")

libraryDependencies += "org.specs2" %% "specs2" % "2.5" % "test"

val checkScalaVersion = inputKey[Unit]("Input task to compare the value of scalaVersion setting with a given input.")
checkScalaVersion := {
  val arg: String = (Space ~> StringBasic).parsed
  if (scalaVersion.value != arg) sys.error(s"scalaVersion [${scalaVersion.value}] not equal to expected [$arg]")
  ()
}

val checkScoverageEnabled = inputKey[Unit]("Input task to compare the value of coverageEnabled setting with a given input.")
checkScoverageEnabled := {
  val arg: String = (Space ~> StringBasic).parsed
  if (coverageEnabled.value.toString != arg) sys.error(s"coverageEnabled [${coverageEnabled.value}] not equal to expected [$arg]")
  ()
}


resolvers ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}

// We force coverage to be always disabled for 2.10. This is not an uncommon real world scenario
coverageEnabled := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) => false
    case _ => coverageEnabled.value
  }
}
