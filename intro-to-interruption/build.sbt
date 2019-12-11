val compilerPlugins = List(
  compilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
  ),
  compilerPlugin(
    "org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full
  ),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

val commonSettings = Seq(
  scalaVersion := "2.12.10",
  scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")),
  libraryDependencies ++= Seq(
    "co.fs2" %% "fs2-io" % "2.1.0",
    "dev.zio" %% "zio" % "1.0.0-RC17",
    "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC10",
    "io.monix" %% "monix-eval" % "3.1.0",
    "org.typelevel" %% "cats-effect" % "2.0.0",
    "dev.profunktor" %% "console4cats" % "0.8.0"
  ) ++ compilerPlugins
)

val interruption = project.in(file(".")).settings(commonSettings)
