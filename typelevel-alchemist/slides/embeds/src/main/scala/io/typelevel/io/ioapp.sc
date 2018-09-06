trait IOApp {

  /**
    * Produces the `IO` to be run as an app.
    *
    * @return the [[cats.effect.ExitCode]] the JVM exits with
    */
  def run(args: List[String]): IO[ExitCode]
}
