# Introduction to interruption

Modern functional effect systems give us great power, but with that power comes interruptibility.
It's important to know if and when your effects can be interrupted, and how to handle that interruption well.

In this talk, I'll provide a concise introduction to the way interruption works in functional IO, as well as some best practices for making our code interruption-safe.

---

- Functional effects recap (2min)
  - side effect, referential transparency
  - IO
- Story: real life interruption (3min)
  - sitting down to work
  - opening IDE, remembering what I was supposed to work on, opening relevant files, etc.
  - working for 5 minutes
  - slack message
  - 2 more minutes pass
  - said person comes up and asks a question              <- interruption
  - I free my mind of resources relevant to previous task <- finalizer
  - I start thinking about and solving person's problem
  - ...
- Fiber model (3min)
  - .start, .cancel/interrupt
- Cause interruption (5min)
- React to interruption (5min)
- Differences: CE, ZIO, Monix, CE3 (3min)
  - names ;)
  - rich operator set (zio, monix)
  - cancelable/uncancelable regions
  - cancelable blocking
  - recovering from interruption
  - supervision
- Protips: (3min)
  - Avoid concurrency like the plague it is! https://github.com/alexandru/scala-best-practices/blob/master/sections/4-concurrency-parallelism.md
  - Avoid `.start` / `.fork` if possible
  - Use high-level abstractions: 
    - `race`, `par`, `Deferred` / `Promise` for simple cases (todo: provide 1-2 concrete examples)
    - `bracket`, `Resource`, etc. to make sure your cleanup runs
    - Stream, Queue, Topic for more complex cases
    - `background` (coming to cats-effect soon)
  - Learn how fibers work https://www.youtube.com/watch?v=x5_MmZVLiSM
  - Build small, composable abstractions that hide the concurrency from core domain/business logic
    - If using Cats Effect, rely on laws and don't make other assumptions
    - Acknowledge differences between various IO monads
    - Aim for the most generic concurrency features possible
    - Only use effect-specific operations in separated, purely technical areas
  - Test all the edge cases you can think of
