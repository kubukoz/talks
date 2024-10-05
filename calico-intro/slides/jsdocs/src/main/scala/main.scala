package demo

import org.scalajs.dom
import scalajs.js
import org.http4s.client.websocket.WSClient
import cats.effect.IO
import org.http4s.dom.WebSocketClient
import org.http4s.client.websocket.WSRequest
import org.http4s.implicits.*
import fs2.dom.KeyboardEvent
import cats.syntax.all.*
import org.http4s.client.websocket.WSFrame
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import cats.effect.std.Dispatcher
import cats.effect.kernel.Resource
import calico.html.io.*
import calico.html.io.given
import calico.syntax.*
import calico.syntax.given
import cats.effect.kernel.Async
import org.http4s.client.websocket.WSConnectionHighLevel
import org.http4s.client.websocket.WSClientHighLevel
import scala.scalajs.js.annotation.JSGlobal
import org.scalajs.dom.ReadableStream
import org.scalajs.dom.WriteableStream
import cats.effect.kernel.Sync
import org.http4s.client.websocket.WSFrame.Close
import cats.effect.kernel.DeferredSource
import org.http4s.client.websocket.WSDataFrame
import cats.effect.implicits.*
import cats.Foldable
import scodec.bits.ByteVector
import fs2.Chunk
import fs2.dom.AbortController
import scala.scalajs.js.JavaScriptException

object demo {
  IntersectionObserver.isVisible(dom.document.body).flatMap { visible =>
    val child = WebSocketStreamClient[IO]
      .connectHighLevel(
        WSRequest(uri"wss://ws.postman-echo.com/raw")
      )
      .flatMap { conn =>
        fs2.dom
          .events[IO, dom.KeyboardEvent](dom.window, "keydown")
          .map(_.key)
          .foreach(conn.sendText)
          .merge(conn.receiveStream)
          .collect {
            case WSFrame.Text(text, _) =>
              Chunk.array(text.getBytes())
            case WSFrame.Binary(bytes, _) =>
              Chunk.byteVector(bytes)
          }
          .unchunks
          .through(fs2.text.utf8.decode)
          .through(fs2.text.lines)
          .holdOptionResource
      }
      .map(_.map(_.toString()))

    div(
      visible.map {
        case true  => div(child)
        case false => div("Not connecting as the slide is not visible")
      }
    )
  }
}

object IntersectionObserver {
  import scala.scalajs.js.JSConverters.*

  def isVisible(target: dom.Element): Resource[IO, Signal[IO, Boolean]] =
    Dispatcher.sequential[IO].flatMap { d =>
      SignallingRef[IO].of(false).toResource.flatTap { ref =>
        Resource.make {
          IO {
            val obs = new dom.IntersectionObserver((entries, _) =>
              d.unsafeRunAndForget {
                entries.headOption
                  .traverse_ { entry =>
                    ref.set(entry.isIntersecting)
                  }
              }
            )

            obs.observe(target)

            obs
          }
        } { obs => IO(obs.disconnect()) }
      }

    }
}

// Trying to upstream this: https://github.com/http4s/http4s-dom/pull/384
object WebSocketStreamClient {
  def apply[F[_]: Async]: WSClientHighLevel[F] = new {
    def connectHighLevel(
        request: WSRequest
    ): Resource[F, WSConnectionHighLevel[F]] =
      for {
        stream <- Resource
          .make {
            Sync[F]
              .delay(
                new facades.WebSocketStream(
                  request.uri.renderString,
                  js.Dynamic.literal( /* signal = abortSignal */ )
                )
              )
          } { stream => Sync[F].delay(stream.close()) }

        opened <- Async[F].fromPromise(Sync[F].delay(stream.opened)).toResource
        reader <- Resource.make(Sync[F].delay(opened.readable.getReader()))(
          reader => Async[F].fromPromise(Sync[F].delay(reader.cancel()))
        )
      } yield {

        new WSConnectionHighLevel[F] {
          def closeFrame: DeferredSource[F, Close] = ???
          def send(wsf: WSDataFrame): F[Unit] = ???
          def sendMany[G[_]: Foldable, A <: WSDataFrame](
              wsfs: G[A]
          ): F[Unit] = ???

          def receive: F[Option[WSDataFrame]] = Async[F]
            .fromPromise {
              Sync[F].delay {
                reader.read()
              }
            }
            .map { chunk =>
              (chunk.value) match {
                case _ if chunk.done => None
                case text: String =>
                  Some(WSFrame.Text(text, last = false))
                case bytes: js.typedarray.Uint8Array =>
                  Some(
                    WSFrame
                      .Binary(ByteVector.fromUint8Array(bytes), last = false)
                  )

              }

            }

          def subprotocol: Option[String] = ???
        }
      }
  }

  extension [F[_]: Async](wsc: WSClientHighLevel[F]) {
    def withFallback(other: WSClientHighLevel[F]) = new WSClientHighLevel[F] {
      def connectHighLevel(
          request: WSRequest
      ): Resource[F, WSConnectionHighLevel[F]] =
        wsc.connectHighLevel(request).recoverWith {
          case jse: JavaScriptException if jse.exception match {
                case ref: js.ReferenceError
                    if ref.message.contains("WebSocketStream") =>
                  true
                case _ => false
              } =>
            Sync[F]
              .delay(
                println(
                  "WebSocketStream not available, falling back to WebSocketClient"
                )
              )
              .toResource *>
              other.connectHighLevel(request)
        }
    }
  }

  private object facades {

    @js.native
    @JSGlobal("WebSocketStream")
    class WebSocketStream(url: String, args: js.Dynamic) extends js.Any {
      def opened: js.Promise[WebSocketStreamOpened] = js.native
      def closed: js.Promise[WebSocketStreamClosed] = js.native
      def close(): Unit = js.native
    }

    @js.native
    trait WebSocketStreamOpened extends js.Object {
      val readable: ReadableStream[String | js.typedarray.Uint8Array]
      // val writable: WriteableStream[js.Any]
    }

    @js.native
    trait WebSocketStreamClosed extends js.Object {
      val closeCode: Int
      val reason: String
    }
  }
}
