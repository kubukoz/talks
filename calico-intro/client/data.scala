object data {

  val C4 = Playable.C4
  val C3 = C4 - 12
  val C5 = C4 + 12
  val REST = Playable.Rest

  val plucks = List(
    C4,
    REST,
    REST,
    REST,
    //
    C4 + 3,
    REST,
    REST,
    C4 + 2,
    //
    REST,
    REST,
    C4,
    REST,
    //
    C4 + 3,
    REST,
    C4 + 2,
    REST,
  ).map(_.atVelocity(0x7f * 3 / 4))

  val melody = List(
    C5 + 7,
    REST,
    REST,
    REST,
    //
    C5 + 3,
    REST,
    C5 + 5,
    REST,
    //
    C5,
    REST,
    REST,
    REST,
    //
    REST,
    REST,
    REST,
    REST,
  ).map(_.atVelocity(0x7f))

  val bass = List(
    C3,
    REST,
    REST,
    REST,
    //
    C3,
    REST,
    REST,
    REST,
    //
    C3 + 7,
    REST,
    REST,
    REST,
    //
    C3 + 7,
    REST,
    REST,
    REST,
  ).map(_.atVelocity(0x7f))

  val initTracks: List[List[Playable]] = List(plucks, melody, bass)
}
