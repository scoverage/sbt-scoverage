package org.scalescc.sbt

import java.util.Properties
import sbt._
import sbt.Keys._
import xml.transform._
import scales.ScalesPlugin

object ScalesSbtPlugin extends Plugin {

  val scalesReportDir = SettingKey[File]("scales-report-dir")

  val scalesExcludePackages = SettingKey[String]("scales-exclude-package")

  lazy val scales = config("scales")
  lazy val scalesTest = config("scales-test") extend scales

  lazy val instrumentSettings =
    inConfig(scales)(Defaults.compileSettings) ++
      inConfig(scalesTest)(Defaults.testSettings) ++
      Seq(
        scalesReportDir <<= crossTarget / "coverage-report",
        scalesExcludePackages <<= scalesExcludePackages ?? "",

        ivyConfigurations ++= Seq(scales, scalesTest),

        //resolvers += Resolver.url("local-ivy", new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
        resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/snapshots",

        libraryDependencies += "org.scalescc" %% "scalac-scales-plugin" % "0.1.0-SNAPSHOT" % "scales",

        sources in scales <<= (sources in Compile),
        sourceDirectory in scales <<= (sourceDirectory in Compile),

        scalacOptions in scales <++= (name in scales, baseDirectory in scales, update) map {
          (n, b, report) =>
            val pluginClasspath = report matching configurationFilter("scales")
            if (pluginClasspath.isEmpty)
              throw new Exception("Fatal: scales not in libraryDependencies. Use e.g. <+= or <++= instead of <<=")
            Seq(
              "-Xplugin:" + pluginClasspath.head.getAbsolutePath,
              "-P:scales:projectId:" + n,
              "-P:scales:basedir:" + b
            )
        },

        sources in scalesTest <<= (sources in Test),
        sourceDirectory in scalesTest <<= (sourceDirectory in Test),
        unmanagedResources in scalesTest <<= (unmanagedResources in Test),

        resourceDirectory in scalesTest <<= (resourceDirectory in Compile),

        externalDependencyClasspath in scales <<= Classpaths
          .concat(externalDependencyClasspath in scales, externalDependencyClasspath in Compile),
        externalDependencyClasspath in scalesTest <<= Classpaths
          .concat(externalDependencyClasspath in scalesTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in scales <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in scalesTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in scalesTest, classDirectory in Compile) map {
          (testDeps, scalesDeps, oldClassDir) =>
            scalesDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in scalesTest <+= testSetup,
        testOptions in scalesTest <+= testCleanup,

        // Sugar: copy test from scalesTest to scales so you can use scales:test
        Keys.test in scales <<= (Keys.test in scalesTest),

        // filter scales dependencies for publishing
        pomPostProcess := {
          (node: xml.Node) => pomTransformer(node)
        }
      )

  val pomTransformer = new RuleTransformer(new RewriteRule {
    override def transform(node: xml.Node): Seq[xml.Node] = node match {
      case e: xml.Elem if e.label == "dependency" =>
        if ((e \ "scope" text) == "scales") Nil else Seq(e)
      case e: xml.Elem if e.label == "repository" =>
        if ((e \ "name" text) == "scales-repository") Nil else Seq(e)
      case e => Seq(e)
    }
  })

  object scalesMergeReportKeys {
    val sourceFiles = TaskKey[Seq[File]]("scales-merge-report-source-files")
    val merge = TaskKey[File]("scales-merge-report")
  }

  import scalesMergeReportKeys._

  val mergeReportSettings = Seq(
    scalesReportDir <<= crossTarget / "coverage-report",
    sourceFiles <<= (thisProjectRef, buildStructure) map coverageResultFiles,
    merge <<= (sourceFiles, scalesReportDir) map generateReport
  )

  def coverageResultFiles(projectRef: ProjectRef, structure: Load.BuildStructure) = {
    val projects = aggregated(projectRef, structure)
    projects flatMap {
      p =>
        val dir = (scalesReportDir in scalesTest in LocalProject(p)).get(structure.data)
        dir.flatMap {
          d =>
            val f = new File(d, "coverage-result.data")
            if (f.exists) Some(f) else None
        }
    }
  }

  def generateReport(input: Seq[File], out: File) = out

  def aggregated(projectRef: ProjectRef, structure: Load.BuildStructure): Seq[String] = {
    val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
    aggregate flatMap {
      ref =>
        ref.project +: aggregated(ref, structure)
    }
  }
  /** Generate hook that is invoked before each tests execution. */
  def testSetup() =
    (name in scales, baseDirectory in scales, scalaSource in scales, classDirectory in scalesTest, scalesExcludePackages in scalesTest, definedTests in scalesTest, scalesReportDir, streams) map {
      (name,
       baseDirectory,
       scalaSource,
       classDirectory,
       scalesExcludePackages,
       definedTests,
       scalesReportDir,
       streams) =>
        if (definedTests.isEmpty) {
          streams.log.debug(logPrefix(name) + "No tests found. Skip scales setup hook.")
          Tests.Setup {
            () => {}
          }
        } else
          Tests.Setup {
            () =>
              val out = classDirectory / "scales.properties"
              streams.log.debug(logPrefix(name) + "Prepare scales environment at %s".format(out.getCanonicalPath()))
              val props = new Properties()
              props.setProperty("scales.basedir", baseDirectory.getAbsolutePath)
              props.setProperty("scales.report.hook", "system.property")
              props.setProperty("scales.project.name", name)
              props.setProperty("scales.report.dir", scalesReportDir.getAbsolutePath)
              props.setProperty("scales.source.dir", scalaSource.getAbsolutePath)
              props.setProperty("scales.excluded.paths.regex", scalesExcludePackages.configuration.getOrElse(""))
              IO.write(props, "Env for scales test run and report generation", out)
          }
    }
  /** Generate hook that is invoked after each tests execution. */
  def testCleanup() =
    (name in scales, classDirectory in scalesTest, definedTests in scalesTest, streams) map {
      (name, classDirectory, definedTests, streams) =>
        if (definedTests.isEmpty) {
          streams.log.debug(logPrefix(name) + "No tests found. Skip scales cleanup hook.")
          Tests.Cleanup {
            () => {}
          }
        } else
          Tests.Cleanup {
            () =>
              streams.log.debug(logPrefix(name) + "Waiting for coverage report generation.")
              val out = classDirectory / "scales.properties"
              val reportProperty = "scales.%s.fire.report".format(name)
              System.setProperty(reportProperty, "true")
              val maxSleep = compat.Platform.currentTime + 60L * 1000L
              while (sys.props(reportProperty) != "done" && compat.Platform.currentTime < maxSleep) Thread.sleep(200L)
              if (sys.props(reportProperty) != "done") streams
                .log
                .debug(logPrefix(name) + "Timed out waiting for coverage report.")
              out.delete
              streams.log.debug(logPrefix(name) + "Cleanup scales environment")
          }
    }
  def logPrefix(name: String) = "scales: [" + name + "] "
}
