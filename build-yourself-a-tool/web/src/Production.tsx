import { useEffect, useState } from 'react'
import { talkApi } from './api'
import type { PlaylistId, Talk } from './generated'
import { TalkRow } from './TalkRow'

export const Production = () => {
  const [playlistId, setPlaylistId] = useState<PlaylistId>()
  const [playlist, setPlaylist] = useState<Talk[]>([])
  const [loading, setLoading] = useState(true)
  const [exhausted, setExhausted] = useState(false)

  useEffect(() => {
    let active = true
    talkApi.createPlaylist({}).then(({ playlistId, talks }) => {
      if (active) {
        setPlaylistId(playlistId)
        setPlaylist(talks)
        setLoading(false)
      }
    })
    return () => {
      active = false
    }
  }, [])

  const more = async (playlistId: PlaylistId) => {
    setLoading(true)
    try {
      const { talk } = await talkApi.extendPlaylist({ playlistId })
      if (talk) setPlaylist((current) => [...current, talk])
      setExhausted(!talk)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="production">
      <h1>Your playlist</h1>
      <ol className="talks">
        {playlist.map((talk, i) => (
          <li key={talk.id}>
            <TalkRow talk={talk} position={i + 1} />
          </li>
        ))}
      </ol>
      {exhausted ? (
        <p className="meta">That's every talk we have.</p>
      ) : (
        <button onClick={() => playlistId && more(playlistId)} disabled={loading || !playlistId}>
          {loading ? 'Loading...' : 'One more'}
        </button>
      )}
    </div>
  )
}
