import sbt.complete.DefaultParsers._

version := "0.1"

scalaVersion := "2.10.4"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

val checkScalaVersion = inputKey[Unit]("Input task to compare the value of scalaVersion setting with a given input.")
checkScalaVersion := {
  val arg: String = (Space ~> StringBasic).parsed
  if (scalaVersion.value != arg) error(s"scalaVersion [${scalaVersion.value}] not equal to expected [$arg]")
  ()
}

resolvers ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
