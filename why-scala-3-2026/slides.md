---
theme: none
title: Why did we choose Scala 3?
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

# Why did we choose Scala 3?

Type system · Scalability · Tooling — and, because it's 2026, AI

---
eyebrow: Agenda
class: spread
---

# Three reasons, and a bonus

<div class="cards">
  <Card number="1" title="Type system" />
  <Card number="2" title="Scalability" />
  <Card number="3" title="Tooling" />
  <Card number="+" title="Bonus: ✨AI✨" dark />
</div>

<p class="muted">And because it's 2026… a bonus section on AI.</p>

---
layout: section
number: '01'
---

# Type system

Two main strengths: **expressiveness** and **type safety**

<!--
Not only why we chose Scala, but also why we continue to invest in it.

You might expect me to make these two separate sections, but in reality they complement each other, so I'll treat them as one thing.
-->

---
eyebrow: Type system
---

# Modeling complex domains

`enum` and `case class` get you 90% of the way there.

```scala
enum ClipKind:
  case Song, Talk, Promo

case class Clip(
  id: ClipId,
  kind: ClipKind,
  duration: DurationSeconds
)
```

Need something more powerful? `sealed trait` is still there.

<!--
DRAFT code example on the sequencing domain — replace with a real one if you prefer.
Idea from the outline: try to make all code examples work on the same domain (sequencing).
-->

---
eyebrow: Type system
---

# Modeling complex domains, pt. 2

`opaque type` gives you cheap value types, so you can describe just the minimal set of operations on primitives, too.

```scala
opaque type DurationSeconds = Int

object DurationSeconds:
  def apply(seconds: Int): DurationSeconds = seconds

  extension (d: DurationSeconds)
    def +(other: DurationSeconds): DurationSeconds = d + other
    def toSeconds: Int = d
```

<!--
DRAFT example — the outline's TODO: use DurationSeconds from algo, which was made for perf. Swap in the real code.
-->

---
eyebrow: Type system
---

# Separation of plan and execution

```scala
def skip(reason: SkipReason): IO[Unit]
val personalise: IO[Unit]

planningService
  .plan(latestAttempt, runId, lastFeedbackTs, assignedExperiments)
  .flatMap {
    case Plan.Noop(reason) => skip(reason)
    case Plan.Personalise  => personalise
  }
```

A clean split between plan and execution, on many levels: ADTs + evaluation, or IO computations + evaluation.

<!--
And I don't mean the highest rated Better Call Saul episode (S6E7).

Capable of clean separation between plan & execution, on many levels (ADTs + eval, or IO comp + eval).
-->

---
eyebrow: Type system
---

# Doing more with less

<div class="grow code-lg">

```go
if err != nil {
  // wicked tomfoolery inside
}
```

</div>

<p class="lead center">How much do you do on your own, and how much can you trust the language to do for you?</p>

<!--
Every language has its uses, so I won't be using any specific code samples from other languages for comparison here...

...but not that long ago I had a brief stint with Go...
...and I could definitely feel the difference in how much I had to do on my own, rather than trust the language to do it for me.

(Outline TODO: "I don't really buy it. Replace with something like 'the power of functional programming'?")
-->

---
eyebrow: Type system · path-dependent types
---

# Keeping things in check

Scala's strong type system lets you be exactly as precise as you need to be.

```scala
// The caller can't just return an arbitrary Expr,
// it has to be _the_ Expr from `ctx`.
def eval(computation: (ctx: Context) => ctx.Expr)
```

<!--
Recently, some of my personal favourites are path-dependent types and context functions.
-->

---
eyebrow: Type system · context functions
---

# Keeping things in check, pt. 2

<div class="code-sm">

```scala
trait Tracker[F[_]] {
  def continue[B]: F[B]
  def tick: F[Unit]
  def peek: F[IterationState]
  def update(f: IterationState => IterationState): F[Unit]
}

type Modifier[F[_], B] = Tracker[F] ?=> F[B]

// The caller passes a code block that uses `Tracker[F]`
// without having to know where it comes from
def loop[F[_]: {Ref.Make, MonadCancelThrow}](
  round: IterationState.Modifier[F, Unit]
): F[Vector[Clip]]
```

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
scala-cli run next-track.sc
```

</Card>

<BlockArrow />

<Card title="CLI app" compact>

…grow it to a command-line app…

```bash
scala-cli package .
./sequencer next-track
```

</Card>

<BlockArrow />

<Card title="Service" compact dark>

…and evolve it all the way to production.

```http
POST /internal/v1/
  sequencer/next-track
```

</Card>

</div>

<p class="muted">Same language, same libraries, all the way up.</p>

---
eyebrow: Scalability
---

# Scale down

<div class="split">
  <div class="main">
    <p class="big">In serverless, time is money and memory is at a premium.</p>
    <p class="lead">We've been experimenting with <strong>Scala Native lambdas</strong> and seeing promising results — and there's constant development in this area.</p>
  </div>
  <div class="side stack">
    <p class="label">The building blocks</p>
    <p class="item">Scala Native</p>
    <p class="item">Feral</p>
    <p class="item">AWS Lambda Web Adapter</p>
  </div>
</div>

<!--
Memory/time-based pricing in serverless — Scala Native lambdas!

Outline TODOs: some diagram with Lambda pricing? Show feral, LWA adapter. Lambda Web Functions (Node.js).
-->

---
eyebrow: Scalability · Scala.js
class: spread
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

<div class="split">
  <div class="main">
    <p class="big">A library for anything between simple scripts and enterprise-grade services.</p>
    <p>A batteries-included stdlib and toolkit — and the Typelevel stack: composable pieces that fit together like certain plastic toy bricks from Denmark.</p>
  </div>
  <div class="side">
    <Honeycomb
      :items="['Cats', 'Cats Effect', 'fs2', 'http4s', 'circe', 'Doobie', 'Skunk', 'log4cats', 'otel4s', 'Feral']"
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
