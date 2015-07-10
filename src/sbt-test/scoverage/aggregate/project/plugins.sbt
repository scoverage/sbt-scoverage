/*
 * ScoveragePlugin is constructed from the sources in the parent directory
 */
lazy val root = (project in file(".")).dependsOn(scoveragePlugin)

lazy val scoveragePlugin = file("../../../..").getAbsoluteFile.toURI