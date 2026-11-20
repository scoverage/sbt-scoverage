libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.14.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
