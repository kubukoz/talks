ThisBuild / scalaVersion := "3.10.1"
ThisBuild / tlBaseVersion := "0.1"
ThisBuild / tlJdkRelease := Some(25)

lazy val api = project
  .enablePlugins(Smithy4sCodegenPlugin, SmithyFormatPlugin)
  .settings(
    fork := true,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.38",
    )
  )

lazy val web = project
  .enablePlugins(SmithyTsCodegenPlugin)
  .settings(
    tsCodegenSmithyDirs := Seq((api / baseDirectory).value / "src" / "main" / "smithy"),
    tsCodegenOutputFile := baseDirectory.value / "src" / "generated.ts",
  )

lazy val root = project
  .in(file("."))
  .aggregate(api, web)
  .settings(
    Compile / run := (api / Compile / run).evaluated,
    run / aggregate := false,
  )
