package scoverage

import sbt._
import sbt.Keys._
import scoverage.report._

import scala.collection.mutable.ListBuffer

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  val OrgScoverage = "org.scoverage"
  val ScalacArtifact = "scalac-scoverage-plugin"
  val ScoverageVersion = "0.99.10"

  object ScoverageKeys {
    val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
    val scoverageExcludedFiles = settingKey[String]("regex for excluded file paths")
    val minimumCoverage = SettingKey[Double]("scoverage-minimum-coverage")
    val failOnMinimumCoverage = SettingKey[Boolean]("scoverage-fail-on-minimum-coverage")
    val highlighting = SettingKey[Boolean]("scoverage-highlighting", "enables range positioning for highlighting")
    val scoverageOutputCobertua = settingKey[Boolean]("enables cobertura XML report generation")
    val scoverageOutputXML = settingKey[Boolean]("enables xml report generation")
    val scoverageOutputHTML = settingKey[Boolean]("enables html report generation")
  }

  import ScoverageKeys._

  lazy val Scoverage: Configuration = config("scoverage")
  lazy val ScoverageTest: Configuration = config("scoverage-test") extend Scoverage

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(Scoverage)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(Scoverage.hide, ScoverageTest.hide),
        libraryDependencies += {
          OrgScoverage % (ScalacArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % "compile"
        },
        // Source paths
        sourceDirectory in Scoverage <<= (sourceDirectory in Compile),
        sourceManaged in Scoverage <<= (sourceManaged in Compile),
        scalaSource in Scoverage <<= (scalaSource in Compile),
        javaSource in Scoverage <<= (javaSource in Compile),
        sourceDirectories in Scoverage <<= (sourceDirectories in Compile),
        unmanagedSourceDirectories in Scoverage <<= (unmanagedSourceDirectories in Compile),
        unmanagedSources in Scoverage <<= (unmanagedSources in Compile),
        managedSourceDirectories in Scoverage <<= (managedSourceDirectories in Compile),
        managedSources in Scoverage <<= (managedSources in Compile),
        sources in Scoverage <<= (sources in Compile),
        sourcesInBase in Scoverage <<= (sourcesInBase in Compile),
        javacOptions in Scoverage <<= (javacOptions in Compile),
        javaOptions in Scoverage <<= (javaOptions in Compile),
        // Resource paths
        resourceDirectory in Scoverage <<= (resourceDirectory in Compile),
        resourceManaged in Scoverage <<= (resourceManaged in Compile),
        unmanagedResourceDirectories in Scoverage <<= (unmanagedResourceDirectories in Compile),
        unmanagedResources in Scoverage <<= (unmanagedResources in Compile),
        managedResourceDirectories in Scoverage <<= (managedResourceDirectories in Compile),
        managedResources in Scoverage <<= (managedResources in Compile),
        resourceDirectories in Scoverage <<= (resourceDirectories in Compile),
        resources in Scoverage <<= (resources in Compile),
        // classpaths
        unmanagedClasspath in Scoverage <<= (unmanagedClasspath in Compile),
        unmanagedJars in Scoverage <<= (unmanagedJars in Compile),
        managedClasspath in Scoverage <<= (managedClasspath in Compile),
        internalDependencyClasspath in Scoverage <<= (internalDependencyClasspath in Compile),
        externalDependencyClasspath in Scoverage <<= (externalDependencyClasspath in Compile),
        dependencyClasspath in Scoverage <<= (dependencyClasspath in Compile),

        excludedPackages := "",
        scoverageExcludedFiles := "",
        minimumCoverage := 0, // default is no minimum
        failOnMinimumCoverage := false,
        highlighting := false,
        scoverageOutputXML := true,
        scoverageOutputHTML := true,
        scoverageOutputCobertua := true,

        scalacOptions in Scoverage ++= {
          val scoverageDeps = update.value matching configurationFilter(Compile.name)
          scoverageDeps.find(_.getAbsolutePath.contains(ScalacArtifact)) match {
            case None => throw new Exception(s"Fatal: $ScalacArtifact not in libraryDependencies")
            case Some(classpath) =>
              Seq(
                Some(s"-Xplugin:${classpath.getAbsolutePath}"),
                Some(s"-P:scoverage:dataDir:${crossTarget.value.getAbsolutePath}/scoverage-data"),
                Option(excludedPackages.value.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedPackages:$v"),
                Option(scoverageExcludedFiles.value.trim).filter(_.nonEmpty).map(v => s"-P:scoverage:excludedFiles:$v")
              ).flatten
          }
        },

        scalacOptions in Scoverage ++= (if (highlighting.value) List("-Yrangepos") else Nil),

        // Source paths
        sourceDirectory in ScoverageTest <<= (sourceDirectory in Test),
        sourceManaged in ScoverageTest <<= (sourceManaged in Test),
        scalaSource in ScoverageTest <<= (scalaSource in Test),
        javaSource in ScoverageTest <<= (javaSource in Test),
        sourceDirectories in ScoverageTest <<= (sourceDirectories in Test),
        unmanagedSourceDirectories in ScoverageTest <<= (unmanagedSourceDirectories in Test),
        unmanagedSources in ScoverageTest <<= (unmanagedSources in Test),
        managedSourceDirectories in ScoverageTest <<= (managedSourceDirectories in Test),
        managedSources in ScoverageTest <<= (managedSources in Test),
        sources in ScoverageTest <<= (sources in Test),
        sourcesInBase in ScoverageTest <<= (sourcesInBase in Test),
        javacOptions in ScoverageTest <<= (javacOptions in Test),
        javaOptions in ScoverageTest <<= (javaOptions in Test),
        // Resource paths
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),
        resourceManaged in ScoverageTest <<= (resourceManaged in Test),
        unmanagedResourceDirectories in ScoverageTest <<= (unmanagedResourceDirectories in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        managedResourceDirectories in ScoverageTest <<= (managedResourceDirectories in Test),
        managedResources in ScoverageTest <<= (managedResources in Test),
        resourceDirectories in ScoverageTest <<= (resourceDirectories in Test),
        resources in ScoverageTest <<= (resources in Test),
        // classpaths
        unmanagedClasspath in ScoverageTest <<= (unmanagedClasspath in Test),
        unmanagedJars in ScoverageTest <<= (unmanagedJars in Test),
        managedClasspath in ScoverageTest <<= (managedClasspath in Test),
        // addds the compiled output of the compile phase to the classpath
        internalDependencyClasspath in ScoverageTest <<=
          (internalDependencyClasspath in ScoverageTest, internalDependencyClasspath in Test, classDirectory in Compile)
            map { (scoverageDeps, testDeps, noninstrumentedClasses) =>
            scoverageDeps ++ testDeps.filter(arg => {
              arg.data != noninstrumentedClasses
            })
          },
        externalDependencyClasspath in ScoverageTest <<= (externalDependencyClasspath in Test),
        fork in ScoverageTest <<= (fork in Test),

        testOptions in ScoverageTest <<= (testOptions in Test),
        testOptions in ScoverageTest <+= testsCleanup,

        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test
        test in Scoverage <<= (test in ScoverageTest),
        testOnly in Scoverage <<= (testOnly in ScoverageTest),
        testQuick in Scoverage <<= (testQuick in ScoverageTest)
      )
  }

  /** Generate hook that is invoked after the tests have executed. */
  def testsCleanup = {
    (crossTarget in ScoverageTest,
      baseDirectory in Compile,
      scalaSource in Compile,
      definedTests in ScoverageTest,
      minimumCoverage in ScoverageTest,
      failOnMinimumCoverage in ScoverageTest,
      streams in Global) map {
      (crossTarget,
       baseDirectory,
       compileSourceDirectory,
       definedTests,
       min,
       failOnMin,
       s) =>
        Tests.Cleanup {
          () =>

            s.log.info(s"[scoverage] Waiting for measurement data to sync...")
            Thread.sleep(2000) // have noticed some delay in writing, hacky but works

            val dataDir = crossTarget / "/scoverage-data"
            val coberturaDir = crossTarget / "coverage-report"
            val reportDir = crossTarget / "scoverage-report"
            coberturaDir.mkdirs()
            reportDir.mkdirs()

            val coverageFile = IOUtils.coverageFile(dataDir)
            val measurementFiles = IOUtils.findMeasurementFiles(dataDir)

            s.log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")

            if (coverageFile.exists) {
              s.log.info(s"[scoverage] Reading scoverage measurements...")
              val coverage = IOUtils.deserialize(coverageFile)
              val measurements = IOUtils.invoked(measurementFiles)
              coverage.apply(measurements)

              s.log.info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
              new CoberturaXmlWriter(baseDirectory, coberturaDir).write(coverage)

              s.log.info(s"[scoverage] Generating XML report [${reportDir.getAbsolutePath}/scoverage.xml]")
              new ScoverageXmlWriter(compileSourceDirectory, reportDir, false).write(coverage)
              new ScoverageXmlWriter(compileSourceDirectory, reportDir, true).write(coverage)

              s.log.info(s"[scoverage] Generating HTML report [${reportDir.getAbsolutePath}/index.html]")
              new ScoverageHtmlWriter(compileSourceDirectory, reportDir).write(coverage)

              s.log.info("[scoverage] Reports completed")

              // check for default minimum
              if (min > 0) {
                def is100(d: Double) = Math.abs(100 - d) <= 0.00001

                if (is100(min) && is100(coverage.statementCoveragePercent)) {
                  s.log.info(s"[scoverage] 100% Coverage !")
                } else if (min > coverage.statementCoveragePercent) {
                  s
                    .log
                    .error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
                  if (failOnMin)
                    throw new RuntimeException("Coverage minimum was not reached")
                } else {
                  s.log.info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
                }
              }

              s.log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
              ()
            } else {
              s.log.info(s"[scoverage] Scoverage data file does not exist. Skipping report generation")
              ()
            }
        }
    }
  }
}

