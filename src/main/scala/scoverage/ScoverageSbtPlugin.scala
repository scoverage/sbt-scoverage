package scoverage

import sbt.Keys._
import sbt.{given, _}
import sbt.internal.util.Util.isWindows
import sbt.plugins.JvmPlugin
import scoverage.ScoverageSbtPluginCompat.*
import scoverage.reporter.CoberturaXmlWriter
import scoverage.domain.Constants
import scoverage.domain.Coverage
import scoverage.reporter.CoverageAggregator
import scoverage.reporter.IOUtils
import scoverage.reporter.ScoverageHtmlWriter
import scoverage.reporter.ScoverageXmlWriter
import scoverage.serialize.Serializer

import java.time.Instant

object ScoverageSbtPlugin extends AutoPlugin {

  val orgScoverage = "org.scoverage"
  val scalacRuntimeArtifact = "scalac-scoverage-runtime"
  val scalacPluginArtifact = "scalac-scoverage-plugin"
  val scalacDomainArtifact = "scalac-scoverage-domain"
  val scalacReporterArtifact = "scalac-scoverage-reporter"
  val scalacSerializerArtifact = "scalac-scoverage-serializer"
  val defaultScoverageVersion = BuildInfo.scoverageVersion
  val autoImport = ScoverageKeys
  lazy val ScoveragePluginConfig = config("scoveragePlugin").hide

  import autoImport._

  val aggregateFilter = ScopeFilter(
    inAggregates(ThisProject),
    inConfigurations(Compile)
  ) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  override def globalSettings: Seq[Def.Setting[?]] =
    super.globalSettings ++ Seq(
      coverageEnabled := false,
      coverageExcludedPackages := "",
      coverageExcludedFiles := "",
      coverageMinimumStmtTotal := 0,
      coverageMinimumBranchTotal := 0,
      coverageMinimumStmtPerPackage := 0,
      coverageMinimumBranchPerPackage := 0,
      coverageMinimumStmtPerFile := 0,
      coverageMinimumBranchPerFile := 0,
      coverageFailOnMinimum := false,
      coverageHighlighting := true,
      coverageOutputXML := true,
      coverageOutputHTML := true,
      coverageOutputCobertura := true,
      coverageOutputDebug := false,
      coverageOutputTeamCity := false,
      coverageScalacPluginVersion := defaultScoverageVersion,
      coverageSourceRoot := (ThisBuild / baseDirectory).value
    )

  override def buildSettings: Seq[Setting[?]] = super.buildSettings ++
    addCommandAlias("coverage", ";set ThisBuild / coverageEnabled := true") ++
    addCommandAlias("coverageOn", ";set ThisBuild / coverageEnabled := true") ++
    addCommandAlias("coverageOff", ";set ThisBuild / coverageEnabled := false")

  override def projectSettings: Seq[Setting[?]] = Seq(
    ivyConfigurations += ScoveragePluginConfig,
    coverageDeleteMeasurements := coverageDeleteMeasurements0.value,
    coverageReport := coverageReport0.value,
    coverageAggregate := coverageAggregate0.value,
    coverageAggregate / aggregate := false,
    coverageDataDir := crossTarget.value,
    coverageScalacPluginVersion := {
      scalaVersion.value match {
        case "2.13.11" => "2.3.0"
        case "2.13.12" => "2.3.0"
        case "2.13.13" => "2.3.0"
        case "2.13.14" => "2.3.0"
        case "2.13.15" => "2.3.0"
        case "2.12.16" => "2.3.0"
        case _         => defaultScoverageVersion
      }
    }
  ) ++ coverageSettings ++ scalacSettings

  private def isScala2(scalaVersion: String) =
    CrossVersion
      .partialVersion(scalaVersion)
      .exists {
        case (2, _) => true
        case _      => false
      }

  private def isScala3SupportingScoverage(scalaVersion: String) =
    CrossVersion
      .partialVersion(scalaVersion)
      .exists {
        case (3, minor) if minor >= 2 => true
        case _                        => false
      }

