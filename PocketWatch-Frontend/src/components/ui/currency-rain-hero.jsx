import { useState, useEffect, useCallback, useRef } from 'react'

class TextScramble {
  constructor(el) {
    this.el = el
    this.chars = '$€£¥₹0123456789+-.,%'
    this.queue = []
    this.frame = 0
    this.frameRequest = 0
    this.resolve = () => {}
    this.update = this.update.bind(this)
  }

  setText(newText) {
    const oldText = this.el.innerText
    const length = Math.max(oldText.length, newText.length)
    const promise = new Promise((resolve) => (this.resolve = resolve))
    this.queue = []

    for (let i = 0; i < length; i++) {
      const from = oldText[i] || ''
      const to = newText[i] || ''
      const start = Math.floor(Math.random() * 40)
      const end = start + Math.floor(Math.random() * 40)
      this.queue.push({ from, to, start, end })
    }

    cancelAnimationFrame(this.frameRequest)
    this.frame = 0
    this.update()
    return promise
  }

  update() {
    let output = ''
    let complete = 0

    for (let i = 0, n = this.queue.length; i < n; i++) {
      let { from, to, start, end, char } = this.queue[i]
      if (this.frame >= end) {
        complete++
        output += to
      } else if (this.frame >= start) {
        if (!char || Math.random() < 0.28) {
          char = this.chars[Math.floor(Math.random() * this.chars.length)]
          this.queue[i].char = char
        }
        output += `<span class="ticker-dud">${char}</span>`
      } else {
        output += from
      }
    }

    this.el.innerHTML = output
    if (complete === this.queue.length) {
      this.resolve()
    } else {
      this.frameRequest = requestAnimationFrame(this.update)
      this.frame++
    }
  }
}

const PHRASES = [
  'POCKETWATCH',
  'TRACK EVERY RUPEE',
  'AI CATEGORIZATION',
  'KNOW YOUR CASH FLOW',
  'SPEND SMARTER',
  'YOUR MONEY, UNDERSTOOD',
]

function ScrambledTitle() {
  const elementRef = useRef(null)
  const scramblerRef = useRef(null)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    if (elementRef.current && !scramblerRef.current) {
      scramblerRef.current = new TextScramble(elementRef.current)
      setMounted(true)
    }
  }, [])

  useEffect(() => {
    if (!mounted || !scramblerRef.current) return
    let counter = 0
    let cancelled = false

    const next = () => {
      if (cancelled || !scramblerRef.current) return
      scramblerRef.current.setText(PHRASES[counter]).then(() => {
        if (!cancelled) setTimeout(next, 2000)
      })
      counter = (counter + 1) % PHRASES.length
    }

    next()
    return () => {
      cancelled = true
    }
  }, [mounted])

  return (
    <h1
      ref={elementRef}
      className="font-mono text-3xl sm:text-5xl font-semibold tracking-wider text-fg text-center drop-shadow-[0_0_24px_rgba(245,185,66,0.15)]"
    >
      POCKETWATCH
    </h1>
  )
}

const GLYPHS = ['$', '€', '£', '¥', '₹', '0', '1', '5', '9', '%', '+', '.']

function createGlyphs(count) {
  const items = []
  for (let i = 0; i < count; i++) {
    items.push({
      char: GLYPHS[Math.floor(Math.random() * GLYPHS.length)],
      x: Math.random() * 100,
      y: Math.random() * 100,
      speed: 0.08 + Math.random() * 0.22,
      tone: Math.random() < 0.14 ? 'debit' : Math.random() < 0.5 ? 'brass' : 'credit',
    })
  }
  return items
}

export function CurrencyRainHero({ children }) {
  const [glyphs, setGlyphs] = useState([])
  const [activeIndices, setActiveIndices] = useState(new Set())

  const spawn = useCallback(() => createGlyphs(140), [])

  useEffect(() => {
    setGlyphs(spawn())
  }, [spawn])

  useEffect(() => {
    const id = setInterval(() => {
      const next = new Set()
      const active = Math.floor(Math.random() * 3) + 2
      for (let i = 0; i < active; i++) {
        next.add(Math.floor(Math.random() * 140))
      }
      setActiveIndices(next)
    }, 90)
    return () => clearInterval(id)
  }, [])

  useEffect(() => {
    let raf
    const tick = () => {
      setGlyphs((prev) =>
        prev.map((g) => {
          const y = g.y + g.speed
          if (y >= 102) {
            return {
              ...g,
              y: -4,
              x: Math.random() * 100,
              char: GLYPHS[Math.floor(Math.random() * GLYPHS.length)],
              tone: Math.random() < 0.14 ? 'debit' : Math.random() < 0.5 ? 'brass' : 'credit',
            }
          }
          return { ...g, y }
        })
      )
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [])

  const toneClass = {
    brass: 'text-brass',
    credit: 'text-credit',
    debit: 'text-debit',
  }

  return (
    <div className="relative min-h-screen w-full flex items-center justify-center overflow-hidden bg-canvas">
      {}
      <div className="absolute inset-0 pointer-events-none">
        {glyphs.map((g, i) => {
          const isActive = activeIndices.has(i)
          return (
            <span
              key={i}
              className={`absolute font-mono select-none ${toneClass[g.tone]} ${
                isActive ? 'text-lg font-bold' : 'font-light'
              }`}
              style={{
                left: `${g.x}%`,
                top: `${g.y}%`,
                transform: `translate(-50%, -50%) scale(${isActive ? 1.3 : 1})`,
                opacity: isActive ? 0.95 : 0.28,
                textShadow: isActive ? '0 0 10px currentColor' : 'none',
                fontSize: isActive ? undefined : '0.95rem',
                transition: 'opacity 0.15s, transform 0.15s',
                willChange: 'transform, top',
              }}
            >
              {g.char}
            </span>
          )
        })}
      </div>

      {}
      <div className="absolute inset-0 bg-gradient-to-b from-canvas/50 via-transparent to-canvas/80 pointer-events-none" />

      {}
      <div className="absolute top-[12%] left-1/2 -translate-x-1/2 z-10 px-4 w-full">
        <ScrambledTitle />
      </div>

      <div className="relative z-20 w-full px-4">{children}</div>
    </div>
  )
}
