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
}