  private def isScala3SupportingFilePackageExclusion(scalaVersion: String) = {
    def patch = scalaVersion.split('.').map(_.toInt).drop(2).headOption
    CrossVersion
      .partialVersion(scalaVersion)
      .exists {
        case (3, minor) if minor > 4                            => true
        case (3, minor) if (minor == 4 && patch.exists(_ >= 2)) => true
        case (3, minor) if (minor == 3 && patch.exists(_ >= 4)) => true
        case _                                                  => false
      }
  }

  private lazy val coverageSettings = Seq(
    libraryDependencies ++= {
      if (coverageEnabled.value && isScala2(scalaVersion.value)) {
        Seq(
          orgScoverage %% scalacDomainArtifact % coverageScalacPluginVersion.value,
          orgScoverage %% scalacReporterArtifact % coverageScalacPluginVersion.value,
          orgScoverage %% scalacSerializerArtifact % coverageScalacPluginVersion.value,
          // We only add for "compile" because of macros. This setting could be optimed to just "test" if the handling
          // of macro coverage was improved.
          orgScoverage %% (scalacRuntime(
            libraryDependencies.value
          )) % coverageScalacPluginVersion.value,
          // We don't want to instrument the test code itself, nor add to a pom when published with coverage enabled.
          (orgScoverage %% scalacPluginArtifact % coverageScalacPluginVersion.value % ScoveragePluginConfig.name)
            .cross(CrossVersion.full)
        )
      } else
        Nil
    }
  )

  private lazy val scalacSettings = Seq(
    Compile / compile / scalacOptions ++= Def.uncached {

      implicit val log: Logger = streams.value.log

      val excludedPackages =
        Option(coverageExcludedPackages.value.trim).filter(_.nonEmpty)
      val excludedFiles = Option(coverageExcludedFiles.value.trim)
        .filter(_.nonEmpty)
        .map(v =>
          // On windows, replace unix file separators with windows file
          // separators. Note that we need to replace / with \\ because
          // the plugin treats this string as a regular expression and
          // backslashes must be escaped in regular expressions.
          if (isWindows) v.replace("/", """\\""") else v
        )

      val updateReport = update.value
      if (coverageEnabled.value && isScala2(scalaVersion.value)) {
        val scoverageDeps: Seq[File] =
          updateReport.matching(configurationFilter(ScoveragePluginConfig.name))

        // Since everything isn't contained in a single plugin jar since we
        // want to share reporter/domain code between the plugin and the
        // reporter which can be used for Scala3 we need to essentially put
        // together the little classpath to pass in to the compiler which
        // includes everything it needs for the compiler plugin phase:
        //  1. the plugin jar
        //  2. the domain jar
        //  3. the serializer jar
        //  NOTE: Even though you'd think that since plugin relies on domain
        //  it'd just auto include it... it doesn't.
        //  https://github.com/sbt/sbt/issues/2255
        val pluginPaths = scoverageDeps.collect {
          case path
              if path.getAbsolutePath().contains(scalacPluginArtifact) || path
                .getAbsolutePath()
                .contains(scalacDomainArtifact) ||
                path.getAbsolutePath().contains(scalacSerializerArtifact) =>
            path.getAbsolutePath()
        }

        // NOTE: A little hacky, but make sure we are matching on the exact
        // number of deps that we expect to collect up above.
        if (pluginPaths.size != 3)
          throw new Exception(
            s"Fatal: Not finding either $scalacDomainArtifact or $scalacPluginArtifact or $scalacSerializerArtifact in libraryDependencies."
          )

        Seq(
          Some(
            s"-Xplugin:${pluginPaths.mkString(java.io.File.pathSeparator)}"
          ),
          Some(
            s"-P:scoverage:dataDir:${new java.io.File(coverageDataDir.value, "scoverage-data").getAbsolutePath}"
          ),
          Some(
            s"-P:scoverage:sourceRoot:${coverageSourceRoot.value.getAbsolutePath}"
          ),
          excludedPackages.map(v => s"-P:scoverage:excludedPackages:$v"),
          excludedFiles.map(v => s"-P:scoverage:excludedFiles:$v"),
          Some("-P:scoverage:reportTestName"),
          // rangepos is broken in some releases of scala so option to turn it off
          if (coverageHighlighting.value) Some("-Yrangepos") else None
        ).flatten
      } else if (
        coverageEnabled.value && isScala3SupportingScoverage(scalaVersion.value)
      ) {
        Seq(
          Some(
            s"-coverage-out:${new java.io.File(coverageDataDir.value, "scoverage-data").getAbsolutePath}"
          ),
          excludedPackages
            .collect {
              case v
                  if isScala3SupportingFilePackageExclusion(
                    scalaVersion.value
                  ) =>
                s"-coverage-exclude-classlikes:${v.replace(';', ',')}"
            },
          excludedFiles
            .collect {
              case v
                  if isScala3SupportingFilePackageExclusion(
                    scalaVersion.value
                  ) =>
                s"-coverage-exclude-files:${v.replace(';', ',')}"
            }
        ).flatten
      } else if (coverageEnabled.value && !isScala2(scalaVersion.value)) {
        log.warn(
          "coverage in Scala 3 needs at least 3.2.x. Please update your Scala version and try again."
        )
        Nil
      } else {
        Nil
      }
    }
  )

