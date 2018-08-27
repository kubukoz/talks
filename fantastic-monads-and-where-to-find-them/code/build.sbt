import sbt.addCompilerPlugin

val akkaHttpVersion        = "10.1.1"
val circeVersion           = "0.9.3"
val catsVersion            = "1.1.0"
val akkaHttpCirceVersion   = "1.20.1"
val kindProjectorVersion   = "0.9.6"
val pureconfigVersion      = "0.9.1"
val scalatestVersion       = "3.0.5"
val monixVersion           = "3.0.0-RC1"
val mainecoonMacrosVersion = "0.6.2"

val commonDeps = Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe"      % akkaHttpCirceVersion,
  "io.circe"          %% "circe-core"           % circeVersion,
  "io.circe"          %% "circe-generic"        % circeVersion,
  "io.circe"          %% "circe-generic-extras" % circeVersion,
  "org.typelevel"     %% "cats-core"            % catsVersion,
  "org.typelevel"     %% "cats-free"            % catsVersion
)

val appDeps = commonDeps ++ Seq(
  "com.kailuowang"        %% "mainecoon-macros"  % mainecoonMacrosVersion,
  "com.github.pureconfig" %% "pureconfig"        % pureconfigVersion,
  "com.typesafe.akka"     %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalatest"         %% "scalatest"         % scalatestVersion % Test,
  "io.monix"              %% "monix"             % monixVersion
)

val kindProjector = addCompilerPlugin("org.spire-math" %% "kind-projector" % kindProjectorVersion)
val macroParadise = addCompilerPlugin(("org.scalameta" % "paradise" % "3.0.0-M11").cross(CrossVersion.full))

val fantasticMonads = (project in file(".")).settings(
  organization := "com.kubukoz",
  scalaVersion := "2.12.4",
  version := "0.1.0",
  name := "fantastic-monads-app",
  scalacOptions += "-Ypartial-unification",
  libraryDependencies ++= appDeps,
  macroParadise,
  kindProjector
)
