scalaVersion := "2.12.8"

libraryDependencies ++= List(
  "org.typelevel" %% "cats-effect" % "1.3.1",
  "org.typelevel" %% "cats-core" % "1.6.1",
  "eu.timepit" %% "refined" % "0.9.8",
  "com.kubukoz" %% "flawless-core" % "0.1.0-SNAPSHOT"
)

scalacOptions ++= List("-Ypartial-unification")
