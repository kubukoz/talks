package fs2.dom.ext

import cats.FlatMap
import cats.MonadThrow
import cats.effect.kernel.Async
import cats.effect.kernel.DeferredSource
import cats.effect.kernel.Resource
import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import fs2.dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLDocument
import org.scalajs.dom.RTCIceCandidate
import org.scalajs.dom.RTCPeerConnectionIceEvent
import org.scalajs.dom.RTCSessionDescription

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSGlobal
import org.scalajs.dom.RTCIceConnectionState

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

trait RTCPeerConnection[F[_]] {
  def createDataChannel(label: String, dataChannelDict: dom.RTCDataChannelInit)
    : Resource[F, RTCDataChannel[F]]

  def onIceCandidate(f: dom.RTCPeerConnectionIceEvent => F[Unit]): F[Unit]
  def onDataChannel(f: RTCDataChannel[F] => F[Unit]): F[Unit]
  def onIceConnectionStateChange(f: dom.RTCIceConnectionState => F[Unit]): F[Unit]

  def setLocalDescription(description: dom.RTCSessionDescription): F[Unit]
  def setRemoteDescription(description: dom.RTCSessionDescription): F[Unit]
  def createAnswer: F[dom.RTCSessionDescription]
  def createOffer: F[dom.RTCSessionDescription]
  def addIceCandidate(candidate: dom.RTCIceCandidate): F[Unit]

}

object RTCPeerConnection {

  def apply[F[_]: Async](config: Option[dom.RTCConfiguration]): Resource[F, RTCPeerConnection[F]] =
    Dispatcher.sequential[F].flatMap { dispatcher =>
      Resource
        .make(
          Sync[F].delay(new dom.RTCPeerConnection(config.orUndefined))
        )(conn => Sync[F].delay(conn.close()))
        .map { pc =>
          new RTCPeerConnection[F] {
            def createDataChannel(label: String, dataChannelDict: dom.RTCDataChannelInit)
              : Resource[F, RTCDataChannel[F]] = Resource
              .make(Sync[F].delay(pc.createDataChannel(label, dataChannelDict)))(dc =>
                Sync[F].delay(dc.close())
              )
              .map(RTCDataChannel.lift(_, dispatcher))

            def onIceCandidate(f: RTCPeerConnectionIceEvent => F[Unit]): F[Unit] = Sync[F].delay {
              pc.onicecandidate = e => dispatcher.unsafeRunAndForget(f(e))
            }

            def onDataChannel(f: RTCDataChannel[F] => F[Unit]): F[Unit] = Sync[F].delay {
              pc.ondatachannel =
                e => dispatcher.unsafeRunAndForget(f(RTCDataChannel.lift(e.channel, dispatcher)))
            }
            def onIceConnectionStateChange(f: RTCIceConnectionState => F[Unit]): F[Unit] = Sync[F]
              .delay {
                pc.oniceconnectionstatechange =
                  e =>
                    dispatcher.unsafeRunAndForget(
                      f(e.target.asInstanceOf[dom.RTCPeerConnection].iceConnectionState)
                    )
              }

            def addIceCandidate(candidate: RTCIceCandidate): F[Unit] = Async[F].fromPromise(
              Sync[F].delay(pc.addIceCandidate(candidate))
            )

            def createAnswer: F[RTCSessionDescription] = Async[F].fromPromise(
              Sync[F].delay(pc.createAnswer())
            )

            def createOffer: F[RTCSessionDescription] = Async[F].fromPromise(
              Sync[F].delay(pc.createOffer())
            )

            def setLocalDescription(description: RTCSessionDescription): F[Unit] = Async[F]
              .fromPromise(
                Sync[F].delay(pc.setLocalDescription(description))
              )

            def setRemoteDescription(description: RTCSessionDescription): F[Unit] = Async[F]
              .fromPromise(
                Sync[F].delay(pc.setRemoteDescription(description))
              )

          }
        }
    }

}

trait RTCDataChannel[F[_]] {
  def send(data: String): F[Unit]
  def onOpen(f: F[Unit]): F[Unit]
  def onMessage(f: dom.MessageEvent => F[Unit]): F[Unit]
}

object RTCDataChannel {

  private[ext] def lift[F[_]: Sync](dc: dom.RTCDataChannel, dispatcher: Dispatcher[F])
    : RTCDataChannel[F] =
    new RTCDataChannel[F] {
      def send(data: String): F[Unit] = Sync[F].delay(dc.send(data))
      def onOpen(f: F[Unit]): F[Unit] = Sync[F].delay {
        dc.onopen = _ => dispatcher.unsafeRunAndForget(f)
      }
      def onMessage(f: dom.MessageEvent => F[Unit]): F[Unit] = Sync[F].delay {
        dc.onmessage = e => dispatcher.unsafeRunAndForget(f(e))
      }
    }

  def suspend[F[_]: FlatMap](dcf: F[RTCDataChannel[F]]): RTCDataChannel[F] =
    new RTCDataChannel[F] {
      def send(data: String): F[Unit] = dcf.flatMap(_.send(data))
      def onOpen(f: F[Unit]): F[Unit] = dcf.flatMap(_.onOpen(f))
      def onMessage(f: dom.MessageEvent => F[Unit]): F[Unit] = dcf.flatMap(_.onMessage(f))
    }

  def fromDeferred[F[_]: MonadThrow](d: DeferredSource[F, RTCDataChannel[F]]): RTCDataChannel[F] =
    suspend {
      d.tryGet.flatMap(_.liftTo[F](new Throwable("remote data channel not available yet")))
    }

}
