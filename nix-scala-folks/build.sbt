ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    version := "0.1.0",
    organization := "com.example",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.0",
      compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.11" cross CrossVersion.full),
    ),
    Compile / doc / sources := Nil,
  )
  .enablePlugins(JavaAppPackaging)
