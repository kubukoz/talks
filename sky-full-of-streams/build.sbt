val compilerPlugins = List(
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
  compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

val commonSettings = Seq(
  scalaVersion := "2.12.8",
  scalacOptions ++= Options.all,
  libraryDependencies ++= Seq(
    "org.apache.geronimo.specs" % "geronimo-jms_1.1_spec" % "1.1.1",
    "org.apache.activemq" % "activemq-client" % "5.15.9",
    "org.typelevel" %% "cats-effect" % "1.4.0",
    "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "co.fs2" %% "fs2-io" % "1.0.5",
    "io.circe" %% "circe-generic" % "0.11.1",
    "io.circe" %% "circe-parser" % "0.11.1",
    "dev.profunktor" %% "console4cats" % "0.7.0",
    "org.scalacheck" %% "scalacheck" % "1.13.5",
    "org.scalatest" %% "scalatest" % "3.0.8"
  ) ++ compilerPlugins
)

val streams = project.in(file(".")).settings(commonSettings)
