# Irresistible party tricks with cats-tagless

Are you writing effect-polymorphic code? Want to ease some of the pains involved? Say no more: this is the talk for you.
Meet cats-tagless: a library full of goodness that help `F[_]` fans achieve goals faster and with less code.

We will go through the most interesting type classes in cats-tagless, as well as ways to derive their instances for your algebras.
We’ll also see some highly practical patterns of using it in a real codebase.
At the end, if you’ve been writing effect-polymorphic code in Scala without the library, you won’t be able to resist the temptation to use it in every upcoming project!


## Plan

- some definitions
  - cats-tagless: a library for working with effect polymorphic code
  - ~> (how it differs from normal functions)
- party tricks
  - ✅ Apply a known effect before, after or around every method in the trait (FunctorK)
  - ✅ Catch errors - mapK to EitherT
  - ✅ Change effects - apply transaction, send messages, etc.
  - ✅ Fallback to different implementation - ApplyK with `orElse`
  - ✅ Race two implementations - ApplyK with `race andThen (_.map(_.merge))`
  - ✅ Run N implementations (potentially mixing effects) and keep results - ApplyK / @autoProductNK 
  - ✅ Add time of execution
  - ✅ Lift effect into stream, repeating actions in exponentially growing spacing
  - ✅ Add retries / circuit breaker / semaphore / rate limiter
  - ✅ Create span for distributed tracing based on method name
  - Customize a bunch of algebras at once - only works if you only need FunctorK though :(
  - (possibly - see my latest issue) cache key calculation, logging failing values, adding tags to tracing spans
