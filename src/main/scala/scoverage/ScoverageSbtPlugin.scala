package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._
import scala.language.postfixOps

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  // This version number should match that imported in build.sbt
  val ScalacScoverageArtifact = "scalac-scoverage-plugin"

  object ScoverageKeys {
    val scoverageVersion = SettingKey[String]("scoverage-version")
    val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
  }

  import ScoverageKeys._

  val ScoverageCompile: Configuration = config("scoverage") extend Compile
  val ScoverageTest: Configuration = config("scoverage-test") extend ScoverageCompile

  val instrumentSettings: Seq[Setting[_]] = {
    inConfig(ScoverageCompile)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(ScoverageCompile hide, ScoverageTest hide),
        libraryDependencies += organization.value %% ScalacScoverageArtifact % version.value % ScoverageCompile.name,

        sources in ScoverageCompile <<= (sources in Compile),
        sourceDirectory in ScoverageCompile <<= (sourceDirectory in Compile),
        resourceDirectory in ScoverageCompile <<= (resourceDirectory in Compile),
        resourceGenerators in ScoverageCompile <<= (resourceGenerators in Compile),
        excludedPackages in ScoverageCompile := "",
        javacOptions in ScoverageCompile <<= (javacOptions in Compile),
        javaOptions in ScoverageCompile <<= (javaOptions in Compile),

        scalacOptions in ScoverageCompile <++= (crossTarget in ScoverageTest, update, excludedPackages in ScoverageCompile) map {
          (target, report, excluded) =>
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
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),
        resourceGenerators in ScoverageTest <<= (resourceGenerators in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        javacOptions in ScoverageTest <<= (javacOptions in Test),
        javaOptions in ScoverageTest <<= (javaOptions in Test),

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
       s) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else {
          Tests.Cleanup {
            () =>

              val coverageFile = IOUtils.coverageFile(crossTarget)
              val measurementFiles = IOUtils.findMeasurementFiles(crossTarget)

              s.log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
              s.log.info(s"[scoverage] Reading scoverage measurements [${measurementFiles.toList}]")

              val coverage = IOUtils.deserialize(coverageFile)
              val measurements = IOUtils.invoked(measurementFiles)
              coverage.apply(measurements)

              coverageFile.delete()
              for ( file <- measurementFiles ) file.delete()

              val coberturaDirectory = crossTarget / "coverage-report"
              val scoverageDirectory = crossTarget / "scoverage-report"

              coberturaDirectory.mkdirs()
              scoverageDirectory.mkdirs()

              s.log.info(s"[scoverage] Generating Cobertura report [${coberturaDirectory.getAbsolutePath}/cobertura.xml]")
              new CoberturaXmlWriter(baseDirectory, coberturaDirectory).write(coverage)

              s.log.info(s"[scoverage] Generating XML report [${scoverageDirectory.getAbsolutePath}/scoverage.xml]")
              new ScoverageXmlWriter(compileSourceDirectory, scoverageDirectory, false).write(coverage)

              new ScoverageXmlWriter(compileSourceDirectory, scoverageDirectory, true).write(coverage)

              s.log.info(s"[scoverage] Generating XML report [${scoverageDirectory.getAbsolutePath}/index.html]")
              new ScoverageHtmlWriter(compileSourceDirectory, scoverageDirectory).write(coverage)

              s.log.info("[scoverage] Reports completed")
              ()
          }
        }
    }
  }
}
