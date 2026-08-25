import { motion, AnimatePresence } from 'framer-motion'
import { X } from 'lucide-react'

export function Modal({ open, title, onClose, children, className = '' }) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-[90] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
          onClick={onClose}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.15 }}
        >
          <motion.div
            className={`bg-card border border-white/[0.08] rounded-card shadow-pop w-full max-w-md p-6 space-y-4 ${className}`}
            onClick={(e) => e.stopPropagation()}
            initial={{ opacity: 0, scale: 0.96, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 8 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
          >
            <div className="flex items-center justify-between">
              <h3 className="font-display text-lg font-semibold text-fg">{title}</h3>
              <button onClick={onClose} className="text-fg-muted hover:text-fg transition-colors">
                <X size={18} />
              </button>
            </div>
            {children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
