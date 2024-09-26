import calico.IOWebApp
import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*

import org.http4s.dom.WebSocketClient
import org.http4s.implicits.*
import org.http4s.client.websocket.WSRequest
import DataChannel.State
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

      makeWs = WebSocketClient[IO]
        .connectHighLevel(
          WSRequest(
            if isLeader then uri"ws://localhost:8080/leader"
            else
              uri"ws://localhost:8080"
          )
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
      _ <- Player.run(
        trackState = trackState,
        midiChannel = channelRef,
        holdAtRef = holdAtRef,
        currentNoteRef = currentNoteRef,
        playingRef = playingRef,
        transposeRef = transposeRef,
      )
      _ <-
        KeyStatus
          .forKey("h")
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
      ),
      NoteEditor.show(editedNoteRef, trackState),
      div(
        "Connection state: ",
        dataChannel.state.map {
          case State.Connected  => span(styleAttr := "color: green", "Connected")
          case State.Connecting => span(styleAttr := "color: orange", "Connecting...")
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
