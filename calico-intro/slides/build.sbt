ThisBuild / scalaVersion := "3.5.1"

lazy val root = project.in(file("."))

lazy val jsdocs = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.0.0",
      "org.http4s" %%% "http4s-dom" % "0.2.11",
      "com.armanbilge" %%% "calico" % "0.2.2-92-a2041d8-SNAPSHOT",
      "com.kubukoz" %%% "calico-fun-frontend" % "0.1.0-SNAPSHOT"
    )
  )
  .enablePlugins(ScalaJSPlugin)

lazy val docs = project // new documentation project
  .in(file("doc-sources")) // important: it must not be docs/
  .settings(
    mdocJS := Some(jsdocs),
    mdocExtraArguments := List("--no-livereload")
  )
  .enablePlugins(MdocPlugin)
