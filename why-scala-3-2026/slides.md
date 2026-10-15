---
theme: none
title: Why did SiriusXM choose Scala 3?
layout: picture
image: /platform/title.png
canvasWidth: 1920
aspectRatio: 16/9
colorSchema: light
fonts:
  sans: IBM Plex Sans
  serif: Space Grotesk Variable
  mono: JetBrains Mono Variable
  provider: none
---


---
eyebrow: SiriusXM
---

# What is SiriusXM?

<div class="split top">
  <div class="main">
    <p class="big">
      Building a <span class="hl">connected audio platform</span> for music,
      sports, news, talk, podcasts, and in-car entertainment
    </p>
    <p class="punch">
      Serving millions of listeners across more than a dozen device platforms,
      including the vehicles they drive.
    </p>
  </div>

  <div class="side phone">
    <img src="/platform/app-home.png" alt="The SiriusXM app home screen" />
  </div>
</div>

<!--
Let's talk briefly about the company!

Leading audio entertainment company in North America.

The Next Gen Platform: backend services built on state-of-the-art architectural principles to power a unified SiriusXM experience for in-vehicle listeners in addition to consumer devices.

Brand new cloud platform and backend services. In production with automotive, mobile, web, and living room devices like smart speakers and TVs.

We're HEAVY users of Scala, and we're trying to grow our presence in Open Source (e.g. snapshot4s - snapshot testing for Scala made easy).
-->

---
layout: picture
image: /platform/car-dashboard.jpg
logo: true
---

---
eyebrow: Next Gen Platform
---

# Why did we build it?

<div class="cards">
  <Card number="1" title="Speed of innovation" />
  <Card number="2" title="Reliability" />
  <Card number="3" title="Unified experience across automotive and consumer devices" />
</div>

<!--
We had three goals when we set out to build this platform, but the common thread is raising the bar for our listener experience.
-->

---
eyebrow: Next Gen Platform
---

# Platform tenets

<div class="cards grid2 uniform grow">
  <Card title="API-first"><p>Modular domain services with versioned contracts enable independent evolution.</p><p><strong>→ Consistent contracts and schemas drive reliability across clients and services.</strong></p></Card>
  <Card title="Elastic scalability &amp; high availability"><p>Auto-scaling, redundant infrastructure designed for automotive-grade reliability.</p><p><strong>→ Low latency, graceful degradation and no cascading failure.</strong></p></Card>
  <Card title="Deep observability"><p>Full-stack visibility into the systems that power SiriusXM.</p><p><strong>→ Fewer production incidents and faster mitigation when they do occur.</strong></p></Card>
  <Card title="Event-driven architecture"><p>Domain services evolve independently and react asynchronously at scale.</p></Card>
</div>

<!--
Since we built this platform from the ground up, we were able to define a set of consistent platform tenets to adhere to.
-->

---
layout: section
---

# Architecture &amp; technical highlights

---
eyebrow: Architecture
class: tight
---

# High-level architecture

<div class="grow figure">
  <img src="/platform/architecture.png" alt="High-level architecture of the Next Gen Platform" />
</div>

---
eyebrow: Architecture
class: tight
---

# Edge

<div class="grow figure">
  <img src="/platform/edge.png" alt="Edge request flow: user, client, edge and services" />
</div>

---
eyebrow: Architecture
---

# Cloud native (AWS) &amp; resilient

<p class="lead">Built cloud-native on AWS, which gives us the resiliency, scalability and automation needed to operate at high reliability.</p>

We run across three AWS regions: **us-east-1**, **us-east-2** and **us-west-2**, with auto-scaling, automatic failover, region evacuation and global data replication.

<p class="punch">We absorb regional failures without service disruption, and scale dynamically with demand.</p>

---
layout: picture
image: /platform/regions.png
---

<svg class="overlay" viewBox="0 0 1920 1080" aria-hidden="true">
  <defs>
    <marker id="map-arrow" viewBox="0 0 10 10" refX="1" refY="5" markerWidth="4" markerHeight="4" orient="auto-start-reverse">
      <path d="M0 0 L10 5 L0 10 z" fill="#fff" />
    </marker>
  </defs>
  <!-- each arrow is aimed at its region's dot and stops in the water just short of the coast -->
  <g fill="none" stroke="#fff" stroke-width="5" stroke-linecap="butt" marker-end="url(#map-arrow)">
    <path d="M520 585 C600 530 690 540 780 520" />
    <path d="M1657 548 C1640 620 1380 600 1269 623" />
    <path d="M1548 772 C1450 790 1360 712 1258 679" />
  </g>
