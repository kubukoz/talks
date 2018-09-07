val program = for {
  fiber <- launchNukes.start
  _     <- IO("oh damn, I didn't mean to do this")
  _     <- fiber.cancel
  _     <- IO("hopefully it worked")
} yield ()
