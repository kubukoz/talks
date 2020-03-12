# Keep your sanity with compositional tracing

Logging is not enough for building distributed systems that you can investigate in case of production issues (which will happen). As an alternative, we can use tracing.

We’ll learn why logging is not the adequate tool for finding issues in distributed systems and how tracing solves its problems.

Later, we’ll see how it can be implemented, starting from the simplest possible solution, progressing through a clean, compositional solution that doesn’t clutter the API with details of tracing, and works with HTTP requests as well as asynchronous message handling.
