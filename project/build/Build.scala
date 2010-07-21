import sbt._

class Build(info: ProjectInfo) extends PluginProject(info) with IdeaProject {
  val scct = "reaktor" % "scct_2.7.7" % "1.0"
}