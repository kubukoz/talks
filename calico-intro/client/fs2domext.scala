package fs2.dom.ext

import cats.effect.kernel.Async
import cats.effect.kernel.Sync
import cats.syntax.all.*
import fs2.dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLDocument

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSGlobal

object FS2DomExtensions {

  extension [F[_]: Async](doc: HtmlDocument[F]) {

    def onKeyDown: fs2.Stream[F, KeyboardEvent[F]] = fs2
      .dom
      .events[F, org.scalajs.dom.KeyboardEvent](
        doc.asInstanceOf[HTMLDocument],
        "keydown",
      )
      .map(KeyboardEvent[F](_))

    def onKeyUp: fs2.Stream[F, KeyboardEvent[F]] = fs2
      .dom
      .events[F, org.scalajs.dom.KeyboardEvent](
        doc.asInstanceOf[HTMLDocument],
        "keyup",
      )
      .map(KeyboardEvent[F](_))

    def querySelector(selector: String): F[Option[HtmlElement[F]]] = Sync[F].delay {
      Option(doc.asInstanceOf[HTMLDocument].querySelector(selector))
        .map(_.asInstanceOf[HtmlElement[F]])
    }

  }

  extension [F[_]: Async](unused: Navigator[F]) {

    // todo: it'd be nice to access the permission as a signal
    // so gotta steal some code from todomeda
    def requestMIDIAccess: F[MIDIAccess[F]] = Async[F]
      .fromPromise {
        Sync[F].delay(
          org
            .scalajs
            .dom
            .window
            .navigator
            .asInstanceOf[scala.scalajs.js.Dynamic]
            .requestMIDIAccess()
            .asInstanceOf[scalajs.js.Promise[natives.MIDIAccess]]
        )
      }
      .map(MIDIAccess.wrap)

  }

  extension [F[_]: Sync](unused: Window[F]) {

    def open(url: String): F[Unit] = Sync[F].delay {
      dom.window.open(url)
    }

  }

}

trait MIDIAccess[F[_]] {
  def outputs: F[Map[String, MIDIOutput[F]]]
}

object MIDIAccess {

  private[ext] def wrap[F[_]: Sync](access: natives.MIDIAccess): MIDIAccess[F] =
    new {

      def outputs: F[Map[String, MIDIOutput[F]]] = Sync[F].delay {
        access
          .outputs
          .toMap
          .view
          .mapValues(MIDIOutput.wrap)
          .toMap
      }

    }

}

trait MIDIOutput[F[_]] {
  def name: String
  def send(data: IArray[Int]): F[Unit]
  def clear: F[Unit]
}

object MIDIOutput {

  private[ext] def wrap[F[_]: Sync](output: natives.MIDIOutput): MIDIOutput[F] =
    new {

      val name: String = output.name
      def send(data: IArray[Int]): F[Unit] = Sync[F].delay {
        output.send(data.toJSArray)
      }

      val clear: F[Unit] = Sync[F].delay {
        output.clear()
      }

    }

}

object natives {

  @js.native
  @JSGlobal
  class MIDIAccess extends js.Any {
    def outputs: MIDIOutputMap = js.native
  }

  @js.native
  @JSGlobal
  class MIDIOutputMap extends js.Map[String, MIDIOutput] {}

  @js.native
  @JSGlobal
  class MIDIOutput extends js.Any {
    def name: String = js.native
    def send(data: scala.scalajs.js.Array[Int]): Unit = js.native
    def clear(): Unit = js.native
  }

}
