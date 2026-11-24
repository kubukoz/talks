import { useEffect, useState } from 'react'
import { z } from 'zod'
import { previewApi } from './api'
import { CraftHistory } from './CraftHistory'
import {
  RuleSchema,
  UnexpectedResponseError,
  type CountryCode,
  type GenerationStep,
  type Rule,
  type RuleEvaluation,
  type Talk,
  type TalkId,
  TalkIdSchema,
  type TalkPredicate,
} from './generated'
import { TalkRow } from './TalkRow'

const initialCount = 5

type PredicateKind = 'speakerCountry' | 'hasTag' | 'conference' | 'maxDurationSeconds'

const predicateKinds: Record<PredicateKind, { label: string; defaultValue: string }> = {
  speakerCountry: { label: 'Speaker country', defaultValue: 'PL' },
  hasTag: { label: 'Has tag', defaultValue: 'ai' },
  conference: { label: 'Conference', defaultValue: 'Scalar 2026' },
  maxDurationSeconds: { label: 'Max duration (min)', defaultValue: '30' },
}

const predicateKind = (predicate: TalkPredicate): PredicateKind => Object.keys(predicate)[0] as PredicateKind

const predicateValue = (predicate: TalkPredicate): string =>
  'maxDurationSeconds' in predicate ? String(predicate.maxDurationSeconds / 60) : String(Object.values(predicate)[0])

const makePredicate = (kind: PredicateKind, value: string): TalkPredicate => {
  switch (kind) {
    case 'speakerCountry':
      return { speakerCountry: value.toUpperCase() as CountryCode }
    case 'hasTag':
      return { hasTag: value }
    case 'conference':
      return { conference: value }
    case 'maxDurationSeconds':
      return { maxDurationSeconds: Math.round((Number(value) || 0) * 60) }
  }
}

const describePredicate = (predicate: TalkPredicate) => {
  if ('speakerCountry' in predicate) return `has a speaker from ${predicate.speakerCountry}`
  if ('hasTag' in predicate) return `is tagged "${predicate.hasTag}"`
  if ('conference' in predicate) return `is from ${predicate.conference}`
  return `is at most ${predicate.maxDurationSeconds / 60} min`
}

type RuleKind = 'noRepeats' | 'require' | 'spacing'

const ruleKinds: Record<RuleKind, string> = {
  noRepeats: 'No repeats',
  require: 'Require',
  spacing: 'Spacing',
}

const ruleKind = (rule: Rule): RuleKind => Object.keys(rule)[0] as RuleKind

const describeRule = (rule: Rule) => {
  const description =
    'noRepeats' in rule ? (
      'No repeats'
    ) : 'require' in rule ? (
      <>
        Every talk <span className="predicate">{describePredicate(rule.require.talk)}</span>
      </>
    ) : (
      <>
        Exactly {rule.spacing.n} apart: talk that <span className="predicate">{describePredicate(rule.spacing.talk)}</span>
      </>
    )
  return (
    <>
      {description}
      {isSkippable(rule) && ' (skippable)'}
    </>
  )
}

const isSkippable = (rule: Rule) =>
  'noRepeats' in rule ? rule.noRepeats.skippable : 'require' in rule ? rule.require.skippable : rule.spacing.skippable

const withSkippable = (rule: Rule, skippable: boolean): Rule =>
  'noRepeats' in rule
    ? { noRepeats: { ...rule.noRepeats, skippable } }
    : 'require' in rule
      ? { require: { ...rule.require, skippable } }
      : { spacing: { ...rule.spacing, skippable } }

const newRule = (kind: RuleKind, skippable: boolean): Rule => {
  switch (kind) {
    case 'noRepeats':
      return { noRepeats: { skippable } }
    case 'require':
      return { require: { skippable, talk: { hasTag: 'ai' } } }
    case 'spacing':
      return { spacing: { skippable, n: 3, talk: { speakerCountry: 'PL' as CountryCode } } }
  }
}

const positiveInt = (value: string) => Math.max(1, Number(value) || 1)

