import bindgen.interface.Binding

val commonSettings = Seq(
  scalaVersion := "3.5.2",
  scalacOptions --= Seq("-Xfatal-warnings"),
  scalacOptions ++= Seq(
    "-Wunused:all"
  ),
  Compile / doc / sources := Nil,
)

val hidapi =
  crossProject(NativePlatform)
    .crossType(CrossType.Full)
    .settings(commonSettings)
    .nativeConfigure(
      _.settings(
        bindgenBindings := Seq(
          Binding(file(sys.env("HIDAPI_PATH")), "libhidapi")
        ),
        bindgenBinary := file(sys.env("BINDGEN_PATH")),
        scalacOptions += "-Wconf:msg=unused import:s",
      )
        .enablePlugins(BindgenPlugin)
    )

val app =
  crossProject(JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings(commonSettings)
    .jvmConfigure(
      _.enablePlugins(JavaAppPackaging)
        .settings(
          libraryDependencies += "org.hid4java" % "hid4java" % "0.8.0",
          fork := true,
        )
    )
    .nativeConfigure(
      _.settings(
        nativeConfig ~= {
          _.withLinkingOptions {
            _ ++ {
              val isLinux = {
                import sys.process._
                "uname".!!.trim == "Linux"
              }

              val hidapiLinkName =
                if (isLinux) "hidapi-hidraw"
                else "hidapi"

              Seq(s"-l$hidapiLinkName")
            }
          }
        }
      ).dependsOn(hidapi.native)
    )

val root = project
  .in(file("."))
  .aggregate(List(app, hidapi).flatMap(_.componentProjects).map(p => p: ProjectReference): _*)
  .settings(publish / skip := true)
