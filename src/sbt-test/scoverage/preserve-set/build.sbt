import sbt.complete.DefaultParsers._

version := "0.1"

scalaVersion := "2.13.14"

crossScalaVersions := Seq("2.13.14")

libraryDependencies += "org.scalameta" %% "munit" % "1.0.1" % Test

val checkScalaVersion = inputKey[Unit](
  "Input task to compare the value of scalaVersion setting with a given input."
)
checkScalaVersion := {
  val arg: String = (Space ~> StringBasic).parsed
  if (scalaVersion.value != arg)
    sys.error(
      s"scalaVersion [${scalaVersion.value}] not equal to expected [$arg]"
    )
  ()
}

val checkScoverageEnabled = inputKey[Unit](
  "Input task to compare the value of coverageEnabled setting with a given input."
)
checkScoverageEnabled := {
  val arg: String = (Space ~> StringBasic).parsed
  if (coverageEnabled.value.toString != arg)
    sys.error(
      s"coverageEnabled [${coverageEnabled.value}] not equal to expected [$arg]"
    )
  ()
}

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Resolver.sonatypeOssRepos("snapshots")
  else Seq.empty
}

coverageEnabled := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case _ => coverageEnabled.value
  }
}
