package org.scalescc.sbt

import java.util.Properties
import sbt._
import sbt.Keys._
import xml.transform._

object ScalesSbtPlugin extends Plugin {

  val scalesReportDir = SettingKey[File]("scales-report-dir")

  lazy val scales = config("scales")
  lazy val scalesTest = config("scales-test") extend scales

  lazy val instrumentSettings =
    inConfig(scales)(Defaults.compileSettings) ++
      inConfig(scalesTest)(Defaults.testSettings) ++
      Seq(
        scalesReportDir <<= crossTarget / "coverage-report",

        ivyConfigurations ++= Seq(scales, scalesTest),

        resolvers += Resolver.url("local-ivy",
          new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),

        libraryDependencies += "org.scalescc" %% "scalac-scales-plugin" % "0.1.0-SNAPSHOT" % "scales",

        sources in scales <<= (sources in Compile),
        sourceDirectory in scales <<= (sourceDirectory in Compile),

        scalacOptions in scales <++= (name in scales, baseDirectory in scales, update) map {
          (n, b, report) =>
            val scalesDeps = report matching configurationFilter("scales")
            scalesDeps.find(_.getAbsolutePath.contains("scalac-scales-plugin")) match {
              case None => throw new Exception("Fatal: scalac-scales-plugin not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath
                  //     "-P:scales:projectId:" + n,
                  //    "-P:scales:basedir:" + b
                )
            }
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
          (node: xml.Node) => filterPomForScalesDeps(node)
        }
      )

  val filterPomForScalesDeps = new RuleTransformer(new RewriteRule {
    override def transform(node: xml.Node): Seq[xml.Node] = node match {
      case e: xml.Elem if e.label == "dependency" => if ((e \ "scope" text) == "scalac-scales-plugin") Nil else Seq(e)
      case e: xml.Elem if e.label == "repository" => if ((e \ "name" text) == "scales-repository") Nil else Seq(e)
      case e => Seq(e)
    }
  })

  /** Generate hook that is invoked before each tests execution. */
  def testSetup() = {
    (name in scales,
      baseDirectory in scales,
      scalaSource in scales,
      classDirectory in scalesTest,
      definedTests in scalesTest,
      scalesReportDir,
      streams) map {
      (name,
       baseDirectory,
       scalaSource,
       classDirectory,
       definedTests,
       scalesReportDir,
       streams) =>
        println("SCALES !! BEOFRE TEST !!!")
        if (definedTests.isEmpty) {
          streams.log.debug(logPrefix(name) + "No tests found. Skip scales setup hook.")
          Tests.Setup {
            () => {}
          }
        } else
          Tests.Setup {
            () =>
              val out = classDirectory / "scales.properties"
              streams.log.debug(logPrefix(name) + "Prepare scales environment at %s".format(out.getCanonicalPath))
              val props = new Properties()
              props.setProperty("scales.basedir", baseDirectory.getAbsolutePath)
              props.setProperty("scales.report.hook", "system.property")
              props.setProperty("scales.project.name", name)
              props.setProperty("scales.report.dir", scalesReportDir.getAbsolutePath)
              props.setProperty("scales.source.dir", scalaSource.getAbsolutePath)
              IO.write(props, "Env for scales test run and report generation", out)
          }
    }
  }

  /** Generate hook that is invoked after each tests execution. */
  def testCleanup() = {
    (name in scales,
      classDirectory in scalesTest,
      definedTests in scalesTest,
      streams) map {
      (name,
       classDirectory,
       definedTests,
       streams) =>
        println("SCALES !! AFTER TEST !!!")
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
              if (sys.props(reportProperty) != "done")
                streams.log.debug(logPrefix(name) + "Timed out waiting for coverage report.")
              out.delete
              streams.log.debug(logPrefix(name) + "Cleanup scales environment")
          }
    }
  }

  def logPrefix(name: String) = "scales: [" + name + "] "
}
