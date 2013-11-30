package com.sksamuel.scoverage

import sbt._
import sbt.Keys._
import sbt.File
import scoverage.{IOUtils, Env}
import scoverage.report.{ScoverageXmlWriter, CoberturaXmlWriter}

object ScoverageSbtPlugin extends Plugin {

  val scoverageReportDir = SettingKey[File]("scoverage-report-dir")

  lazy val scoverage = config("scoverage")
  lazy val scoverageTest = config("scoverage-test") extend scoverage

  lazy val instrumentSettings = {
    inConfig(scoverage)(Defaults.compileSettings) ++
      inConfig(scoverageTest)(Defaults.testSettings) ++
      Seq(
        scoverageReportDir <<= crossTarget / "coverage-report",

        ivyConfigurations ++= Seq(scoverage, scoverageTest),

        resolvers += Resolver.url("local-ivy",
          new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),

        libraryDependencies +=
          "com.sksamuel.scoverage" %% "scalac-scoverage-plugin" % "0.92.0-SNAPSHOT" % scoverage.name,

        sources in scoverage <<= (sources in Compile),
        sourceDirectory in scoverage <<= (sourceDirectory in Compile),

        scalacOptions in scoverage <++= (name in scoverage, baseDirectory in scoverage, update) map {
          (n, b, report) =>
            val scoverageDeps = report matching configurationFilter("scoverage")
            scoverageDeps.find(_.getAbsolutePath.contains("scalac-scoverage-plugin")) match {
              case None => throw new Exception("Fatal: scalac-scoverage-plugin not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-Yrangepos"
                )
            }
        },

        sources in scoverageTest <<= (sources in Test),
        sourceDirectory in scoverageTest <<= (sourceDirectory in Test),
        unmanagedResources in scoverageTest <<= (unmanagedResources in Test),

        resourceDirectory in scoverageTest <<= (resourceDirectory in Compile),

        externalDependencyClasspath in scoverage <<= Classpaths
          .concat(externalDependencyClasspath in scoverage, externalDependencyClasspath in Compile),
        externalDependencyClasspath in scoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in scoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in scoverage <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in scoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in scoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in scoverageTest <+= testsCleanup,

        // make scoverage config the same as scoverageTest config
        test in scoverage <<= (test in scoverageTest)
      )
  }

  /** Generate hook that is invoked after each tests execution. */
  def testsCleanup = {
    (target in scoverageTest,
      definedTests in scoverageTest,
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

              println("Generating Cobertura XML report...")
              CoberturaXmlWriter.write(coverage, targetDirectory)

              println("Generating Scoverage XML report...")
              ScoverageXmlWriter.write(coverage, targetDirectory)

              println("Generating Scoverage HTML report...")
              ScoverageXmlWriter.write(coverage, targetDirectory)

              ()
          }
        }
    }
  }
}
