def crossPlugin(x: sbt.librarymanagement.ModuleID) =
  compilerPlugin(x cross CrossVersion.full)

val compilerPlugins = List(
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  crossPlugin("org.typelevel" % "kind-projector" % "0.11.0"),
  crossPlugin("com.github.cb372" % "scala-typed-holes" % "0.1.1")
)

val commonSettings = Seq(
  scalaVersion := "2.13.1",
  scalacOptions --= Seq("-Xfatal-warnings"),
  scalacOptions ++= Seq("-Ymacro-annotations"),
  fork in Test := true,
  updateOptions := updateOptions.value.withGigahorse(false),
  libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-blaze-server" % "0.21.1",
    "org.http4s" %% "http4s-blaze-client" % "0.21.1",
    "org.http4s" %% "http4s-dsl" % "0.21.1",
    "org.http4s" %% "http4s-circe" % "0.21.1",
    "io.circe" %% "circe-generic-extras" % "0.13.0",
    "org.typelevel" %% "cats-tagless-macros" % "0.11",
    "co.fs2" %% "fs2-core" % "2.2.2",
    "dev.profunktor" %% "console4cats" % "0.8.1",
    "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  ) ++ compilerPlugins
)

val core = project.settings(commonSettings)

val upstream =
  project.settings(commonSettings).dependsOn(core)

val client =
  project.in(file(".")).settings(commonSettings).dependsOn(core)
