import { cn } from '@/lib/utils'

export function Card({ className, torn = false, glass = false, children, ...props }) {
  return (
    <div
      className={cn(
        'rounded-card shadow-card border border-white/[0.06] p-5 transition-colors duration-200',
        glass ? 'glass-panel' : 'bg-card',
        torn && 'torn-edge text-brass mb-2',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  )
}

export function CardLabel({ children, className }) {
  return (
    <p className={cn('text-xs uppercase tracking-wider text-fg-muted font-medium mb-1', className)}>
      {children}
    </p>
  )
}

export function CardValue({ children, className }) {
  return <p className={cn('font-mono tabular text-3xl font-semibold text-fg', className)}>{children}</p>
}