  private def scalacRuntime(deps: Seq[ModuleID]): String = {
    scalacRuntimeArtifact + optionalScalaJsSuffix(deps)
  }

  // returns "_sjs$sjsVersion" for Scala.js projects or "" otherwise
  private def optionalScalaJsSuffix(deps: Seq[ModuleID]): String = {
    val sjsClassifier = deps
      .collectFirst {
        case moduleId
            if moduleId.organization == "org.scala-js" && moduleId.name == "scalajs-library" =>
          moduleId.revision
      }
      .map(_.take(1))
      .map(sjsVersion => "_sjs" + sjsVersion)

    sjsClassifier getOrElse ""
  }

  private lazy val coverageDeleteMeasurements0 = Def.task {
    val dataDir = coverageDataDir.value / "scoverage-data"
    implicit val log: Logger = streams.value.log

    log.info("Deleting existing coverage measurements...")
    IOUtils.findMeasurementFiles(dataDir).foreach(IO.delete)
  }

  private lazy val coverageReport0 = Def.task {
    val target = coverageDataDir.value
    implicit val log: Logger = streams.value.log

    log.info("Waiting for measurement data to sync...")
    if (System.getProperty("os.name").toLowerCase.contains("windows")) {
      Thread.sleep(
        1000
      ) // have noticed some delay in writing on windows, hacky but works
    }

    loadCoverage(
      target,
      log,
      coverageSourceRoot.value.getAbsoluteFile()
    ) match {
      case Some(cov) =>
        writeReports(
          target,
          (Compile / sourceDirectories).value,
          cov,
          coverageOutputCobertura.value,
          coverageOutputXML.value,
          coverageOutputHTML.value,
          coverageOutputDebug.value,
          coverageOutputTeamCity.value,
          sourceEncoding((Compile / scalacOptions).value),
          log
        )

        CoverageMinimum.all.value
          .checkCoverage(cov, coverageFailOnMinimum.value)
      case None => log.warn("No coverage data, skipping reports")
    }
  }

  private lazy val coverageAggregate0 = Def.task {
    implicit val log: Logger = streams.value.log
    log.info(s"Aggregating coverage from subprojects...")

    val dataDirs = coverageDataDir.?.all(aggregateFilter).value
      .collect {
        case Some(file) if (file / Constants.DataDir).isDirectory =>
          file / Constants.DataDir
      }

    CoverageAggregator.aggregate(dataDirs, coverageSourceRoot.value) match {
      case Some(cov) =>
        writeReports(
          coverageDataDir.value,
          sourceDirectories.all(aggregateFilter).value.flatten,
          cov,
          coverageOutputCobertura.value,
          coverageOutputXML.value,
          coverageOutputHTML.value,
          coverageOutputDebug.value,
          coverageOutputTeamCity.value,
          sourceEncoding((Compile / scalacOptions).value),
          log
        )
        val cfmt = cov.statementCoverageFormatted
        log.info(s"Aggregation complete. Coverage was [$cfmt]")

        CoverageMinimum.all.value
          .checkCoverage(cov, coverageFailOnMinimum.value)
      case None =>
        log.info("No subproject data to aggregate, skipping reports")
    }
  }

