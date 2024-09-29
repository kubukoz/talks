import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*
import org.scalajs.dom

import scala.scalajs.js.JSConverters.*

object FileDownload {

  def runDownload(data: String): IO[Unit] = {
    val blob =
      new dom.Blob(
        Array(data).toJSArray,
        new dom.BlobPropertyBag {
          `type` = "application/octet-stream"
        },
      )

    for {
      url <-
        Resource.make(IO(dom.URL.createObjectURL(blob)))(url => IO(dom.URL.revokeObjectURL(url)))
      elem <- a(styleAttr := "display: none", href := url, download := "track.json")
      _ <- Window[IO].document.querySelector("body").toResource.flatMap {
        _.traverse_ { parent =>
          Resource.make(parent.appendChild(elem))(_ => parent.removeChild(elem))
        }
      }
    } yield elem
  }
    .use(_.click)

}
