package scoverage

import sbt.Keys._
import sbt._

private[scoverage] object ScoverageSbtPluginCompat {
  implicit class DefOps(private val self: sbt.Def.type) extends AnyVal {
    def uncached[A](a: A): A = a
  }

  /** Default for `coverageDataDir` on sbt 1. */
  val defaultCoverageDataDir: Def.Initialize[File] =
    Def.setting(crossTarget.value)
}
