package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._
import scala.language.postfixOps

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  // This version number should match that imported in build.sbt
  val ScoverageGroupId = "org.scoverage"
  val ScalacScoverageVersion = "0.98.2"
  val ScalacScoverageArtifact = "scalac-scoverage-plugin"

  object ScoverageKeys {
    val scoverageVersion = SettingKey[String]("scoverage-version")
    val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
  }

  import ScoverageKeys._

  lazy val ScoverageCompile = config("scoverage")
  lazy val ScoverageTest = config("scoverage-test") extend ScoverageCompile

  lazy val instrumentSettings = {
    inConfig(ScoverageCompile)(Defaults.compileSettings) ++
    inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(ScoverageCompile hide, ScoverageTest hide),

        libraryDependencies += ScoverageGroupId %% ScalacScoverageArtifact % ScalacScoverageVersion % ScoverageCompile.name,

        sources in ScoverageCompile <<= (sources in Compile),
        sourceDirectory in ScoverageCompile <<= (sourceDirectory in Compile),
        resourceDirectory in ScoverageCompile <<= (resourceDirectory in Compile),
        excludedPackages in ScoverageCompile := "",

        scalacOptions in ScoverageCompile <++= (name in ScoverageCompile,
          baseDirectory in ScoverageCompile,
          crossTarget in ScoverageTest,
          update,
          excludedPackages in ScoverageCompile) map {
          (n, b, target, report, excluded) =>
            val scoverageDeps = report matching configurationFilter(ScoverageCompile.name)
            scoverageDeps.find(_.getAbsolutePath.contains(ScalacScoverageArtifact)) match {
              case None => throw new Exception(s"Fatal: $ScalacScoverageArtifact not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-Yrangepos",
                  "-P:scoverage:excludedPackages:" + Option(excluded).getOrElse(""),
                  "-P:scoverage:dataDir:" + target
                )
            }
        },

        sources in ScoverageTest <<= (sources in Test),
        sourceDirectory in ScoverageTest <<= (sourceDirectory in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),

        externalDependencyClasspath in ScoverageCompile <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageCompile, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in ScoverageCompile <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in ScoverageTest <+= testsCleanup,

        // make scoverage config the same as scoverageTest config
        test in ScoverageCompile <<= (test in ScoverageTest)
      )
  }

  /** Generate hook that is invoked after each tests execution. */
  def testsCleanup = {
    (crossTarget in ScoverageTest,
      baseDirectory in Compile,
      scalaSource in Compile,
      definedTests in ScoverageTest,
      streams in Global) map {
      (crossTarget,
       baseDirectory,
       compileSourceDirectory,
       definedTests,
       streams) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else {
          Tests.Cleanup {
            () =>

              val coverageFile = IOUtils.coverageFile(crossTarget)
              val measurementFiles = IOUtils.findMeasurementFiles(crossTarget)

              streams.log.info(s"Reading scoverage profile file [$coverageFile]")
              streams.log.info(s"Reading scoverage measurement files [$measurementFiles]")

              val coverage = IOUtils.deserialize(getClass.getClassLoader, coverageFile)
              val measurements = IOUtils.invoked(measurementFiles)
              coverage.apply(measurements)

              coverageFile.delete()
              for ( file <- measurementFiles ) file.delete()

              val coberturaDirectory = crossTarget / "coverage-report"
              val scoverageDirectory = crossTarget / "scoverage-report"

              coberturaDirectory.mkdirs()
              scoverageDirectory.mkdirs()

              streams.log.info("Generating Cobertura XML report...")
              new CoberturaXmlWriter(baseDirectory, coberturaDirectory).write(coverage)

              streams.log.info("Generating Scoverage XML report...")
              new ScoverageXmlWriter(compileSourceDirectory, scoverageDirectory, false).write(coverage)

              streams.log.info("Generating Scoverage Debug report...")
              new ScoverageXmlWriter(compileSourceDirectory, scoverageDirectory, true).write(coverage)

              streams.log.info("Generating Scoverage HTML report...")
              new ScoverageHtmlWriter(compileSourceDirectory, scoverageDirectory).write(coverage)

              streams.log.info("Scoverage reports completed")
              ()
          }
        }
    }
  }
}
