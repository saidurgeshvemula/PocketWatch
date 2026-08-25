import { useEffect, useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import { Modal } from '@/components/ui/modal'
import { Button } from '@/components/ui/button'

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const currentYear = new Date().getFullYear()

const YEAR_OPTIONS = Array.from({ length: 6 }, (_, i) => currentYear - i)

export default function DeleteMonthDialog({ open, onClose, onConfirm, loading }) {
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(currentYear)

  useEffect(() => {
    if (open) {
      setMonth(new Date().getMonth() + 1)
      setYear(currentYear)
    }
  }, [open])

  const handleClose = () => {
    if (loading) return
    onClose()
  }

  const handleConfirm = () => {
    onConfirm({ year, month })
  }

  return (
    <Modal open={open} title="Delete transactions by month" onClose={handleClose}>
      <div className="space-y-4 text-sm">
        <div className="flex items-start gap-2.5 rounded-md bg-debit-light px-3 py-2.5">
          <AlertTriangle size={16} className="text-debit shrink-0 mt-0.5" />
          <p className="text-debit/90">
            This permanently deletes every transaction dated in the selected month. This
            cannot be undone.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="delete-month" className="block text-fg/60 mb-1">
              Month
            </label>
            <select
              id="delete-month"
              value={month}
              onChange={(e) => setMonth(Number(e.target.value))}
              className="w-full h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            >
              {MONTH_NAMES.map((name, i) => (
                <option key={name} value={i + 1}>
                  {name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="delete-year" className="block text-fg/60 mb-1">
              Year
            </label>
            <select
              id="delete-year"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
              className="w-full h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            >
              {YEAR_OPTIONS.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={handleClose} disabled={loading}>
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleConfirm}
            disabled={loading}
            className="bg-debit hover:bg-debit/90 text-white"
          >
            {loading ? 'Deleting…' : `Delete ${MONTH_NAMES[month - 1]} ${year}`}
          </Button>
        </div>
      </div>
    </Modal>
  )
}