import bindgen.interface.Binding

val root = project
  .in(file("."))
  .enablePlugins(ScalaNativePlugin)
  .settings(
    scalaVersion := "3.5.2",
    scalacOptions += "-Wunused:all",
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
    },
  )
  .enablePlugins(BindgenPlugin)
  .settings(
    bindgenBindings := Seq(
      Binding(file(sys.env("HIDAPI_PATH")), "libhidapi")
    ),
    bindgenBinary := file(sys.env("BINDGEN_PATH")),
    scalacOptions += "-Wconf:msg=unused import:s",
  )
