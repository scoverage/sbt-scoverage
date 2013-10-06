package sbt.scct

import java.util.Properties
import sbt._
import sbt.Keys._
import xml.transform._

object ScctPlugin extends Plugin {

  val scctReportDir = SettingKey[File]("scct-report-dir")

  lazy val Scct = config("scct")
  lazy val ScctTest = config("scct-test") extend Scct

  lazy val instrumentSettings =
    inConfig(Scct)(Defaults.compileSettings) ++
    inConfig(ScctTest)(Defaults.testSettings) ++
    Seq(
      scctReportDir <<= crossTarget / "coverage-report",

      ivyConfigurations ++= Seq(Scct, ScctTest),

      resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/snapshots",

      libraryDependencies += "com.github.scct" %% "scct" % "0.3-SNAPSHOT" % "scct",

      sources in Scct <<= (sources in Compile),
      sourceDirectory in Scct <<= (sourceDirectory in Compile),

      scalacOptions in Scct <++= (name in Scct, baseDirectory in Scct, update) map { (n, b, report) =>
        val pluginClasspath = report matching configurationFilter("scct")
        if (pluginClasspath.isEmpty) throw new Exception("Fatal: scct not in libraryDependencies. Use e.g. <+= or <++= instead of <<=")
        Seq(
          "-Xplugin:" + pluginClasspath.head.getAbsolutePath,
          "-P:scct:projectId:" + n,
          "-P:scct:basedir:" + b,
          "-P:scct:excludePackages:"
        )
      },

      sources in ScctTest <<= (sources in Test),
      sourceDirectory in ScctTest <<= (sourceDirectory in Test),

      resourceDirectory in ScctTest <<= (resourceDirectory in Compile),

      externalDependencyClasspath in Scct <<= Classpaths.concat(externalDependencyClasspath in Scct, externalDependencyClasspath in Compile),
      externalDependencyClasspath in ScctTest <<= Classpaths.concat(externalDependencyClasspath in ScctTest, externalDependencyClasspath in Test),

      internalDependencyClasspath in Scct <<= (internalDependencyClasspath in Compile),
      internalDependencyClasspath in ScctTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScctTest, classDirectory in Compile) map { (testDeps, scctDeps, oldClassDir) =>
        scctDeps ++ testDeps.filter(_.data != oldClassDir)
      },

      testOptions in ScctTest <+= testSetup,
      testOptions in ScctTest <+= testCleanup,

      // Sugar: copy test from ScctTest to Scct so you can use scct:test
      Keys.test in Scct <<= (Keys.test in ScctTest),

      // filter scct dependencies for publishing
      pomPostProcess := { (node: xml.Node) => pomTransformer(node) }
    )

  val pomTransformer = new RuleTransformer(new RewriteRule {
    override def transform(node: xml.Node): Seq[xml.Node] = node match {
      case e: xml.Elem if e.label == "dependency" =>
        if ((e \ "scope" text)  == "scct") Nil else Seq(e)
      case e: xml.Elem if e.label == "repository" =>
        if ((e \ "name" text) == "scct-repository") Nil else Seq(e)
      case e => Seq(e)
    }
  })

  def scctJarPath = {
    val url = classOf[com.github.scct.ScctInstrumentPlugin].getProtectionDomain().getCodeSource().getLocation()
    new File(url.toURI).getAbsolutePath
  }

  // Report mergin':

  object ScctMergeReportKeys {
    val sourceFiles = TaskKey[Seq[File]]("scct-merge-report-source-files")
    val merge = TaskKey[File]("scct-merge-report")
  }
  import ScctMergeReportKeys._

  val mergeReportSettings = Seq(
    scctReportDir <<= crossTarget / "coverage-report",
    sourceFiles <<= (thisProjectRef, buildStructure) map coverageResultFiles,
    merge <<= (sourceFiles, scctReportDir) map generateReport
  )

  def coverageResultFiles(projectRef: ProjectRef, structure: Load.BuildStructure) = {
    val projects = aggregated(projectRef, structure)
    projects flatMap { p =>
      val dir = (scctReportDir in ScctTest in LocalProject(p)).get(structure.data)
      dir.flatMap { d =>
        val f = new File(d, "coverage-result.data")
        if (f.exists) Some(f) else None
      }
    }
  }

  def generateReport(input: Seq[File], out: File) = {
    import com.github.scct.report._
    MultiProjectHtmlReporter.report(input, out)
    out
  }

  def aggregated(projectRef: ProjectRef, structure: Load.BuildStructure): Seq[String] = {
    val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
    aggregate flatMap { ref =>
      ref.project +: aggregated(ref, structure)
    }
  }
  /** Generate hook that is invoked before each tests execution. */
  def testSetup() =
    (name in Scct, baseDirectory in Scct, scalaSource in Scct, classDirectory in ScctTest, definedTests in ScctTest, scctReportDir, streams) map {
      (name, baseDirectory, scalaSource, classDirectory, definedTests, scctReportDir, streams) =>
        if (definedTests.isEmpty) {
          streams.log.debug(logPrefix(name) + "No tests found. Skip SCCT setup hook.")
          Tests.Setup { () => {} }
        } else
          Tests.Setup { () =>
            val out = classDirectory / "scct.properties"
            streams.log.debug(logPrefix(name) + "Prepare SCCT environment at %s".format(out.getCanonicalPath()))
            val props = new Properties()
            props.setProperty("scct.basedir", baseDirectory.getAbsolutePath)
            props.setProperty("scct.report.hook", "system.property")
            props.setProperty("scct.project.name", name)
            props.setProperty("scct.report.dir", scctReportDir.getAbsolutePath)
            props.setProperty("scct.source.dir", scalaSource.getAbsolutePath)
            IO.write(props, "Env for scct test run and report generation", out)
          }
    }
  /** Generate hook that is invoked after each tests execution. */
  def testCleanup() =
    (name in Scct, classDirectory in ScctTest, definedTests in ScctTest, streams) map {
      (name, classDirectory, definedTests, streams) =>
        if (definedTests.isEmpty) {
          streams.log.debug(logPrefix(name) + "No tests found. Skip SCCT cleanup hook.")
          Tests.Cleanup { () => {} }
        } else
          Tests.Cleanup { () =>
            streams.log.debug(logPrefix(name) + "Waiting for coverage report generation.")
            val out = classDirectory / "scct.properties"
            val reportProperty = "scct.%s.fire.report".format(name)
            System.setProperty(reportProperty, "true")
            val maxSleep = compat.Platform.currentTime + 60L * 1000L
            while (sys.props(reportProperty) != "done" && compat.Platform.currentTime < maxSleep) Thread.sleep(200L)
            if (sys.props(reportProperty) != "done") streams.log.debug(logPrefix(name) + "Timed out waiting for coverage report.")
            out.delete
            streams.log.debug(logPrefix(name) + "Cleanup SCCT environment")
          }
    }
  def logPrefix(name: String) = "scct: [" + name + "] "
}
