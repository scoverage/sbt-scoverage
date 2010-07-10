import sbt._
class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val ideaPluginRepo = "GH-pages repo" at "http://mpeltonen.github.com/maven/"
  val ideaPlugin = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"
}