</svg>

---
eyebrow: Architecture
class: tight
---

# Disaster recovery via change data capture

<div class="grow figure">
  <img src="/platform/cdc.png" alt="CDC pipeline: DynamoDB streams through Kinesis and Lambda into domain events" />
</div>

---
eyebrow: Architecture
---

# Infrastructure as code (AWS CDK)

<div class="split top">
<div class="main">

<p class="lead">IaC automates creating our environments.</p>

<div class="features">
  <ul>
    <li>Reduces <strong>manual errors</strong> and prevents configuration drift across environments.</li>
    <li>Improves <strong>consistency at scale</strong>.</li>
    <li>Version-controlled changes are easier to validate, automate and <strong>roll back</strong> to a known stable state.</li>
  </ul>
</div>

</div>
<div class="side code-sm">

```ts
const table = new dynamodb.TableV2(this, 'Profiles', {
  partitionKey: { name: 'userId', type: STRING },
  replicas: [
    { region: 'us-east-2' },
    { region: 'us-west-2' },
  ],
  dynamoStream: StreamViewType.NEW_AND_OLD_IMAGES,
});
```

</div>
</div>

---
eyebrow: Architecture
---

# Domain services

<div class="split">
<div class="side figure">
  <img src="/platform/domain-services.png" alt="Domain services behind the edge: Browse, Profiles, Search, Billing, Recommender, Subscriptions, Playback, CMS" />
</div>
<div class="main features">
  <ul>
    <li><strong>Ownership bounded:</strong> services are modelled as domains and owned by a single team.</li>
    <li><strong>Loosely coupled:</strong> latency issues in Search won't affect Recommendations or Billing.</li>
    <li><strong>Event-driven:</strong> each service holds the local state it needs for its own logic, e.g. catalog metadata or listening history.</li>
    <li><strong>Type safe:</strong> services are built with type safe languages such as <strong>Scala&nbsp;3</strong>.</li>
  </ul>
</div>
</div>

---
eyebrow: Architecture · Smithy
---

# Contract-driven development with API Registry &amp; Smithy

<p class="lead">We build our services on top of the <strong>AWS Smithy</strong> IDL.</p>

<div class="cards">
  <Card title="Protocol agnostic"><p>Works with any programming language, describes services running in any environment, over any transport or serialization format.</p></Card>
  <Card title="API governance"><p>API standards and validation rules are enforced, incl. versioning, non-breaking changes and backwards compatibility.</p></Card>
</div>

---
eyebrow: Architecture · Smithy
---

# Code generation

<div class="split">
<div class="side figure">
  <img src="/platform/smithy-service.png" alt="Smithy service definition for PlaybackApi" />
</div>
<div class="main">

Client and server code is **automatically generated** from the Smithy model and enforced across the platform.

<p class="punch">Generated code is type safe and backward compatible.</p>

</div>
</div>

---
eyebrow: Architecture · Smithy
class: tight
---

# One model, every client

<div class="grow flow shots">
  <div class="figure"><img src="/platform/smithy-structure.png" alt="Smithy structure TuneSourceRequest" /></div>
  <BlockArrow v-click />
  <div v-after class="code-sm">

```scala
case class TuneSourceRequest(
  hlsVersion: HLSVersion = HLSVersion.V4,
  manifestVariant: Option[ManifestVariant] = None,
  encrypted: Option[Boolean] = None,
  explicit: Option[Boolean] = None,
  maxStreams: Option[Int] = None,
  mediaFormat: Option[MediaFormat] = None,
)
```

</div>
</div>

---
eyebrow: Architecture · Testing
hide: true
---

# Services automated testing

<div class="split">
<div class="side figure">
  <img src="/platform/testing-plugin.png" alt="SiriusXM E2E testing plugin documentation site" />
</div>
<div class="main">

Automated testing is a **first-class citizen** of the platform: we validate quality continuously and at scale.

Our end-to-end testing framework, built on top of Smithy, runs **contract-enforced tests** against the gateway across all regions or within one, including validation of downstream events such as Kinesis messages consumed by other services.

</div>
</div>

---
eyebrow: Architecture · Testing
hide: true
---

# Services automated testing, pt. 2

<div class="split">
<div class="main">

The **isTest** metadata capability lets us safely generate throw-away test data and run thousands of scenarios every few minutes: registration, login, purchase, playback etc.

All tests report back to **Datadog**, and teams are alerted on failures.

</div>
<div class="side shots-stack">
  <div class="figure"><img src="/platform/e2e-tests.png" alt="An end-to-end test written with the framework" /></div>
  <div class="figure"><img src="/platform/test-alerts.png" alt="Test failure notifications in Slack" /></div>
