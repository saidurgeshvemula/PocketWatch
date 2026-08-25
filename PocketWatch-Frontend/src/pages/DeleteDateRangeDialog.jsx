import { useEffect, useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import { Modal } from '@/components/ui/modal'
import { Button } from '@/components/ui/button'

const todayIso = () => new Date().toISOString().slice(0, 10)

export default function DeleteDateRangeDialog({ open, onClose, onConfirm, loading }) {
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setStartDate('')
      setEndDate('')
      setError('')
    }
  }, [open])

  const handleClose = () => {
    if (loading) return
    onClose()
  }

  const handleConfirm = () => {
    if (!startDate || !endDate) {
      setError('Both a start and end date are required.')
      return
    }
    if (startDate > endDate) {
      setError('Start date must be on or before the end date.')
      return
    }
    setError('')
    onConfirm({ startDate, endDate })
  }

  return (
    <Modal open={open} title="Delete transactions by date range" onClose={handleClose}>
      <div className="space-y-4 text-sm">
        <div className="flex items-start gap-2.5 rounded-md bg-debit-light px-3 py-2.5">
          <AlertTriangle size={16} className="text-debit shrink-0 mt-0.5" />
          <p className="text-debit/90">
            This permanently deletes every transaction dated within the selected range
            (inclusive). This cannot be undone.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="range-start" className="block text-fg/60 mb-1">
              Start date
            </label>
            <input
              id="range-start"
              type="date"
              value={startDate}
              max={endDate || undefined}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            />
          </div>

          <div>
            <label htmlFor="range-end" className="block text-fg/60 mb-1">
              End date
            </label>
            <input
              id="range-end"
              type="date"
              value={endDate}
              min={startDate || undefined}
              max={todayIso()}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            />
          </div>
        </div>

        {error && <p className="text-debit text-xs">{error}</p>}

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
            {loading ? 'Deleting…' : 'Delete range'}
          </Button>
        </div>
      </div>
    </Modal>
  )
}