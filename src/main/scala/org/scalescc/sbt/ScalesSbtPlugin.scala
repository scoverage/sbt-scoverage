package org.scalescc.sbt

import java.util.Properties
import sbt._
import sbt.Keys._
import xml.transform._
import scales.{Env, IOUtils}
import scales.reporters.{ScalesHtmlWriter, CoberturaXmlWriter, ScalesXmlWriter}

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

        testOptions in scalesTest <+= testsSetup,
        testOptions in scalesTest <+= testsCleanup,

        // make scales config the same as scalesTest config
        test in scales <<= (test in scalesTest),

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
  def testsSetup = {
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
        if (definedTests.isEmpty) {
          Tests.Setup {
            () => {}
          }
        } else
          Tests.Setup {
            () =>
              val out = classDirectory / "scales.properties"
              val props = new Properties()
              props.setProperty("scales.basedir", baseDirectory.getAbsolutePath)
              IO.write(props, "Env for scales", out)
          }
    }
  }

  /** Generate hook that is invoked after each tests execution. */
  def testsCleanup = {
    (name in scales,
      classDirectory in scalesTest,
      definedTests in scalesTest,
      streams) map {
      (name,
       classDirectory,
       definedTests,
       streams) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else
          Tests.Cleanup {
            () =>

              val coverage = IOUtils.deserialize(Env.coverageFile)
              val measurements = IOUtils.invoked(Env.measurementFile)

              coverage.apply(measurements)

              val targetDirectory = new File("target/coverage-report")
              targetDirectory.mkdirs()

              println("Generating ScalesXML report...")
              ScalesXmlWriter.write(coverage, targetDirectory)

              println("Generating CoberturaXML report...")
              CoberturaXmlWriter.write(coverage, targetDirectory)

              println("Generating Scales HTML report...")
              ScalesHtmlWriter.write(coverage, targetDirectory)

              ()
          }
    }
  }
}