//package scoverage
//
//import sbt.Keys._
//import sbt._
//import scoverage.report.{CoberturaXmlWriter, ScoverageHtmlWriter, ScoverageXmlWriter}
//
//object ScoverageSbtPlugin extends ScoverageSbtPlugin
//
//class ScoverageSbtPlugin extends sbt.Plugin {
//
//  val OrgScoverage = "org.scoverage"
//  val ScalacArtifact = "scalac-scoverage-plugin"
//  val ScoverageVersion = "0.99.7"
//
//  object ScoverageKeys {
//    val excludedPackages = settingKey[String]("scoverage-excluded-packages")
//    val minimumCoverage = settingKey[Double]("scoverage-minimum-coverage")
//    val failOnMinimumCoverage = settingKey[Boolean]("scoverage-fail-on-minimum-coverage")
//    val highlighting = settingKey[Boolean]("enables range positioning for highlighting")
//    val scoverageOutputCobertua = settingKey[Boolean]("enables cobertura XML report generation")
//    val scoverageOutputXML = settingKey[Boolean]("enables xml report generation")
//    val scoverageOutputHTML = settingKey[Boolean]("enables html report generation")
//    val scoverageReport = taskKey[Unit]("runs post test report generation")
//  }
//
//  import ScoverageKeys._
//
//  lazy val Scoverage: Configuration = config("scoverage")
//  lazy val ScoverageTest: Configuration = config("scoverage-test") extend Scoverage
//
//  lazy val baseInstrumentSettings: Seq[Setting[_]] = Seq(
//    minimumCoverage := 0, // default is no minimum
//    failOnMinimumCoverage := false,
//    highlighting := false,
//    excludedPackages := "",
//    scoverageOutputXML := true,
//    scoverageOutputHTML := true,
//    scoverageOutputCobertua := true
//  )
//
//  lazy val report: Def.Initialize[Task[Unit]] = Def.task {
//    streams.value.log.info(s"[scoverage] Waiting for measurement data to sync...")
//    Thread.sleep(2000) // have noticed some delay in writing, hacky but works
//
//    val dataDir = crossTarget.value / "/scoverage-data"
//    val coberturaDir = crossTarget.value / "coverage-report"
//    val reportDir = crossTarget.value / "scoverage-report"
//    coberturaDir.mkdirs()
//    reportDir.mkdirs()
//
//    val coverageFile = IOUtils.coverageFile(dataDir)
//    val measurementFiles = IOUtils.findMeasurementFiles(dataDir)
//
//    streams.value.log.info(s"[scoverage] Reading scoverage instrumentation [$coverageFile]")
//    streams.value.log.info(s"[scoverage] Reading scoverage measurements...")
//
//    val coverage = IOUtils.deserialize(coverageFile)
//    val measurements = IOUtils.invoked(measurementFiles)
//    coverage.apply(measurements)
//
//    if (scoverageOutputCobertua.value) {
//      streams.value.log
//        .info(s"[scoverage] Generating Cobertura report [${coberturaDir.getAbsolutePath}/cobertura.xml]")
//      new CoberturaXmlWriter((baseDirectory in Compile).value, coberturaDir).write(coverage)
//    }
//
//    if (scoverageOutputXML.value) {
//      streams.value.log.info(s"[scoverage] Generating XML report [${reportDir.getAbsolutePath}/scoverage.xml]")
//      new ScoverageXmlWriter((scalaSource in Compile).value, reportDir, false).write(coverage)
//      new ScoverageXmlWriter((scalaSource in Compile).value, reportDir, true).write(coverage)
//    }
//
//    if (scoverageOutputHTML.value) {
//      streams.value.log.info(s"[scoverage] Generating HTML report [${reportDir.getAbsolutePath}/index.html]")
//      new ScoverageHtmlWriter((scalaSource in Compile).value, reportDir).write(coverage)
//    }
//
//    val min = minimumCoverage.value
//    val failOnMin = failOnMinimumCoverage.value
//
//    // check for default minimum
//    if (min > 0) {
//      def is100(d: Double) = Math.abs(100 - d) <= 0.00001
//
//      if (is100(coverage.statementCoveragePercent)) {
//        streams.value.log.info(s"[scoverage] 100% Coverage !")
//      } else if (min > coverage.statementCoveragePercent) {
//        streams.value.log
//          .error(s"[scoverage] Coverage is below minimum [${coverage.statementCoverageFormatted}% < $min%]")
//        if (failOnMin)
//          throw new RuntimeException("Coverage minimum was not reached")
//      } else {
//        streams.value.log
//          .info(s"[scoverage] Coverage is above minimum [${coverage.statementCoverageFormatted}% > $min%]")
//      }
//    }
//
//    streams.value.log.info(s"[scoverage] All done. Coverage was [${coverage.statementCoverageFormatted}%]")
//    ()
//  }
//
//  lazy val instrumentSettings: Seq[Setting[_]] = {
//    baseInstrumentSettings ++
//      inConfig(Scoverage)(Defaults.compileSettings) ++
//      inConfig(ScoverageTest)(Defaults.testSettings) ++
//      Seq(
//        ivyConfigurations ++= Seq(Scoverage.hide),
//        libraryDependencies += {
//          OrgScoverage % (ScalacArtifact + "_" + scalaBinaryVersion.value) % ScoverageVersion % "compile"
//        },
//        // Source paths
//        sourceDirectory in Scoverage := (sourceDirectory in Compile).value,
//        sourceManaged in Scoverage := (sourceManaged in Compile).value,
//        scalaSource in Scoverage := (scalaSource in Compile).value,
//        javaSource in Scoverage := (javaSource in Compile).value,
//        sourceDirectories in Scoverage := (sourceDirectories in Compile).value,
//        unmanagedSourceDirectories in Scoverage := (unmanagedSourceDirectories in Compile).value,
//        unmanagedSources in Scoverage := (unmanagedSources in Compile).value,
//        managedSourceDirectories in Scoverage := (managedSourceDirectories in Compile).value,
//        managedSources in Scoverage := (managedSources in Compile).value,
//        sources in Scoverage := (sources in Compile).value,
//        sourcesInBase in Scoverage := (sourcesInBase in Compile).value,
//        javacOptions in Scoverage := (javacOptions in Compile).value,
//        javaOptions in Scoverage := (javaOptions in Compile).value,
//        // Resource paths
//        resourceDirectory in Scoverage := (resourceDirectory in Compile).value,
//        resourceManaged in Scoverage := (resourceManaged in Compile).value,
//        unmanagedResourceDirectories in Scoverage := (unmanagedResourceDirectories in Compile).value,
//        unmanagedResources in Scoverage := (unmanagedResources in Compile).value,
//        managedResourceDirectories in Scoverage := (managedResourceDirectories in Compile).value,
//        managedResources in Scoverage := (managedResources in Compile).value,
//        resourceDirectories in Scoverage := (resourceDirectories in Compile).value,
//        resources in Scoverage := (resources in Compile).value,
//        // classpaths
//        unmanagedClasspath in Scoverage := (unmanagedClasspath in Compile).value,
//        unmanagedJars in Scoverage := (unmanagedJars in Compile).value,
//        managedClasspath in Scoverage := (managedClasspath in Compile).value,
//        internalDependencyClasspath in Scoverage := (internalDependencyClasspath in Compile).value,
//        externalDependencyClasspath in Scoverage := (externalDependencyClasspath in Compile).value,
//        dependencyClasspath in Scoverage := (dependencyClasspath in Compile).value,
//        fullClasspath in Scoverage := (fullClasspath in Compile).value,
//
//        fork in Scoverage := (fork in Compile).value,
//
//        scalacOptions in Scoverage <++= (update, crossTarget, excludedPackages) map {
//          (report, target, excludedPackages) =>
//            val scoverageDeps = report matching configurationFilter(Compile.name)
//            scoverageDeps.find(_.getAbsolutePath.contains(ScalacArtifact)) match {
//              case None => throw new Exception(s"Fatal: $ScalacArtifact not in libraryDependencies")
//              case Some(classpath) =>
//                Seq(
//                  "-Xplugin:" + classpath.getAbsolutePath,
//                  "-P:scoverage:excludedPackages:" + Option(excludedPackages).getOrElse(""),
//                  "-P:scoverage:dataDir:" + target.getAbsolutePath + "/scoverage-data"
//                )
//            }
//        },
//
//        // Source paths
//        sourceDirectory in ScoverageTest := (sourceDirectory in Test).value,
//        sourceManaged in ScoverageTest := (sourceManaged in Test).value,
//        scalaSource in ScoverageTest := (scalaSource in Test).value,
//        javaSource in ScoverageTest := (javaSource in Test).value,
//        sourceDirectories in ScoverageTest := (sourceDirectories in Test).value,
//        unmanagedSourceDirectories in ScoverageTest := (unmanagedSourceDirectories in Test).value,
//        unmanagedSources in ScoverageTest := (unmanagedSources in Test).value,
//        managedSourceDirectories in ScoverageTest := (managedSourceDirectories in Test).value,
//        managedSources in ScoverageTest := (managedSources in Test).value,
//        sources in ScoverageTest := (sources in Test).value,
//        sourcesInBase in ScoverageTest := (sourcesInBase in Test).value,
//        javacOptions in ScoverageTest := (javacOptions in Test).value,
//        javaOptions in ScoverageTest := (javaOptions in Test).value,
//        // Resource paths
//        resourceDirectory in ScoverageTest := (resourceDirectory in Test).value,
//        resourceManaged in ScoverageTest := (resourceManaged in Test).value,
//        unmanagedResourceDirectories in ScoverageTest := (unmanagedResourceDirectories in Test).value,
//        unmanagedResources in ScoverageTest := (unmanagedResources in Test).value,
//        managedResourceDirectories in ScoverageTest := (managedResourceDirectories in Test).value,
//        managedResources in ScoverageTest := (managedResources in Test).value,
//        resourceDirectories in ScoverageTest := (resourceDirectories in Test).value,
//        resources in ScoverageTest := (resources in Test).value,
//        // classpaths
//        unmanagedClasspath in ScoverageTest := (unmanagedClasspath in Test).value,
//        unmanagedJars in ScoverageTest := (unmanagedJars in Test).value,
//        managedClasspath in ScoverageTest := (managedClasspath in Test).value,
//        internalDependencyClasspath in ScoverageTest := (internalDependencyClasspath in Test).value,
//        externalDependencyClasspath in ScoverageTest := (externalDependencyClasspath in Test).value,
//        dependencyClasspath in ScoverageTest := (dependencyClasspath in Test).value,
//        fullClasspath in ScoverageTest := (fullClasspath in Test).value,
//
//        fork in ScoverageTest := (fork in Test).value,
//
//        // test only
//        testOptions in ScoverageTest <<= (testOptions in Test),
//        scoverageReport in Scoverage := report.value,
//
//        // copy the test task into Scoverage config so we can do scoverage:test instead of scoverage-test:test
//        //test in Scoverage := (test in ScoverageTest)
//        test in Scoverage <<= (test in ScoverageTest)
//        //        testOnly in Scoverage := (testOnly in ScoverageTest) andFinally scoverageReportTaskvalue,
//        //        testQuick in Scoverage := (testQuick in ScoverageTest) doFinally scoverageReportTask
//      )
//  }
//
//  //        internalDependencyClasspath in ScoverageCompile <<= (internalDependencyClasspath in Compile),
//  //        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
//  //          (testDeps, scoverageDeps, oldClassDir) =>
//  //            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
//  //        },
//  //
//  //        testOptions in ScoverageTest <<= (testOptions in Test),
//  //        testOptions in ScoverageTest <+= testsCleanup,
//  //
//  //        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test
//  //        test in ScoverageCompile <<= (test in ScoverageTest)
//  //      )
//
//}
