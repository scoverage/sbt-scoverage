package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  val OrgScoverage = "org.scoverage"
  val ScalacArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "0.99.7"

  object ScoverageKeys {
    lazy val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
    lazy val minimumCoverage = SettingKey[Double]("scoverage-minimum-coverage")
    lazy val failOnMinimumCoverage = SettingKey[Boolean]("scoverage-fail-on-minimum-coverage")
    lazy val highlighting = SettingKey[Boolean]("scoverage-highlighting", "enables range positioning for highlighting")
    lazy val scoverageReport = taskKey[Unit]("scoverage-report")
    lazy val scoverageMultiReport = taskKey[Unit]("scoverage-multireport")
  }

  import ScoverageKeys._

  lazy val ScoverageCompile: Configuration = config("scoverage")
  lazy val ScoverageTest: Configuration = config("scoverage-test") extend ScoverageCompile

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(ScoverageCompile)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(ScoverageCompile.hide, ScoverageTest.hide),
        libraryDependencies += {
          OrgScoverage % (ScalacArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % ScoverageCompile.name
        },

        minimumCoverage := 0, // default is no minimum
        failOnMinimumCoverage := false,
        highlighting := false,

        scalacOptions in ScoverageCompile <++= (crossTarget in ScoverageTest, update, excludedPackages in ScoverageCompile) map {
          (target, report, excluded) =>
            val scoverageDeps = report matching configurationFilter(ScoverageCompile.name)
            scoverageDeps.find(_.getAbsolutePath.contains(ScalacArtifact)) match {
              case None => throw new Exception(s"Fatal: $ScalacArtifact not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-P:scoverage:excludedPackages:" + Option(excluded).getOrElse(""),
                  "-P:scoverage:dataDir:" + target.getAbsolutePath + "/scoverage-data"
                )
            }
        },

        scoverageReport := {
          val cross = crossTarget.value
          val compileSourceDirectory = (scalaSource in Compile).value
          val baseDir = (baseDirectory in Compile).value
          val log = sbt.Keys.streams.value.log

          log.info(s"[scoverage] Waiting for measurement data to sync...")
          Thread.sleep(3000) // have noticed some delay in writing, hacky but works

          val dataDir = cross / "/scoverage-data"
          val coberturaDir = cross / "coverage-report"
          val scoverageDir = cross / "scoverage-report"
          coberturaDir.mkdirs()
          scoverageDir.mkdirs()

          val coverageFile = IOUtils.coverageFile(dataDir)
          val measurementFiles = IOUtils.findMeasurementFiles(dataDir)

          log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
          log.info(s"[scoverage] Reading scoverage measurements...")

          val coverage = IOUtils.deserialize(coverageFile)
          val measurements = IOUtils.invoked(measurementFiles)
          coverage.apply(measurements)

          log.info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
          new CoberturaXmlWriter(baseDir, coberturaDir).write(coverage)

          log.info(s"[scoverage] Generating XML report [${scoverageDir.getAbsolutePath}/scoverage.xml]")
          new ScoverageXmlWriter(compileSourceDirectory, scoverageDir, false).write(coverage)
          new ScoverageXmlWriter(compileSourceDirectory, scoverageDir, true).write(coverage)

          log.info(s"[scoverage] Generating HTML report [${scoverageDir.getAbsolutePath}/index.html]")
          new ScoverageHtmlWriter(compileSourceDirectory, scoverageDir).write(coverage)

          log.info("[scoverage] Reports completed")
          val min = minimumCoverage.value

          // check for default minimum
          if (min > 0) {
            if (min > coverage.statementCoveragePercent) {
              log.error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
              if (failOnMinimumCoverage.value) {
                // todo better way to fail an SBT build ?
                throw new RuntimeException("Coverage minimum was not reached. Failing build.")
              }
            } else {
              log.info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
            }
          }

          log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
        },

        scoverageMultiReport := {

        },

        scalacOptions in ScoverageCompile ++= (if (highlighting.value) List("-Yrangepos") else Nil),

        sources in ScoverageCompile <<= (sources in Compile),
        sourceDirectory in ScoverageCompile <<= (sourceDirectory in Compile),
        resourceDirectory in ScoverageCompile <<= (resourceDirectory in Compile),
        resourceGenerators in ScoverageCompile <<= (resourceGenerators in Compile),
        unmanagedResources in ScoverageCompile <<= (unmanagedResources in Compile),
        javaOptions in ScoverageCompile <<= (javaOptions in Compile),
        javacOptions in ScoverageCompile <<= (javacOptions in Compile),
        fork in ScoverageCompile <<= (fork in Compile),
        excludedPackages in ScoverageCompile := "",

        sources in ScoverageTest <<= (sources in Test),
        sourceDirectory in ScoverageTest <<= (sourceDirectory in Test),
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),
        resourceGenerators in ScoverageTest <<= (resourceGenerators in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        javaOptions in ScoverageTest <<= (javaOptions in Test),
        javacOptions in ScoverageTest <<= (javacOptions in Test),
        fork in ScoverageTest <<= (fork in Test),
        testOptions in ScoverageTest <<= (testOptions in Test),

        externalDependencyClasspath in ScoverageCompile <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageCompile, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in ScoverageCompile <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

	// After running the tests, create the coverage report:
        test in ScoverageTest := sequence(List(
          test in ScoverageTest,
          scoverageReport in ScoverageTest))
            .value,

        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test etc
        test in ScoverageCompile <<= (test in ScoverageTest)
      )
  }

  /**
   * Runs the given tasks sequentially.
   * See http://eed3si9n.com/sequencing-tasks-with-sbt-sequential
   */
  private def sequence(tasks: List[Def.Initialize[Task[Unit]]]): Def.Initialize[Task[Unit]] =
    tasks match {
      case Nil => Def.task{ () }
      case x :: xs => Def.taskDyn { val _ = x.value; sequence(xs) }
    }
}
