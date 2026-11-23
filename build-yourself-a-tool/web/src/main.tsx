import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { fetchTransport } from '@polyvariant/smithy-ts-runtime'
import { TalkServiceClient, type PlaylistId, type Talk } from './generated'
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
        {talk.speakers.map((speaker) => speaker.name).join(', ')} · {talk.conference}
      </div>
      <div className="tags">
        {talk.tags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
    </div>
  </li>
)

const Production = () => {
  const [playlistId, setPlaylistId] = useState<PlaylistId>()
  const [playlist, setPlaylist] = useState<Talk[]>([])
  const [loading, setLoading] = useState(true)
  const [exhausted, setExhausted] = useState(false)

  useEffect(() => {
    let active = true
    api.createPlaylist({}).then(({ playlistId, talks }) => {
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
      const { talk } = await api.extendPlaylist({ playlistId })
      if (talk) setPlaylist((current) => [...current, talk])
      setExhausted(!talk)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <h1>Your playlist</h1>
      <ol>
        {playlist.map((talk, i) => (
          <TalkRow key={talk.id} talk={talk} index={i} />
        ))}
      </ol>
      {exhausted ? (
        <p className="meta">That's every talk we have.</p>
      ) : (
        <button onClick={() => playlistId && more(playlistId)} disabled={loading || !playlistId}>
          {loading ? 'Loading...' : 'One more'}
        </button>
      )}
    </>
  )
}

const Preview = () => null

const tabs = { production: 'Production', preview: 'Preview' } as const

type Tab = keyof typeof tabs

const App = () => {
  const [tab, setTab] = useState<Tab>('production')

  return (
    <main>
      <nav className="tabs">
        {(Object.keys(tabs) as Tab[]).map((key) => (
          <button key={key} className={key === tab ? 'active' : ''} onClick={() => setTab(key)}>
            {tabs[key]}
          </button>
        ))}
      </nav>
      <section hidden={tab !== 'production'}>
        <Production />
      </section>
      <section hidden={tab !== 'preview'}>
        <Preview />
      </section>
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
