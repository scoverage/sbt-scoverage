package org.scalescc.sbt

import sbt._
import sbt.Keys._
import scales._
import sbt.File
import scales.report.{ScalesHtmlWriter, CoberturaXmlWriter, ScalesXmlWriter}

object ScalesSbtPlugin extends Plugin {

  val scalesReportDir = SettingKey[File]("scoot-report-dir")

  lazy val Scales = config("scoot")
  lazy val ScalesTest = config("scoot-test") extend Scales

  lazy val instrumentSettings = {
    inConfig(Scales)(Defaults.compileSettings) ++
      inConfig(ScalesTest)(Defaults.testSettings) ++
      Seq(
        scalesReportDir <<= crossTarget / "coverage-report",

        ivyConfigurations ++= Seq(Scales, ScalesTest),

        resolvers += Resolver.url("local-ivy",
          new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),

        libraryDependencies += "com.sksamuel.scoot" %% "scalac-scoot-plugin" % "0.91.0" % Scales.name,

        sources in Scales <<= (sources in Compile),
        sourceDirectory in Scales <<= (sourceDirectory in Compile),

        scalacOptions in Scales <++= (name in Scales, baseDirectory in Scales, update) map {
          (n, b, report) =>
            val scalesDeps = report matching configurationFilter("scoot")
            scalesDeps.find(_.getAbsolutePath.contains("scalac-scoot-plugin")) match {
              case None => throw new Exception("Fatal: scalac-scoot-plugin not in libraryDependencies")
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

              println("Generating ScootXML report...")
              ScalesXmlWriter.write(coverage, targetDirectory)

              println("Generating CoberturaXML report...")
              CoberturaXmlWriter.write(coverage, targetDirectory)

              println("Generating ScootHTML report...")
              ScalesHtmlWriter.write(coverage, targetDirectory)
          }
        }
    }
  }
}