</div>
</div>

---
eyebrow: Architecture
hide: true
---

# Architecture for safe, rapid iteration

<div class="tiles roomy">
  <div class="tile"><strong>Feature flags</strong> enable dark launches and targeted rollouts before broad exposure.</div>
  <div class="tile"><strong>Canary deploys</strong> ramp changes up gradually, keeping customer impact low if we need to roll back.</div>
  <div class="tile"><strong>Remote configuration</strong> lets us change behavior at runtime without a full deployment.</div>
  <div class="tile"><strong>Loosely coupled services</strong> and small, focused deployments keep the blast radius small, so teams can ship frequently with confidence.</div>
</div>

---
layout: section
---

# Why we chose Scala&nbsp;3

<p class="topics">The language <span>·</span> Scalability <span>·</span> Tooling <span>·</span> ✨AI✨</p>


---
layout: section
number: '01'
---

# The language

<!--
Not only why we chose Scala, but also why we continue to invest in it.

Two main strengths: expressiveness and type safety. You might expect me to make these two separate sections, but in reality they complement each other, so I'll treat them as one thing.
-->

---
eyebrow: The language
---

# Modeling complex domains

`enum` and `case class` get you 90% of the way there.

<v-click>

```scala
enum Source {
  case Station(id: StationId, artistId: ArtistId)
  case Channel(id: ChannelId, config: ChannelConfig)
}

case class ChannelConfig(rules: Ruleset, launchDate: Option[LocalDate], ...)
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

<div class="split">

<div class="code-sm plan-code">

```scala
enum Plan {
  case Noop(reason: SkipReason)
  case Personalise
}

def skip(reason: SkipReason): IO[Unit]
val personalise: IO[Unit]
```

<v-click>

```scala {1-2|all}
planningService
  .plan(latestAttempt, runId, lastFeedbackTs, assignedExperiments)
  .flatMap {
    case Plan.Noop(reason) => skip(reason)
    case Plan.Personalise  => personalise
  }
```

</v-click>

</div>

<div class="features">
  <p v-click class="label">This works thanks to</p>
  <ul>
    <li v-after>ADTs</li>
    <li v-click>Exhaustive pattern matching</li>
    <li v-click>Case lambdas</li>
    <li v-click>Effects as values</li>
  </ul>
</div>

</div>

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

[click] Idiomatic Go: errgroup + context. It's explicit and it works - but it's on you to get it right:
- every call has to take a ctx and respect it, or the timeout silently stops applying
- results come back through variables the closures write to; reading them before g.Wait() is a data race
- three contexts in play: the fallback has to use the original ctx - pass the timed-out one and it fails immediately. The "natural" shadowing style (ctx, cancel := ...; g, ctx := ...) gets this wrong.
- adding/removing the timeout or fallback means restructuring the function

Be fair: cancellation is cooperative in both - cats-effect can't interrupt a blocking call either.

None of this is magic in the compiler - it's a library. But it's a library you can only write, and only make this pleasant, in a language this expressive.
-->

---
layout: statement
---

<SlideGif class="meme-gif" src="/bateman-card-drop.gif" poster="/bateman-card-drop.png" alt="Patrick Bateman dropping his business card" />

---
eyebrow: The language · path-dependent types
hide: true
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
hide: true
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
eyebrow: The language
---

# Binary stability

<div class="cards">
  <Card title="Scala 2" compact>
    <p>2.11 → 2.12 → 2.13: every library re-published for every version, and the ecosystem waits for the slowest one.</p>
  </Card>
  <Card title="Scala 3" compact dark v-click>
    <p>A library built with 3.x keeps working on every later 3.x. Compile once, upgrade whenever.</p>
  </Card>
</div>

<p v-click class="lead">And finally: <strong>LTS versions</strong> (3.3, 3.9) - a stable baseline for libraries and long-lived services.</p>

<!--
Scala 3 minor versions are backward binary compatible: code compiled with 3.x can be used from 3.y for any y >= x. Libraries publish once (often on the LTS line) and every newer Scala 3 project can use them.

Scala 3 also finally has LTS versions (3.3, 3.9): libraries can target the LTS line to reach the widest audience, and services can stay on it without missing fixes.

Contrast with Scala 2, where each minor version (2.11, 2.12, 2.13) was a separate binary world: libraries had to cross-publish, and upgrading meant waiting for all your dependencies.
-->

---
eyebrow: The language
hide: true
---

# Stability at scale

<div class="split">
  <div class="main">
    <p class="stat">255M</p>
    <p class="lead">monthly active users across all our apps. Our job: change things without anyone noticing.</p>
  </div>
  <div class="side features">

<v-clicks>

- **Predictable upgrades:** LTS and binary compatibility mean patches and new versions don't turn into rewrites
- **Code that ages well:** services written years ago still build on today's Scala&nbsp;3, and types keep refactors safe
- **A mature runtime:** the JVM is battle-tested, observable, and well understood by ops

</v-clicks>

  </div>
</div>

---
layout: section
number: '02'
---

# Scalability

---
eyebrow: Scalability
---

# Scale up

<div class="flow">

<Card title="Script" compact>

Start with a single-file script...

```bash
scala-cli next-track.sc
```

</Card>

<BlockArrow v-click />

<Card title="CLI app" compact v-after>

...grow it to a command-line app...

```bash
scala-cli package .

