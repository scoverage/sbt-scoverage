import sbt._

class Build(info: ProjectInfo) extends PluginProject(info) with IdeaProject {
	override def managedStyle = ManagedStyle.Maven
	lazy val publishTo = Resolver.file("github-pages-repo", new java.io.File("../../gh-pages/maven-repo/"))

  val scct = "reaktor" % "scct_2.9.0-1" % "0.1-SNAPSHOT"
}