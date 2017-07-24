package scoverage

import java.io.File

import sbt._
import sbt.librarymanagement.UpdateReport

object Deps {
  def scoverageDeps(updateReport: UpdateReport, configName: String): Seq[File] = updateReport matching configurationFilter(configName)
}
