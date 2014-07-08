resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies <+= sbtVersion(v => "org.scala-sbt" % "scripted-plugin" % v)
