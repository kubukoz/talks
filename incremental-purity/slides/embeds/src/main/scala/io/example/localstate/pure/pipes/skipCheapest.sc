def skipCheapest(allCount: Int): Pipe[Flight] =
  if (isMacbookUser(user)) _.drop(allCount / 2)
  else identity