./sequencer next-track
```

</Card>

<BlockArrow v-click />

<Card title="Service" compact dark v-after>

...and evolve it all the way to production.

```http
POST /internal/v1/sequencer/next-track
```

</Card>

</div>

<!--
Same language, same libraries, all the way up.
-->

---
eyebrow: Scalability
---

# Scale down

<p class="lead">In serverless, time is money:</p>

<div class="grow figure">
  <img src="/lambda-power-tuning.png" alt="AWS Lambda Power Tuning results: execution time and cost by memory size" />
</div>

<p v-click class="muted">Potential solution: <strong>Scala Native</strong>?</p>

<div v-after class="puzzle">
  <div class="piece native"><img src="/logos/scala-native.png" alt="" />Scala Native</div>
  <span class="op">+</span>
  <div class="piece alt"><img class="mono" src="/logos/aws.svg" alt="" />Custom runtime</div>
  <span class="op">or</span>
  <div class="piece alt"><img class="mono" src="/logos/aws.svg" alt="" />AWS Lambda Web Adapter</div>
</div>


<!--
Memory/time-based pricing in serverless - Scala Native lambdas!

We've been experimenting with Scala Native lambdas and seeing promising results - and there's constant development in this area.

Feral doesn't support Scala Native, so for native we'd go with a Lambda custom runtime (provided.al2023): the binary talks to the Lambda Runtime API directly.

LWA doesn't need Docker: it works with a zip package too, but you still have to attach the adapter layer to the Lambda.

Outline TODO: Lambda Web Functions (Node.js).
-->

---
eyebrow: Scalability · Scala.js
hide: true
---

# Scale... aside?

You can also build frontend applications with Scala.js. We haven't seen much of it at SXM - my best guess why:

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

The honeycomb is a stand-in for the Typelevel picture (outline TODO) - adjust the library list to what you actually use.
-->

---
layout: section
number: '03'
---

# Tooling

---
eyebrow: Tooling
---

# We have a great compiler!

<div class="tiles">
  <div class="tile">Catches problems early: type mismatches, exhaustiveness, unsafe initialization</div>
  <div class="tile">(increasingly) helpful error messages</div>
  <div class="tile">Fast incremental builds, and getting faster</div>
  <div class="tile">Local optimizations, inlining, unboxing, TCO</div>
</div>

<p v-click class="muted">bonus: prototyping? disable fatal warnings and use <code>???</code> freely!</p>

<p v-click class="punch">We take it for granted, but the compiler has your back.</p>

<!--
Initialization checks: Scala 3's safe-init checker (`-Wsafe-init`) catches things like reading a `val` before it's initialized.

Compiler optimizations: opaque types are erased to the underlying type, `inline` is guaranteed compile-time inlining, and self-recursive tail calls become loops (@tailrec makes the compiler check it).
-->

---
eyebrow: Tooling
---

# ...but there's more

<div class="cards grid2">
  <Card title="Scalafmt + Scalafix">
    <ul class="points">
      <li>Consistent formatting - no more "please move this to another line"</li>
      <li>Rules can be shared within a team and applied automatically</li>
      <li>Lints, rewrites, best practices</li>
    </ul>
  </Card>
  <Card title="Scala Steward" v-click>
    <ul class="points">
      <li>Prevents versions becoming stale</li>
      <li>Runs Scalafix migrations if needed</li>
      <li>Flexible in configuration (e.g. cooldowns)</li>
      <li>Actively helps in supply&nbsp;chain&nbsp;management</li>
    </ul>
  </Card>
  <Card title="MiMa" v-click>
    <ul class="points">
      <li>Binary compatibility checks for the libraries we publish</li>
      <li>Another layer of protection against issues reaching
      production</li>
    </ul>
  </Card>
  <Card title="Scala CLI" v-click>
    <ul class="points">
      <li>Scripts, REPL, tests and packaging in one command</li>
      <li>Perfect for prototypes and small tools / microlibraries</li>
    </ul>
  </Card>
</div>

<!--
Scala Steward bumps a dependency, the library ships Scalafix migration rules, and the upgrade PR rewrites our code too.
-->
---
eyebrow: Tooling
---

# IDEs: two great options

<div class="cards">
  <Card title="Metals" dark class="big-title"><p>Fast, accurate diagnostics, completions and more - straight from the compiler.</p>
  <p>LSP-based (bring your own editor)</p></Card>
  <Card title="IntelliJ IDEA" dark class="big-title"><p>Powerful refactors, Scala-aware debugger, profiling, and more.</p></Card>
</div>

<div class="takeaway">
  <p class="punch">Sometimes we forget how good we have it!</p>
  <p class="muted">There's an option for everyone.</p>
</div>

---
layout: accent
eyebrow: Bonus · It's 2026
---

## How did the age of ✨agentic AI✨ change our approach to Scala?

<v-click>

# Not much.

</v-click>

---
eyebrow: Bonus · AI
---

# Everything is mostly the same!

AI might actually *amplify* the benefits of the language.

<div class="compare">
  <div></div>
  <p class="col-head">For developers</p>
  <div></div>
  <p v-click class="col-head hot">For agents</p>

  <h3>Expressiveness</h3>
  <div class="cell">Information-dense code, little boilerplate to write</div>
  <div v-after class="arrow-cell"><BlockArrow :width="48" /></div>
  <div v-after class="cell dark">Fewer tokens to read, write and keep in context</div>

  <h3>Types</h3>
  <div class="cell">Type-driven development, fearless refactoring</div>
  <div v-click class="arrow-cell"><BlockArrow :width="48" /></div>
  <div v-after class="cell dark">Semantic analysis and sanity checks that catch hallucinations</div>

  <h3>Tooling</h3>
  <div class="cell">Compiler &amp; friends, Metals, IntelliJ</div>
  <div v-click class="arrow-cell"><BlockArrow :width="48" /></div>
  <div v-after class="cell dark">Same tools via MCP, and more: Cellar, Scalex skills</div>

  <h3>Scaling up</h3>
  <div class="cell">Script → CLI app → production service</div>
  <div v-click class="arrow-cell"><BlockArrow :width="48" /></div>
  <div v-after class="cell dark">Vibe-coded MVP → production, with types as guardrails</div>
</div>

<!--
If anything, AI amplifies the benefits of the language.

What was useful for developers is equally (if not more) useful to AI.

- An expressive language means code is more information-dense: fewer tokens to read/write and put into context. If large swathes of boilerplate don't need to be written, they don't need to be read. You still read and review your code, right?
- Types: type-driven development, semantic analysis, sanity checks/regression tests (preventing hallucination).
- Most tools are still applicable (even IntelliJ has an MCP).
- Tools save time and cycles jumping around the codebase: Metals MCP, Cellar, Scalex skills... (TODO pic: Bamboozling... Fidgeting...)
- A vibe-coded MVP can be scaled to a production system. Scala isn't immune to slop, but agents can be gently steered towards the right patterns, and types can serve as a guardrail.
-->

---
eyebrow: Bonus · AI
hide: true
---

# It goes both ways

<div class="cards">
  <Card><p class="lead">New AI-friendly tools are useful to developers too - directly or programmatically (e.g. <strong>Cellar</strong>).</p></Card>
  <Card><p class="lead">Tools like <strong>Metals</strong> get better as bugs become cheaper to fix.</p></Card>
</div>

<p v-click class="punch xl">It's a win-win situation.</p>

---
layout: statement
---

Some would say the language doesn't matter anymore, and ask “why Scala?”

<v-clicks>

# Why would you use anything else?

If the language doesn't matter, nothing is stopping you from using Scala.

</v-clicks>

<!--
For us, it's now even more of a “why would you use anything else?”
-->
---
layout: end
---

# Scala&nbsp;(3) isn't leaving SXM anytime soon

The initial decision to use Scala&nbsp;3 is paying off every day.

<!--
Even now, when the language supposedly doesn't matter...

we continue to see benefits on every new feature we launch

but also, most importantly, on every alarm that **doesn't** ring!
 -->

---
layout: end
class: thanks
---

# Thank you!

<img class="thanks-dog" src="/logos/siriusxm-dog-white.png" alt="" />
