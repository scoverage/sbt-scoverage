package scoverage

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import scoverage.report.{CoverageAggregator, CoberturaXmlWriter, Deserializer, ScoverageHtmlWriter, ScoverageXmlWriter}

object ScoverageSbtPlugin extends AutoPlugin {

  // this should match the version defined in build.sbt
  private final val DefaultScoverageVersion = "2.0.0-M0"

  val autoImport = ScoverageKeys
  lazy val ScoveragePluginConfig = config("scoveragePlugin").hide

  import autoImport._

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject),
    inConfigurations(Compile)) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires: JvmPlugin.type = plugins.JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = super.globalSettings ++ Seq(
    coverageEnabled := false,
    coverageExcludedPackages := "",
    coverageExcludedFiles := "",
    coverageExcludedSymbols := "",
    coverageMinimum := 0, // default is no minimum
    coverageFailOnMinimum := false,
    coverageHighlighting := true,
    coverageOutputXML := true,
    coverageOutputHTML := true,
    coverageOutputCobertura := true,
    coverageOutputDebug := false,
    coverageCleanSubprojectFiles := true,
    coverageOutputTeamCity := false,
    coverageScalacPluginOrg := "org.scoverage",
    coverageScalacPluginArtifact := "scalac-scoverage-plugin",
    coverageScalacPluginVersion := DefaultScoverageVersion,
    coverageScalacRuntimeOrg := coverageScalacPluginOrg.value,
    coverageScalacRuntimeArtifact := "scalac-scoverage-runtime-scala",
    coverageScalacRuntimeVersion := coverageScalacPluginVersion.value,
    coverageLibraryDependencies := Seq(),
    coverageIsCompilerPlugin := false
  )

  override def buildSettings: Seq[Setting[_]] = super.buildSettings ++
    addCommandAlias("coverage", ";set coverageEnabled in ThisBuild := true") ++
    addCommandAlias("coverageOn", ";set coverageEnabled in ThisBuild := true") ++
    addCommandAlias("coverageOff", ";set coverageEnabled in ThisBuild := false")

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += ScoveragePluginConfig,
    coverageReport <<= coverageReport0,
    coverageAggregate <<= coverageAggregate0,
    aggregate in coverageAggregate := false
<<<<<<< HEAD
  ) ++ coverageSettings ++ scalacSettings
=======
  ) ++ coverageSettings ++ scalacSettings ++ coverageCompilerPluginpSettings
>>>>>>> v2.0.0-M0 changes

  private lazy val coverageSettings = Seq(
    libraryDependencies ++= {
      if (coverageEnabled.value)
        if (coverageLibraryDependencies.value.isEmpty)
          Seq(
            // We only add for "compile"" because of macros. This setting could be optimed to just "test" if the handling
            // of macro coverage was improved.
            coverageScalacRuntimeOrg.value %% (coverageScalacRuntimeArtifact.value + optionalScalaJsSuffix(libraryDependencies.value)) % coverageScalacRuntimeVersion.value,
            //coverageScalacRuntimeOrg.value %% coverageScalacRuntimeArtifact.value  % coverageScalacRuntimeVersion.value cross CrossVersion.full,

            // We don't want to instrument the test code itself, nor add to a pom when published with coverage enabled.
            coverageScalacPluginOrg.value %% coverageScalacPluginArtifact.value % coverageScalacPluginVersion.value % ScoveragePluginConfig.name cross CrossVersion.full
          )
        else
          coverageLibraryDependencies.value
      else
        Nil
    }
  )

  // I've not endeavoured to factor out the SBT repeated code
  // as we don't know yet that this approach will be adopted. Let's wait for that first
  // Also, this has been tested on single compiler plugin projects and seems to work.
  // I have no idea how well it will scala, but if nothing else highlights the problem with on solution.
  //
  // The problem is that the scoverage plugin modifies compiled code to call Invoker.invoked,
  // and hence a library implementing that must be available at the runtime of instrumented code.
  // In the normal case, this is just a case of adding a runtime library to the test code.
  //
  // But, a compiler plugin wil not pick up that library, unless it has a custom loader to do so.
  // So what I do here is, if coverageIsCompilerPlugin is true, add the Invoker code itself to the
  // user plugin - that way it most definitely has an invoker to call.
  //
  // I think this, if nothing else, makes the problem easier to see.
  // FTR, similar issues exist with macros and especially macros in scala.js cross compiled
  // code as actually different invokers are required - a jvm only for runtime and a js one for
  // runtime. This is another reason why I have excluded scala.js for now, as in the short term
  // it will just confues issues. Adding it back later is trivial.
  private lazy val coverageCompilerPluginpSettings = Seq(
    unmanagedSources in Compile ++= {
      if (coverageEnabled.value && coverageIsCompilerPlugin.value)
        mkCpUnmanagedSources(crossTarget.value.toString)
      else
        Nil
    },
    coverageExcludedPackages := coverageExcludedPackages.value + {
      if (coverageEnabled.value && coverageIsCompilerPlugin.value) ";scoverage\\..*"
      else ""
    },
    coverageScalacRuntimeArtifact := {
      if (coverageEnabled.value && coverageIsCompilerPlugin.value) "scalac-scoverage-runtime-java"
      else coverageScalacRuntimeArtifact.value
    }
  )

  private def mkCpUnmanagedSources(target: String) = {
    import java.nio.file.Files

    val scoverageDir = s"$target/scoverage-data"
    val invokerFile = "Invoker.scala"
    val embeddedInvoker = file(s"$scoverageDir/$invokerFile")

    if (!embeddedInvoker.exists()) {
      val invoker = getClass.getClassLoader.getResourceAsStream(invokerFile)
      Files.createDirectories(file(s"$scoverageDir").toPath)
      Files.copy(invoker, embeddedInvoker.toPath)
    }
    Seq(embeddedInvoker)
  }

  private lazy val scalacSettings = Seq(
    scalacOptions in(Compile, compile) ++= {
      if (coverageEnabled.value) {
        val scoverageDeps: Seq[File] = update.value matching configurationFilter(ScoveragePluginConfig.name)
        val pluginPath: File = scoverageDeps.find(_.getAbsolutePath.contains(coverageScalacPluginArtifact.value)) match {
          case None => throw new Exception(s"Fatal: ${coverageScalacPluginArtifact.value} not in libraryDependencies")
          case Some(pluginPath) => pluginPath
        }
        Seq(
          Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
          Some(s"-P:scoverage:dataDir:${crossTarget.value.getAbsolutePath}/scoverage-data"),
          Option(coverageExcludedPackages.value.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedPackages:$v"),
          Option(coverageExcludedFiles.value.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedFiles:$v"),
          Option(coverageExcludedSymbols.value.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedSymbols:$v"),
          // rangepos is broken in some releases of scala so option to turn it off
          if (coverageHighlighting.value) Some("-Yrangepos") else None
        ).flatten
      } else {
        Nil
      }
    }
  )

  def isScalaJsProject(deps: Seq[ModuleID]): Boolean =
    !optionalScalaJsSuffix(deps).isEmpty

  // returns "_sjs$sjsVersion" for Scala.js projects or "" otherwise
  def optionalScalaJsSuffix(deps: Seq[ModuleID]): String = {
    val sjsClassifier = deps.collectFirst {
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

      val coverage = Deserializer.deserialize(coverageFile)

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
