//> using dep org.http4s::http4s-dom::0.2.11
//> using dep com.armanbilge::calico::0.2.2
//> using dep dev.optics::monocle-core::3.3.0
//> using dep org.typelevel::kittens::3.4.0
//> using dep io.circe::circe-core::0.14.10
//> using dep io.circe::circe-scalajs::0.14.10
//> using dep io.circe::circe-parser::0.14.10
//> using dep org.typelevel::keypool::0.4.10
//> using platform js
//> using jsModuleKind common
//> using option -no-indent
//> using option -Wunused:all
//> using option -Xkind-projector
import calico.IOWebApp
import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.*
import fs2.dom.ext.FS2DomExtensions.*

import scala.concurrent.duration.{span as _, *}

val stepCount = 16

trait SeqApp extends IOWebApp {

  def render: Resource[IO, HtmlElement[IO]] = {
    for {
      tracksRef <- SignallingRef[IO].of(data.initTracks).toResource
      currentNoteRef <- SignallingRef[IO].of(0).toResource
      holdAtRef <- SignallingRef[IO].of(none[Int]).toResource
      channelRef <- SignallingRef[IO].of(2).toResource
      playingRef <- SignallingRef[IO].of(false).toResource
      transposeRef <- SignallingRef[IO].of(0).toResource
      _ <- Player.run(
        tracksRef = tracksRef,
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
      _ <-
        KeyStatus
          .forKey("ArrowUp")
          .filter(identity)
          .foreach(_ => transposeRef.update(_ + 12))
          .compile
          .drain
          .background
      _ <-
        KeyStatus
          .forKey("ArrowDown")
          .filter(identity)
          .foreach(_ => transposeRef.update(_ - 12))
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
      trackState = TrackState.fromSignallingRef(tracksRef)
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
      )
  }

}

object SequencerView {

  def show(
    trackState: TrackState,
    currentNoteRef: Signal[IO, Int],
    holdAtRef: Signal[IO, Option[Int]],
    editedNoteRef: SignallingRef[IO, (Int, Int)],
  ): Resource[IO, HtmlTableElement[IO]] = table(
    styleAttr := """
                   |border: 2px solid black
                   |""".stripMargin,
    thead(
      tr(
        th("·"),
        (0 until stepCount).map { i =>
          th(
            holdAtRef.map { holdAt =>
              holdAt.getOrElse(i).show
            },
            styleAttr <-- currentNoteRef.map { current =>
              if i == current then "color: red"
              else
                ""
            },
          )
        }.toList,
      ),
      styleAttr := """
                     |border: 2px solid black
                     |""".stripMargin,
    ),
    tbody(
      children[(List[Playable], Int)] { (track, trackIndex) =>
        tr(
          td(
            ""
          ),
          track.zipWithIndex.map { (playable, noteIndex) =>
            td(
              styleAttr := """
                             |border: 2px solid black
                             |""".stripMargin,
              (holdAtRef, trackState.read)
                .mapN {
                  case (None, _)              => playable
                  case (Some(holdAt), tracks) => tracks(trackIndex)(holdAt)
                }
                .map {
                  case Playable.Rest                   => "_"
                  case Playable.Play(noteId, velocity) => "x"
                },
              input.withSelf { self =>
                (
                  `type` := "radio",
                  nameAttr := "edit-playable",
                  value := show"$trackIndex,$noteIndex",
                  checked <-- editedNoteRef.map { case (editedTrack, editedNote) =>
                    editedTrack == trackIndex && editedNote == noteIndex
                  },
                  onChange --> {
                    _.foreach { _ =>
                      self.value.get.flatMap {
                        case s"$trackIndex,$noteIndex" =>
                          editedNoteRef.set((trackIndex.toInt, noteIndex.toInt))
                        case _ => IO.raiseError(new Exception("invalid value of edit-playable"))
                      }
                    }
                  },
                )
              },
            )
          },
        )
      } <-- trackState.read.map(_.zipWithIndex)
    ),
  )

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

object ChannelSelector {

  def show(midiChannel: SignallingRef[IO, Int]): Resource[IO, HtmlDivElement[IO]] = div(
    "MIDI Channel:",
    select.withSelf { self =>
      (
        idAttr := "midichannel-select",
        (0 to 15).map { i =>
          option(i.toString)
        }.toList,
        value <-- midiChannel.map(_.toString),
        onChange --> {
          _.foreach(_ =>
            self
              .value
              .get
              /* wishful thinking */
              .map(_.toInt)
              .flatMap(midiChannel.set)
          )
        },
      )
    },
  )

}

object Player {

  def run(
    tracksRef: Signal[IO, List[List[Playable]]],
    midiChannel: Ref[IO, Int],
    holdAtRef: Ref[IO, Option[Int]],
    currentNoteRef: Ref[IO, Int],
    playingRef: Signal[IO, Boolean],
    transposeRef: Ref[IO, Int],
  ): Resource[IO, Unit] = Window[IO]
    .navigator
    .requestMIDIAccess
    .flatMap(_.outputs)
    .map(_.values.head)
    .toResource
    .flatMap { output =>
      val period = 1.minute / 120 / 4
      fs2
        .Stream
        .fixedRateStartImmediately[IO](period)
        .zipRight(fs2.Stream.emits(0 until stepCount).repeat)
        .pauseWhen(playingRef.map(!_))
        .evalTap(currentNoteRef.set)
        .foreach { noteIndex =>
          (midiChannel.get, holdAtRef.get, transposeRef.get, tracksRef.get)
            .flatMapN { (channel, holdAt, transpose, tracks) =>
              // we play notes of each track in parallel
              // because their Off messages need to be sent at roughly the same time
              // and we can't wait for one note to finish before starting the next
              tracks
                .parTraverse_ { track =>
                  (track(holdAt.getOrElse(noteIndex)) + transpose) match {
                    case Playable.Play(noteId, velocity) =>
                      IO.uncancelable { poll =>
                        // playing is cancelable, stopping isn't.
                        // (browsers ignore this anyway though)
                        poll(
                          output.send(MIDI.NoteOn(channel, noteId, velocity).toArray)
                        ) *>
                          IO.sleep(period / 4) *>
                          output.send(MIDI.NoteOff(channel, noteId, 0).toArray)
                      }
                    case Playable.Rest => IO.unit
                  }
                }
            }
        }
        .compile
        .drain
        .background
        .void
    }

}
