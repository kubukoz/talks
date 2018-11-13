import sbt.compilerPlugin

val plugins = Seq(
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  compilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

val embeds = project
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2"                     %% "fs2-core"      % "1.0.0",
      "com.olegpy"                 %% "meow-mtl"      % "0.2.0",
      "com.github.gvolpe"          %% "console4cats"  % "0.4",
      "com.github.julien-truffaut" %% "monocle-macro" % "1.5.0-cats"
    ) ++ plugins,
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-language:higherKinds"
    )
  )
