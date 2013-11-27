package org.scalescc.sbt

import sbt._
import sbt.Keys._
import scales._
import sbt.File
import scales.report.{ScalesHtmlWriter, CoberturaXmlWriter, ScalesXmlWriter}

object ScootSbtPlugin extends Plugin {

  val scootReportDir = SettingKey[File]("scoot-report-dir")

  lazy val Scoot = config("scoot")
  lazy val ScootTest = config("scoot-test") extend Scoot

  lazy val instrumentSettings = {
    inConfig(Scoot)(Defaults.compileSettings) ++
      inConfig(ScootTest)(Defaults.testSettings) ++
      Seq(
        scootReportDir <<= crossTarget / "coverage-report",

        ivyConfigurations ++= Seq(Scoot, ScootTest),

        resolvers += Resolver.url("local-ivy",
          new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),

        libraryDependencies += "com.sksamuel.scoot" %% "scalac-scoot-plugin" % "0.91.0" % Scoot.name,

        sources in Scoot <<= (sources in Compile),
        sourceDirectory in Scoot <<= (sourceDirectory in Compile),

        scalacOptions in Scoot <++= (name in Scoot, baseDirectory in Scoot, update) map {
          (n, b, report) =>
            val scootDeps = report matching configurationFilter("scoot")
            scootDeps.find(_.getAbsolutePath.contains("scalac-scoot-plugin")) match {
              case None => throw new Exception("Fatal: scalac-scoot-plugin not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-Yrangepos"
                )
            }
        },

        sources in ScootTest <<= (sources in Test),
        sourceDirectory in ScootTest <<= (sourceDirectory in Test),
        unmanagedResources in ScootTest <<= (unmanagedResources in Test),

        resourceDirectory in ScootTest <<= (resourceDirectory in Compile),

        externalDependencyClasspath in Scoot <<= Classpaths
          .concat(externalDependencyClasspath in Scoot, externalDependencyClasspath in Compile),
        externalDependencyClasspath in ScootTest <<= Classpaths
          .concat(externalDependencyClasspath in ScootTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in Scoot <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in ScootTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in ScootTest, classDirectory in Compile) map {
          (testDeps, scootDeps, oldClassDir) =>
            scootDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in ScootTest <+= testsCleanup,

        // make scoot config the same as scootTest config
        test in Scoot <<= (test in ScootTest)
      )
  }

  /** Generate hook that is invoked after each tests execution. */
  def testsCleanup = {
    (target in ScootTest,
      definedTests in ScootTest,
      streams) map {
      (target,
       definedTests,
       streams) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else {
          Tests.Cleanup {
            () =>

              println(target)
              println(Env.coverageFile)
              println(Env.measurementFile)

              val coverage = IOUtils.deserialize(getClass.getClassLoader, Env.coverageFile)
              val measurements = IOUtils.invoked(Env.measurementFile)
              coverage.apply(measurements)

              val targetDirectory = new File("target/coverage-report")
              targetDirectory.mkdirs()

              println("Generating ScootXML report...")
              ScootXmlWriter.write(coverage, targetDirectory)

              println("Generating CoberturaXML report...")
              CoberturaXmlWriter.write(coverage, targetDirectory)

              println("Generating ScootHTML report...")
              ScootXmlWriter.write(coverage, targetDirectory)
          }
        }
    }
  }
}
