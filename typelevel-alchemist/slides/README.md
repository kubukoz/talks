# typelevel alchemist

## Agenda
- typeclasses recap
- higher kinded types
- referential transparency
- how RT helps write modular, composable, refactorable code
- Tagless Final
- cats intro
- cats-effect
- fs2
- http4s
- doobie
- patterns for applications

## Agenda (long, for me - make slides from this)
- typeclasses recap
  - what's a typeclass (trait with implementations for types, ad-hoc polymorphism)
  - examples
  - typeclass coherence
  - comparison to pattern matching on types, runtime reflection

- higher kinded types - type-level functions. nullary types, unary types (List, Option, Future, Ordering), binary types (Either) etc.
  - partial unification - 2.11.x plugin, 2.12.x flag, 2.13 default

  
- referential transparency:
  - definition of a pure function (John de goes's tweet for sure)
  - definition of rt as in a ====== f(x)
  - "effects are good, side effects are bugs" https://twitter.com/hseeberger/status/942774034068049920 (extract screenshot from video)
(in above point)
- how RT helps write modular, composable, refactorable code
  - you can inline or extract any expression you want and keep the same semantics/behavior
  - you can treat programs as values and pass them around to functions which can manipulate your programs like data
  - because of that, you can build abstractions like `forever`, `retry`, `loopWhile` for any effect that matches a constraint (i.e. has the expected typeclass instance)
  - show Ref here? just so that they can see functional mutable state
  - there's no implicit state passing, mutable state needs to be passed around as a function argument (e.g. Ref). State is explicit and testing becomes actually pleasant
  - local reasoning - build large programs from small programs, treat them as independent unless proven otherwise (through function calls that you can find and inspect with the help of any good IDE)
  - effects are explicit - no more Unit return type, unless you just return `()`
  - pure functions limit the amount of implementations
    - a => a - 1
    - (a, a) => a - 2
    - Monoid a => (a, a) => a - 4
    - (a, b) => b
    - (Boolean, Boolean, Boolean) => Unit
    - Any => Unit
    - as soon as we return IO we get pretty much infinite possibilities: IO[Unit] can be pretty much any action. (Show comparison to impure function returning String - again infinite possibilities)
- Tagless Final
  - type lambdas (comparison to value-level function lambdas), kind projector, natural transformations (plus kind projector syntax)
  - with typeclasses: show some simple repository algebra
  - with normal code: show some service algebra using other algebras
  - benefit: abstraction over the effect type, which limits our capabilities to what the types/implicits give us, but enables interchangeability if we have another type with a lawful instance
- cats intro
  - what it is - core library for all things typelevel (FP)
  - contains typeclasses, data types, extra syntax for stdlib
  - a slide per datatype for some basic datatypes we use like NEL, Validated, OptionT, EitherT (need to explain monad transformers)
  - a slide per typeclass of Eq, Order, Monoid, Functor, Applicative, Monad, Traverse, ApplicativeError, MonadError
  - some more slides about Validated, ValidatedNel, error composition, compare applicative source code with Either (also show some example)
  - explanation of Parallel, cats-par
  - where to look for syntax, instances, how to import (reference tweet from impurepics about imports in cats)
- cats-effect
  - IO monad for Scala, Cats. Suspended effects and evaluation, stack safety, sync/async, pure/delay/suspend
  - type classes (Sync, Bracket, Async, Concurrent, Effect)
  - data types (IO, SyncIO, Fiber, Ref, Deferred, Semaphore, Clock (show getting time), ContextShift, Timer (shifting, sleeping) - needed for some instances (e.g. Concurrent - true?), for IO needs implicit EC in scope) - make sure to say these are not type classes so there's no coherence requirement
  - Ref as functional mutable state
  - Resource - functional loan pattern
  - IOApp - pure alternative to main function
  - testing pure applications - series of flatmaps and unsaferunsync at the end. Mention there's no pure testing library but also point out testz is in development
  - mocking - encapsulating state in Refs, 
- fs2
  - Stream[F[_], A] - effectful stream of A values suspended in F
  - building streams
  - operating on streams
  - actually running a stream
  - aren't akka streams lazy too? (sometimes yes but based on futures, sometimes can be non-reusable when coupled with resources)
  - pitfalls: Stream *> Stream, `recover` on stream level instead of effect level
  - Queue, Signal, Topic, getting streams from them, publishing to topic
  - Pipe, Sink as type aliases
  - testing streams
- http4s  
  - HttpService type, short intro to Kleisli
  - creating services - builtin DSL, mention rho as alternative for typed self-documenting routes 
  - building and binding a server with blaze
  - why are request/response parameterized with the effect - streaming nature of HTTP request/response
  - reading body as json, writing body as json - http4s-circe
  - stream input/output in endpoints, client API, testing HttpServices
- doobie
  - connecting to a database: creating a transactor.
  - querying data from the database: doobie DSL + type-safe SQL interpolation
  - updating rows in a database, updateMany
  - custom column type mappings
  - typechecking queries
  - query streaming, update "streaming" (actually, batching)
  - "It's still JDBC". Blocking threads. Thoughtful blocking. ContextShift and Linebacker.
  - testing queries with a real database
- patterns for applications
  - dependency injection: argument passing, modules, macwire.
  - Value-level injection with Kleisli. Authenticated[F[_], T] and Configured[T].
- Resources for learning cats, CE, circe, fs2, doobie, http4s (readme, website, gitter) + videos

everything by Luka, Fabio, Ross, Rob