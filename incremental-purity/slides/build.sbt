import sbt.compilerPlugin

val derivations = Seq(
  "org.typelevel" %% "kittens"        % "1.2.0",
  "org.scalaz"    %% "deriving-macro" % "1.0.0",
  compilerPlugin("org.scalaz" %% "deriving-plugin" % "1.0.0")
)

val plugins = Seq(
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  compilerPlugin("org.scalameta"  % "paradise"        % "3.0.0-M11" cross CrossVersion.full)
) ++ derivations

val embeds = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-actor"          % "2.5.18",
      "co.fs2"                     %% "fs2-core"            % "1.0.0",
      "co.fs2"                     %% "fs2-io"              % "1.0.0",
      "org.http4s"                 %% "http4s-blaze-client" % "0.20.0-M3",
      "io.chrisdavenport"          %% "log4cats-slf4j"      % "0.2.0",
      "org.apache.activemq"        % "activemq-client"      % "5.15.7",
      "com.olegpy"                 %% "meow-mtl"            % "0.2.0",
      "io.chrisdavenport"          %% "cats-par"            % "0.2.0",
      "com.github.gvolpe"          %% "console4cats"        % "0.4",
      "com.github.julien-truffaut" %% "monocle-macro"       % "1.5.0-cats",
      "org.typelevel"              %% "cats-tagless-macros" % "0.1.0",
    ) ++ plugins,
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-language:higherKinds"
    )
  )
