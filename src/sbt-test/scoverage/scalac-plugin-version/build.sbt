lazy val root = (project in file(".")).settings(
  coverageScalacPluginVersion := "1.3.0"
)

TaskKey[Unit]("check") := {
  assert(
    libraryDependencies.value.count(module =>
      module.organization == "org.scoverage" && module.revision == "1.3.0"
    ) == 2
  )
}

resolvers ++= {
  if (sys.props.get("plugin.version").exists(_.endsWith("-SNAPSHOT")))
    Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}
