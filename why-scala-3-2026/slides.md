---
theme: none
title: Why did SiriusXM choose Scala 3?
layout: cover
canvasWidth: 1920
aspectRatio: 16/9
colorSchema: light
fonts:
  sans: IBM Plex Sans
  serif: Space Grotesk Variable
  mono: JetBrains Mono Variable
  provider: none
---

# Why SiriusXM chose Scala 3
## for its next-gen platform


---
class: spread
---

# Agenda

<div class="cards">
  <Card number="1" title="The language" />
  <Card number="2" title="Scalability" />
  <Card number="3" title="Tooling" />
  <Card number="+" title="Bonus: ✨AI✨" dark />
</div>


---
layout: section
number: '01'
---

# The language

**expressiveness** and **type safety**

<!--
Not only why we chose Scala, but also why we continue to invest in it.

You might expect me to make these two separate sections, but in reality they complement each other, so I'll treat them as one thing.
-->

---
eyebrow: The language
---

# Modeling complex domains

`enum` and `case class` get you 90% of the way there.

<v-click>

```scala
enum Source {
  case Station(id: StationId)
  case Channel(id: ChannelId, config: ChannelConfig)
}

case class ChannelConfig(rules: Ruleset, ...)
```

</v-click>

<v-click>

Need something more powerful? `sealed trait` is still there.

</v-click>

---
eyebrow: The language
---

# Modeling complex domains, pt. 2

`opaque type` for **flexible** zero-cost abstractions

<!-- `opaque type` gives you cheap value types, so you can describe just a small, **focused** set of operations on a primitive, too. -->

<v-click>

```scala {1|all}
opaque type Ruleset = List[Rule]

object Ruleset {
  extension (r: Ruleset) {
    def size: Int = r.size
    //...
  }
}
```

</v-click>

<!--
- the underlying implementation becomes invisible to the callers
- UserId =!= ChannelId
- the callers avoid coupling to a specific implementation and expensive refactors
- you can expand to a case class later if needed
-->

---
eyebrow: The language
---

# Plan and execution

<v-click>

```scala {1-2|1-5|all}
def skip(reason: SkipReason): IO[Unit]
val personalise: IO[Unit]

planningService
  .plan(latestAttempt, runId, lastFeedbackTs, assignedExperiments)
  .flatMap {
    case Plan.Noop(reason) => skip(reason)
    case Plan.Personalise  => personalise
  }
```

</v-click>

<p v-click class="muted">ADTs · exhaustive pattern matching · case lambdas · effects as values</p>

<!--
And I don't mean the highest rated Better Call Saul episode (season 6 episode 7).

Clean separation between plan & execution, on many levels (ADTs + eval, or IO comp + eval).

"Case lambdas": the spec calls them pattern matching anonymous functions. Often called "partial function literals", but here `flatMap` expects a plain function, so it's a total one.
-->

---
eyebrow: The language
class: tight
---

# Doing more with less

<p v-click class="lead">Programs as values → transform them like values</p>

<div class="grow versus">

<v-click>

```scala {1-2|1-3|all}
(history(user), experiments(user))
  .parMapN(generatePlaylist)
  .timeout(150.millis)
  .handleErrorWith(_ => genericPlaylist(user))
```

</v-click>

<div class="peek">

<div v-click>

```go
func playlistFor(ctx context.Context, user User) (Playlist, error) {
	tctx, cancel := context.WithTimeout(ctx, 150*time.Millisecond)
	defer cancel()
	g, gctx := errgroup.WithContext(tctx)
	var h History
	var e Experiments
	g.Go(func() (err error) {
		h, err = history(gctx, user)
		return err
	})
	g.Go(func() (err error) {
		e, err = experiments(gctx, user)
		return err
	})
	if err := g.Wait(); err != nil {
		return genericPlaylist(ctx, user)
	}
	return generatePlaylist(h, e), nil
}
```

</div>

<div v-click.hide class="peek-cover-wrap">
  <div v-after class="peek-cover"><img src="/go-peek.jpg" alt="" /><p>Now let's see Go's code.</p></div>
</div>

</div>

</div>

<!--
Every language has its uses, so I won't be using any specific code samples from other languages for comparison here...

...but not that long ago I had a brief stint with Go...
...and I could definitely feel the difference in how much I had to do on my own, rather than trust the language to do it for me.

[click] (tagline)

[click] In Scala, four lines:
- both lookups run in parallel; if one fails, the other is cancelled
- [click] if they don't finish within 150ms, both are cancelled and resources are released
- [click] any failure falls back to a generic playlist (which is an effect too)

[click] (Go block appears, blurred) Here's the same thing in Go. Just look at the size of it for now.

