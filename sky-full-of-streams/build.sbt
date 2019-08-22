val compilerPlugins = List(
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
  compilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
  compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
)

val commonSettings = Seq(
  scalaVersion := "2.12.8",
  scalacOptions ++= Options.all,
  name := "datas",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "1.4.0",
    "co.fs2"        %% "fs2-io"      % "1.0.5"
  ) ++ compilerPlugins
)

val streams = project.in(file(".")).settings(commonSettings)
