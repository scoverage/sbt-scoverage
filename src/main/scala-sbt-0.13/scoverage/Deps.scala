package scoverage

import java.io.File

import sbt._
import sbt.{UpdateReport, configurationFilter}

object Deps {
  def scoverageDeps(report: UpdateReport, configName: String): Seq[File] = report matching configurationFilter(configName)
}
