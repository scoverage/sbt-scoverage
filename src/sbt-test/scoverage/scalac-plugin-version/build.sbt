lazy val root = (project in file(".")).settings(
  coverageEnabled := true,
  coverageScalacPluginVersion := "1.1.0"
)
TaskKey[Unit]("check") := {
  assert(
    libraryDependencies.value
      .filter(module =>
        module.organization == "org.scoverage" && module.revision == "1.1.0")
      .size == 2)
}