[click] Idiomatic Go: errgroup + context. It's explicit and it works — but it's on you to get it right:
- every call has to take a ctx and respect it, or the timeout silently stops applying
- results come back through variables the closures write to; reading them before g.Wait() is a data race
- three contexts in play: the fallback has to use the original ctx — pass the timed-out one and it fails immediately. The "natural" shadowing style (ctx, cancel := ...; g, ctx := ...) gets this wrong.
- adding/removing the timeout or fallback means restructuring the function

Be fair: cancellation is cooperative in both — cats-effect can't interrupt a blocking call either.

None of this is magic in the compiler — it's a library. But it's a library you can only write, and only make this pleasant, in a language this expressive.
-->

---
layout: statement
---

<SlideGif class="meme-gif" src="/bateman-card-drop.gif" poster="/bateman-card-drop.png" alt="Patrick Bateman dropping his business card" />

---
eyebrow: The language · path-dependent types
---

# Keeping things in check

Scala's strong type system lets you be exactly as precise as you need to be.

<v-click>

```scala
// The caller can't just return an arbitrary Expr,
// it has to be _the_ Expr from `ctx`.
def eval(computation: (ctx: Context) => ctx.Expr)
```

</v-click>

<!--
Recently, some of my personal favourites are path-dependent types and context functions.
-->

---
eyebrow: The language · context functions
---

# Keeping things in check, pt. 2

<div class="code-sm">

<v-click>

```scala
trait Tracker[F[_]] {
  def continue[B]: F[B]
  def tick: F[Unit]
  def peek: F[IterationState]
  def update(f: IterationState => IterationState): F[Unit]
}

// The caller passes a code block that uses `Tracker[F]`
// as if it came from the enclosing scope
def loop[F[_]: {Ref.Make, MonadCancelThrow}](
  round: Tracker[F] ?=> F[B]
): F[Vector[Clip]]
```

</v-click>

</div>

<!--
Example from the sequencing loop (algo-sequencing-service PR 1840).
-->

---
layout: section
number: '02'
---

# Scalability

Up, down, and sideways

---
eyebrow: Scalability
class: spread
---

# Scale up

<div class="flow">

<Card title="Script" compact>

Start with a single-file script…

```bash
scala-cli next-track.sc
```

</Card>

<BlockArrow v-click />

<Card title="CLI app" compact v-after>

…grow it to a command-line app…

```bash
scala-cli package .

./sequencer next-track
```

</Card>

<BlockArrow v-click />

<Card title="Service" compact dark v-after>

…and evolve it all the way to production.

```http
POST /internal/v1/sequencer/next-track
```

</Card>

</div>

<p v-click class="muted">Same language, same libraries, all the way up.</p>

---
eyebrow: Scalability
---

# Scale down

<p class="lead">In serverless, time is money:</p>

<div class="grow figure">
  <img src="/lambda-power-tuning.png" alt="AWS Lambda Power Tuning results: execution time and cost by memory size" />f
</div>

<p class="muted">Potential solution: <strong>Scala Native</strong>?</p>

<div class="puzzle">
  <div class="piece native"><img src="/logos/scala-native.png" alt="" />Scala Native</div>
  <span class="op">+</span>
  <div class="piece alt"><img src="/logos/typelevel-outline.svg" alt="" />Feral</div>
  <span class="op">or</span>
  <div class="piece alt"><img class="mono" src="/logos/aws.svg" alt="" />AWS Lambda Web Adapter</div>
</div>


<!--
Memory/time-based pricing in serverless — Scala Native lambdas!

We've been experimenting with Scala Native lambdas and seeing promising results — and there's constant development in this area.

Outline TODOs: show feral, LWA adapter. Lambda Web Functions (Node.js).
-->

---
eyebrow: Scalability · Scala.js
class: spread
hide: true
---

# Scale… aside?

You can also build frontend applications with Scala.js. We haven't seen much of it at SXM — my best guess why:

<div class="cards">
  <Card title="Mostly internal tools" compact><p>Bundle size, library support etc.</p></Card>
  <Card title="Glue and UI code" compact><p>These tools are rarely complex logic.</p></Card>
  <Card title="Tooling friction" compact><p>Standard tooling doesn't work that well with such a heavily transpiled language.</p></Card>
</div>

<p class="punch">But I've used it personally, and for the things I did, it worked great!</p>

---
eyebrow: Scalability
---

# Rich ecosystem

<div class="split even">
  <div class="main">
    <p class="label">Scala Toolkit</p>
    <ul class="toolkit">
      <li><strong>MUnit</strong><span>Testing</span></li>
      <li><strong>OS-Lib</strong><span>Files and processes</span></li>
      <li><strong>uPickle</strong><span>JSON parsing and serialization</span></li>
      <li><strong>sttp</strong><span>HTTP requests and file uploads</span></li>
      <li><strong>Cask</strong><span>Web servers</span></li>
    </ul>
  </div>
  <div class="side">
    <p class="label">Typelevel stack</p>
    <Honeycomb
      :items="['Cats', 'Cats Effect', 'fs2', 'skunk', 'weaver', 'circe', 'http4s', 'otel4s', 'log4cats', 'feral']"
      :highlight="1"
    />
  </div>
