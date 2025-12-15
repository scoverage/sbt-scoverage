package scoverage

import sbt._

object ScoverageKeys {
  // format: off
  lazy val coverageEnabled = settingKey[Boolean](
    "controls whether code instrumentation is enabled or not"
  )
  @transient
  lazy val coverageDeleteMeasurements = taskKey[Unit]("delete existing coverage measurements")
  @transient
  lazy val coverageReport = taskKey[Unit]("run report generation")
  @transient
  lazy val coverageAggregate = taskKey[Unit]("aggregate reports from subprojects")
  lazy val coverageExcludedPackages = settingKey[String]("regex for excluded packages")
  lazy val coverageExcludedFiles = settingKey[String]("regex for excluded file paths")
  lazy val coverageHighlighting = settingKey[Boolean]("enables range positioning for highlighting")
  lazy val coverageOutputCobertura = settingKey[Boolean]("enables cobertura XML report generation")
  lazy val coverageOutputXML = settingKey[Boolean]("enables xml report generation")
  lazy val coverageOutputHTML = settingKey[Boolean]("enables html report generation")
  lazy val coverageOutputDebug = settingKey[Boolean]("turn on the debug report")
  lazy val coverageOutputTeamCity = settingKey[Boolean]("turn on teamcity reporting")
  lazy val coverageScalacPluginVersion = settingKey[String]("version of scalac-scoverage-plugin to use")
  lazy val coverageDataDir = settingKey[File]("directory where the measurements and report files will be stored")
  lazy val coverageSourceRoot = settingKey[File]("the source root of the project")
  // format: on

  lazy val coverageMinimumStmtTotal =
    settingKey[Double]("scoverage minimum coverage: statement total")
  lazy val coverageMinimumBranchTotal =
    settingKey[Double]("scoverage minimum coverage: branch total")
  lazy val coverageMinimumStmtPerPackage =
    settingKey[Double]("scoverage minimum coverage: statement per package")
  lazy val coverageMinimumBranchPerPackage =
    settingKey[Double]("scoverage minimum coverage: branch per package")
  lazy val coverageMinimumStmtPerFile =
    settingKey[Double]("scoverage minimum coverage: statement per file")
  lazy val coverageMinimumBranchPerFile =
    settingKey[Double]("scoverage minimum coverage: branch per file")
  lazy val coverageFailOnMinimum =
    settingKey[Boolean]("if coverage is less than minimum then fail build")
}
