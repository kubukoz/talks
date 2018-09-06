val init = IO(getFile("~/path-to-file"))

def consume(file: File): IO[String] = ???

def close(file: File): IO[Unit] = ???

val safeConsume =
  init.bracketCase(consume)((file, _) => close(file))
