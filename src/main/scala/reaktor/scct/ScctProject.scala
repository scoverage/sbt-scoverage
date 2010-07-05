package reaktor.scct

import sbt._
import java.util.jar.Manifest

trait ScctProject extends DefaultProject {
  private lazy val pluginLibDir = rootProject.info.pluginsManagedDependencyPath / String.format("scala_%s", defScalaVersion.value)
  def coverageSelfTest = false
  def scctPluginJar = testClasspath ** "scct_2.8.0.RC7-1.0.jar"

  def testRuntimeScctPluginJar = if (!coverageSelfTest) scctPluginJar else outputPath / "scct-xml.jar"
  def instrumentedClassDir = outputPath / "coverage-classes"
  def reportDir = outputPath / "coverage-report"

  class InstrumentCompileConfig extends MainCompileConfig {
    override def label = "coverage"
    override def outputDirectory = instrumentedClassDir
    override def analysisPath = outputPath / "coverage-analysis"
    override def classpath = scctPluginJar +++ super.classpath
    override def baseCompileOptions = coverageCompileOption :: super.baseCompileOptions.toList
    def coverageCompileOption = CompileOption("-Xplugin:"+scctPluginJar.get.mkString)
  }
  class InstrumentedTestCompileConfig extends TestCompileConfig {
    override def classpath = scctPluginJar +++ instrumentedClassDir +++ (super.classpath --- mainCompilePath)
    override def analysisPath = outputPath / "coverage-test-analysis"
  }

  override def cleanOptions =
    ClearAnalysis(instrumentCompileConditional.analysis) ::
    ClearAnalysis(instrumentTestCompileConditional.analysis) :: (super.cleanOptions).toList

  def instrumentTestCompileConfiguration = new InstrumentedTestCompileConfig
  def instrumentCompileConfiguration = new InstrumentCompileConfig
  lazy val instrumentTestCompileConditional = new CompileConditional(instrumentTestCompileConfiguration, buildCompiler)
  lazy val instrumentCompileConditional = new CompileConditional(instrumentCompileConfiguration, buildCompiler)

  protected def xmlJarAction = task {
    if (coverageSelfTest) {
      FileUtilities.jar(((mainResources / "scalac-plugin.xml").get), outputPath / "scct-xml.jar", new Manifest(), false, log)
    }
    None
  }
  protected def instrumentAction =
    task { instrumentCompileConditional.run } dependsOn xmlJar
  protected def testCoverageCompileAction =
    task { instrumentTestCompileConditional.run } dependsOn instrument
  protected def instrumentedTestRunClassPath =
    testRuntimeScctPluginJar +++ instrumentedClassDir +++ (testClasspath --- mainCompilePath)
  protected def instrumentedTestOptions =
    testOptions

  protected def testCoverageAction =
    testTask(testFrameworks, instrumentedTestRunClassPath, instrumentTestCompileConditional.analysis, instrumentedTestOptions).dependsOn(testCoverageCompile, copyResources, copyTestResources, setupCoverageEnv)

  lazy val xmlJar = xmlJarAction
  lazy val instrument = instrumentAction
  lazy val testCoverage = testCoverageAction
  lazy val testCoverageCompile = testCoverageCompileAction
  lazy val setupCoverageEnv = task {
    if (reportDir.exists) FileUtilities.clean(reportDir, log)
    FileUtilities.createDirectory(reportDir, log)
    if (coverageSelfTest) System.setProperty("scct-self-test", true.toString)
    System.setProperty("scct.report.dir", reportDir.toString)
    System.setProperty("scct.src.reference.dir", mainScalaSourcePath.absolutePath)
    None
  }

}