lazy val root = (project in file(".")).aggregate(crossJS, crossJVM)

lazy val cross = crossProject.in(file("sjstest")).settings(
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
    )
  )


lazy val crossJS = cross.js
lazy val crossJVM = cross.jvm
