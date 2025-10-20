val pluginVersion = sys.props.getOrElse(
  "plugin.version",
  throw new RuntimeException(
    """|The system property 'plugin.version' is not defined.
       |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
  )
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % pluginVersion)

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.1")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
