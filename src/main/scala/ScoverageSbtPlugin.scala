import sbt._
import sbt.Keys._

object ScoverageSbtPlugin extends Plugin {

  val scoverageVersion = SettingKey[String]("scoverage-version")

  lazy val scoverage = config("scoverage")
  lazy val scoverageTest = config("scoverage-test") extend scoverage

  lazy val instrumentSettings = {
    inConfig(scoverage)(Defaults.compileSettings) ++
      inConfig(scoverageTest)(Defaults.testSettings) ++
      Seq(
        ivyConfigurations ++= Seq(scoverage, scoverageTest),

        //        resolvers += Resolver.url("local-ivy",
        //        new URL("file://" + Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),

        libraryDependencies +=
          "com.sksamuel.scoverage" %% "scalac-scoverage-plugin" % "0.93" % scoverage.name,

        sources in scoverage <<= (sources in Compile),
        sourceDirectory in scoverage <<= (sourceDirectory in Compile),

        scalacOptions in scoverage <++= (name in scoverage, baseDirectory in scoverage, update) map {
          (n, b, report) =>
            val scoverageDeps = report matching configurationFilter("scoverage")
            scoverageDeps.find(_.getAbsolutePath.contains("scalac-scoverage-plugin")) match {
              case None => throw new Exception("Fatal: scalac-scoverage-plugin not in libraryDependencies")
              case Some(classpath) =>
                Seq(
                  "-Xplugin:" + classpath.getAbsolutePath,
                  "-Yrangepos"
                )
            }
        },

        sources in scoverageTest <<= (sources in Test),
        sourceDirectory in scoverageTest <<= (sourceDirectory in Test),
        unmanagedResources in scoverageTest <<= (unmanagedResources in Test),

        resourceDirectory in scoverageTest <<= (resourceDirectory in Compile),

        externalDependencyClasspath in scoverage <<= Classpaths
          .concat(externalDependencyClasspath in scoverage, externalDependencyClasspath in Compile),
        externalDependencyClasspath in scoverageTest <<= Classpaths
          .concat(externalDependencyClasspath in scoverageTest, externalDependencyClasspath in Test),

        internalDependencyClasspath in scoverage <<= (internalDependencyClasspath in Compile),
        internalDependencyClasspath in scoverageTest <<= (internalDependencyClasspath in Test, internalDependencyClasspath in scoverageTest, classDirectory in Compile) map {
          (testDeps, scoverageDeps, oldClassDir) =>
            scoverageDeps ++ testDeps.filter(_.data != oldClassDir)
        },

        testOptions in scoverageTest <+= testsCleanup,

        // make scoverage config the same as scoverageTest config
        test in scoverage <<= (test in scoverageTest)
      )
  }

  /** Generate hook that is invoked after each tests execution. */
  def testsCleanup = {
    (crossTarget in scoverageTest,
      baseDirectory in Compile,
      scalaSource in Compile,
      definedTests in scoverageTest,
      streams in Global) map {
      (crossTarget,
       baseDirectory,
       compileSourceDirectory,
       definedTests,
       streams) =>
        if (definedTests.isEmpty) {
          Tests.Cleanup {
            () => {}
          }
        } else {
          Tests.Cleanup {
            () =>

              streams.log.info("Reading scoverage profile file from " + Env.coverageFile)
              streams.log.info("Reading scoverage measurement file from " + Env.measurementFile)

              val coverage = IOUtils.deserialize(getClass.getClassLoader, Env.coverageFile)
              val measurements = IOUtils.invoked(Env.measurementFile)
              coverage.apply(measurements)

              val coberturaDirectory = crossTarget / "coverage-report"
              val scoverageDirectory = crossTarget / "scoverage-report"

              coberturaDirectory.mkdirs()
              scoverageDirectory.mkdirs()

              streams.log.info("Generating Cobertura XML report...")
              new CoberturaXmlWriter(baseDirectory, coberturaDirectory).write(coverage)

              streams.log.info("Generating Scoverage XML report...")
              new ScoverageXmlWriter(compileSourceDirectory, scoverageDirectory).write(coverage)

              streams.log.info("Generating Scoverage HTML report...")
              new ScoverageHtmlWriter(compileSourceDirectory, scoverageDirectory).write(coverage)

              streams.log.info("Scoverage reports completed")
              ()
          }
        }
    }
  }
}
