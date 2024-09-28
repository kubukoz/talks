enum MIDI {
  case NoteOn(channelId: Int, noteId: Int, velocity: Int)
  case NoteOff(channelId: Int, noteId: Int, velocity: Int)

  def toArray: IArray[Int] =
    this match {
      case NoteOn(channelId, noteId, velocity)  => IArray(0x90 + channelId, noteId, velocity)
      case NoteOff(channelId, noteId, velocity) => IArray(0x80 + channelId, noteId, velocity)
    }

}
