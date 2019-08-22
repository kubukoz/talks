# A sky full of streams

### Embracing compositionality of functional streams

Stream processing may sound intimidating, and often unnecessary. There's a reason - many of us rarely (or never) have an actual need to do stream processing, because the size of their data is never large enough not to fit in memory. But is that the only reason to use stream processing?

As it turns out, most of us stream data on a regular basis, just without being fully aware of it. What is more, having realized that, we can benefit from streaming in all kinds of situations, by decomposing a larger problem into smaller pieces we can reuse and reason about independently.

In this talk, I'll briefly introduce to you fs2 - the functional streaming library for Scala, and its many usecases. You'll see what problems you can solve with it, as well as rough outlines of potential solutions.

We'll also learn a bit about what compositionality means and what makes fs2 a truly compositional library.
