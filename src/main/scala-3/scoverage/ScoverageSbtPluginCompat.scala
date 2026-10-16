package scoverage

import sbt.Keys.*
import sbt.*

private[scoverage] object ScoverageSbtPluginCompat {

  /** Default for `coverageDataDir` on sbt 2.
    *
    * sbt 2 caches `compile` and, on a cache hit, restores only its declared
    * outputs, i.e. the class directory. The scoverage-data directory
    * (`scoverage.coverage`, plus the directory the instrumented classes write
    * measurements into) is a side effect of compilation. Under `crossTarget` it
    * is not restored, so tests fail with a FileNotFoundException or
    * coverageReport finds no data. Keeping it inside the class directory makes
    * it part of the cached compile output.
    *
    * See https://github.com/scoverage/sbt-scoverage/issues/696
    */
  val defaultCoverageDataDir: Def.Initialize[File] =
    Def.setting((Compile / classDirectory).value)
}
