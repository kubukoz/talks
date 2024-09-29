import DataChannel.State
import calico.IOWebApp
import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.implicits.*
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*
import io.circe.syntax.*
import org.http4s.Uri
import org.http4s.client.websocket.WSRequest
import org.http4s.dom.WebSocketClient
import org.scalajs.dom

import scala.scalajs.js.JSConverters.*

val stepCount = 16

object SeqApp extends IOWebApp {

  def render: Resource[IO, HtmlElement[IO]] = {
    for {
      isLeader <-
        window
          .location
          .search
          .get
          .map {
            case "?leader" => true
            case _         => false
          }
          .toResource

      host <- window.location.host.get.toResource
      secure <- window.location.protocol.get.toResource.map(_ == "https:")
      makeWs = WebSocketClient[IO]
        .connectHighLevel(
          WSRequest {
            val protocol =
              if secure then "wss"
              else
                "ws"
            if isLeader then Uri.unsafeFromString(s"$protocol://$host/ws/leader")
            else
              Uri.unsafeFromString(s"$protocol://$host/ws")
          }
        )
        .evalTap(_ => IO.println("Acquired new WebSocket connection"))

      dataChannel <- DataChannel.fromFallible(makeWs.map(DataChannel.fromWebSocket))
      trackState <- TrackState.remote(dataChannel, data.initTracks)
      // trackState <- makeWs
      //   .map(DataChannel.fromWebSocket)
      //   .flatMap(TrackState.remote(_, data.initTracks))
      // trackState <- SignallingRef[IO]
      //   .of(data.initTracks)
      //   .toResource
      //   .map(TrackState.fromSignallingRef)

      // Propagate state whenever a leader shows up
      // todo: also request state when a new follower shows up
      _ <-
        if isLeader then trackState.read.get.flatMap(trackState.set).background
        else
          // request broadcast of latest state
          dataChannel.send(Message.Get).toResource

      currentNoteRef <- SignallingRef[IO].of(0).toResource
      holdAtRef <- SignallingRef[IO].of(none[Int]).toResource
      channelRef <- SignallingRef[IO].of(2).toResource
      playingRef <- SignallingRef[IO].of(false).toResource
      transposeRef <- SignallingRef[IO].of(0).toResource
      recordingRef <- SignallingRef[IO].of(false).toResource
      recordingTrackRef <- SignallingRef[IO].of(1).toResource

      midiAccessLazy <-
        window
          .navigator
          .requestMIDIAccess
          .flatMap(_.outputs)
          .map(_.head._2)
          .flatTap(out => IO.println(s"Connected device: ${out.name}"))
          .memoize
          .toResource

      instrument = Instrument.fromSos()
      // instrument = Instrument.suspend(
      //   midiAccessLazy.map(Instrument.fromMidiOutput(_, channelRef.get))
      // )

      _ <-
        Player
          .run(
            instrument = instrument,
            trackState = trackState,
            holdAtRef = holdAtRef,
            currentNoteRef = currentNoteRef,
            playingRef = playingRef,
            transposeRef = transposeRef,
          )
          .background
          .flattenK
      _ <-
        KeyStatus
          .forKey("b")
          .changes
          .evalMap {
            // we inc by one because we want to hold the next note
            case true  => currentNoteRef.get.map(_.+(1).%(stepCount)).map(_.some)
            case false => none[Int].pure[IO]
          }
          .foreach(holdAtRef.set)
          .compile
          .drain
          .background
      _ <-
        KeyStatus
          .forKey(" ")
          // these lines make sure we only toggle when the key is pressed
          .changes
          .filter(identity)
          //
          .foreach(_ => playingRef.update(!_))
          .compile
          .drain
          .background
      _ <- (0 to 9).toList.traverse_ { key =>
        KeyStatus
          .forKey(key.show)
          .filter(identity)
          .foreach(_ => transposeRef.set(key))
          .compile
          .drain
          .background
      }
      _ <-
        Recorder
          .run(
            instrument,
            currentNoteRef,
            recordingRef,
            recordingTrackRef,
            trackState,
          )
          .background
      editedNoteRef <- SignallingRef[IO].of((0, 0)).toResource
    } yield div(
      ChannelSelector.show(channelRef),
      div("current note: ", currentNoteRef.map(_.show)),
      div("hold: ", holdAtRef.map(_.show)),
      div(
        playingRef.map {
          if _
          then "playing"
          else
            "paused"
        }
      ),
      div("transpose: ", transposeRef.map(_.show)),
      SequencerView.show(
        trackState,
        currentNoteRef,
        holdAtRef,
        editedNoteRef,
        recordingTrackRef,
        recordingRef,
      ),
      NoteEditor.show(editedNoteRef, trackState),
      div(
        "Connection state: ",
        dataChannel.state.map {
          case State.Connected  => span(styleAttr := "color: green", "Connected")
          case State.Connecting => span(styleAttr := "color: orange", "Connecting...")
        },
      ),
      button(
        "Download track",
        onClick --> {
          _.foreach { _ =>
            trackState.read.get.map(_.asJson.noSpaces).flatMap(FileDownload.runDownload)
          }
        },
      ),
      div(
        "Upload track:",
        input.withSelf { self =>
          (
            `type` := "file",
            onChange --> {
              _.foreach { _ =>
                IO.fromPromise(IO(self.asInstanceOf[dom.HTMLInputElement].files(0).text()))
                  .flatMap(io.circe.parser.decode[List[List[Playable]]](_).liftTo[IO])
                  .flatMap(trackState.set)
                  .handleErrorWith(IO.consoleForIO.printStackTrace(_))
              }
            },
          )
        },
      ),
    )
  }.flatten

}

object NoteEditor {

  def show(
    editedNoteRef: Signal[IO, (Int, Int)],
    trackState: TrackState,
  ): Signal[IO, Resource[IO, HtmlDivElement[IO]]] = editedNoteRef.map {
    case (editedTrack, editedNote) =>
      div(
        "edited note: ",
        trackState.read.map(_(editedTrack)(editedNote).toString()),
        button(
          "clear",
          onClick --> {
            _.foreach { _ =>
              trackState.update { tracks =>
                tracks
                  .updated(
                    editedTrack,
                    tracks(editedTrack).updated(editedNote, Playable.Rest),
                  )
              }
            }
          },
          styleAttr <-- trackState
            .read
            .map(_(editedTrack)(editedNote) match {
              case Playable.Rest => "display: none"
              case _             => ""
            }),
        ),
        button(
          "C4",
          onClick --> {
            _.foreach { _ =>
              trackState.update { tracks =>
                tracks
                  .updated(
                    editedTrack,
                    tracks(editedTrack).updated(editedNote, Playable.C4),
                  )
              }
            }
          },
          styleAttr <-- trackState
            .read
            .map(_(editedTrack)(editedNote) match {
              case _: Playable.Play => "display: none"
              case _                => ""
            }),
        ),
        button(
          "pitch up",
          onClick --> {
            _.foreach { _ =>
              trackState.update { tracks =>
                tracks
                  .updated(
                    editedTrack,
                    tracks(editedTrack).updated(editedNote, tracks(editedTrack)(editedNote) + 1),
                  )
              }
            }
          },
        ),
        button(
          "pitch down",
          onClick --> {
            _.foreach { _ =>
              trackState.update { tracks =>
                tracks
                  .updated(
                    editedTrack,
                    tracks(editedTrack).updated(editedNote, tracks(editedTrack)(editedNote) - 1),
                  )
              }
            }
          },
        ),
      )
  }

}

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
