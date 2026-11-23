import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { fetchTransport } from '@polyvariant/smithy-ts-runtime'
import { TalkServiceClient, type Talk } from './generated'
import './index.css'

const api = new TalkServiceClient(fetchTransport())

const TalkRow = ({ talk, index }: { talk: Talk; index: number }) => (
  <li className="talk">
    <span className="index">{index + 1}</span>
    <a href={talk.url} target="_blank" rel="noreferrer">
      <img src={talk.thumbnailUrl} alt="" />
    </a>
    <div>
      <a className="title" href={talk.url} target="_blank" rel="noreferrer">
        {talk.title}
      </a>
      <div className="meta">
        {talk.speakers.join(', ')} · {talk.conference}
      </div>
      <div className="tags">
        {talk.tags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
    </div>
  </li>
)

const App = () => {
  const [playlist, setPlaylist] = useState<Talk[]>([])
  const [loading, setLoading] = useState(true)
  const [exhausted, setExhausted] = useState(false)

  useEffect(() => {
    let active = true
    api.extendPlaylist({ playlist: [], count: 5 }).then(({ talks }) => {
      if (active) {
        setPlaylist(talks)
        setLoading(false)
      }
    })
    return () => {
      active = false
    }
  }, [])

  const more = async () => {
    setLoading(true)
    try {
      const { talks } = await api.extendPlaylist({ playlist: playlist.map((t) => t.id), count: 1 })
      setPlaylist((current) => [...current, ...talks])
      setExhausted(talks.length === 0)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <h1>Your playlist</h1>
      <ol>
        {playlist.map((talk, i) => (
          <TalkRow key={talk.id} talk={talk} index={i} />
        ))}
      </ol>
      {exhausted ? (
        <p className="meta">That's every talk we have.</p>
      ) : (
        <button onClick={more} disabled={loading}>
          {loading ? 'Loading...' : 'One more'}
        </button>
      )}
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
