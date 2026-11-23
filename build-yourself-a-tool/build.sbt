ThisBuild / scalaVersion := "3.10.1"
ThisBuild / tlBaseVersion := "0.1"
ThisBuild / tlJdkRelease := Some(25)
// smithy4s generates `$ordinal`, which trips E230 since Scala 3.9: https://github.com/disneystreaming/smithy4s/issues/2000
ThisBuild / scalacOptions += "-Wconf:id=E230&src=.*/src_managed/.*:s"

lazy val api = project
  .enablePlugins(Smithy4sCodegenPlugin, SmithyFormatPlugin)
  .settings(
    fork := true,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.38",
      "com.kubuszok" %% "kindlings-cats-derivation" % "0.4.0",
    ),
  )

lazy val web = project
  .enablePlugins(SmithyTsCodegenPlugin)
  .settings(
    tsCodegenSmithyDirs := Seq((api / baseDirectory).value / "src" / "main" / "smithy"),
    tsCodegenOutputFile := baseDirectory.value / "src" / "generated.ts",
    // The TS codegen CLI has its own classpath: Smithy IDL 2.1 (inline collections) needs smithy 1.75.0+,
    // and smithy4s-protocol provides smithy4s.meta#adt.
    tsCodegenExtensions ++= Seq(
      "software.amazon.smithy" % "smithy-model" % "1.75.0",
      "com.disneystreaming.smithy4s" % "smithy4s-protocol" % smithy4sVersion.value,
    ),
  )

lazy val root = project
  .in(file("."))
  .aggregate(api, web)
  .settings(
    Compile / run := (api / Compile / run).evaluated,
    run / aggregate := false,
  )
