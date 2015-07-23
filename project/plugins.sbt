libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
