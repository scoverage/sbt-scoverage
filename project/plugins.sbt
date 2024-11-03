libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
