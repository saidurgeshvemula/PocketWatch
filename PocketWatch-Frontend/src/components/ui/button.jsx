import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center whitespace-nowrap rounded-control text-sm font-medium ring-offset-canvas transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-40 active:scale-[0.98]',
  {
    variants: {
      variant: {
        default: 'bg-fg text-canvas hover:bg-fg/90 shadow-sm',
        brass: 'bg-gold-gradient text-ink font-semibold shadow-glow hover:brightness-110',
        outline: 'border border-white/12 bg-transparent text-fg hover:bg-white/5 hover:border-white/20',
        ghost: 'text-fg hover:bg-white/5',
        destructive: 'bg-debit text-white hover:bg-debit/90',
        link: 'text-brass underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 rounded-control px-3',
        lg: 'h-12 rounded-control px-8 text-base',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

const Button = React.forwardRef(({ className, variant, size, asChild = false, ...props }, ref) => {
  const Comp = asChild ? Slot : 'button'
  return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
})
Button.displayName = 'Button'

export { Button, buttonVariants }
