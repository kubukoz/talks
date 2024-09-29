```scala mdoc:js:shared:invisible
import scalajs.js
import calico.html.io.*
import calico.html.io.given
import calico.syntax.*
import calico.syntax.given
import cats.effect.IO
import scala.concurrent.duration.*
import fs2.dom.Node
import org.scalajs.dom
import cats.effect.Resource
import cats.syntax.all.*
import fs2.concurrent.Signal
import org.http4s.dom.*
import org.http4s.implicits.*
import demo.IntersectionObserver
import demo.WebSocketStreamClient

extension [A <: Node[IO]](ioRes: Resource[IO, A]) {
  def renderHere(node: dom.Node) = {
    ioRes
    .renderInto(node.asInstanceOf[Node[IO]])
    .useForever.unsafeRunAndForget()(using cats.effect.unsafe.IORuntime.global)
  }
}
```

```scala mdoc:js
fs2.Stream.awakeEvery[IO](100.millis).holdResource(0.seconds).flatMap { signal =>
  div(
    "Presentation has been running for ",
    signal.map(_.toSeconds.toString),
    " seconds"
  )
}
.renderHere(node)
```

---

```scala mdoc:js
val mouseEvents = fs2.dom.events[IO, dom.MouseEvent](dom.document, "mousemove")

mouseEvents.map(e => (e.clientX, e.clientY)).holdResource((0, 0)).flatMap {
  sig => div(sig.map(_.toString))
}
.renderHere(node)
```

---

Some setup...

```scala mdoc:js:shared
def keyboardEvent(key: Char, tpe: String) =
  fs2.dom.events[IO, dom.KeyboardEvent](dom.document, tpe).filter(_.key == key.toString)

def keyEvents(key: Char) =
  keyboardEvent(key, "keydown").either(
    keyboardEvent(key, "keyup")
  ).map(_.isLeft)

def showBool(b: Boolean) = if b then "✅" else "❌"
```

And the show begins!

```scala mdoc:js
keyEvents('k')
  .holdResource(false)
  .flatMap {
    sig => div("k: ", sig.map(showBool))
  }
  .renderHere(node)
```

---

```scala mdoc:js
def showLetter(k: Char, state: Signal[IO, Boolean]) =
  div(code(k.toString), s": ", state.map(showBool))

"qwerty"
  .toList
  .traverse { key =>
    keyEvents(key).holdResource(false).tupleLeft(key)
  }
  .flatMap {
    signals => div(signals.map(showLetter))
  }
  .renderHere(node)
```

---

```scala mdoc:js
import org.http4s.client.websocket.{WSRequest, WSFrame}

// Implementation of unstable web API: https://github.com/http4s/http4s-dom/pull/384
val wsMessages = WebSocketStreamClient[IO]
  .connectHighLevel(
    WSRequest(uri"ws://localhost:9001")
  )
  .flatMap {
    _.receiveStream.collect { case WSFrame.Text(text, _) => text }.holdOptionResource
  }
  .map(_.map(_.map(code(_))))

  IntersectionObserver.isVisible(node).flatMap { visible =>
    div(
      visible.map {
        case true  => div(wsMessages)
        case false => div("")
      }
    )
  }
  .renderHere(node)
```
