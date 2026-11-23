import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { Preview } from './Preview'
import { Production } from './Production'
import './index.css'

const tabs = {
  production: { label: 'Production', path: '/' },
  preview: { label: 'Preview', path: '/preview' },
} as const

type Tab = keyof typeof tabs

const tabKeys = Object.keys(tabs) as Tab[]

const tabAt = (path: string): Tab => tabKeys.find((key) => tabs[key].path === path) ?? 'production'

const usePath = () => {
  const [path, setPath] = useState(window.location.pathname)

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const navigate = (to: string) => {
    window.history.pushState(null, '', to)
    setPath(to)
  }

  return [path, navigate] as const
}

const themes = ['system', 'light', 'dark'] as const

type Theme = (typeof themes)[number]

const themeLabels: Record<Theme, string> = { system: 'Theme: system', light: 'Theme: light', dark: 'Theme: dark' }

const storedTheme = (): Theme => {
  try {
    const value = localStorage.getItem('theme')
    return themes.find((theme) => theme === value) ?? 'system'
  } catch {
    return 'system'
  }
}

const useTheme = () => {
  const [theme, setTheme] = useState<Theme>(storedTheme)

  useEffect(() => {
    if (theme === 'system') document.documentElement.removeAttribute('data-theme')
    else document.documentElement.setAttribute('data-theme', theme)
    try {
      localStorage.setItem('theme', theme)
    } catch {
      return
    }
  }, [theme])

  const next = () => setTheme(themes[(themes.indexOf(theme) + 1) % themes.length])

  return [theme, next] as const
}

const App = () => {
  const [path, navigate] = usePath()
  const [theme, nextTheme] = useTheme()
  const tab = tabAt(path)

  const onClick = (key: Tab) => (e: React.MouseEvent) => {
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return
    e.preventDefault()
    navigate(tabs[key].path)
  }

  return (
    <main>
      <nav className="tabs">
        {tabKeys.map((key) => (
          <a key={key} href={tabs[key].path} className={key === tab ? 'active' : ''} onClick={onClick(key)}>
            {tabs[key].label}
          </a>
        ))}
        <button className="theme" onClick={nextTheme}>
          {themeLabels[theme]}
        </button>
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
