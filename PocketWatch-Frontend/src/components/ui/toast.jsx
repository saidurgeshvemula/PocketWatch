import { createContext, useCallback, useContext, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { CheckCircle2, XCircle, Info, X } from 'lucide-react'
import { cn } from '@/lib/utils'

const ToastContext = createContext(null)

const ICONS = {
  success: CheckCircle2,
  error: XCircle,
  info: Info,
}

const TONE = {
  success: 'border-credit/30 text-credit',
  error: 'border-debit/30 text-debit',
  info: 'border-brass/30 text-brass',
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const toast = useCallback(
    ({ title, description, variant = 'info', duration = 4000 }) => {
      const id = crypto.randomUUID()
      setToasts((prev) => [...prev, { id, title, description, variant }])
      if (duration) setTimeout(() => dismiss(id), duration)
    },
    [dismiss],
  )

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="fixed bottom-5 right-5 z-[100] flex flex-col gap-2 w-80 max-w-[90vw]">
        <AnimatePresence>
          {toasts.map(({ id, title, description, variant }) => {
            const Icon = ICONS[variant]
            return (
              <motion.div
                key={id}
                role="status"
                initial={{ opacity: 0, y: 10, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, x: 40 }}
                transition={{ duration: 0.18 }}
                className={cn(
                  'bg-card text-fg rounded-card shadow-pop border p-3.5 flex items-start gap-2.5',
                  TONE[variant],
                )}
              >
                <Icon size={18} className="shrink-0 mt-0.5" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-fg">{title}</p>
                  {description && <p className="text-xs text-fg-muted mt-0.5">{description}</p>}
                </div>
                <button onClick={() => dismiss(id)} className="text-fg-subtle hover:text-fg shrink-0">
                  <X size={14} />
                </button>
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
