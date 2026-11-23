import { StrictMode, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { fetchTransport } from '@polyvariant/smithy-ts-runtime'
import { HelloServiceClient } from './generated'

const api = new HelloServiceClient(fetchTransport())

const App = () => {
  const [name, setName] = useState('')
  const [greeting, setGreeting] = useState<string>()

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    const { greeting } = await api.hello({ name })
    setGreeting(greeting)
  }

  return (
    <form onSubmit={submit}>
      <input value={name} onChange={(e) => setName(e.target.value)} />
      <button>Greet</button>
      {greeting && <p>{greeting}</p>}
    </form>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
