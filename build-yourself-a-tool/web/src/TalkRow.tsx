import type { Talk, TalkPredicate } from './generated'

export const formatDuration = (seconds: number) =>
  `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`

const hit = (active: boolean) => (active ? 'hit' : undefined)

export const TalkRow = ({
  talk,
  position,
  highlight,
}: {
  talk: Talk
  position: number
  highlight?: TalkPredicate
}) => (
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
        <span className={hit(highlight !== undefined && 'speakerCountry' in highlight)}>
          {talk.speakers.map((speaker) => speaker.name).join(', ')}
        </span>{' '}
        · <span className={hit(highlight !== undefined && 'conference' in highlight)}>{talk.conference}</span> ·{' '}
        <span className={hit(highlight !== undefined && 'maxDurationSeconds' in highlight)}>
          {formatDuration(talk.durationSeconds)}
        </span>
      </div>
      <div className="tags">
        {talk.tags.map((tag) => (
          <span key={tag} className={hit(highlight !== undefined && 'hasTag' in highlight && highlight.hasTag === tag)}>
            {tag}
          </span>
        ))}
      </div>
    </div>
  </div>
)
