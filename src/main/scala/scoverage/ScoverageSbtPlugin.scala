package scoverage

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import scoverage.report.{CoverageAggregator, CoberturaXmlWriter, ScoverageHtmlWriter, ScoverageXmlWriter}

object ScoverageSbtPlugin extends AutoPlugin {

  val OrgScoverage = "org.scoverage"
  val ScalacRuntimeArtifact = "scalac-scoverage-runtime"
  val ScalacPluginArtifact = "scalac-scoverage-plugin"
  // this should match the version defined in build.sbt
  val DefaultScoverageVersion = "1.2.0"
  val autoImport = ScoverageKeys
  lazy val ScoveragePluginConfig = config("scoveragePlugin").hide

  import autoImport._

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject),
    inConfigurations(Compile)) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  override lazy val projectSettings = Seq(
    coverageEnabled := false,
    commands += Command.command("coverage", "enable compiled code with instrumentation", "")(toggleCoverage(true)),
    commands += Command.command("coverageOff", "disable compiled code with instrumentation", "")(toggleCoverage(false)),
    coverageReport <<= coverageReport0,
    coverageAggregate <<= coverageAggregate0,
    ivyConfigurations := ivyConfigurations.value :+ ScoveragePluginConfig,
    coverageScalacPluginVersion := DefaultScoverageVersion,
    libraryDependencies ++= {
      if (coverageEnabled.value) Seq(
        OrgScoverage %% (ScalacRuntimeArtifact + optionalScalaJsSuffix(libraryDependencies.value)) % coverageScalacPluginVersion.value,
        OrgScoverage %% ScalacPluginArtifact % coverageScalacPluginVersion.value % ScoveragePluginConfig.name
      ) else Nil
    },
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
    coverageCleanSubprojectFiles := true,
    coverageOutputTeamCity := false
  )

  /**
    * The "coverage" command enables or disables instrumentation for all projects
    * in the build.
    */
  private def toggleCoverage(status: Boolean): State => State = { state =>
    val extracted = Project.extract(state)
    val currentProjRef = extracted.currentRef
    val newSettings = extracted.structure.allProjectRefs.flatMap(proj =>
      Seq(coverageEnabled in proj := status)
    )
    val appendSettings = Load.transformSettings(Load.projectScope(currentProjRef), currentProjRef.build, extracted.rootProject, newSettings)
    val newSessionSettings = extracted.session.appendRaw(appendSettings)
    SessionSettings.reapply(newSessionSettings, state)
  }

  // returns "_sjs$sjsVersion" for Scala.js projects or "" otherwise
  private def optionalScalaJsSuffix(deps: Seq[ModuleID]): String = {
    val sjsClassifier = deps.collectFirst{
      case ModuleID("org.scala-js", "scalajs-library", v, _, _, _, _, _, _, _, _) => v
    }.map(_.take(3)).map(sjsVersion => "_sjs" + sjsVersion)

    sjsClassifier getOrElse ""
  }

  private lazy val coverageReport0 = Def.task {
    val target = crossTarget.value
    val log = streams.value.log

    log.info(s"Waiting for measurement data to sync...")
    Thread.sleep(1000) // have noticed some delay in writing on windows, hacky but works

    loadCoverage(target, log) match {
      case Some(cov) =>
        writeReports(
          target,
          (sourceDirectories in Compile).value,
          cov,
          coverageOutputCobertura.value,
          coverageOutputXML.value,
          coverageOutputHTML.value,
          coverageOutputDebug.value,
          coverageOutputTeamCity.value,
          sourceEncoding((scalacOptions in (Compile)).value),
          log)

        checkCoverage(cov, log, coverageMinimum.value, coverageFailOnMinimum.value)
      case None => log.warn("No coverage data, skipping reports")
    }
  }

  private lazy val coverageAggregate0 = Def.task {
    val log = streams.value.log
    log.info(s"Aggregating coverage from subprojects...")

    val xmlReportFiles = crossTarget.all(aggregateFilter).value map (_ / "scoverage-report" / Constants
      .XMLReportFilename) filter (_.isFile())
    CoverageAggregator.aggregate(xmlReportFiles, coverageCleanSubprojectFiles.value) match {
      case Some(cov) =>
        writeReports(
          crossTarget.value,
          sourceDirectories.all(aggregateFilter).value.flatten,
          cov,
          coverageOutputCobertura.value,
          coverageOutputXML.value,
          coverageOutputHTML.value,
          coverageOutputDebug.value,
          coverageOutputTeamCity.value,
          sourceEncoding((scalacOptions in (Compile)).value),
          log)
        val cfmt = cov.statementCoverageFormatted
        log.info(s"Aggregation complete. Coverage was [$cfmt]")

        checkCoverage(cov, log, coverageMinimum.value, coverageFailOnMinimum.value)
      case None =>
        log.info("No subproject data to aggregate, skipping reports")
    }
  }

  private lazy val scoverageScalacOptions = Def.task {
    update.value
      .matching(configurationFilter(ScoveragePluginConfig.name))
      .find(_.getAbsolutePath.contains(ScalacPluginArtifact))
      .fold[Seq[String]](Nil)(pluginPath =>
        scalaArgs(coverageEnabled.value,
          pluginPath,
          crossTarget.value,
          coverageExcludedPackages.value,
          coverageExcludedFiles.value,
          coverageHighlighting.value))
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
                           coverageOutputTeamCity: Boolean,
                           coverageSourceEncoding: Option[String],
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
      new ScoverageHtmlWriter(compileSourceDirectories, reportDir, coverageSourceEncoding).write(coverage)
    }
    if (coverageOutputTeamCity) {
      log.info("Writing coverage report to teamcity")
      reportToTeamcity(coverage, coverageOutputHTML, reportDir, crossTarget, log)
    }

    log.info(s"Statement coverage.: ${coverage.statementCoverageFormatted}%")
    log.info(s"Branch coverage....: ${coverage.branchCoverageFormatted}%")
    log.info("Coverage reports completed")
  }

  private def reportToTeamcity(coverage: Coverage,
                               createCoverageZip: Boolean,
                               reportDir: File,
                               crossTarget: File,
                               log: Logger) {

    def statsKeyValue(key: String, value: Int): String = s"##teamcity[buildStatisticValue key='$key' value='$value']"

    // Log statement coverage as per: https://devnet.jetbrains.com/message/5467985
    log.info(statsKeyValue("CodeCoverageAbsSCovered", coverage.invokedStatementCount))
    log.info(statsKeyValue("CodeCoverageAbsSTotal", coverage.statementCount))

    // Log branch coverage as a custom metrics (in percent)
    log.info(statsKeyValue("CodeCoverageBranch", "%.0f".format(coverage.branchCoveragePercent).toInt))

    // Create the coverage report for teamcity (HTML files)
    if (createCoverageZip)
      IO.zip(Path.allSubpaths(reportDir), crossTarget / "coverage.zip")
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

  private def sourceEncoding(scalacOptions: Seq[String]): Option[String] = {
    val i = scalacOptions.indexOf("-encoding") + 1
    if (i > 0 && i < scalacOptions.length) Some(scalacOptions(i)) else None
  }

}
