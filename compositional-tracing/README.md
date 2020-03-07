# Keep your sanity with compositional tracing

Logging is not enough for building distributed systems that you can investigate in case of production issues (which will happen). As an alternative, we can use tracing.

We’ll learn why logging is not the adequate tool for finding issues in distributed systems and how tracing solves its problems.

Later, we’ll see how it can be implemented, starting from the simplest possible solution, progressing through a clean, compositional solution that doesn’t clutter the API with details of tracing, and works with HTTP requests as well as asynchronous message handling.

## Plan

- logging: why we use it, what problems it solves
  - MDC: adding context to logs
  - the problems with logging: what happens if you need to cross thread boundaries? Machine boundaries?
  - patching the problem: preserving MDC context across thread boundaries with a custom executor
  - patching the problem more: storing MDC context in request/message headers

- tracing: a better alternative to logging in distributed systems
  - approaches to tracing: manual - passing lots of stuff in parameters
  - approaches to tracing: instrumentation - wrap your executors and all integration points automatically with Kamon
  - approaches to tracing: ReaderT - put context in a reader monad with natchez

- implementation hackery
  - lifting thread-based constructs to fiber-based constructs: wrapping executors and async boundaries with ones that keep the context
  - escape hatch: preserve context around uncontrolled async boundaries
  - escape hatch: temporarily set MDC from reader monad - thread safety
  - middleware for http4s client, log4cats
  - attempt to make Resource work
