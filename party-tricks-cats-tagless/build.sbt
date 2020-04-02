def crossPlugin(x: sbt.librarymanagement.ModuleID) =
  compilerPlugin(x cross CrossVersion.full)

val compilerPlugins = List(
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  crossPlugin("org.typelevel" % "kind-projector" % "0.11.0"),
  crossPlugin("com.github.cb372" % "scala-typed-holes" % "0.1.2")
)

val commonSettings = Seq(
  scalaVersion := "2.13.1",
  scalacOptions ++= Seq("-Ymacro-annotations"),
  scalacOptions --= Seq("-Xfatal-warnings"),
  fork in Test := true,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "2.1.2",
    "org.typelevel" %% "cats-tagless-macros" % "0.11",
    "dev.profunktor" %% "console4cats" % "0.8.1",
    "co.fs2" %% "fs2-core" % "2.3.0",
    "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
    "org.tpolecat" %% "doobie-core" % "0.8.6",
    "org.tpolecat" %% "natchez-core" % "0.0.11",
    "org.tpolecat" %% "natchez-log" % "0.0.11",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.typelevel" %% "simulacrum" % "1.0.0",
    "com.codecommit" %% "skolems" % "0.2.0"
  ) ++ compilerPlugins
)

val root =
  project
    .in(file("."))
    .settings(commonSettings)
    .enablePlugins(JavaAppPackaging, DockerPlugin)