const PredicateEditor = ({
  predicate,
  onChange,
}: {
  predicate: TalkPredicate
  onChange: (predicate: TalkPredicate) => void
}) => (
  <div className="row predicate">
    <span className="label">Talk</span>
    <select
      value={predicateKind(predicate)}
      onChange={(e) => {
        const kind = e.target.value as PredicateKind
        onChange(makePredicate(kind, predicateKinds[kind].defaultValue))
      }}
    >
      {(Object.keys(predicateKinds) as PredicateKind[]).map((kind) => (
        <option key={kind} value={kind}>
          {predicateKinds[kind].label}
        </option>
      ))}
    </select>
    <input
      type={predicateKind(predicate) === 'maxDurationSeconds' ? 'number' : 'text'}
      value={predicateValue(predicate)}
      onChange={(e) => onChange(makePredicate(predicateKind(predicate), e.target.value))}
    />
  </div>
)

const RuleEditor = ({
  rule,
  onChange,
  onRemove,
  onMoveUp,
  onMoveDown,
  onHover,
  hovered,
}: {
  rule: Rule
  onChange: (rule: Rule) => void
  onRemove: () => void
  onMoveUp?: () => void
  onMoveDown?: () => void
  onHover: (rule: Rule | undefined) => void
  hovered: boolean
}) => (
  <li className={`rule ${hovered ? 'hovered' : ''}`} onMouseEnter={() => onHover(rule)} onMouseLeave={() => onHover(undefined)}>
    <div className="row">
      <select value={ruleKind(rule)} onChange={(e) => onChange(newRule(e.target.value as RuleKind, isSkippable(rule)))}>
        {(Object.keys(ruleKinds) as RuleKind[]).map((kind) => (
          <option key={kind} value={kind}>
            {ruleKinds[kind]}
          </option>
        ))}
      </select>
      <label className="check">
        <input type="checkbox" checked={isSkippable(rule)} onChange={(e) => onChange(withSkippable(rule, e.target.checked))} />
        skippable
      </label>
      <span className="spacer" />
      <button className="icon" onClick={onMoveUp} disabled={!onMoveUp} aria-label="Move up">
        ↑
      </button>
      <button className="icon" onClick={onMoveDown} disabled={!onMoveDown} aria-label="Move down">
        ↓
      </button>
      <button className="icon" onClick={onRemove} aria-label="Remove">
        ✕
      </button>
    </div>
    {'require' in rule && (
      <>
        <PredicateEditor predicate={rule.require.talk} onChange={(talk) => onChange({ require: { ...rule.require, talk } })} />
      </>
    )}
    {'spacing' in rule && (
      <>
        <div className="row">
          <span className="label">Gap</span>
          <input
            type="number"
            min={1}
            value={rule.spacing.n}
            onChange={(e) => onChange({ spacing: { ...rule.spacing, n: positiveInt(e.target.value) } })}
          />
          <span className="hint">matches exactly this far apart</span>
        </div>
        <PredicateEditor predicate={rule.spacing.talk} onChange={(talk) => onChange({ spacing: { ...rule.spacing, talk } })} />
      </>
    )}
  </li>
)

const formatNanos = (nanos: number) => {
  if (nanos < 1_000) return `${nanos} ns`
  if (nanos < 1_000_000) return `${(nanos / 1_000).toFixed(1)} µs`
  return `${(nanos / 1_000_000).toFixed(2)} ms`
}

const EvaluationRow = ({
  evaluation,
  onHover,
  hovered,
}: {
  evaluation: RuleEvaluation
  onHover: (rule: Rule | undefined) => void
  hovered: boolean
}) => (
  <>
  <tr className={hovered ? 'hovered' : ''} onMouseEnter={() => onHover(evaluation.rule)} onMouseLeave={() => onHover(undefined)}>
    <td>{describeRule(evaluation.rule)}</td>
    <td>
      <span className={`outcome ${evaluation.outcome.toLowerCase()}`}>{evaluation.outcome.toLowerCase()}</span>
    </td>
    <td className="number">
      {evaluation.matchedBefore} → {evaluation.matchedAfter}
    </td>
    <td className="number">
      {evaluation.rejected.length === 0 ? (
        0
      ) : (
        <details>
          <summary>{evaluation.rejected.length}</summary>
          <ul>
            {evaluation.rejected.map((talk) => (
              <li key={talk.id}>{talk.title}</li>
            ))}
          </ul>
        </details>
      )}
    </td>
    <td className="number">{formatNanos(evaluation.durationNanos)}</td>
  </tr>
  {(evaluation.logs.length > 0 || Object.keys(evaluation.context).length > 0) && (
    <tr
      className={`logs ${hovered ? 'hovered' : ''}`}
      onMouseEnter={() => onHover(evaluation.rule)}
      onMouseLeave={() => onHover(undefined)}
    >
      <td colSpan={5}>
        <dl>
          {Object.entries(evaluation.context).map(([key, value]) => (
            <div key={key}>
              <dt>{key}</dt>
              <dd>{JSON.stringify(value)}</dd>
            </div>
          ))}
        </dl>
        <ul>
          {evaluation.logs.map((line, i) => (
            <li key={i}>{line}</li>
          ))}
        </ul>
      </td>
    </tr>
  )}
  </>
)

