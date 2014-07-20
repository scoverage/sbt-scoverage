package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  val OrgScoverage = "org.scoverage"
  val ScalacArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "0.99.5"

  object ScoverageKeys {
    lazy val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
    lazy val minimumCoverage = SettingKey[Double]("scoverage-minimum-coverage")
    lazy val failOnMinimumCoverage = SettingKey[Boolean]("scoverage-fail-on-minimum-coverage")
    lazy val highlighting = SettingKey[Boolean]("scoverage-highlighting", "enables range positioning for highlighting")
    lazy val postTestTask = taskKey[Unit]("scoverage-posttestcleanup")
  }

  import ScoverageKeys._

  lazy val ScoverageCompile: Configuration = config("scoverage")
  lazy val ScoverageTest: Configuration = config("scoverage-test") extend ScoverageCompile
  lazy val ScoverageITest: Configuration = config("scoverage-itest") extend ScoverageTest

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(ScoverageCompile)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      inConfig(ScoverageITest)(Defaults.itSettings) ++
      Seq(
        ivyConfigurations ++= Seq(ScoverageCompile.hide, ScoverageTest.hide),
        libraryDependencies += {
          OrgScoverage % (ScalacArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % ScoverageCompile.name
        },
        sources in ScoverageCompile <<= (sources in Compile),
        sourceDirectory in ScoverageCompile <<= (sourceDirectory in Compile),
        resourceDirectory in ScoverageCompile <<= (resourceDirectory in Compile),
        resourceGenerators in ScoverageCompile <<= (resourceGenerators in Compile),
        excludedPackages in ScoverageCompile := "",
        javacOptions in ScoverageCompile <<= (javacOptions in Compile),
        javaOptions in ScoverageCompile <<= (javaOptions in Compile),

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

        postTestTask := {
          val cross = crossTarget.value
          val compileSourceDirectory = (scalaSource in Compile).value
          val baseDir = (baseDirectory in Compile).value
          val log = sbt.Keys.streams.value.log

          log.info(s"[scoverage] Waiting for measurement data to sync...")
          Thread.sleep(3000) // have noticed some delay in writing, hacky but works

          val dataDir = cross / "/scoverage-data"
          val coberturaDir = cross / "coverage-report"
          val reportDir = cross / "scoverage-report"
          coberturaDir.mkdirs()
          reportDir.mkdirs()

          val coverageFile = IOUtils.coverageFile(dataDir)
          val measurementFiles = IOUtils.findMeasurementFiles(dataDir)

          log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
          log.info(s"[scoverage] Reading scoverage measurements...")

          val coverage = IOUtils.deserialize(coverageFile)
          val measurements = IOUtils.invoked(measurementFiles)
          coverage.apply(measurements)

          log.info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
          new CoberturaXmlWriter(baseDir, coberturaDir).write(coverage)

          log.info(s"[scoverage] Generating XML report [${reportDir.getAbsolutePath}/scoverage.xml]")
          new ScoverageXmlWriter(compileSourceDirectory, reportDir, false).write(coverage)
          new ScoverageXmlWriter(compileSourceDirectory, reportDir, true).write(coverage)

          log.info(s"[scoverage] Generating HTML report [${reportDir.getAbsolutePath}/index.html]")
          new ScoverageHtmlWriter(compileSourceDirectory, reportDir).write(coverage)

          log.info("[scoverage] Reports completed")
          val min = minimumCoverage.value
          val failOnMin = failOnMinimumCoverage.value

          // check for default minimum
          if (min > 0) {
            if (min > coverage.statementCoveragePercent) {
              log
                .error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
              if (failOnMin)
                throw new RuntimeException("Coverage minimum was not reached")
            } else {
              log
                .info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
            }
          }

          log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
        },

        scalacOptions in ScoverageCompile ++= (if (highlighting.value) List("-Yrangepos") else Nil),

        sources in ScoverageTest <<= (sources in Test),
        sourceDirectory in ScoverageTest <<= (sourceDirectory in Test),
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),
        resourceGenerators in ScoverageTest <<= (resourceGenerators in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        javacOptions in ScoverageTest <<= (javacOptions in Test),
        javaOptions in ScoverageTest <<= (javaOptions in Test),
        fork in ScoverageTest <<= (fork in Test),
        testOptions in ScoverageTest <<= (testOptions in Test),
        sources in ScoverageITest <<= (sources in Test),

        sourceDirectory in ScoverageITest <<= (sourceDirectory in Test),
        resourceDirectory in ScoverageITest <<= (resourceDirectory in Test),
        resourceGenerators in ScoverageITest <<= (resourceGenerators in Test),
        unmanagedResources in ScoverageITest <<= (unmanagedResources in Test),
        javacOptions in ScoverageITest <<= (javacOptions in Test),
        javaOptions in ScoverageITest <<= (javaOptions in Test),
        fork in ScoverageITest <<= (fork in Test),
        testOptions in ScoverageITest <<= (testOptions in Test),

        externalDependencyClasspath in ScoverageCompile <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageCompile, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),
        externalDependencyClasspath in ScoverageITest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in ScoverageCompile <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },
        internalDependencyClasspath in ScoverageITest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageITest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        test in ScoverageTest := {
          (test in Test).value
          postTestTask.value
        },
        test in ScoverageITest := {
          (test in Test).value
          postTestTask.value
        },

        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test etc
        test in ScoverageCompile <<= (test in ScoverageTest),
        test in ScoverageCompile <<= (test in ScoverageITest)
      )
  }
}
