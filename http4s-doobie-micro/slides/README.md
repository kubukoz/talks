# Lightweight, functional microservices with http4s and doobie

It’s no secret that using full-blown frameworks that provide every functionality you could ask for
tends to be troublesome - some of them impose a certain structure on your applications,
hide implementation details of the parts of your program generated on the fly (making them
harder to debug), and require testing your programs in a specific way.
Thankfully, there are alternatives, such as not depending on frameworks to do
all the work - instead, you delegate parts of the functionality to libraries.
In this talk, I’m going to talk about two of them - http4s (a HTTP server/client library)
and doobie (a functional JDBC wrapper), how they interact together thanks to a common core
(cats-effect, fs2), and how they allow you to write resource-safe applications that you can fully control,
and whose parts you can easily test independently or as a whole.

---

Have you ever worked with Spring? Do you remember adding annotations/beans and seeing things not work because you needed to configure 3 more things?

Have you ever tried to upgrade Play across 2.x versions?

If you have, you've seen the horrors of relying on frameworks to do everything in your application.

I propose other solutions.

- Dependency injection -> function/constructor parameters
- Autowiring -> implicits
- Beans -> Tagless Final algebras
- Runtime errors -> compile-time errors
- Lifecycle management through constructor bodies and shutdown hooks -> resource monad
- Scheduled jobs -> pure streams
- Bean registration -> implicits/`flatMap`

## What are microservices?

A set of architectural patterns for building distributed systems

### A "micro" service is

- small
- doing one thing well
- autonomous
- deployed in another (virtual?) machine

### Microservices are dangerous because
	
- lots of infrastructure needed (tracing,logging,provisioning,monitoring,metrics)
- the network isn't reliable! (distributed transactions, CAP theorem, resilience patterns)
- stack traces are useless (every bug is a murder mystery)
- generally challenging and complex

### Microservices can help with

- frequent, independent deployment
- resilience
- scalability
- ease of development (smaller codebases)

### http4s

- functional http interface in Scala
- multiple backends
- built on fs2 and cats-effect

### doobie

- functional JDBC layer
- built on fs2 and cats-effect

### Why http4s

- A server/client is a function: Request[F] => F[Response[F]]
- Middleware (tracing, logging, rate limiting, circuit breaker)
- servers/clients can be mocked without magic/reflection
- Resource safety: server/client, request/response bodies are streams
- Type safety: standard headers are well-typed

### Why doobie

- No magic - you just write SQL
- No need to manage transactions, connections/threads manually (thread safe transactions)
- works well with http4s because of the common core