type Entry = { step: GenerationStep; seed: number }

const canonical = (value: unknown): string =>
  JSON.stringify(value, (_, v) =>
    v && typeof v === 'object' && !Array.isArray(v)
      ? Object.fromEntries(Object.entries(v).sort(([a], [b]) => a.localeCompare(b)))
      : v,
  )

const isHovered = (rule: Rule, hoveredRule: Rule | undefined) =>
  hoveredRule !== undefined && canonical(rule) === canonical(hoveredRule)

const rulePredicate = (rule: Rule): TalkPredicate | undefined =>
  'require' in rule ? rule.require.talk : 'spacing' in rule ? rule.spacing.talk : undefined

const highlightFor = (step: GenerationStep, rule: Rule | undefined) => {
  if (!rule || !step.talk) return { className: '' }
  const key = canonical(rule)
  const evaluation = step.evaluation.rules.find((e) => canonical(e.rule) === key)
  if (!evaluation) return { className: '' }
  const talkId = step.talk.id
  return {
    className: evaluation.rejected.some((talk) => talk.id === talkId) ? 'rejected' : 'matched',
    predicate: evaluation.satisfiesPredicate ? rulePredicate(rule) : undefined,
  }
}

const StepView = ({
  entry: { step, seed },
  hoveredRule,
  onHover,
  onSeed,
}: {
  entry: Entry
  hoveredRule: Rule | undefined
  onHover: (rule: Rule | undefined) => void
  onSeed: (seed: number) => void
}) => {
  const highlight = highlightFor(step, hoveredRule)
  const [open, setOpen] = useState(false)

  const onCardClick = (e: React.MouseEvent) => {
    if (e.target instanceof Element && e.target.closest('a')) return
    setOpen(!open)
  }

  return (
    <li className={`step ${highlight.className}`}>
      <div className="card" onClick={onCardClick}>
        {step.talk ? (
          <TalkRow talk={step.talk} position={step.position} highlight={highlight.predicate} />
        ) : (
          <div className="talk none">
            <span className="index">{step.position}</span>
            <span>No talk matched the rules.</span>
          </div>
        )}
      </div>
      <details className="trace" open={open} onToggle={(e) => setOpen(e.currentTarget.open)}>
        <summary>
          {formatNanos(step.durationNanos)} · seed{' '}
          <button
            className="link"
            title="Use this seed"
            onClick={(e) => {
              e.preventDefault()
              onSeed(seed)
            }}
          >
            {seed}
          </button>{' '}
          · {step.evaluation.matching.length} matching
        </summary>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Outcome</th>
              <th className="number">Matched</th>
              <th className="number">Rejected</th>
              <th className="number">Time</th>
            </tr>
          </thead>
          <tbody>
            {step.evaluation.rules.map((evaluation, i) => (
              <EvaluationRow
                key={i}
                evaluation={evaluation}
                onHover={onHover}
                hovered={isHovered(evaluation.rule, hoveredRule)}
              />
            ))}
          </tbody>
        </table>
      </details>
    </li>
  )
}

const randomSeed = () => Math.floor(Math.random() * Number.MAX_SAFE_INTEGER)

const parseSeeds = (text: string): number[] | undefined => {
  const parts = text.split(/[\s,]+/).filter((part) => part !== '')
  const seeds = parts.map(Number)
  return seeds.every(Number.isSafeInteger) ? seeds : undefined
}

const CallSchema = z.object({ rules: z.array(RuleSchema), seed: z.number().int(), count: z.number().int() })

type Call = z.infer<typeof CallSchema>

const SequenceSchema = z.object({ history: z.array(TalkIdSchema), calls: z.array(CallSchema) })

