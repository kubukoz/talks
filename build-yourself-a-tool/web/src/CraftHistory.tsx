import { useEffect, useRef, useState } from 'react'
import type { Talk, TalkId } from './generated'
import { formatDuration } from './TalkRow'

type DragSource = { from: 'pool'; id: TalkId } | { from: 'sequence'; index: number }

const matches = (talk: Talk, query: string) => {
  const needle = query.trim().toLowerCase()
  return (
    needle === '' ||
    [talk.title, talk.conference, ...talk.speakers.map((speaker) => speaker.name), ...talk.tags].some((text) =>
      text.toLowerCase().includes(needle),
    )
  )
}

const insert = <A,>(values: A[], index: number, value: A) => [...values.slice(0, index), value, ...values.slice(index)]

const TalkSummary = ({ talk }: { talk: Talk }) => (
  <>
    <img src={talk.thumbnailUrl} alt="" />
    <div>
      <div className="name">{talk.title}</div>
      <div className="meta">
        {talk.speakers.map((speaker) => speaker.name).join(', ')} · {talk.conference} · {formatDuration(talk.durationSeconds)}
      </div>
    </div>
  </>
)

export const CraftHistory = ({
  talks,
  history,
  onSave,
  onClose,
}: {
  talks: Talk[]
  history: TalkId[]
  onSave: (history: TalkId[]) => void
  onClose: () => void
}) => {
  const dialog = useRef<HTMLDialogElement>(null)
  const [draft, setDraft] = useState(history)
  const [query, setQuery] = useState('')
  const [dragged, setDragged] = useState<DragSource>()
  const [dropIndex, setDropIndex] = useState<number>()

  useEffect(() => {
    if (!dialog.current?.open) dialog.current?.showModal()
  }, [])

  const byId = new Map(talks.map((talk) => [talk.id, talk]))
  const pool = talks.filter((talk) => matches(talk, query))

  const startDrag = (source: DragSource) => (e: React.DragEvent) => {
    e.dataTransfer.setData('text/plain', '')
    e.dataTransfer.effectAllowed = 'copyMove'
    setDragged(source)
  }

  const endDrag = () => {
    setDragged(undefined)
    setDropIndex(undefined)
  }

  const dropAt = (index: number) => {
    if (dragged?.from === 'pool') setDraft(insert(draft, index, dragged.id))
    if (dragged?.from === 'sequence') {
      const without = draft.filter((_, i) => i !== dragged.index)
      setDraft(insert(without, dragged.index < index ? index - 1 : index, draft[dragged.index]))
    }
    endDrag()
  }

  const overItem = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.preventDefault()
    e.stopPropagation()
    const { top, height } = e.currentTarget.getBoundingClientRect()
    setDropIndex(e.clientY < top + height / 2 ? index : index + 1)
  }

  const save = () => {
    onSave(draft)
    dialog.current?.close()
  }

  return (
    <dialog ref={dialog} className="craft" onClose={onClose}>
      <header>
        <h2>Craft history</h2>
        <span className="hint">Drag talks into the sequence. Drag them back out to remove.</span>
      </header>
      <section
        className="pool"
        onDragOver={(e) => dragged?.from === 'sequence' && e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault()
          if (dragged?.from === 'sequence') setDraft(draft.filter((_, i) => i !== dragged.index))
          endDrag()
        }}
      >
        <input type="search" placeholder="Search title, speaker, tag, conference" value={query} onChange={(e) => setQuery(e.target.value)} />
        <ul>
          {pool.map((talk) => (
            <li key={talk.id} draggable onDragStart={startDrag({ from: 'pool', id: talk.id })} onDragEnd={endDrag}>
              <TalkSummary talk={talk} />
              <button className="icon" title="Append to sequence" onClick={() => setDraft([...draft, talk.id])}>
                +
              </button>
            </li>
          ))}
        </ul>
      </section>
      <section
        className={`sequence ${dropIndex === draft.length ? 'drop-end' : ''}`}
        onDragOver={(e) => {
          e.preventDefault()
          setDropIndex(draft.length)
        }}
        onDragLeave={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setDropIndex(undefined)
        }}
        onDrop={(e) => {
          e.preventDefault()
          dropAt(dropIndex ?? draft.length)
        }}
      >
        <ol>
          {draft.map((id, i) => {
            const talk = byId.get(id)
            return (
              <li
                key={i}
                draggable
                className={dropIndex === i ? 'drop-before' : ''}
                onDragStart={startDrag({ from: 'sequence', index: i })}
                onDragEnd={endDrag}
                onDragOver={overItem(i)}
              >
                <span className="index">{i + 1}</span>
                {talk ? <TalkSummary talk={talk} /> : <span className="error">Unknown talk {id}</span>}
                <button className="icon" title="Remove" onClick={() => setDraft(draft.filter((_, j) => j !== i))}>
                  ×
                </button>
              </li>
            )
          })}
        </ol>
        {draft.length === 0 && <p className="hint">Drop talks here.</p>}
      </section>
      <footer className="actions">
        <button className="secondary" onClick={() => setDraft([])} disabled={draft.length === 0}>
          Clear
        </button>
        <span className="spacer" />
        <button className="secondary" onClick={() => dialog.current?.close()}>
          Cancel
        </button>
        <button onClick={save}>Save</button>
      </footer>
    </dialog>
  )
}
