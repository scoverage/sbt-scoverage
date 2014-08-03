package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  val OrgScoverage = "org.scoverage"
  val ScalacArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "0.99.9"

  object ScoverageKeys {
    val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
    val minimumCoverage = SettingKey[Double]("scoverage-minimum-coverage")
    val failOnMinimumCoverage = SettingKey[Boolean]("scoverage-fail-on-minimum-coverage")
    val highlighting = SettingKey[Boolean]("scoverage-highlighting", "enables range positioning for highlighting")
  }

  import ScoverageKeys._

  lazy val ScoverageCompile: Configuration = config("scoverage")
  lazy val ScoverageTest: Configuration = config("scoverage-test") extend ScoverageCompile

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(ScoverageCompile)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(ScoverageCompile.hide, ScoverageTest.hide),
        libraryDependencies += {
          OrgScoverage % (ScalacArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % ScoverageCompile.name
        },
        sources in ScoverageCompile <<= (sources in Compile),
        sourceDirectory in ScoverageCompile <<= (sourceDirectory in Compile),
        resourceDirectory in ScoverageCompile <<= (resourceDirectory in Compile),
        resourceGenerators in ScoverageCompile <<= (resourceGenerators in Compile),
        excludedPackages in ScoverageCompile := "",
        javacOptions in ScoverageCompile <<= (javacOptions in Compile),
        javaOptions in ScoverageCompile <<= (javaOptions in Compile),

        minimumCoverage := 0, // default is no minimum
        failOnMinimumCoverage := false,
        highlighting := false,

        scalacOptions in ScoverageCompile <++= (crossTarget in ScoverageTest, update, excludedPackages in ScoverageCompile) map {
          (target, report, excluded) =>
            val scoverageDeps = report matching configurationFilter(ScoverageCompile.name)
            scoverageDeps.find(_.getAbsolutePath.contains(ScalacArtifact)) match {
              case None => throw new Exception(s"Fatal: $ScalacArtifact not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-P:scoverage:excludedPackages:" + Option(excluded).getOrElse(""),
                  "-P:scoverage:dataDir:" + target.getAbsolutePath + "/scoverage-data"
                )
            }
        },

        scalacOptions in ScoverageCompile ++= (if (highlighting.value) List("-Yrangepos") else Nil),

        sources in ScoverageTest <<= (sources in Test),
        sourceDirectory in ScoverageTest <<= (sourceDirectory in Test),
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),
        resourceGenerators in ScoverageTest <<= (resourceGenerators in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        javacOptions in ScoverageTest <<= (javacOptions in Test),
        javaOptions in ScoverageTest <<= (javaOptions in Test),
        fork in ScoverageTest <<= (fork in Test),

        externalDependencyClasspath in ScoverageCompile <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageCompile, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in ScoverageCompile <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in ScoverageTest <<= (testOptions in Test),
        testOptions in ScoverageTest <+= testsCleanup,

        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test
        test in ScoverageCompile <<= (test in ScoverageTest)
      )
  }

  /** Generate hook that is invoked after the tests have executed. */
  def testsCleanup = {
    (crossTarget in ScoverageTest,
      baseDirectory in Compile,
      scalaSource in Compile,
      definedTests in ScoverageTest,
      minimumCoverage in ScoverageTest,
      failOnMinimumCoverage in ScoverageTest,
      streams in Global) map {
      (crossTarget,
       baseDirectory,
       compileSourceDirectory,
       definedTests,
       min,
       failOnMin,
       s) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else {
          Tests.Cleanup {
            () =>

              s.log.info(s"[scoverage] Waiting for measurement data to sync...")
              Thread.sleep(2000) // have noticed some delay in writing, hacky but works

              val dataDir = crossTarget / "/scoverage-data"
              val coberturaDir = crossTarget / "coverage-report"
              val reportDir = crossTarget / "scoverage-report"
              coberturaDir.mkdirs()
              reportDir.mkdirs()

              val coverageFile = IOUtils.coverageFile(dataDir)
              val measurementFiles = IOUtils.findMeasurementFiles(dataDir)

              s.log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
              s.log.info(s"[scoverage] Reading scoverage measurements...")

              val coverage = IOUtils.deserialize(coverageFile)
              val measurements = IOUtils.invoked(measurementFiles)
              coverage.apply(measurements)

              s.log.info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
              new CoberturaXmlWriter(baseDirectory, coberturaDir).write(coverage)

              s.log.info(s"[scoverage] Generating XML report [${reportDir.getAbsolutePath}/scoverage.xml]")
              new ScoverageXmlWriter(compileSourceDirectory, reportDir, false).write(coverage)
              new ScoverageXmlWriter(compileSourceDirectory, reportDir, true).write(coverage)

              s.log.info(s"[scoverage] Generating HTML report [${reportDir.getAbsolutePath}/index.html]")
              new ScoverageHtmlWriter(compileSourceDirectory, reportDir).write(coverage)

              s.log.info("[scoverage] Reports completed")

              // check for default minimum
              if (min > 0) {
                if (min > coverage.statementCoveragePercent) {
                  s.log.error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
                  if (failOnMin)
                    throw new RuntimeException("Coverage minimum was not reached")
                } else {
                  s.log.info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
                }
              }

              s.log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
              ()
          }
        }
    }
  }
}
