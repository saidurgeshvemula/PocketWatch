import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Pencil, Trash2, Search, Check, X, ArrowUpDown, Repeat, Plus, ChevronDown } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Modal } from '@/components/ui/modal'
import { ConfirmDialog } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast'
import {
  useTransactions,
  useDeleteTransaction,
  useUpdateTransaction,
  useCategories,
  useBankAccounts,
  useDeleteSelectedTransactions,
  useDeleteTransactionsByMonth,
  useDeleteTransactionsByDateRange,
}  from '@/api/queries'
import { formatDate } from '@/utils/date'
import CashExpenseModal from './Cashexpensemodal'
import DeleteMonthDialog from './DeleteMonthDialog'
import DeleteDateRangeDialog from './DeleteDateRangeDialog'
const formatCurrency = (value) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'INR' }).format(value ?? 0)

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const DEFAULT_FILTERS = {
  page: 0,
  size: 10,
  search: '',
  categoryId: '',
  type: 'ALL',
  source: 'ALL',
  bankAccountId: '',
  startDate: '',
  endDate: '',
}

const SOURCE_META = {
  MERCHANT_DB: { label: 'Merchant', className: 'bg-info-light text-info' },
  RULE_ENGINE: { label: 'Rule', className: 'bg-[#8B5CF6]/15 text-[#A78BFA]' },
  AI: { label: 'AI', className: 'bg-brass/15 text-brass' },
  USER_OVERRIDE: { label: 'You', className: 'bg-credit-light text-credit' },
}

const TX_ORIGIN_META = {
  CASH: { label: 'Cash', className: 'bg-brass/15 text-brass' },
  IMPORTED: { label: 'Imported', className: 'bg-white/8 text-fg-muted' },
}

