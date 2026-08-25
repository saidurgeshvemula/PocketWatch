import { AlertTriangle, Inbox } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

const STATUS_TONE = {
  PENDING: 'bg-white/8 text-fg-muted',
  PROCESSING: 'bg-brass/15 text-brass',
  COMPLETED: 'bg-credit-light text-credit',
  FAILED: 'bg-debit-light text-debit',
  RULE: 'bg-white/8 text-fg-muted',
  AI: 'bg-brass/15 text-brass',
  USER: 'bg-credit-light text-credit',
}

export function StatusBadge({ status, className }) {
  return (
    <span
      className={cn(
        'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium capitalize',
        STATUS_TONE[status] || 'bg-white/8 text-fg-muted',
        className,
      )}
    >
      {status?.toLowerCase()}
    </span>
  )
}

export function Skeleton({ className }) {
  return (
    <div
      className={cn(
        'rounded-md bg-gradient-to-r from-white/[0.04] via-white/[0.09] to-white/[0.04] animate-shimmer',
        className,
      )}
    />
  )
}

export function SkeletonRows({ rows = 5, cols = 4 }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: rows }).map((_, r) => (
        <div key={r} className="flex gap-4">
          {Array.from({ length: cols }).map((_, c) => (
            <Skeleton key={c} className="h-5 flex-1" />
          ))}
        </div>
      ))}
    </div>
  )
}

export function EmptyState({ icon: Icon = Inbox, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      <div className="w-14 h-14 rounded-full bg-white/5 border border-white/10 flex items-center justify-center mb-4">
        <Icon size={24} className="text-fg-subtle" />
      </div>
      <p className="text-sm font-medium text-fg/80">{title}</p>
      {description && <p className="text-xs text-fg-muted mt-1 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function ErrorState({ message = 'Something went wrong.', onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      <AlertTriangle size={28} className="text-debit/70 mb-3" />
      <p className="text-sm font-medium text-debit">{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}

export function ConfirmDialog({ open, title, description, confirmLabel = 'Confirm', destructive = true, onConfirm, onCancel, loading }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-[90] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" onClick={onCancel}>
      <div
        className="bg-card border border-white/[0.08] rounded-card shadow-pop p-6 w-full max-w-sm space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div>
          <h3 className="font-display text-lg font-semibold text-fg">{title}</h3>
          {description && <p className="text-sm text-fg-muted mt-1.5">{description}</p>}
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="outline" size="sm" onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
          <Button
            variant={destructive ? 'destructive' : 'brass'}
            size="sm"
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Working…' : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
