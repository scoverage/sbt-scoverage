package scoverage

import sbt.Keys._
import sbt._
import scoverage.report.{CoberturaXmlWriter, ScoverageHtmlWriter, ScoverageXmlWriter}

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.AutoPlugin {

  val OrgScoverage = "org.scoverage"
  val ScalacRuntimeArtifact = "scalac-scoverage-runtime"
  val ScalacPluginArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "1.0.0.BETA4"

  object ScoverageKeys {
    lazy val coverage = taskKey[Unit]("enable compiled code with instrumentation")
    lazy val coverageReport = taskKey[Unit]("run report generation")
    lazy val coverageAggregate = taskKey[Unit]("aggregate reports from subprojects")
    val coverageExcludedPackages = settingKey[String]("regex for excluded packages")
    val coverageExcludedFiles = settingKey[String]("regex for excluded file paths")
    val coverageMinimumCoverage = settingKey[Double]("scoverage-minimum-coverage")
    val coverageFailOnMinimumCoverage = settingKey[Boolean]("if coverage is less than this value then fail build")
    val coverageHighlighting = settingKey[Boolean]("enables range positioning for highlighting")
    val coverageOutputCobertua = settingKey[Boolean]("enables cobertura XML report generation")
    val coverageOutputXML = settingKey[Boolean]("enables xml report generation")
    val coverageOutputHTML = settingKey[Boolean]("enables html report generation")
  }

  var enabled = false

  import ScoverageKeys._

  override def trigger = allRequirements
  override lazy val projectSettings = Seq(

    coverage := {
      enabled = true
      println("[info] Scoverage code coverage is enabled")
    },

    coverageReport := {

      streams.value.log.info(s"Waiting for measurement data to sync...")
      Thread.sleep(1000) // have noticed some delay in writing on windows, hacky but works

      val target = (crossTarget in Test).value
      val s = (streams in Global).value

      loadCoverage(target, s) foreach {
        _ =>
          writeReports(target,       (baseDirectory in Compile).value, (scalaSource in Compile).value, _, s)
          checkCoverage(_, s, coverageMinimumCoverage.value, coverageFailOnMinimumCoverage.value)
      }
    },

    testOptions in Test <+= postTestReport,

    testOptions in IntegrationTest <+= postTestReport,

    coverageAggregate := {
      streams.value.log.info(s"Aggregating coverage from subprojects...")
      IOUtils.aggregator(baseDirectory.value, new File(crossTarget.value, "/scoverage-report"))

    },

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
    coverageMinimumCoverage := 0, // default is no minimum
    coverageFailOnMinimumCoverage := false,
    coverageHighlighting := true,
    coverageOutputXML := true,
    coverageOutputHTML := true,
    coverageOutputCobertua := true,

    // disable parallel execution to work around "classes.bak" bug in SBT
    parallelExecution in Test := false
  )

  private def postTestReport = {
    (crossTarget in Test, baseDirectory in Compile, scalaSource in Compile, coverageMinimumCoverage, coverageFailOnMinimumCoverage, streams in Global) map {
      (target, baseDirectory, compileSource, min, failOnMin, streams) =>
        Tests.Cleanup {
          () => if (enabled) {
            loadCoverage(target, streams) foreach {
              _ =>
                writeReports(target, baseDirectory, compileSource, _, streams)
                checkCoverage(_, streams, min, failOnMin)
            }
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
                           s: TaskStreams): Unit = {
    s.log.info(s"Generating scoverage reports")

    val coberturaDir = crossTarget / "coverage-report"
    val reportDir = crossTarget / "scoverage-report"
    coberturaDir.mkdirs()
    reportDir.mkdirs()

    s.log.info(s"Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
    new CoberturaXmlWriter(baseDirectory, coberturaDir).write(coverage)

    s.log.info(s"Generating XML coverage report [${reportDir.getAbsolutePath}/scoverage.xml]")
    new ScoverageXmlWriter(compileSourceDirectory, reportDir, false).write(coverage)
    new ScoverageXmlWriter(compileSourceDirectory, reportDir, true).write(coverage)

    s.log.info(s"Generating HTML coverage report [${reportDir.getAbsolutePath}/index.html]")
    new ScoverageHtmlWriter(compileSourceDirectory, reportDir).write(coverage)

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
        s.log.error(s"Coverage is below minimum [$coverage.statementCoverageFormatted}% < $min%]")
        if (failOnMin)
          throw new RuntimeException("Coverage minimum was not reached")
      } else {
        s.log.info(s"Coverage is above minimum [$cfmt% > $min%]")
      }
    }

    s.log.info(s"All done. Coverage was [$cfmt%]")
  }
}