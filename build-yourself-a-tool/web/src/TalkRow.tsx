import type { Talk } from './generated'

export const formatDuration = (seconds: number) =>
  `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`

export const TalkRow = ({ talk, position }: { talk: Talk; position: number }) => (
  <div className="talk">
    <span className="index">{position}</span>
    <a href={talk.url} target="_blank" rel="noreferrer">
      <img src={talk.thumbnailUrl} alt="" />
    </a>
    <div>
      <a className="title" href={talk.url} target="_blank" rel="noreferrer">
        {talk.title}
      </a>
      <div className="meta">
        {talk.speakers.map((speaker) => speaker.name).join(', ')} · {talk.conference} ·{' '}
        {formatDuration(talk.durationSeconds)}
      </div>
      <div className="tags">
        {talk.tags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
    </div>
  </div>
)
