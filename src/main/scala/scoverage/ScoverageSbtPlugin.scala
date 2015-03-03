package scoverage

import sbt.Keys._
import sbt._
import scoverage.report.{CoverageAggregator, CoberturaXmlWriter, ScoverageHtmlWriter, ScoverageXmlWriter}

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.AutoPlugin {

  val OrgScoverage = "org.scoverage"
  val ScalacRuntimeArtifact = "scalac-scoverage-runtime"
  val ScalacPluginArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "1.0.5-SNAPSHOT"

  object ScoverageKeys {
    lazy val coverage = taskKey[Unit]("enable compiled code with instrumentation")
    lazy val coverageReport = taskKey[Unit]("run report generation")
    lazy val coverageAggregate = taskKey[Unit]("aggregate reports from subprojects")
    val coverageExcludedPackages = settingKey[String]("regex for excluded packages")
    val coverageExcludedFiles = settingKey[String]("regex for excluded file paths")
    val coverageMinimum = settingKey[Double]("scoverage-minimum-coverage")
    val coverageFailOnMinimum = settingKey[Boolean]("if coverage is less than this value then fail build")
    val coverageHighlighting = settingKey[Boolean]("enables range positioning for highlighting")
    val coverageOutputCobertua = settingKey[Boolean]("enables cobertura XML report generation")
    val coverageOutputXML = settingKey[Boolean]("enables xml report generation")
    val coverageOutputHTML = settingKey[Boolean]("enables html report generation")
    val coverageOutputDebug = settingKey[Boolean]("turn on the debug report")
    val coverageCleanSubprojectFiles = settingKey[Boolean]("removes subproject data after an aggregation")
  }

  var enabled = false

  import ScoverageKeys._

  override def trigger = allRequirements
  override lazy val projectSettings = Seq(

    coverage := {
      enabled = true
    },

    coverageReport := {
      val target = crossTarget.value
      val s = (streams in Global).value

      streams.value.log.info(s"Waiting for measurement data to sync...")
      Thread.sleep(1000) // have noticed some delay in writing on windows, hacky but works

      loadCoverage(target, s) match {
        case Some(cov) => writeReports(target,
          baseDirectory.value,
          (scalaSource in Compile).value,
          cov,
          coverageOutputCobertua.value,
          coverageOutputXML.value,
          coverageOutputHTML.value,
          coverageOutputDebug.value,
          s)
        case None => s.log.warn("No coverage data, skipping reports")
      }
    },

    testOptions in Test <+= postTestReport,

    testOptions in IntegrationTest <+= postTestReport,

    coverageAggregate := {
      val s = (streams in Global).value
      s.log.info(s"Aggregating coverage from subprojects...")
      val base = baseDirectory.value
      CoverageAggregator.aggregate(base, coverageCleanSubprojectFiles.value) match {
        case Some(cov) =>
          writeReports(crossTarget.value,
            base,
            (scalaSource in Compile).value,
            cov,
            coverageOutputCobertua.value,
            coverageOutputXML.value,
            coverageOutputHTML.value,
            coverageOutputDebug.value,
            s)
          val cfmt = cov.statementCoverageFormatted
          s.log.info(s"Aggregation complete. Coverage was [$cfmt]")
        case None =>
          s.log.info("No subproject data to aggregate, skipping reports")
      }
    },

    aggregate in coverageAggregate := false,

    libraryDependencies ++= Seq(
      OrgScoverage % (ScalacRuntimeArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % "provided",
      OrgScoverage % (ScalacPluginArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % "provided"
    ),

    scalacOptions in(Compile, compile) ++= {
      val scoverageDeps: Seq[File] = update.value matching configurationFilter("provided")
      scoverageDeps.find(_.getAbsolutePath.contains(ScalacPluginArtifact)) match {
        case None => throw new Exception(s"Fatal: $ScalacPluginArtifact not in libraryDependencies")
        case Some(pluginPath) =>
          scalaArgs(pluginPath,
            crossTarget.value,
            coverageExcludedPackages.value,
            coverageExcludedFiles.value,
            coverageHighlighting.value)
      }
    },

    coverageExcludedPackages := "",
    coverageExcludedFiles := "",
    coverageMinimum := 0, // default is no minimum
    coverageFailOnMinimum := false,
    coverageHighlighting := true,
    coverageOutputXML := true,
    coverageOutputHTML := true,
    coverageOutputCobertua := true,
    coverageOutputDebug := false,
    coverageCleanSubprojectFiles := true,

    // disable parallel execution to work around "classes.bak" bug in SBT
    parallelExecution in Test := false,

    parallelExecution in IntegrationTest := false
  )

  private def postTestReport = {
    (crossTarget, baseDirectory, scalaSource in Compile, coverageMinimum, coverageFailOnMinimum, coverageOutputCobertua, coverageOutputXML, coverageOutputHTML, coverageOutputDebug, streams in Global) map {
      (target, baseDirectory, compileSource, min, failOnMin, outputCobertua, outputXML, outputHTML, coverageDebug, streams) =>
        Tests.Cleanup {
          () => if (enabled) {
            loadCoverage(target, streams) foreach {
              c =>
                writeReports(target,
                  baseDirectory,
                  compileSource,
                  c,
                  outputCobertua,
                  outputXML,
                  outputHTML,
                  coverageDebug,
                  streams)
                checkCoverage(c, streams, min, failOnMin)
            }
            ()
          }
        }
    }
  }

  private def scalaArgs(pluginPath: File,
                        target: File,
                        excludedPackages: String,
                        excludedFiles: String,
                        coverageHighlighting: Boolean) = {
    if (enabled) {
      Seq(
        Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
        Some(s"-P:scoverage:dataDir:${target.getAbsolutePath}/scoverage-data"),
        Option(excludedPackages.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedPackages:$v"),
        Option(excludedFiles.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedFiles:$v"),
        // rangepos is broken in some releases of scala so option to turn it off
        if (coverageHighlighting) Some("-Yrangepos") else None
      ).flatten
    } else {
      Nil
    }
  }

  private def writeReports(crossTarget: File,
                           baseDirectory: File,
                           compileSourceDirectory: File,
                           coverage: Coverage,
                           coverageOutputCobertua: Boolean,
                           coverageOutputXML: Boolean,
                           coverageOutputHTML: Boolean,
                           coverageDebug: Boolean,
                           s: TaskStreams): Unit = {
    s.log.info(s"Generating scoverage reports...")

    val coberturaDir = crossTarget / "coverage-report"
    val reportDir = crossTarget / "scoverage-report"
    coberturaDir.mkdirs()
    reportDir.mkdirs()

    if (coverageOutputCobertua) {
      s.log.info(s"Written Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
      new CoberturaXmlWriter(baseDirectory, coberturaDir).write(coverage)
    }

    if (coverageOutputXML) {
      s.log.info(s"Written XML coverage report [${reportDir.getAbsolutePath}/scoverage.xml]")
      new ScoverageXmlWriter(compileSourceDirectory, reportDir, false).write(coverage)
      if (coverageDebug) {
        new ScoverageXmlWriter(compileSourceDirectory, reportDir, true).write(coverage)
      }
    }

    if (coverageOutputHTML) {
      s.log.info(s"Written HTML coverage report [${reportDir.getAbsolutePath}/index.html]")
      new ScoverageHtmlWriter(compileSourceDirectory, reportDir).write(coverage)
    }

    s.log.info("Coverage reports completed")
  }

  private def loadCoverage(crossTarget: File, s: TaskStreams): Option[Coverage] = {

    val dataDir = crossTarget / "/scoverage-data"
    val coverageFile = Serializer.coverageFile(dataDir)

    s.log.info(s"Reading scoverage instrumentation [$coverageFile]")

    if (coverageFile.exists) {

      val coverage = Serializer.deserialize(coverageFile)

      s.log.info(s"Reading scoverage measurements...")
      val measurementFiles = IOUtils.findMeasurementFiles(dataDir)
      val measurements = IOUtils.invoked(measurementFiles)
      coverage.apply(measurements)
      Some(coverage)

    } else {
      None
    }
  }

  private def checkCoverage(coverage: Coverage,
                            s: TaskStreams,
                            min: Double,
                            failOnMin: Boolean): Unit = {

    val cper = coverage.statementCoveragePercent
    val cfmt = coverage.statementCoverageFormatted

    // check for default minimum
    if (min > 0) {
      def is100(d: Double) = Math.abs(100 - d) <= 0.00001

      if (is100(min) && is100(cper)) {
        s.log.info(s"100% Coverage !")
      } else if (min > cper) {
        s.log.error(s"Coverage is below minimum [$cfmt% < $min%]")
        if (failOnMin)
          throw new RuntimeException("Coverage minimum was not reached")
      } else {
        s.log.info(s"Coverage is above minimum [$cfmt% > $min%]")
      }
    }

    s.log.info(s"All done. Coverage was [$cfmt%]")
  }
}