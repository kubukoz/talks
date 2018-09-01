# typelevel alchemist

## Agenda
- typeclasses recap
- functional purity, referential transparency
- how RT helps write modular, composable code
- Tagless Final
- cats intro (first some TCs like Order, Monoid then Functor and HKTs up to Monad and Traverse)
- cats-effect
- IOApp
- fs2 - streams, concurrency utils
- http4s
- doobie
- how it all connects in an application (dependency injection, concurrency)
- testing



## Agenda (long, for me - make slides from this)
- typeclasses recap (what is a typeclass: trait with implementations for types, ad-hoc polymorphism), typeclass coherency!
- higher kinded types - type-level functions. nullary types, unary types (List, Option, Future, Ordering), binary types (Either) etc.
  - partial unification - 2.11.x plugin, 2.12.x flag, 2.13 default
- referential transparency:
  - definition of a pure function (John de goes's tweet for sure)
  - definition of rt as in a ====== f(x)
  - "effects are good, side effects are bugs" https://twitter.com/hseeberger/status/942774034068049920 (extract screenshot from video)
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
    - as soon as we return IO we get pretty much infinite possibilities: IO[Unit] can be pretty much any action
- tagless final
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
  - data types (IO, SyncIO, Fiber, Ref, Deferred, Semaphore, Clock (show getting time), ContextShift, Timer (shifting, sleeping) - needed for some instances (e.g. Concurrent - true?), for IO needs implicit EC in scope) - make sure to say these are not type classes so there's no coherency requirement
  - Ref as functional mutable state
  - Resource - functional loan pattern
  - type classes (Sync, Bracket, Async, Concurrent, Effect)
  - IOApp - pure alternative to main function
- fs2
  - Stream[F[_], A] - effectful stream of A values suspended in F
  - building streams
  - operating on streams
  - actually running a stream
  - aren't akka streams lazy too? (sometimes yes but based on futures, sometimes can be non-reusable when coupled with resources)
  - pitfalls: Stream *> Stream, `recover` on stream level instead of effect level
  - Queue, Signal, Topic, getting streams from them, publishing to topic
  - Pipe, Sink as type aliases
- http4s  
  - HttpService type, short intro to Kleisli
  - creating services - default DSL, mention rho as alternative for typed self-documenting routes 
  - 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 ENDING
 - resources for learning cats, CE, circe, fs2, doobie, http4s (readme, website, gitter)