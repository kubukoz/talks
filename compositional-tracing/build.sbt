def crossPlugin(x: sbt.librarymanagement.ModuleID) =
  compilerPlugin(x cross CrossVersion.full)

val compilerPlugins = List(
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  crossPlugin("org.scalamacros" % "paradise" % "2.1.1"),
  crossPlugin("org.typelevel" % "kind-projector" % "0.11.0"),
  crossPlugin("com.github.cb372" % "scala-typed-holes" % "0.1.1")
)

val commonSettings = Seq(
  // scalaVersion := "2.13.1",
  scalaVersion := "2.12.10", //kamon-cats-io isn't released on 2.13 yet
  // scalacOptions ++= Seq("-Ymacro-annotations"),
  scalacOptions --= Seq("-Xfatal-warnings"),
  fork in Test := true,
  updateOptions := updateOptions.value.withGigahorse(false),
  libraryDependencies ++= Seq(
    "io.kamon" %% "kamon-logback" % "2.0.2",
    "io.kamon" %% "kamon-http4s" % "2.0.2",
    "io.kamon" %% "kamon-zipkin" % "2.0.1",
    "io.kamon" %% "kamon-cats-io" % "2.0.1",
    "io.kamon" %% "kamon-scala-future" % "2.0.1",
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
  project.settings(commonSettings).dependsOn(core).enablePlugins(JavaAppPackaging)

val client =
  project
    .in(file("."))
    .settings(commonSettings)
    .dependsOn(core)
    .enablePlugins(JavaAppPackaging)
