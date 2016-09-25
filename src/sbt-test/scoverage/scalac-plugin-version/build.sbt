lazy val root = (project in file(".")).settings(
  coverageScalacPluginVersion := "1.1.0"
)

TaskKey[Unit]("check") := {
  assert(
    libraryDependencies.value
      .filter(module =>
        module.organization == "org.scoverage" && module.revision == "1.1.0")
      .size == 2)
}

resolvers ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
