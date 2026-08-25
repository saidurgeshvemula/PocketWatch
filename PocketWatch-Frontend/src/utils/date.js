

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/
const DMY_DATE = /^(\d{2})\/(\d{2})\/(\d{4})$/

const pad = (n) => String(n).padStart(2, '0')

export function formatDate(value) {
  if (!value) return ''

  if (typeof value === 'string') {
    if (DMY_DATE.test(value)) return value
    const isoMatch = value.match(ISO_DATE)
    if (isoMatch) {
      const [, y, m, d] = isoMatch
      return `${d}/${m}/${y}`
    }
  }

  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

export function apiDate(value) {
  if (!value) return value

  const isoMatch = value.match(ISO_DATE)
  if (isoMatch) {
    const [, y, m, d] = isoMatch
    return `${d}/${m}/${y}`
  }

  return value
}

export function parseDate(value) {
  if (!value) return value

  const dmyMatch = value.match(DMY_DATE)
  if (dmyMatch) {
    const [, d, m, y] = dmyMatch
    return `${y}-${m}-${d}`
  }

  return value
}
