import { describe, it, expect } from 'vitest'
import { formatDate, parseDate, apiDate } from './date'

describe('formatDate', () => {
  it('converts an ISO date string to DD/MM/YYYY', () => {
    expect(formatDate('2026-03-05')).toBe('05/03/2026')
  })

  it('leaves an already-DD/MM/YYYY string unchanged', () => {
    expect(formatDate('05/03/2026')).toBe('05/03/2026')
  })

  it('formats a Date object', () => {
    expect(formatDate(new Date(2026, 2, 5))).toBe('05/03/2026')
  })

  it('returns an empty string for null/undefined', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined)).toBe('')
  })

  it('returns an empty string for an unparseable value', () => {
    expect(formatDate('not-a-date')).toBe('')
  })
})

describe('parseDate', () => {
  it('converts DD/MM/YYYY to ISO for sending to the API', () => {
    expect(parseDate('05/03/2026')).toBe('2026-03-05')
  })

  it('passes through a value that is not DD/MM/YYYY', () => {
    expect(parseDate('2026-03-05')).toBe('2026-03-05')
  })

  it('passes through falsy values unchanged', () => {
    expect(parseDate('')).toBe('')
    expect(parseDate(null)).toBe(null)
  })
})

describe('apiDate', () => {
  it('converts an ISO date to DD/MM/YYYY', () => {
    expect(apiDate('2026-03-05')).toBe('05/03/2026')
  })

  it('passes through a non-ISO value unchanged', () => {
    expect(apiDate('05/03/2026')).toBe('05/03/2026')
  })
})
