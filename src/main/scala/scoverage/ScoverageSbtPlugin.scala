package scoverage

import sbt.Keys._
import sbt._

object ScoverageSbtPlugin extends ScoverageSbtPlugin

class ScoverageSbtPlugin extends sbt.Plugin {

  val OrgScoverage = "org.scoverage"
  val ArtifactId = "scalac-scoverage-plugin"
  val ScoverageVersion = "0.99.7"

  object ScoverageKeys {
    val excludedPackages = SettingKey[String]("scoverage-excluded-packages")
    val minimumCoverage = SettingKey[Double]("scoverage-minimum-coverage")
    val failOnMinimumCoverage = SettingKey[Boolean]("scoverage-fail-on-minimum-coverage")
    val highlighting = settingKey[Boolean]("enables range positioning for highlighting")
  }

  import ScoverageKeys._

  lazy val Scoverage: Configuration = config("scoverage") extend Compile
  lazy val ScoverageTest: Configuration = config("scoverage-test") extend Scoverage

  lazy val instrumentSettings: Seq[Setting[_]] = {
    inConfig(Scoverage)(Defaults.compileSettings) ++
      inConfig(ScoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(Scoverage.hide, ScoverageTest.hide),
        libraryDependencies += {
          OrgScoverage % (ArtifactId + "_" + scalaBinaryVersion.value) % ScoverageVersion % Scoverage.name
        },
        sources in Scoverage := (sources in Compile).value,
        sourceDirectory in Scoverage := (sourceDirectory in Compile).value,
        resourceDirectory in Scoverage := (resourceDirectory in Compile).value,
        resourceGenerators in Scoverage := (resourceGenerators in Compile).value,
        javacOptions in Scoverage := (javacOptions in Compile).value,
        javaOptions in Scoverage := (javaOptions in Compile).value,

        minimumCoverage := 0, // default is no minimum
        failOnMinimumCoverage := false,
        highlighting := false,
        excludedPackages in Scoverage := "",

        scalacOptions in Scoverage := {
            val target = crossTarget.value
            val scoverageDeps = update.value matching configurationFilter(Scoverage.name)
            scoverageDeps.find(_.getAbsolutePath.contains(ArtifactId)) match {
              case None => throw new Exception(s"Fatal: $ArtifactId not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-P:scoverage:excludedPackages:" + Option(excludedPackages.value).getOrElse(""),
                  "-P:scoverage:dataDir:" + target.getAbsolutePath + "/scoverage-data"
                )
            }
        },

        scalacOptions in Scoverage ++= (if (highlighting.value) List("-Yrangepos") else Nil),

        sources in ScoverageTest <<= (sources in Test),
        sourceDirectory in ScoverageTest <<= (sourceDirectory in Test),
        resourceDirectory in ScoverageTest <<= (resourceDirectory in Test),
        resourceGenerators in ScoverageTest <<= (resourceGenerators in Test),
        unmanagedResources in ScoverageTest <<= (unmanagedResources in Test),
        javacOptions in ScoverageTest <<= (javacOptions in Test),
        javaOptions in ScoverageTest <<= (javaOptions in Test),
        fork in ScoverageTest <<= (fork in Test),

        externalDependencyClasspath in Scoverage <<= Classpaths
          .concat(externalDependencyClasspath in Scoverage, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in ScoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in Scoverage <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in ScoverageTest <<= (testOptions in Test),

        // copy the test task into compile so we can do scoverage:test instead of scoverage-test:test
        test in Scoverage <<= (test in ScoverageTest)
      )
  }
}