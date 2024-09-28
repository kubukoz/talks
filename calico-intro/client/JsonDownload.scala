import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*
import io.circe.syntax.*
import org.scalajs.dom

import scala.scalajs.js.JSConverters.*
import io.circe.Encoder

object JsonDownload {

  def runDownload[A: Encoder](data: A): IO[Unit] = {
    val blob =
      new dom.Blob(
        Array(data.asJson.noSpaces).toJSArray,
        new dom.BlobPropertyBag {
          `type` = "application/octet-stream"
        },
      )

    Resource
      .make(IO(dom.URL.createObjectURL(blob)))(url => IO(dom.URL.revokeObjectURL(url)))
      .flatMap { url =>
        a(styleAttr := "display: none", href := url, download := "track.json")
      }
      .flatTap { elem =>
        Window[IO].document.querySelector("body").toResource.flatMap {
          _.traverse_ { parent =>
            Resource.make(parent.appendChild(elem))(_ => parent.removeChild(elem))
          }
        }
      }
      .use(_.click)
  }

}