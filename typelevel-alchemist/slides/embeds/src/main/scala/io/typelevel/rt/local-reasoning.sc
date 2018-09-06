def withCloned[T](repoName: String)(
  action: Repository => IO[T]): IO[T] = {

  val prepareRepo =
    cloneRepository *>
      prepareDirectory *>
      checkoutMaster *>
      log.info("prepared")

  val runInRepo = repoInfo.flatMap(action)

  val cleanup = removeDirectory

  (prepareRepo *> runInRepo).guarantee(cleanup)
}
