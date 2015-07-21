package scoverage

import sbt.Keys._
import sbt._
import scoverage.report.{CoverageAggregator, CoberturaXmlWriter, ScoverageHtmlWriter, ScoverageXmlWriter}

object ScoverageSbtPlugin extends AutoPlugin {

  val OrgScoverage = "org.scoverage"
  val ScalacRuntimeArtifact = "scalac-scoverage-runtime"
  val ScalacPluginArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "1.1.0"
  val autoImport = ScoverageKeys

  import autoImport._

  val aggregateFilter = ScopeFilter( inAggregates(ThisProject), inConfigurations(Compile) ) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    coverageEnabled := false,
    commands += Command.command("coverage", "enable compiled code with instrumentation", "")(toggleCoverage(true)),
    commands += Command.command("coverageOff", "disable compiled code with instrumentation", "")(toggleCoverage(false)),
    coverageReport <<= coverageReport0,
    testOptions in Test += postTestReport.value,
    testOptions in IntegrationTest += postTestReport.value,
    coverageAggregate <<= coverageAggregate0,
    libraryDependencies ++= Seq(
      OrgScoverage % (ScalacRuntimeArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % "provided" intransitive(),
      OrgScoverage % (ScalacPluginArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % "provided" intransitive()
    ),
    scalacOptions in(Compile, compile) ++= scoverageScalacOptions.value,
    aggregate in coverageAggregate := false,
    coverageExcludedPackages := "",
    coverageExcludedFiles := "",
    coverageMinimum := 0, // default is no minimum
    coverageFailOnMinimum := false,
    coverageHighlighting := true,
    coverageOutputXML := true,
    coverageOutputHTML := true,
    coverageOutputCobertura := true,
    coverageOutputDebug := false,
    coverageCleanSubprojectFiles := true
  )

  private def toggleCoverage(status:Boolean): State => State =
    state => Project.extract(state).append(Seq(coverageEnabled := status), state)

  private lazy val coverageReport0 = Def.task {
    val target = crossTarget.value
    val log = streams.value.log

    log.info(s"Waiting for measurement data to sync...")
    Thread.sleep(1000) // have noticed some delay in writing on windows, hacky but works

    loadCoverage(target, log) match {
      case Some(cov) => writeReports(target,
        (sourceDirectories in Compile).value,
        cov,
        coverageOutputCobertura.value,
        coverageOutputXML.value,
        coverageOutputHTML.value,
        coverageOutputDebug.value,
        log)
      case None => log.warn("No coverage data, skipping reports")
    }
  }

  private lazy val coverageAggregate0 = Def.task {
    val log = streams.value.log
    log.info(s"Aggregating coverage from subprojects...")

    val xmlReportFiles = crossTarget.all(aggregateFilter).value map (_ / "scoverage-report" / Constants.XMLReportFilename) filter (_.isFile())
    CoverageAggregator.aggregate(xmlReportFiles, coverageCleanSubprojectFiles.value) match {
      case Some(cov) =>
        writeReports(crossTarget.value,
          sourceDirectories.all(aggregateFilter).value.flatten,
          cov,
          coverageOutputCobertura.value,
          coverageOutputXML.value,
          coverageOutputHTML.value,
          coverageOutputDebug.value,
          log)
        val cfmt = cov.statementCoverageFormatted
        log.info(s"Aggregation complete. Coverage was [$cfmt]")
      case None =>
        log.info("No subproject data to aggregate, skipping reports")
    }
  }

  private lazy val scoverageScalacOptions = Def.task {
    val scoverageDeps: Seq[File] = update.value matching configurationFilter("provided")
    scoverageDeps.find(_.getAbsolutePath.contains(ScalacPluginArtifact)) match {
      case None => throw new Exception(s"Fatal: $ScalacPluginArtifact not in libraryDependencies")
      case Some(pluginPath) =>
        scalaArgs(coverageEnabled.value,
          pluginPath,
          crossTarget.value,
          coverageExcludedPackages.value,
          coverageExcludedFiles.value,
          coverageHighlighting.value)
    }
  }

  private lazy val postTestReport = Def.task {
    val log = streams.value.log
    val target = crossTarget.value
    Tests.Cleanup {
      () => if (coverageEnabled.value) {
        loadCoverage(target, log) foreach { c =>
          writeReports(
            target,
            (sourceDirectories in Compile).value,
            c,
            coverageOutputCobertura.value,
            coverageOutputXML.value,
            coverageOutputHTML.value,
            coverageOutputDebug.value,
            log
          )
          checkCoverage(c, log, coverageMinimum.value, coverageFailOnMinimum.value)
        }
        ()
      }
    }
  }

  private def scalaArgs(coverageEnabled: Boolean,
                        pluginPath: File,
                        target: File,
                        excludedPackages: String,
                        excludedFiles: String,
                        coverageHighlighting: Boolean) = {
    if (coverageEnabled) {
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
                           compileSourceDirectories: Seq[File],
                           coverage: Coverage,
                           coverageOutputCobertura: Boolean,
                           coverageOutputXML: Boolean,
                           coverageOutputHTML: Boolean,
                           coverageDebug: Boolean,
                           log: Logger): Unit = {
    log.info(s"Generating scoverage reports...")

    val coberturaDir = crossTarget / "coverage-report"
    val reportDir = crossTarget / "scoverage-report"
    coberturaDir.mkdirs()
    reportDir.mkdirs()

    if (coverageOutputCobertura) {
      log.info(s"Written Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
      new CoberturaXmlWriter(compileSourceDirectories, coberturaDir).write(coverage)
    }

    if (coverageOutputXML) {
      log.info(s"Written XML coverage report [${reportDir.getAbsolutePath}/scoverage.xml]")
      new ScoverageXmlWriter(compileSourceDirectories, reportDir, false).write(coverage)
      if (coverageDebug) {
        new ScoverageXmlWriter(compileSourceDirectories, reportDir, true).write(coverage)
      }
    }

    if (coverageOutputHTML) {
      log.info(s"Written HTML coverage report [${reportDir.getAbsolutePath}/index.html]")
      new ScoverageHtmlWriter(compileSourceDirectories, reportDir).write(coverage)
    }

    log.info("Coverage reports completed")
  }

  private def loadCoverage(crossTarget: File, log: Logger): Option[Coverage] = {

    val dataDir = crossTarget / "/scoverage-data"
    val coverageFile = Serializer.coverageFile(dataDir)

    log.info(s"Reading scoverage instrumentation [$coverageFile]")

    if (coverageFile.exists) {

      val coverage = Serializer.deserialize(coverageFile)

      log.info(s"Reading scoverage measurements...")
      val measurementFiles = IOUtils.findMeasurementFiles(dataDir)
      val measurements = IOUtils.invoked(measurementFiles)
      coverage.apply(measurements)
      Some(coverage)

    } else {
      None
    }
  }

  private def checkCoverage(coverage: Coverage,
                            log: Logger,
                            min: Double,
                            failOnMin: Boolean): Unit = {

    val cper = coverage.statementCoveragePercent
    val cfmt = coverage.statementCoverageFormatted

    // check for default minimum
    if (min > 0) {
      def is100(d: Double) = Math.abs(100 - d) <= 0.00001

      if (is100(min) && is100(cper)) {
        log.info(s"100% Coverage !")
      } else if (min > cper) {
        log.error(s"Coverage is below minimum [$cfmt% < $min%]")
        if (failOnMin)
          throw new RuntimeException("Coverage minimum was not reached")
      } else {
        log.info(s"Coverage is above minimum [$cfmt% > $min%]")
      }
    }

    log.info(s"All done. Coverage was [$cfmt%]")
  }
}
