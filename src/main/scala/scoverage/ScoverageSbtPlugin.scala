package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._

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

  lazy val Scoverage: Configuration = config("scoverage") extend Test

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(Scoverage)(Defaults.compileSettings) ++
      inConfig(Scoverage)(Defaults.testSettings) ++
      Seq(
        //ivyConfigurations ++= Seq(Scoverage.hide, Scoverage.hide),
        libraryDependencies in(Scoverage, compile) += {
          OrgScoverage % (ArtifactId + "_" + scalaBinaryVersion.value) % ScoverageVersion
        },

        excludedPackages in Scoverage := "",
        minimumCoverage in Scoverage := 0, // default is no minimum
        failOnMinimumCoverage in Scoverage := false,
        highlighting in Scoverage := false,

        scalacOptions in(Scoverage, compile) ++= {
            val target = crossTarget.value
            val scoverageDeps = (update in(Scoverage, compile)).value matching configurationFilter(Scoverage.name)
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

        scalacOptions in(Scoverage, compile) ++= (if ((highlighting in Scoverage).value) List("-Yrangepos") else Nil),

        test in Scoverage := {
          (test in Scoverage).value
          streams.value.log.info(s"[scoverage] Waiting for measurement data to sync...")
          Thread.sleep(2000) // have noticed some delay in writing, hacky but works

          val targetPath = crossTarget.value

          val dataDir = targetPath / "/scoverage-data"
          val coberturaDir = targetPath / "coverage-report"
          val reportDir = targetPath / "scoverage-report"
          coberturaDir.mkdirs()
          reportDir.mkdirs()

          val coverageFile = IOUtils.coverageFile(dataDir)
          val measurementFiles = IOUtils.findMeasurementFiles(dataDir)

          streams.value.log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
          streams.value.log.info(s"[scoverage] Reading scoverage measurements...")

          val coverage = IOUtils.deserialize(coverageFile)
          val measurements = IOUtils.invoked(measurementFiles)
          coverage.apply(measurements)

          streams.value.log
            .info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
          new CoberturaXmlWriter(baseDirectory.value, coberturaDir).write(coverage)

          streams.value.log.info(s"[scoverage] Generating XML report [${reportDir.getAbsolutePath}/scoverage.xml]")
          new ScoverageXmlWriter((scalaSource in Compile).value, reportDir, false).write(coverage)
          new ScoverageXmlWriter((scalaSource in Compile).value, reportDir, true).write(coverage)

          streams.value.log.info(s"[scoverage] Generating HTML report [${reportDir.getAbsolutePath}/index.html]")
          new ScoverageHtmlWriter((scalaSource in Compile).value, reportDir).write(coverage)

          streams.value.log.info("[scoverage] Reports completed")

          // check for default minimum
          val min = (minimumCoverage in Scoverage).value
          val failOnMin = (failOnMinimumCoverage in Scoverage).value
          if (min > 0) {
            def is100(d: Double) = Math.abs(100 - d) <= 0.00001

            if (is100(coverage.statementCoveragePercent)) {
              streams.value.log.info(s"[scoverage] 100% Coverage")
            } else if (min > coverage.statementCoveragePercent) {
              streams.value.log
                .error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
              if (failOnMin)
                throw new RuntimeException("Coverage minimum was not reached")
            } else {
              streams.value.log
                .info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
            }
          }

          streams.value.log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
          ()
        }
      )
  }
}