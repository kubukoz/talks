<!-- TODOs

- emphasize earlier on that the concurrency primitives are just fs2/CE?
- lead up to the demo with a short story (think about whether I can put it in the beginning)

 -->

# <a href="https://armanbilge.com/calico/" target="_blank">Calico</a> – the functional frontend library you didn’t know you needed
## Jakub Kozłowski | Art Of Scala | 10.10.2024, Warsaw
Slides, contact etc.: https://linktr.ee/kubukoz

<div style="width: 100%; text-align: center">
  <br/>
  <a href="https://typelevel.org" target="_blank"><img src="img/typelevel.png" style="height: 100px"/></a>
  <a href="https://typelevel.org/cats-effect" target="_blank"><img src="img/cats-effect.png" style="height: 100px"/></a>
  <a href="https://fs2.io" target="_blank"><img src="img/fs2.png" style="height: 100px"/></a>
</div>

```scala mdoc:js:shared:invisible
import scalajs.js
import cats.syntax.all.*
import calico.html.io.*
import calico.html.io.given
import calico.syntax.*
import calico.syntax.given
import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import fs2.dom.Node
import fs2.dom.HtmlDivElement
import org.scalajs.dom
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.Chunk
import org.http4s.dom.*
import org.http4s.implicits.*
import demo.IntersectionObserver
import demo.WebSocketStreamClient
import demo.WebSocketStreamClient.*
import scala.concurrent.duration.{span => _, *}
import cats.effect.unsafe.implicits.*
import org.http4s.client.websocket.WSRequest
import org.http4s.client.websocket.WSFrame


extension [A <: Node[IO]](ioRes: Resource[IO, A]) {
  def renderHere(node: dom.Node) = {
    ioRes
      .renderInto(node.asInstanceOf[Node[IO]])
      .useForever.unsafeRunAndForget()
  }
}
```

---

## ⚠️ Warning: optimized for replay value™️

<div style="width: 100%; text-align: center">
  <img src="./img/replay-value.png" style="width: 800px"/>
</div>

Check out **the recording, slides and links** later!

---

## What's Calico?

- A _truly_ functional frontend library for Scala.js
- Builds up on cats-effect and fs2

---

```scala
// core idea
def div(...): Resource[IO, HtmlElement[IO]]
```

---

## `cats.effect.Resource`

```scala
trait Resource[F[_], A] {
  def use[B](f: A => F[B]): F[B]
}

object Resource {
  def make[F[_], A](acquire: F[A])(release: A => F[Unit]): Resource[F, A]
}
```

Encapsulates the lifecycle of a stateful resource: **allocation** -> usage -> **cleanup**.

---

## Resource composition? 😬

```scala
mkConnection.use { conn =>
  makeClient(conn).use { client =>
    makeServer(conn).use { server =>
      client.call(server)
    }
  }
}
```

Looks familiar...

---

<img src="img/hadouken.jpg" style="width: 800px"/>

---

## Resource composition: ✨ `flatMap` ✨

```scala
val myApp = for {
  conn   <- mkConnection
  client <- makeClient(conn)
  server <- makeServer(conn)
} yield (client, server)

myApp.use { (client, server) =>
  client.call(server)
}
```

---

## Components in Calico are Resources...

...on steroids!

```scala mdoc:js
val myComponent1 = for {
  d <- div("Hello, world!")
  b <- button("Click me!")
} yield div(d, b)

// or, usually better:
val myComponent2 = div(
  div("Hello, world!"),
  button("Click me!")
)

myComponent2.renderHere(node)
```

---

## OK, we have a component that's a resource.

How do we make it interactive?

---

## 👂 We can add listeners...

```scala mdoc:js
button(
  onClick(IO(dom.window.alert("Button clicked!"))),
  "Click me!"
)
.renderHere(node)
```

---

## We can get state out of the DOM...

```scala mdoc:js
input.withSelf { self =>
  onChange(self.value.get.flatMap { value => IO(dom.window.alert(value))})
}
.renderHere(node)
```

---

## How do we share state between elements?

Hint: it's the same as with any other cats-effect application.

---

`cats.effect.Ref`!

```scala
// simplified
trait Ref[A] {
  def get: IO[A]
  def update(f: A => A): IO[Unit]
}

Ref[IO].of(initialValue: A): IO[Ref[A]]
```

---

## `Ref` 101

```scala
val mkRef = Ref[IO].of(0)

// results in 1
mkRef.flatMap { ref =>
  ref.update(_ + 1) *>
    ref.get
}

// results in 0
mkRef.flatMap(_.update(_ + 1)) *>
  mkRef.flatMap(_.get)
```

To share a `Ref`, you need to pass it around after **creating it once**.

---

## `Ref`s in Calico

```scala mdoc:js
Resource.eval(Ref[IO].of("")).flatMap { ref =>
  div(
    input.withSelf { self =>
      (
        // set
        onInput(self.value.get.flatMap(ref.set)),
        placeholder := "What's your name?"
      )
    },
    button(
      "Submit",
      // get
      onClick(ref.get.flatMap(name => IO(dom.window.alert(s"Hello, $name!"))))
    )
  )
}
.renderHere(node)
```

