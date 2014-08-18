package scoverage

import sbt.Keys._
import sbt._
import scoverage.report.{ScoverageHtmlWriter, ScoverageXmlWriter, CoberturaXmlWriter}

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  val OrgScoverage = "org.scoverage"
  val ArtifactId = "scalac-scoverage-plugin"
  val ScoverageVersion = "0.99.7"

  object ScoverageKeys {
    val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
    val minimumCoverage = SettingKey[Double]("scoverage-minimum-coverage")
    val failOnMinimumCoverage = SettingKey[Boolean]("scoverage-fail-on-minimum-coverage")
    val highlighting = settingKey[Boolean]("enables range positioning for highlighting")
  }

  import ScoverageKeys._

  lazy val Scoverage: Configuration = config("scoverage") extend Compile
  lazy val ScoverageTest: Configuration = config("scoverage-test") extend Scoverage

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(Scoverage)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(Scoverage.hide, ScoverageTest.hide),
        libraryDependencies += {
          OrgScoverage % (ArtifactId + "_" + scalaBinaryVersion.value) % ScoverageVersion % Scoverage.name
        },
        sources in Scoverage := (sources in Compile).value,
        sourceDirectory in Scoverage := (sourceDirectory in Compile).value,
        resourceDirectory in Scoverage := (resourceDirectory in Compile).value,
        resourceGenerators in Scoverage := (resourceGenerators in Compile).value,
        javacOptions in Scoverage := (javacOptions in Compile).value,
        javaOptions in Scoverage := (javaOptions in Compile).value,

        minimumCoverage := 0, // default is no minimum
        failOnMinimumCoverage := false,
        highlighting := false,
        excludedPackages in Scoverage := "",

        scalacOptions in Scoverage := {
            val target = crossTarget.value
            val scoverageDeps = update.value matching configurationFilter(Scoverage.name)
            scoverageDeps.find(_.getAbsolutePath.contains(ArtifactId)) match {
              case None => throw new Exception(s"Fatal: $ArtifactId not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-P:scoverage:excludedPackages:" + Option((excludedPackages in Scoverage).value).getOrElse(""),
                  "-P:scoverage:dataDir:" + target.getAbsolutePath + "/scoverage-data"
                )
            }
        },

        scalacOptions in Scoverage ++= (if (highlighting.value) List("-Yrangepos") else Nil),

        sources in ScoverageTest := (sources in Test).value,
        sourceDirectory in ScoverageTest := (sourceDirectory in Test).value,
        resourceDirectory in ScoverageTest := (resourceDirectory in Test).value,
        resourceGenerators in ScoverageTest := (resourceGenerators in Test).value,
        unmanagedResources in ScoverageTest := (unmanagedResources in Test).value,
        javacOptions in ScoverageTest := (javacOptions in Test).value,
        javaOptions in ScoverageTest := (javaOptions in Test).value,
        fork in ScoverageTest := (fork in Test).value,

        externalDependencyClasspath in Scoverage <<= Classpaths
          .concat(externalDependencyClasspath in Scoverage, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in Scoverage <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in ScoverageTest <<= (testOptions in Test),

        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test
        test in Scoverage <<= (test in ScoverageTest),

        test in Scoverage := {
          (test in Scoverage).value

          streams.value.log.info(s"[scoverage] Waiting for measurement data to sync...")
          Thread.sleep(2000) // have noticed some delay in writing, hacky but works

          val dataDir = crossTarget.value / "/scoverage-data"
          val coberturaDir = crossTarget.value / "coverage-report"
          val reportDir = crossTarget.value / "scoverage-report"
          coberturaDir.mkdirs()
          reportDir.mkdirs()

          val coverageFile = IOUtils.coverageFile(dataDir)
          val measurementFiles = IOUtils.findMeasurementFiles(dataDir)

          streams.value.log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
          streams.value.log.info(s"[scoverage] Reading scoverage measurements...")

          val coverage = IOUtils.deserialize(coverageFile)
          val measurements = IOUtils.invoked(measurementFiles)
          coverage.apply(measurements)

          streams.value.log.info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
          new CoberturaXmlWriter(baseDirectory in Compile value, coberturaDir).write(coverage)

          streams.value.log.info(s"[scoverage] Generating XML report [${reportDir.getAbsolutePath}/scoverage.xml]")
          new ScoverageXmlWriter(scalaSource in Compile value, reportDir, false).write(coverage)
          new ScoverageXmlWriter(scalaSource in Compile value, reportDir, true).write(coverage)

          streams.value.log.info(s"[scoverage] Generating HTML report [${reportDir.getAbsolutePath}/index.html]")
          new ScoverageHtmlWriter(scalaSource in Compile value, reportDir).write(coverage)

          streams.value.log.info("[scoverage] Reports completed")

          val min = minimumCoverage.value
          val failOnMin = failOnMinimumCoverage.value

          // check for default minimum
          if (min > 0) {
            def is100(d: Double) = Math.abs(100 - d) <= 0.00001

            if (is100(min) && is100(coverage.statementCoveragePercent)) {
              streams.value.log.info(s"[scoverage] 100% Coverage !")
            } else if (min > coverage.statementCoveragePercent) {
              streams.value.log.error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
              if (failOnMin)
                throw new RuntimeException("Coverage minimum was not reached")
            } else {
              streams.value.log.info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
            }
          }

          streams.value.log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
          ()
        }
      )
  }
}