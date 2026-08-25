import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Modal } from '@/components/ui/modal'
import { Button } from '@/components/ui/button'
import { useToast } from '@/components/ui/toast'
import { useCreateCashExpense } from '@/api/queries'
import { apiDate } from '@/utils/date'

const todayIso = () => new Date().toISOString().slice(0, 10)

const DEFAULT_VALUES = {
  transactionDate: todayIso(),
  description: '',
  amount: '',
  categoryId: '',
  notes: '',
}

export default function CashExpenseModal({ open, onClose, onSuccess, categories }) {
  const { toast } = useToast()
  const createCashExpense = useCreateCashExpense()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: DEFAULT_VALUES })

  useEffect(() => {
    if (open) {
      reset(DEFAULT_VALUES)
    }
  }, [open, reset])

  const onSubmit = (values) => {
    const payload = {
      transactionDate: apiDate(values.transactionDate),
      description: values.description.trim(),
      amount: Number(values.amount),
      categoryId: Number(values.categoryId),
      notes: values.notes.trim() || null,
    }

    createCashExpense.mutate(payload, {
      onSuccess: () => {
        reset(DEFAULT_VALUES)
        onSuccess?.()
      },
      onError: (err) => {
        toast({
          title: 'Could not add cash expense',
          description: err.response?.data?.message || 'Please try again.',
          variant: 'error',
        })
      },
    })
  }

  const handleClose = () => {
    if (isSubmitting || createCashExpense.isPending) return
    reset(DEFAULT_VALUES)
    onClose()
  }

  return (
    <Modal open={open} title="Add cash expense" onClose={handleClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 text-sm">
        <div>
          <label htmlFor="cash-date" className="block text-fg/60 mb-1">
            Date
          </label>
          <input
            id="cash-date"
            type="date"
            {...register('transactionDate', { required: 'Date is required' })}
            className="w-full h-9 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          />
          {errors.transactionDate && (
            <p className="text-debit text-xs mt-1">{errors.transactionDate.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="cash-description" className="block text-fg/60 mb-1">
            Description
          </label>
          <input
            id="cash-description"
            type="text"
            placeholder="e.g. Tea"
            {...register('description', {
              required: 'Description is required',
              maxLength: { value: 255, message: 'Keep it under 255 characters' },
            })}
            className="w-full h-9 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          />
          {errors.description && (
            <p className="text-debit text-xs mt-1">{errors.description.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="cash-amount" className="block text-fg/60 mb-1">
            Amount
          </label>
          <input
            id="cash-amount"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            {...register('amount', {
              required: 'Amount is required',
              min: { value: 0.01, message: 'Amount must be greater than 0' },
            })}
            className="w-full h-9 rounded-md border border-white/15 bg-elevated px-3 text-sm font-mono tabular focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          />
          {errors.amount && <p className="text-debit text-xs mt-1">{errors.amount.message}</p>}
        </div>

        <div>
          <label htmlFor="cash-category" className="block text-fg/60 mb-1">
            Category
          </label>
          <select
            id="cash-category"
            {...register('categoryId', { required: 'Category is required' })}
            className="w-full h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          >
            <option value="">Select a category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          {errors.categoryId && (
            <p className="text-debit text-xs mt-1">{errors.categoryId.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="cash-notes" className="block text-fg/60 mb-1">
            Notes <span className="text-fg/35">(optional)</span>
          </label>
          <textarea
            id="cash-notes"
            rows={3}
            placeholder="e.g. Office"
            {...register('notes')}
            className="w-full rounded-md border border-white/15 bg-elevated px-3 py-2 text-sm resize-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={handleClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting || createCashExpense.isPending}>
            {createCashExpense.isPending ? 'Saving…' : 'Add expense'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}