type Sequence = z.infer<typeof SequenceSchema>

const parseParam = <A,>(params: URLSearchParams, name: string, schema: z.ZodType<A>): A | undefined => {
  const value = params.get(name)
  if (value === null) return undefined
  try {
    const result = schema.safeParse(JSON.parse(value))
    return result.success ? result.data : undefined
  } catch {
    return undefined
  }
}

const readUrl = () => {
  const params = new URLSearchParams(window.location.search)
  return {
    rules: parseParam(params, 'rules', z.array(RuleSchema)),
    seeds: params.get('seeds') ?? '',
    history: parseParam(params, 'history', z.array(TalkIdSchema)) ?? [],
    sequence: parseParam(params, 'sequence', SequenceSchema),
  }
}

const writeUrl = (rules: Rule[], seeds: string, history: TalkId[], sequence: Sequence | undefined) => {
  const params = new URLSearchParams()
  params.set('rules', JSON.stringify(rules))
  if (seeds.trim() !== '') params.set('seeds', seeds.trim())
  if (history.length > 0) params.set('history', JSON.stringify(history))
  if (sequence) params.set('sequence', JSON.stringify(sequence))
  const url = `${window.location.pathname}?${params}`
  if (url !== `${window.location.pathname}${window.location.search}`) window.history.replaceState(null, '', url)
}

const historyOf = (entries: Entry[]) => entries.flatMap(({ step }) => (step.talk ? [step.talk.id] : []))

const execute = async (call: Call, base: TalkId[], entries: Entry[]): Promise<Entry[]> => {
  const { trace } = await previewApi.preview({ ...call, history: [...base, ...historyOf(entries)] })
  return [...entries, ...trace.map((step) => ({ step, seed: call.seed }))]
}

const replay = ({ history, calls }: Sequence) =>
  calls.reduce<Promise<Entry[]>>((entries, call) => entries.then((previous) => execute(call, history, previous)), Promise.resolve([]))

const describeError = (error: unknown) =>
  error instanceof UnexpectedResponseError
    ? `${error.status}: ${JSON.stringify(error.body)}`
    : error instanceof Error
      ? error.message
      : String(error)