  private def writeReports(
      crossTarget: File,
      compileSourceDirectories: Seq[File],
      coverage: Coverage,
      coverageOutputCobertura: Boolean,
      coverageOutputXML: Boolean,
      coverageOutputHTML: Boolean,
      coverageDebug: Boolean,
      coverageOutputTeamCity: Boolean,
      coverageSourceEncoding: Option[String],
      log: Logger
  ): Unit = {
    log.info(s"Generating scoverage reports...")

    val coberturaDir = crossTarget / "coverage-report"
    val reportDir = crossTarget / "scoverage-report"
    coberturaDir.mkdirs()
    reportDir.mkdirs()

    if (coverageOutputCobertura) {
      new CoberturaXmlWriter(
        compileSourceDirectories,
        coberturaDir,
        coverageSourceEncoding
      ).write(
        coverage
      )
      log.info(
        s"Written Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]"
      )
    }

    if (coverageOutputXML) {
      new ScoverageXmlWriter(
        compileSourceDirectories,
        reportDir,
        false,
        coverageSourceEncoding
      ).write(
        coverage
      )
      if (coverageDebug) {
        new ScoverageXmlWriter(
          compileSourceDirectories,
          reportDir,
          true,
          coverageSourceEncoding
        ).write(
          coverage
        )
      }
      log.info(
        s"Written XML coverage report [${reportDir.getAbsolutePath}/scoverage.xml]"
      )
    }

    if (coverageOutputHTML) {
      new ScoverageHtmlWriter(
        compileSourceDirectories,
        reportDir,
        coverageSourceEncoding
      ).write(coverage)
      log.info(
        s"Written HTML coverage report [${reportDir.getAbsolutePath}/index.html]"
      )
    }
    if (coverageOutputTeamCity) {
      reportToTeamcity(
        coverage,
        coverageOutputHTML,
        reportDir,
        crossTarget,
        log
      )
      log.info("Written coverage report to TeamCity")
    }

    log.info(s"Statement coverage.: ${coverage.statementCoverageFormatted}%")
    log.info(s"Branch coverage....: ${coverage.branchCoverageFormatted}%")
    log.info("Coverage reports completed")
  }

  private def reportToTeamcity(
      coverage: Coverage,
      createCoverageZip: Boolean,
      reportDir: File,
      crossTarget: File,
      log: Logger
  ): Unit = {

    def statsKeyValue(key: String, value: Int): String =
      s"##teamcity[buildStatisticValue key='$key' value='$value']"

    // Log statement coverage as per: https://devnet.jetbrains.com/message/5467985
    log.info(
      statsKeyValue("CodeCoverageAbsSCovered", coverage.invokedStatementCount)
    )
    log.info(statsKeyValue("CodeCoverageAbsSTotal", coverage.statementCount))
    log.info(
      statsKeyValue("CodeCoverageAbsRCovered", coverage.invokedBranchesCount)
    )
    log.info(statsKeyValue("CodeCoverageAbsRTotal", coverage.branchCount))

    // Log branch coverage as a custom metrics (in percent)
    log.info(
      statsKeyValue(
        "CodeCoverageBranch",
        "%.0f".format(coverage.branchCoveragePercent).toInt
      )
    )

    // Create the coverage report for teamcity (HTML files)
    if (createCoverageZip)
      IO.zip(
        Path.allSubpaths(reportDir),
        crossTarget / "coverage.zip",
        Some(Instant.now().toEpochMilli())
      )
  }

  private def loadCoverage(
      crossTarget: File,
      log: Logger,
      sourceRoot: File
  ): Option[Coverage] = {

    val dataDir = crossTarget / "scoverage-data"
    val coverageFile = Serializer.coverageFile(dataDir)

    log.info(s"Reading scoverage instrumentation [$coverageFile]")

    if (coverageFile.exists) {

      val coverage = Serializer.deserialize(
        coverageFile,
        sourceRoot
      )

      log.info(s"Reading scoverage measurements...")
      val measurementFiles = IOUtils.findMeasurementFiles(dataDir)
      val measurements = IOUtils.invoked(measurementFiles)
      coverage.apply(measurements)
      Some(coverage)

    } else {
      None
    }
  }

  private def sourceEncoding(scalacOptions: Seq[String]): Option[String] = {
    val i = scalacOptions.indexOf("-encoding") + 1
    if (i > 0 && i < scalacOptions.length) Some(scalacOptions(i)) else None
  }

}