</div>

<!--
Rich ecosystem of libraries for anything between simple scripts and enterprise-grade services. Batteries-included stdlib and toolkit, and there's a library for anything you wanna do. The Typelevel stack is made of composable pieces, and they fit together like certain plastic toy bricks from Denmark.

The honeycomb is a stand-in for the Typelevel picture (outline TODO) — adjust the library list to what you actually use.
-->

---
layout: section
number: '03'
---

# Tooling

The compiler, Scalafix, and two great IDEs

---
eyebrow: Tooling
class: spread
---

# scalac, scalafix, …

<div class="tiles">
  <div class="tile">Catch problems early</div>
  <div class="tile">Efficient bytecode for prime-time production use 🔥</div>
  <div class="tile">In your way? Temporary tricks like <code>???</code> usually help</div>
  <div class="tile">Fast compilation: pay once for a full compile, incremental builds take no time — and it keeps getting better</div>
</div>

<p class="muted"><strong>Scalafix</strong> lets you apply organization-wide standards on the “micro” scale, automatically.</p>

<p class="punch">We take it for granted, but the compiler has your back.</p>

---
eyebrow: Tooling
class: spread
---

# IDEs: two great options

Just the fact that we have two, both maintained to this day, is a testament to how good we have it.

<div class="cards">
  <Card title="Metals" dark class="big-title"><p>Fast, accurate diagnostics, completions and more — straight from the compiler.</p></Card>
  <Card title="IntelliJ" dark class="big-title"><p>Powerful refactors, debugger, profiler, and more.</p></Card>
</div>

<p class="muted">Everyone should be able to find the one that solves their problem best.</p>

---
layout: accent
eyebrow: Bonus · It's 2026
---

## How did the age of agentic AI change our approach to Scala?

# Not much.

---
eyebrow: Bonus · AI
---

# Everything is mostly the same!

If anything, AI amplifies the benefits of the language.

<div class="compare">
  <div></div>
  <p class="col-head">For developers</p>
  <div></div>
  <p class="col-head hot">For agents</p>

  <h3>Expressiveness</h3>
  <div class="cell">Information-dense code, little boilerplate to write</div>
  <div class="arrow-cell"><BlockArrow :width="48" /></div>
  <div class="cell dark">Fewer tokens to read, write and keep in context</div>

  <h3>Types</h3>
  <div class="cell">Type-driven development, fearless refactoring</div>
  <div class="arrow-cell"><BlockArrow :width="48" /></div>
  <div class="cell dark">Semantic analysis and sanity checks that catch hallucinations</div>

  <h3>Tooling</h3>
  <div class="cell">Compiler, Metals and IntelliJ at your fingertips</div>
  <div class="arrow-cell"><BlockArrow :width="48" /></div>
  <div class="cell dark">Same tools via MCP: Metals MCP, Cellar, Scalex skills</div>

  <h3>Scaling up</h3>
  <div class="cell">Script → CLI app → production service</div>
  <div class="arrow-cell"><BlockArrow :width="48" /></div>
  <div class="cell dark">Vibe-coded MVP → production, with types as guardrails</div>
</div>

<!--
What was useful for developers is equally (if not more) useful to AI.

- An expressive language means code is more information-dense: fewer tokens to read/write and put into context. If large swathes of boilerplate don't need to be written, they don't need to be read. You still read and review your code, right?
- Types: type-driven development, semantic analysis, sanity checks/regression tests (preventing hallucination).
- Most tools are still applicable (even IntelliJ has an MCP).
- Tools save time and cycles jumping around the codebase: Metals MCP, Cellar, Scalex skills... (TODO pic: Bamboozling... Fidgeting...)
- A vibe-coded MVP can be scaled to a production system. Scala isn't immune to slop, but agents can be gently steered towards the right patterns, and types can serve as a guardrail.
-->

---
layout: statement
---

Some would say the language doesn't matter anymore, and ask “why Scala?”

# For us, it's now even more of a “why would you use anything else?”

If the language doesn't matter, nothing is stopping you from using Scala.

---
eyebrow: Bonus · AI
class: spread
---

# It goes both ways

<div class="cards">
  <Card><p class="lead">New AI-friendly tools are useful to developers too — directly or programmatically (e.g. <strong>Cellar</strong>).</p></Card>
  <Card><p class="lead">Tools like <strong>Metals</strong> get better as bugs become cheaper to fix.</p></Card>
</div>

<p class="punch xl">It's a win-win situation.</p>

---
layout: end
---

# Scala (3) isn't leaving SXM anytime soon

The initial decision to use Scala 3 is paying off every day.

Even now, when the language supposedly doesn't matter, we continue to see benefits on every new feature we launch — but also, most importantly, on every alarm that **doesn't** ring!