export const Preview = ({ active }: { active: boolean }) => {
  const [initial] = useState(readUrl)
  const [defaultRules, setDefaultRules] = useState<Rule[]>()
  const [talks, setTalks] = useState<Talk[]>()
  const [rules, setRules] = useState<Rule[] | undefined>(initial.rules)
  const [seedText, setSeedText] = useState(initial.seeds)
  const [history, setHistory] = useState<TalkId[]>(initial.history)
  const [crafting, setCrafting] = useState(false)
  const [base, setBase] = useState<TalkId[]>(initial.sequence?.history ?? [])
  const [calls, setCalls] = useState<Call[]>([])
  const [frozen, setFrozen] = useState<Sequence | undefined>(initial.sequence)
  const [entries, setEntries] = useState<Entry[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const [hoveredRule, setHoveredRule] = useState<Rule>()

  useEffect(() => {
    let active = true
    Promise.all([previewApi.getDefaultRules({}), previewApi.listTalks({})])
      .then(([{ rules }, { talks }]) => {
        if (active) {
          setDefaultRules(rules)
          setRules((current) => current ?? rules)
          setTalks(talks)
        }
      })
      .catch((e) => active && setError(describeError(e)))
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    const sequence = initial.sequence
    if (!sequence) return
    let active = true
    setLoading(true)
    replay(sequence)
      .then((entries) => {
        if (active) {
          setEntries(entries)
          setCalls(sequence.calls)
        }
      })
      .catch((e) => active && setError(describeError(e)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [initial])

  useEffect(() => {
    if (active && rules) writeUrl(rules, seedText, history, frozen)
  }, [active, rules, seedText, history, frozen])

  const seeds = parseSeeds(seedText)
  const exhausted = entries.length > 0 && !entries[entries.length - 1].step.talk
  const byId = new Map((talks ?? []).map((talk) => [talk.id, talk]))

  const run = async (count: number, nextBase: TalkId[], previousEntries: Entry[], previousCalls: Call[]) => {
    if (!rules || !seeds) return
    const seed = seeds.length === 0 ? randomSeed() : seeds[Math.min(previousCalls.length, seeds.length - 1)]
    const call = { rules, seed, count }
    setLoading(true)
    setError(undefined)
    setFrozen(undefined)
    try {
      const next = await execute(call, nextBase, previousEntries)
      setBase(nextBase)
      setEntries(next)
      setCalls([...previousCalls, call])
    } catch (e) {
      setError(describeError(e))
    } finally {
      setLoading(false)
    }
  }

  const freeze = () => {
    setFrozen({ history: base, calls })
    setHistory(base)
    setSeedText(calls.map((call) => call.seed).join(', '))
  }

  const currentRules = rules ?? []

  const updateRule = (index: number, rule: Rule) => setRules(currentRules.map((r, i) => (i === index ? rule : r)))

  const moveRule = (from: number, to: number) => {
    const next = [...currentRules]
    const [moved] = next.splice(from, 1)
    next.splice(to, 0, moved)
    setRules(next)
  }

  return (
    <div className="preview">
      <aside className="panel">
        <h2>Rules</h2>
        <ol className="rules">
          {currentRules.map((rule, i) => (
            <RuleEditor
              key={i}
              rule={rule}
              onChange={(next) => updateRule(i, next)}
              onRemove={() => setRules(currentRules.filter((_, j) => j !== i))}
              onMoveUp={i > 0 ? () => moveRule(i, i - 1) : undefined}
              onMoveDown={i < currentRules.length - 1 ? () => moveRule(i, i + 1) : undefined}
              onHover={setHoveredRule}
              hovered={isHovered(rule, hoveredRule)}
            />
          ))}
        </ol>
        <div className="actions">
          <button className="secondary" onClick={() => setRules([...currentRules, newRule('require', false)])}>
            Add rule
          </button>
          <button className="secondary" onClick={() => defaultRules && setRules(defaultRules)} disabled={!defaultRules}>
            Reset to defaults
          </button>
        </div>
        <h2>History</h2>
        {history.length === 0 ? (
          <p className="hint">Empty. Generated talks start at position 1.</p>
        ) : (
          <ol className="history-list">
            {history.map((id, i) => (
              <li key={i}>{byId.get(id)?.title ?? id}</li>
            ))}
          </ol>
        )}
        <div className="actions">
          <button className="secondary" onClick={() => setCrafting(true)} disabled={!talks}>
            Craft history
          </button>
          <button className="secondary" onClick={() => setHistory([])} disabled={history.length === 0}>
            Clear
          </button>
        </div>
        <h2>Seeds</h2>
        <div className="seeds">
          <input
            className={seeds ? '' : 'invalid'}
            placeholder="Random for each call"
            value={seedText}
            onChange={(e) => setSeedText(e.target.value)}
          />
          <button className="secondary" onClick={() => setSeedText('')} disabled={seedText === ''}>
            Clear
          </button>
        </div>
        <p className="hint">One per call, comma-separated. The last one repeats.</p>
      </aside>
      <div className="results">
        <button onClick={() => run(initialCount, history, [], [])} disabled={loading || !seeds || !rules}>
          Generate
        </button>
        {error && <p className="error">{error}</p>}
        {base.length > 0 && entries.length > 0 && (
          <ol className="talks injected">
            {base.map((id, i) => {
              const talk = byId.get(id)
              return talk ? <TalkRow key={i} talk={talk} position={i + 1} /> : null
            })}
          </ol>
        )}
        <ol className="talks">
          {entries.map((entry, i) => (
            <StepView
              key={i}
              entry={entry}
              hoveredRule={hoveredRule}
              onHover={setHoveredRule}
              onSeed={(seed) => setSeedText(String(seed))}
            />
          ))}
        </ol>
        {entries.length > 0 && (
          <div className="actions">
            {exhausted ? (
              <p className="meta">No talk matched, so the sequence ends here.</p>
            ) : (
              <button onClick={() => run(1, base, entries, calls)} disabled={loading || !seeds}>
                {loading ? 'Loading...' : 'One more'}
              </button>
            )}
            <button className="secondary" onClick={freeze} disabled={loading || frozen !== undefined}>
              {frozen ? 'Frozen in URL' : 'Freeze'}
            </button>
          </div>
        )}
      </div>
      {crafting && talks && (
        <CraftHistory talks={talks} history={history} onSave={setHistory} onClose={() => setCrafting(false)} />
      )}
    </div>
  )
}
