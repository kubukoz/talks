# Incremental purity - transitioning systems to functional programming principles

In the light of the hard work performed by countless people in the open-source Scala community, including creators and contributors of cats-effect, fs2, scalaz-zio and alike, there's been an influx of the language's users who want to take part in the journey to purely functional programming in Scala. Sometimes, though, it might not be as straightforward to apply the newly gained knowledge as we wish, especially in existing, mission-critical production systems. In this talk, I want to exhibit the typical obstacles one can encounter when trying to make their application code pure, ways to overcome these obstacles, as well as some mistakes and "gotchas" frequently confronted.

## Agenda
1. Why FP
2. Commonly found impure patterns (overview)
3. Reducing impurity
4. Benefit summary

## Why FP

- programs become values
- effects become explicit - a whole world of abstractions opens to us
- equational reasoning (refactoring - inline/extract)
- local reasoning - understand pieces and their composition
- explicit ordering and concurrency

## Commonly found impure patterns (overview)
- local state (strange loops, incremental filtering)
- throwing exceptions
- raw actors API (mutable state, concurrency control aka locks/semaphores, asynchronous message processing, distributed computing, akka persistence aka event sourcing)
- Futures (akka-http routes, akka streams, Play controllers, Slick database calls)
- blocking, inherently side effecting IO on the caller thread (anorm, squeryl)
- reading/writing to files
- streaming data
- Java APIs (thousands of libraries for the JVM)
- logging, metrics

## Reducing impurity







## Links

- https://vimeo.com/294736344
- https://www.youtube.com/watch?v=x3GLwl1FxcA
- https://twitter.com/jdegoes/status/936301872066977792
- https://twitter.com/impurepics/status/983407934574153728
- https://github.com/pauljamescleary/scala-pet-store
- https://www.youtube.com/watch?v=oFk8-a1FSP0
- https://www.youtube.com/watch?v=sxudIMiOo68
- https://www.youtube.com/watch?v=GZPup5Iuaqw
- https://www.youtube.com/watch?v=EL3xy9DKhno
- https://www.youtube.com/watch?v=X-cEGEJMx_4
- https://www.youtube.com/watch?v=0jIaeXMaH2c
- https://www.youtube.com/watch?v=po3wmq4S15A
- https://www.youtube.com/watch?v=7xSfLPD6tiQ
- https://www.youtube.com/watch?v=5S03zTekRJc
- https://typelevel.org/blog/2018/10/06/intro-to-mtl.html
- https://typelevel.org/blog/2018/08/25/http4s-error-handling-mtl.html
- https://typelevel.org/blog/2018/08/07/refactoring-monads.html
- https://typelevel.org/blog/2018/06/07/shared-state-in-fp.html
- https://www.reddit.com/r/scala/comments/8ygjcq/can_someone_explain_to_me_the_benefits_of_io/e2jfp9b/
- https://github.com/gvolpe/typelevel-stack.g8
- https://github.com/gvolpe/http4s-good-practices
- https://github.com/gvolpe/advanced-http4s
- https://github.com/kubukoz/brick-store
- https://github.com/kubukoz/classy-playground
- https://github.com/ChristopherDavenport/log4cats
- https://github.com/ChristopherDavenport/linebacker
- https://github.com/ChristopherDavenport/cats-par
- https://github.com/ChristopherDavenport/log4cats-writer-example
- https://github.com/SystemFw/upperbound