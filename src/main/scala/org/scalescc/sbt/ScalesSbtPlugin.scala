package org.scalescc.sbt

import sbt._
import sbt.Keys._
import scales._
import sbt.File
import scales.report.{ScalesHtmlWriter, CoberturaXmlWriter, ScalesXmlWriter}
import scala.Some

object ScalesSbtPlugin extends Plugin {

  val scalesReportDir = SettingKey[File]("scales-report-dir")

  lazy val Scales = config("scales")
  lazy val ScalesTest = config("scales-test") extend Scales

  lazy val instrumentSettings = {
    inConfig(Scales)(Defaults.compileSettings) ++
      inConfig(ScalesTest)(Defaults.testSettings) ++
      Seq(
        scalesReportDir <<= crossTarget / "coverage-report",

        ivyConfigurations ++= Seq(Scales, ScalesTest),

        resolvers += Resolver.url("local-ivy",
          new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),

        libraryDependencies += "org.scalescc" %% "scalac-scales-plugin" % "0.11.0-SNAPSHOT" % Scales.name,

        sources in Scales <<= (sources in Compile),
        sourceDirectory in Scales <<= (sourceDirectory in Compile),

        scalacOptions in Scales <++= (name in Scales, baseDirectory in Scales, update) map {
          (n, b, report) =>
            val scalesDeps = report matching configurationFilter("scales")
            scalesDeps.find(_.getAbsolutePath.contains("scalac-scales-plugin")) match {
              case None => throw new Exception("Fatal: scalac-scales-plugin not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-Yrangepos"
                )
            }
        },

        sources in ScalesTest <<= (sources in Test),
        sourceDirectory in ScalesTest <<= (sourceDirectory in Test),
        unmanagedResources in ScalesTest <<= (unmanagedResources in Test),

        resourceDirectory in ScalesTest <<= (resourceDirectory in Compile),

        externalDependencyClasspath in Scales <<= Classpaths
          .concat(externalDependencyClasspath in Scales, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScalesTest <<= Classpaths
          .concat(externalDependencyClasspath in ScalesTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in Scales <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScalesTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScalesTest, classDirectory in Compile) map {
          (testDeps, scalesDeps, oldClassDir) =>
            scalesDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in ScalesTest <+= testsCleanup,

        // make scales config the same as scalesTest config
        test in Scales <<= (test in ScalesTest)
      )
  }

  /** Generate hook that is invoked after each tests execution. */
  def testsCleanup = {
    (target in ScalesTest,
      definedTests in ScalesTest,
      streams) map {
      (target,
       definedTests,
       streams) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else {
          Tests.Cleanup {
            () =>

              println(target)
              println(Env.coverageFile)
              println(Env.measurementFile)

              val coverage = IOUtils.deserialize(getClass.getClassLoader, Env.coverageFile)
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
          }
        }
    }
  }
}
