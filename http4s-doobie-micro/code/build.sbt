val compilerPlugins = List(
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
  compilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.9"),
  compilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.0-M4"),
)

val http4s = List(
  "org.http4s"        %% "http4s-blaze-server"  % "0.20.0-M4",
  "org.http4s"        %% "http4s-blaze-client"  % "0.20.0-M4",
  "org.http4s"        %% "http4s-dsl"           % "0.20.0-M4",
  "org.http4s"        %% "http4s-circe"         % "0.20.0-M4",
  "io.circe"          %% "circe-generic"        % "0.10.1",
  "io.circe"          %% "circe-generic-extras" % "0.10.1",
  "io.chrisdavenport" %% "cats-par"             % "0.2.0",
)

val doobie = List(
  "org.tpolecat"   %% "doobie-core"      % "0.6.0",
  "org.tpolecat"   %% "doobie-postgres"  % "0.6.0",
  "org.tpolecat"   %% "doobie-hikari"    % "0.6.0",
  "org.tpolecat"   %% "doobie-scalatest" % "0.6.0",
  "org.postgresql" % "postgresql"        % "42.2.4",
  "org.flywaydb"   % "flyway-core"       % "5.2.4"
)

val commonSettings = Seq(
  scalaVersion := "2.12.8",
  scalacOptions ++= Options.all,
  fork in Test := true,
  libraryDependencies ++= Seq(
    "ch.qos.logback"    % "logback-classic" % "1.2.3",
    "io.chrisdavenport" %% "log4cats-slf4j" % "0.2.0",
    "org.scalatest"     %% "scalatest"      % "3.0.5" % Test
  ) ++ compilerPlugins ++ http4s ++ doobie,
)

val sushiData = project.settings(commonSettings).in(file("sushi/data"))
val sushi     = project.settings(commonSettings).dependsOn(sushiData)

val paymentsData = project.settings(commonSettings).in(file("payments/data"))
val payments     = project.settings(commonSettings).dependsOn(paymentsData)

val orderData = project.settings(commonSettings).in(file("orders/data"))
val orders    = project.settings(commonSettings).dependsOn(sushiData, paymentsData, orderData)

val `sushi-place` =
  project.in(file(".")).dependsOn(orders).aggregate(orders)
