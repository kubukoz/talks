import cats.effect.IO
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*

object KeyStatus {

  def forKey(key: String): fs2.Stream[IO, Boolean] = Window[IO]
    .document
    .onKeyDown
    .filter(_.key == key)
    .as(true)
    .merge(
      Window[IO]
        .document
        .onKeyUp
        .filter(_.key == key)
        .as(false)
    )

}
