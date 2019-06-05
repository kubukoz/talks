val compilerPlugins = List(
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1")
    .cross(CrossVersion.full),
  compilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.0"),
  compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
)

val root = project
  .in(file("."))
  .settings(
    scalaVersion := "2.12.8",
    scalacOptions ++= Options.all,
    libraryDependencies ++= List(
      "org.http4s"     %% "http4s-core"         % "0.20.1",
      "org.http4s"     %% "http4s-dsl"          % "0.20.1",
      "org.http4s"     %% "http4s-circe"        % "0.20.1",
      "org.http4s"     %% "http4s-blaze-server" % "0.20.1",
      "org.http4s"     %% "http4s-blaze-client" % "0.20.1",
      "io.circe"       %% "circe-generic"       % "0.11.1",
      "ch.qos.logback" % "logback-classic"      % "1.2.3",
      "org.scalatest"  %% "scalatest"           % "3.0.7" % Test
    ) ++ compilerPlugins
  )
