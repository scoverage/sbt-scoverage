import sbt._

class Build(info: ProjectInfo) extends PluginProject(info) with IdeaProject {
  val scct = "reaktor" % "scct_2.8.0.RC7" % "1.0"
}