import { describe, it, expect } from 'vitest'
import { cn } from './utils'

describe('cn', () => {
  it('joins plain class strings', () => {
    expect(cn('px-2', 'py-2')).toBe('px-2 py-2')
  })

  it('drops falsy values', () => {
    expect(cn('px-2', false && 'hidden', null, undefined, 'py-2')).toBe('px-2 py-2')
  })

  it('lets a later conflicting Tailwind class win', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4')
  })

  it('keeps classes from both sides when nothing conflicts', () => {
    expect(cn('text-sm font-medium', 'text-fg/60')).toBe('text-sm font-medium text-fg/60')
  })
})
