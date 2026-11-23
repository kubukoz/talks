addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.19.13")
addSbtPlugin("org.polyvariant" % "sbt-smithy-ts-codegen" % "0.5.1")
addSbtPlugin("org.polyvariant" % "smithy-scala-tools-sbt" % "0.3.2")
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.8.7")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
// Smithy IDL 2.1 (inline collections) needs smithy 1.75.0+; the plugins above bundle older versions.
libraryDependencies ++= Seq(
  "software.amazon.smithy" % "smithy-model" % "1.75.0",
  "software.amazon.smithy" % "smithy-syntax" % "1.75.0",
)
