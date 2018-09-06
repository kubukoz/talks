trait ValidationService {

  //don't even try to make sense of it
  def determineCorrectName(ad: Ad): String = {

    val originalName   = ad.name
    val replacedDashes = originalName.replaceAll("\\-", "_")

    val shouldBeTruncated = originalName.length >= limit

    val afterTruncation = if (shouldBeTruncated) {
      val ellipsis = "..."
      val limit    = nameLengthLimit(ad)

      val overLimit    = originalName.length - limit
      val fitsEllipsis = overLimit >= ellipsis.length

      if (fitsEllipsis) replacedDashes.take(limit) ++ ellipsis
      else replacedDashes.take(limit) ++ ellipsis.take(overLimit)
    }

    val (h, t) =
      if (afterTruncation.isEmpty) ('_', "")
      else (afterTruncation.head, afterTruncation.tail)

    h.toUpperCase + t
  }
}
