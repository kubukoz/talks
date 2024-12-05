import calico.html.io.*
import calico.html.io.given
import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.*
import org.http4s.implicits.*

object ChannelSelector {

  def show(midiChannel: SignallingRef[IO, Int], instrumentLock: Resource[IO, Unit])
    : Resource[IO, HtmlDivElement[IO]] = div(
    "MIDI Channel:",
    select.withSelf { self =>
      (
        idAttr := "midichannel-select",
        (0 to 15).map { i =>
          option(i.toString)
        }.toList,
        value <-- midiChannel.map(_.toString),
        onChange {
          self
            .value
            .get
            /* wishful thinking */
            .map(_.toInt)
            .flatMap(v => instrumentLock.surround(midiChannel.set(v)))
        },
      )
    },
  )

}