---

## OK, so we can store state in a Ref.

But how do we display it as it changes?

```scala
Ref[IO].of(0).toResource.flatMap { ref =>
  div(
    "Counter: ",
    value := ref, // compile error
    button(
      onClick(ref.update(_ + 1)),
      "Increment"
    )
  )
}
```

---

## We need something that can:

- provide a value to be displayed, and
- notify us when the state changes

🤔

---

## We need a `Signal`!

```scala
// simplified
trait Signal[A] {
  def continuous: fs2.Stream[IO, A]
  def discrete: fs2.Stream[IO, A]
  def get: IO[A]
}
```

- always has a value that can be read
- provides continuous/discrete updates as streams

---

## Signals come in two main flavors

- A signal from an `fs2.Stream`: `.hold` and its variants
- A `SignallingRef` (`Ref` + `Signal` created together)

---

## `Signal` from a `Stream`

```scala mdoc:js
fs2.Stream
  .awakeEvery[IO](100.millis).holdResource(0.seconds)
  .flatMap { signal =>
    div(
      "The presentation has been running for ",
      signal.map(_.toSeconds.toString),
      " seconds."
    )
  }
  .renderHere(node)
```

---

## `SignallingRef`

Acts like a `Ref`, is also a `Signal`

```scala mdoc:js
SignallingRef[IO].of(0).toResource
  .flatMap { ref =>
    button(
      onClick(ref.update(_ + 1)),
      styleAttr := "font-size: 1em",
      ref.map(_.toString),
      " clicks"
    )
  }
  .renderHere(node)
```

---

## `Signal`s can be used for more than just displaying state

For example, styling:

<!-- also shows separation of refs -->

```scala mdoc:js
val component = SignallingRef[IO].of(0).toResource.flatMap { ref =>
  button(
    onClick(ref.update(_ + 1)),
    "Clicks: ", ref.map(_.toString),

    styleAttr <-- ref.map(s => s"font-size: ${(s + 1)}em")
  )
}

div(component, component).renderHere(node)
```

---

## There's plenty of Streams in the DOM

Courtesy of [fs2-dom](https://github.com/armanbilge/fs2-dom)

```scala mdoc:js
val mouseEvents = fs2.dom.events[IO, dom.MouseEvent](dom.document, "mousemove")

mouseEvents
  .map(e => (e.clientX, e.clientY))
  .holdResource((0, 0))
  .flatMap {
    sig => div(sig.map(_.toString))
  }
  .renderHere(node)
```

---

## With some setup...

```scala mdoc:js:shared
def keyboardEvent(key: Char, tpe: String) =
  fs2.dom.events[IO, dom.KeyboardEvent](dom.document, tpe).filter(_.key == key.toString)

def keyEvents(key: Char) =
  keyboardEvent(key, "keydown").either(
    keyboardEvent(key, "keyup")
  ).map(_.isLeft)

def showBool(b: Boolean) = if b then "✅" else "❌"

def showLetter(k: Char, state: Signal[IO, Boolean]) =
  div(code(k.toString), ": ", state.map(showBool))
```

---

## ...we can display live keyboard state

```scala mdoc:js
keyEvents('k')
  .holdResource(false)
  .flatMap(showLetter('k', _))
  .renderHere(node)
```

What if we want to track more keys?

---

<!-- Monadic composition of Resource in a traverse -->

```scala mdoc:js

"qwerty"
  .toList
  // !
  .traverse { key =>
    keyEvents(key).holdResource(false).tupleLeft(key)
  }
  .flatMap {
    signals => div(signals.map(showLetter))
  }
  .renderHere(node)
```

---

## What about something more complex?

Like a WebSocket stream?

---

<!-- WebSocketStreamClient: Implementation of unstable web API: https://github.com/http4s/http4s-dom/pull/384 -->

```scala mdoc:js:shared
val client = WebSocketStreamClient[IO].withFallback(WebSocketClient[IO])

val wsMessages = client.connectHighLevel(WSRequest(uri"ws://localhost:8080"))
  .flatMap {
    _.receiveStream.collect { case WSFrame.Text(text, _) => text }
      .filterNot(_.isBlank)
      .sliding(10).map(lines => div(lines.map(p(_)).toList))
      .metered(1.second / 10)
      .holdResource(div(""))
  }
```

---

```scala mdoc:js:shared
def shrek(node: org.scalajs.dom.Element) = IntersectionObserver.isVisible(node).flatMap { visible =>
  div(
    visible.map {
      case true  => div(wsMessages)
      case false => div("")
    }
  )
}.renderHere(node)
```

---

```scala mdoc:js
shrek(node)
```

---

![rmba-20](./img/rbma.avif)

---

## Demo time!

<video src="./img/rbma-demo.MOV" width="320" height="240" controls loop></video>

---


## Summary

- `Resource`s and `Stream`s are powerful constructs
- They compose well with each other, and can model DOM interactions effectively
- These are **the same constructs** and **the same patterns** as those we use in backend code

---

## Thank you

- Slides: https://linktr.ee/kubukoz
- My YouTube: https://www.youtube.com/@kubukoz_
- Calico: https://armanbilge.com/calico/