export default function Transactions() {
  const { toast } = useToast()
  const [searchParams] = useSearchParams()

  const [filters, setFilters] = useState(() => ({
    ...DEFAULT_FILTERS,
    search: searchParams.get('merchant') || '',
  }))
  const [editingId, setEditingId] = useState(null)
  const [editCategoryId, setEditCategoryId] = useState('')
  const [detailsTx, setDetailsTx] = useState(null)
  const [pendingDelete, setPendingDelete] = useState(null)
  const [cashModalOpen, setCashModalOpen] = useState(false)

  const [selectedIds, setSelectedIds] = useState(() => new Set())
  const [bulkMenuOpen, setBulkMenuOpen] = useState(false)
  const [bulkConfirmOpen, setBulkConfirmOpen] = useState(false)
  const [monthDialogOpen, setMonthDialogOpen] = useState(false)
  const [dateRangeDialogOpen, setDateRangeDialogOpen] = useState(false)
  const bulkMenuRef = useRef(null)
  const selectAllRef = useRef(null)

const { data: categories } = useCategories()
const { data: bankAccounts } = useBankAccounts()
  const { data, isLoading, isFetching } = useTransactions(filters)
  const deleteTransaction = useDeleteTransaction()
  const updateTransaction = useUpdateTransaction()
  const deleteSelected = useDeleteSelectedTransactions()
  const deleteByMonth = useDeleteTransactionsByMonth()
  const deleteByDateRange = useDeleteTransactionsByDateRange()

  const rows = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const categoryName = (id) => categories?.find((c) => c.id === id)?.name ?? '—'

  useEffect(() => {
    setSelectedIds(new Set())
  }, [filters])

  useEffect(() => {
    if (!bulkMenuOpen) return
    const handleClick = (e) => {
      if (bulkMenuRef.current && !bulkMenuRef.current.contains(e.target)) {
        setBulkMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [bulkMenuOpen])

  const isAllSelected = rows.length > 0 && rows.every((r) => selectedIds.has(r.id))
  const isSomeSelected = rows.some((r) => selectedIds.has(r.id))

  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = isSomeSelected && !isAllSelected
    }
  }, [isSomeSelected, isAllSelected])

  const toggleRow = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const toggleSelectAll = () => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (isAllSelected) {
        rows.forEach((r) => next.delete(r.id))
      } else {
        rows.forEach((r) => next.add(r.id))
      }
      return next
    })
  }

  const updateFilter = (patch) => setFilters((prev) => ({ ...prev, ...patch, page: 0 }))

  const toggleSort = (field) => {
    setFilters((prev) => {
      const [prevField, prevDir] = (prev.sort || 'transactionDate,desc').split(',')
      const nextDir = prevField === field && prevDir === 'desc' ? 'asc' : 'desc'
      return { ...prev, sort: `${field},${nextDir}`, page: 0 }
    })
  }
  const [sortField, sortDir] = (filters.sort || 'transactionDate,desc').split(',')

  const confirmDelete = async () => {
    try {
      await deleteTransaction.mutateAsync(pendingDelete.id)
      toast({ title: 'Transaction deleted', variant: 'success' })
      setPendingDelete(null)
    } catch (err) {
      toast({
        title: 'Could not delete transaction',
        description: err.response?.data?.message || 'Please try again.',
        variant: 'error',
      })
    }
  }

  const confirmBulkDeleteSelected = async () => {
    const requested = selectedIds.size
    try {
      const result = await deleteSelected.mutateAsync(Array.from(selectedIds))
      const deleted = typeof result === 'number' ? result : requested

      if (deleted === 0) {
        toast({ title: 'No matching transactions found', variant: 'info' })
      } else {
        toast({ title: `${deleted} transaction${deleted === 1 ? '' : 's'} deleted`, variant: 'success' })
      }
      setSelectedIds(new Set())
      setBulkConfirmOpen(false)
    } catch (err) {
      toast({
        title: 'Could not delete selected transactions',
        description: err.response?.data?.message || 'Please try again.',
        variant: 'error',
      })
    }
  }

  const confirmDeleteByMonth = async ({ year, month }) => {
    try {
      const deleted = await deleteByMonth.mutateAsync({ year, month })
      const label = `${MONTH_NAMES[month - 1]} ${year}`

      if (deleted === 0) {
        toast({ title: `No transactions found for ${label}`, variant: 'info' })
      } else {
        toast({
          title: `${deleted} transaction${deleted === 1 ? '' : 's'} deleted for ${label}`,
          variant: 'success',
        })
      }
      setMonthDialogOpen(false)
    } catch (err) {
      toast({
        title: 'Could not delete transactions',
        description: err.response?.data?.message || 'Please try again.',
        variant: 'error',
      })
    }
  }

  const confirmDeleteByDateRange = async ({ startDate, endDate }) => {
    try {
      const deleted = await deleteByDateRange.mutateAsync({ startDate, endDate })

      if (deleted === 0) {
        toast({ title: 'No transactions found in that date range', variant: 'info' })
      } else {
        toast({
          title: `${deleted} transaction${deleted === 1 ? '' : 's'} deleted`,
          variant: 'success',
        })
      }
      setDateRangeDialogOpen(false)
    } catch (err) {
      toast({
        title: 'Could not delete transactions',
        description: err.response?.data?.message || 'Please try again.',
        variant: 'error',
      })
    }
  }

  const startEdit = (tx) => {
    setEditingId(tx.id)
    setEditCategoryId(tx.categoryId ?? '')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditCategoryId('')
  }

  const saveEdit = (tx) => {
    if (!editCategoryId || Number(editCategoryId) === tx.categoryId) {
      cancelEdit()
      return
    }
    updateTransaction.mutate(
      { id: tx.id, categoryId: Number(editCategoryId) },
      {
        onSuccess: cancelEdit,
        onError: (err) =>
          toast({
            title: 'Could not update category',
            description: err.response?.data?.message || 'Please try again.',
            variant: 'error',
          }),
      },
    )
  }

  const handleCashExpenseSuccess = () => {
    setCashModalOpen(false)
    toast({ title: 'Cash expense added', variant: 'success' })
  }

  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl">Transactions</h1>
          <p className="text-fg/50 text-sm mt-1">Every debit and credit, in one ledger.</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <div className="relative" ref={bulkMenuRef}>
            <Button variant="outline" onClick={() => setBulkMenuOpen((o) => !o)} className="gap-1.5">
              <Trash2 size={15} />
              Bulk Delete
              <ChevronDown size={14} />
            </Button>
            {bulkMenuOpen && (
              <div className="absolute right-0 mt-1 w-52 rounded-md border border-white/10 bg-elevated shadow-lg z-10 py-1">
                <button
                  disabled={selectedIds.size === 0}
                  onClick={() => {
                    setBulkMenuOpen(false)
                    setBulkConfirmOpen(true)
                  }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Delete Selected{selectedIds.size > 0 ? ` (${selectedIds.size})` : ''}
                </button>
                <button
                  onClick={() => {
                    setBulkMenuOpen(false)
                    setMonthDialogOpen(true)
                  }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-white/5"
                >
                  Delete By Month
                </button>
                <button
                  onClick={() => {
                    setBulkMenuOpen(false)
                    setDateRangeDialogOpen(true)
                  }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-white/5"
                >
                  Delete By Date Range
                </button>
              </div>
            )}
          </div>
          <Button onClick={() => setCashModalOpen(true)} className="gap-1.5">
            <Plus size={15} />
            Cash Expense
          </Button>
        </div>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-6 gap-3">
          <div className="relative sm:col-span-2">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-fg/40" />
            <input
              type="text"
              placeholder="Search description or merchant"
              value={filters.search}
              onChange={(e) => updateFilter({ search: e.target.value })}
              className="w-full h-9 rounded-md border border-white/15 bg-elevated pl-8 pr-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            />
          </div>

          <select
            value={filters.categoryId}
            onChange={(e) => updateFilter({ categoryId: e.target.value })}
            className="h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm"
          >
            <option value="">All categories</option>
            {(categories ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>

          <select
            value={filters.type}
            onChange={(e) => updateFilter({ type: e.target.value })}
            className="h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm"
          >
            <option value="ALL">All types</option>
            <option value="DEBIT">Debit</option>
            <option value="CREDIT">Credit</option>
          </select>

          <select
            value={filters.source}
            onChange={(e) => updateFilter({ source: e.target.value })}
            className="h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm"
          >
            <option value="ALL">All sources</option>
            <option value="IMPORTED">Imported</option>
            <option value="CASH">Cash</option>
          </select>

          <select
            value={filters.bankAccountId}
            onChange={(e) => updateFilter({ bankAccountId: e.target.value })}
            className="h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm"
          >
            <option value="">All accounts</option>
            {(bankAccounts ?? []).map((a) => (
              <option key={a.id} value={a.id}>
                {a.nickname || a.bankName}
              </option>
            ))}
          </select>

          <div className="flex gap-2 sm:col-span-1">
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => updateFilter({ startDate: e.target.value })}
              className="h-9 flex-1 rounded-md border border-white/15 bg-elevated px-2 text-sm"
            />
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => updateFilter({ endDate: e.target.value })}
              className="h-9 flex-1 rounded-md border border-white/15 bg-elevated px-2 text-sm"
            />
          </div>
        </div>
      </Card>

      <Card className="p-0 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-fg/50 border-b border-white/10 uppercase text-xs tracking-wide">
              <th className="px-4 py-3 font-medium w-10">
                <input
                  type="checkbox"
                  ref={selectAllRef}
                  checked={isAllSelected}
                  onChange={toggleSelectAll}
                  disabled={rows.length === 0}
                  className="h-3.5 w-3.5 rounded border-white/30 text-brass"
                  aria-label="Select all transactions on this page"
                />
              </th>
              <th className="px-4 py-3 font-medium">
                <button
                  onClick={() => toggleSort('transactionDate')}
                  className="flex items-center gap-1 hover:text-fg uppercase tracking-wide"
                >
                  Date
                  <ArrowUpDown size={11} className={sortField === 'transactionDate' ? 'text-brass' : 'text-fg/25'} />
                </button>
              </th>
              <th className="px-4 py-3 font-medium">Description</th>
              <th className="px-4 py-3 font-medium">Merchant</th>
              <th className="px-4 py-3 font-medium">Category</th>
              <th className="px-4 py-3 font-medium text-right">
                <button
                  onClick={() => toggleSort('amount')}
                  className="flex items-center gap-1 ml-auto hover:text-fg uppercase tracking-wide"
                >
                  Amount
                  <ArrowUpDown size={11} className={sortField === 'amount' ? 'text-brass' : 'text-fg/25'} />
                </button>
              </th>
              <th className="px-4 py-3 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-fg/40">
                  Loading transactions…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-fg/40">
                  No transactions match these filters.
                </td>
              </tr>
            ) : (
              rows.map((tx) => {
                const isEditing = editingId === tx.id
                const sourceMeta = SOURCE_META[tx.categorySource]
                const originMeta = TX_ORIGIN_META[tx.source]
                const isSelected = selectedIds.has(tx.id)

                return (
                  <tr
                    key={tx.id}
                    className={`border-b border-white/5 last:border-0 hover:bg-elevated/[0.03] ${isSelected ? 'bg-brass/5' : ''}`}
                  >
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => toggleRow(tx.id)}
                        className="h-3.5 w-3.5 rounded border-white/30 text-brass"
                        aria-label={`Select transaction ${tx.description}`}
                      />
                    </td>
                    <td
                      className="px-4 py-3 text-fg/70 tabular font-mono text-xs cursor-pointer"
                      onClick={() => setDetailsTx(tx)}
                    >
                      {formatDate(tx.transactionDate)}
                    </td>
                    <td className="px-4 py-3 cursor-pointer" onClick={() => setDetailsTx(tx)}>
                      <span className="inline-flex items-center gap-1.5 flex-wrap">
                        {tx.description}
                        {tx.isRecurring && (
                          <span title="Recurring">
                            <Repeat size={11} className="text-fg/35" />
                          </span>
                        )}
                        {originMeta && (
                          <span
                            className={`inline-block px-1.5 py-0.5 rounded-full text-[10px] font-medium whitespace-nowrap ${originMeta.className}`}
                          >
                            {originMeta.label}
                          </span>
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-fg/60 cursor-pointer" onClick={() => setDetailsTx(tx)}>
                      {tx.merchantName || '—'}
                    </td>
                    <td className="px-4 py-3">
                      {isEditing ? (
                        <div className="flex items-center gap-1">
                          <select
                            autoFocus
                            value={editCategoryId}
                            onChange={(e) => setEditCategoryId(e.target.value)}
                            className="h-8 rounded-md border border-white/15 bg-elevated px-2 text-xs"
                          >
                            {(categories ?? []).map((c) => (
                              <option key={c.id} value={c.id}>
                                {c.name}
                              </option>
                            ))}
                          </select>
                          <button
                            onClick={() => saveEdit(tx)}
                            disabled={updateTransaction.isPending}
                            className="p-1 rounded hover:bg-credit-light text-fg/50 hover:text-credit disabled:opacity-50"
                            title="Save"
                          >
                            <Check size={15} />
                          </button>
                          <button
                            onClick={cancelEdit}
                            className="p-1 rounded hover:bg-debit-light text-fg/50 hover:text-debit"
                            title="Cancel"
                          >
                            <X size={15} />
                          </button>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5">
                          <span className="inline-block px-2 py-0.5 rounded-full bg-white/5 text-xs">
                            {categoryName(tx.categoryId)}
                          </span>
                          {sourceMeta && (
                            <span
                              className={`inline-block px-1.5 py-0.5 rounded-full text-[10px] font-medium ${sourceMeta.className}`}
                              title={
                                tx.categoryReason
                                  ? `${tx.categoryReason}${tx.categoryConfidence != null ? ` (${tx.categoryConfidence}% confidence)` : ''}`
                                  : sourceMeta.label
                              }
                            >
                              {sourceMeta.label}
                            </span>
                          )}
                        </div>
                      )}
                    </td>
                    <td
                      className={`px-4 py-3 text-right font-mono tabular font-medium cursor-pointer ${
                        tx.type === 'CREDIT' ? 'text-credit' : 'text-debit'
                      }`}
                      onClick={() => setDetailsTx(tx)}
                    >
                      {tx.type === 'CREDIT' ? '+' : '-'}
                      {formatCurrency(Math.abs(tx.amount))}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        <button
                          onClick={() => (isEditing ? cancelEdit() : startEdit(tx))}
                          className="p-1.5 rounded hover:bg-white/5 text-fg/50 hover:text-fg"
                          title="Edit category"
                        >
                          <Pencil size={15} />
                        </button>
                        <button
                          onClick={() => setPendingDelete(tx)}
                          className="p-1.5 rounded hover:bg-debit-light text-fg/50 hover:text-debit"
                          title="Delete"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>

        <div className="flex items-center justify-between px-4 py-3 border-t border-white/10">
          <p className="text-xs text-fg/50">
            Page {filters.page + 1} of {totalPages} {isFetching && '· updating…'}
            {selectedIds.size > 0 && ` · ${selectedIds.size} selected`}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={filters.page <= 0}
              onClick={() => setFilters((p) => ({ ...p, page: p.page - 1 }))}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={filters.page + 1 >= totalPages}
              onClick={() => setFilters((p) => ({ ...p, page: p.page + 1 }))}
            >
              Next
            </Button>
          </div>
        </div>
      </Card>

      <Modal open={Boolean(detailsTx)} title="Transaction details" onClose={() => setDetailsTx(null)}>
        {detailsTx && (
          <div className="space-y-3 text-sm">
            <div className="grid grid-cols-2 gap-y-2.5">
              <span className="text-fg/45">Date</span>
              <span className="text-right font-mono tabular">{formatDate(detailsTx.transactionDate)}</span>

              <span className="text-fg/45">Description</span>
              <span className="text-right">{detailsTx.description}</span>

              <span className="text-fg/45">Merchant</span>
              <span className="text-right">{detailsTx.merchantName || '—'}</span>

              <span className="text-fg/45">Category</span>
              <span className="text-right">{categoryName(detailsTx.categoryId)}</span>

              <span className="text-fg/45">Category source</span>
              <span className="text-right">{SOURCE_META[detailsTx.categorySource]?.label || '—'}</span>

              <span className="text-fg/45">Confidence</span>
              <span className="text-right">
                {detailsTx.categoryConfidence != null ? `${detailsTx.categoryConfidence}%` : '—'}
              </span>

              <span className="text-fg/45">Origin</span>
              <span className="text-right">{TX_ORIGIN_META[detailsTx.source]?.label || '—'}</span>

              <span className="text-fg/45">Recurring</span>
              <span className="text-right">{detailsTx.isRecurring ? 'Yes' : 'No'}</span>

              <span className="text-fg/45">Amount</span>
              <span
                className={`text-right font-mono tabular font-semibold ${
                  detailsTx.type === 'CREDIT' ? 'text-credit' : 'text-debit'
                }`}
              >
                {detailsTx.type === 'CREDIT' ? '+' : '-'}
                {formatCurrency(Math.abs(detailsTx.amount))}
              </span>
            </div>

            {detailsTx.notes && (
              <div className="border-t border-white/10 pt-3">
                <p className="text-fg/45 mb-1">Notes</p>
                <p className="text-fg/80">{detailsTx.notes}</p>
              </div>
            )}

            {detailsTx.categoryReason && (
              <div className="border-t border-white/10 pt-3">
                <p className="text-fg/45 mb-1">Why this category</p>
                <p className="text-fg/80">{detailsTx.categoryReason}</p>
              </div>
            )}
          </div>
        )}
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Delete this transaction?"
        description={pendingDelete ? `"${pendingDelete.description}" will be permanently removed.` : ''}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
        loading={deleteTransaction.isPending}
      />

      <ConfirmDialog
        open={bulkConfirmOpen}
        title="Delete selected transactions?"
        description={`${selectedIds.size} selected transaction${selectedIds.size === 1 ? '' : 's'} will be permanently removed.`}
        confirmLabel="Delete"
        onConfirm={confirmBulkDeleteSelected}
        onCancel={() => setBulkConfirmOpen(false)}
        loading={deleteSelected.isPending}
      />

      <DeleteMonthDialog
        open={monthDialogOpen}
        onClose={() => setMonthDialogOpen(false)}
        onConfirm={confirmDeleteByMonth}
        loading={deleteByMonth.isPending}
      />

      <DeleteDateRangeDialog
        open={dateRangeDialogOpen}
        onClose={() => setDateRangeDialogOpen(false)}
        onConfirm={confirmDeleteByDateRange}
        loading={deleteByDateRange.isPending}
      />

      <CashExpenseModal
        open={cashModalOpen}
        onClose={() => setCashModalOpen(false)}
        onSuccess={handleCashExpenseSuccess}
        categories={categories ?? []}
      />
    </div>
  )
}