package scoverage

import sbt._

object ScoverageKeys {
  lazy val coverageEnabled = settingKey[Boolean]("controls whether code instrumentation is enabled or not")
  lazy val coverageReport = taskKey[Unit]("run report generation")
  lazy val coverageAggregate = taskKey[Unit]("aggregate reports from subprojects")
  lazy val coverageExcludedPackages = settingKey[String]("regex for excluded packages")
  lazy val coverageExcludedFiles = settingKey[String]("regex for excluded file paths")
  lazy val coverageMinimum = settingKey[Double]("scoverage-minimum-coverage")
  lazy val coverageFailOnMinimum = settingKey[Boolean]("if coverage is less than this value then fail build")
  lazy val coverageHighlighting = settingKey[Boolean]("enables range positioning for highlighting")
  lazy val coverageOutputCobertura = settingKey[Boolean]("enables cobertura XML report generation")
  lazy val coverageOutputXML = settingKey[Boolean]("enables xml report generation")
  lazy val coverageOutputHTML = settingKey[Boolean]("enables html report generation")
  lazy val coverageOutputDebug = settingKey[Boolean]("turn on the debug report")
  lazy val coverageCleanSubprojectFiles = settingKey[Boolean]("removes subproject data after an aggregation")
  lazy val coverageOutputTeamCity = settingKey[Boolean]("turn on teamcity reporting")

  // Artifact settings allow the override of default settings for custom applications.
  // The use of these settings is not advised for regular applications, and most definitely "breaks all warranties"
  lazy val coverageScalacPluginOrg = settingKey[String]("organisation name of scalac-scoverage-plugin to use")
  lazy val coverageScalacPluginArtifact = settingKey[String]("artifact name of scalac-scoverage-plugin to use")
  lazy val coverageScalacPluginVersion = settingKey[String]("version of scalac-scoverage-plugin to use")
  lazy val coverageScalacRuntimeOrg = settingKey[String]("organisation name of scalac-scoverage-runtime to use")
  lazy val coverageScalacRuntimeArtifact = settingKey[String]("artifact name of scalac-scoverage-runtime to use")
  lazy val coverageScalacRuntimeVersion = settingKey[String]("version of scalac-scoverage-runtime to use")

  //Use this to completely overide the library settings: This is the last resort option: use this, on you're on your own.
  lazy val coverageLibraryDependencies = settingKey[Seq[ModuleID]]("Use these library dependencies if coverage is enabled")
}
