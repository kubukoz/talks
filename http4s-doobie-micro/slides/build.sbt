import sbt.compilerPlugin

val `http4s-doobie-micro` = project in file(".")

val plugins = Seq(
  "org.scalaz" %% "deriving-macro" % "1.0.0",
  compilerPlugin("org.scalaz" %% "deriving-plugin" % "1.0.0"),
  compilerPlugin(
    "com.olegpy"                  %% "better-monadic-for" % "0.3.0-M4"),
  compilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.7"),
  compilerPlugin(
    "org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full)
)

val embeds = project
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "kittens"             % "1.2.0",
      "org.tpolecat"               %% "doobie-core"         % "0.6.0",
      "org.tpolecat"               %% "doobie-postgres"     % "0.6.0",
      "org.tpolecat"               %% "doobie-refined"      % "0.6.0",
      "org.tpolecat"               %% "doobie-hikari"       % "0.6.0",
      "org.tpolecat"               %% "doobie-scalatest"    % "0.6.0",
      "org.postgresql"             % "postgresql"           % "42.2.4",
      "org.typelevel"              %% "cats-tagless-macros" % "0.1.0",
      "org.typelevel"              %% "cats-effect"         % "1.1.0",
      "co.fs2"                     %% "fs2-core"            % "1.0.2",
      "co.fs2"                     %% "fs2-io"              % "1.0.2",
      "io.circe"                   %% "circe-generic"       % "0.11.1",
      "org.http4s"                 %% "http4s-dsl"          % "0.20.0-M5",
      "org.http4s"                 %% "http4s-circe"        % "0.20.0-M5",
      "org.http4s"                 %% "http4s-blaze-server" % "0.20.0-M5",
      "org.http4s"                 %% "http4s-blaze-client" % "0.20.0-M5",
      "io.chrisdavenport"          %% "cats-par"            % "0.2.0",
      "io.chrisdavenport"          %% "log4cats-slf4j"      % "0.2.0",
      "com.olegpy"                 %% "meow-mtl"            % "0.2.0",
      "com.github.gvolpe"          %% "console4cats"        % "0.5",
      "com.github.julien-truffaut" %% "monocle-macro"       % "1.5.0-cats",
      "ch.qos.logback"             % "logback-classic"      % "1.2.3",
    ) ++ plugins,
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-language:higherKinds"
    )
  )
