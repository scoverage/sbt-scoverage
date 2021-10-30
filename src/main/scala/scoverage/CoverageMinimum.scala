package scoverage

import sbt._
import scoverage.domain.Coverage
import scoverage.domain.CoverageMetrics
import scoverage.domain.DoubleFormat.twoFractionDigits

case class CoverageMinimum(
    statement: Double,
    branch: Double
) {
  def checkCoverage(
      metrics: CoverageMetrics,
      metric: String
  )(implicit log: Logger): Boolean = {
    CoverageMinimum.checkCoverage(
      s"Branch:$metric",
      branch,
      metrics.branchCoveragePercent
    ) &&
    CoverageMinimum.checkCoverage(
      s"Stmt:$metric",
      statement,
      metrics.statementCoveragePercent
    )
  }

}

object CoverageMinimum {

  def checkCoverage(
      metric: String,
      min: Double,
      cper: Double
  )(implicit log: Logger): Boolean = {
    // check for default minimum
    min <= 0 || {
      def is100(d: Double) = Math.abs(100 - d) <= 0.00001

      if (is100(min) && is100(cper)) {
        log.debug(s"100% Coverage: $metric")
        true
      } else {
        val ok: Boolean = min <= cper
        def minfmt = twoFractionDigits(min)
        def cfmt = twoFractionDigits(cper)
        if (ok) {
          log.debug(s"Coverage is above minimum [$cfmt% >= $minfmt%]: $metric")
        } else {
          log.error(s"Coverage is below minimum [$cfmt% < $minfmt%]: $metric")
        }
        ok
      }
    }
  }

  case class All(
      total: CoverageMinimum,
      perPackage: CoverageMinimum,
      perFile: CoverageMinimum
  ) {
    def checkCoverage(
        coverage: Coverage,
        failOnMin: Boolean
    )(implicit log: Logger): Unit = {
      val ok: Boolean = total.checkCoverage(coverage, "Total") &&
        coverage.packages.forall(pkg =>
          perPackage.checkCoverage(pkg, s"Package:${pkg.name}")
        ) &&
        coverage.files.forall(file =>
          perFile.checkCoverage(file, s"File:${file.filename}")
        )

      if (!ok && failOnMin)
        throw new RuntimeException("Coverage minimum was not reached")

      log.info(
        s"All done. Coverage was" +
          s" stmt=[${coverage.statementCoverageFormatted}%]" +
          s" branch=[${coverage.branchCoverageFormatted}%]"
      )
    }

  }

  def all = Def.setting {
    import ScoverageKeys._
    val stmtTotal =
      math.max(coverageMinimum.value, coverageMinimumStmtTotal.value)
    All(
      total = CoverageMinimum(
        statement = stmtTotal,
        branch = coverageMinimumBranchTotal.value
      ),
      perPackage = CoverageMinimum(
        statement = coverageMinimumStmtPerPackage.value,
        branch = coverageMinimumBranchPerPackage.value
      ),
      perFile = CoverageMinimum(
        statement = coverageMinimumStmtPerFile.value,
        branch = coverageMinimumBranchPerFile.value
      )
    )
  }

}
