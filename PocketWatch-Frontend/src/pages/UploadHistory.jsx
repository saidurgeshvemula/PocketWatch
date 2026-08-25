import { useState } from 'react'
import { Link } from 'react-router-dom'
import { History, FileText, UploadCloud } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { StatusBadge, EmptyState, ErrorState, SkeletonRows } from '@/components/ui/feedback'
import { useUploadHistory } from '@/api/queries'

const STATUSES = ['ALL', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED']

const DEFAULT_FILTERS = { page: 0, size: 10, status: 'ALL' }

export default function UploadHistory() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const { data, isLoading, isError, isFetching, refetch } = useUploadHistory(filters)

  const updateFilter = (patch) => setFilters((prev) => ({ ...prev, ...patch, page: 0 }))

  const rows = data?.content ?? []
  const totalPages = data?.totalPages ?? 1

  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl">Upload history</h1>
          <p className="text-fg/50 text-sm mt-1">Every statement you've sent in, and how it was processed.</p>
        </div>
        <Button asChild variant="brass" className="gap-2">
          <Link to="/upload">
            <UploadCloud size={16} /> New upload
          </Link>
        </Button>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <select
            value={filters.status}
            onChange={(e) => updateFilter({ status: e.target.value })}
            className="h-9 rounded-md border border-white/15 bg-elevated px-2 text-sm"
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s === 'ALL' ? 'All statuses' : s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>
      </Card>

      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <div className="p-4">
            <SkeletonRows rows={5} cols={5} />
          </div>
        ) : isError ? (
          <ErrorState message="Couldn't load upload history." onRetry={refetch} />
        ) : rows.length === 0 ? (
          <EmptyState
            icon={History}
            title="No uploads yet"
            description="Statements you upload will show up here with their processing status."
            action={
              <Button asChild variant="brass" size="sm" className="gap-2">
                <Link to="/upload">
                  <UploadCloud size={14} /> Upload a statement
                </Link>
              </Button>
            }
          />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-fg/50 border-b border-white/10 uppercase text-xs tracking-wide">
                <th className="px-4 py-3 font-medium">File</th>
                <th className="px-4 py-3 font-medium">Type</th>
                <th className="px-4 py-3 font-medium">Uploaded</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium text-right">Transactions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <tr key={u.id} className="border-b border-white/5 last:border-0 hover:bg-elevated/[0.03] align-top">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <FileText size={14} className="text-fg/40 shrink-0" />
                      <span className="truncate max-w-[220px]">{u.originalFilename}</span>
                    </div>
                    {u.status === 'FAILED' && u.errorMessage && (
                      <p className="text-xs text-debit mt-1">{u.errorMessage}</p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-fg/60">{u.fileType ?? '—'}</td>
                  <td className="px-4 py-3 text-fg/60 font-mono text-xs tabular">
                    {u.uploadedAt ? new Date(u.uploadedAt).toLocaleString() : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={u.status} />
                  </td>
                  <td className="px-4 py-3 text-right font-mono tabular">
                    {u.status === 'COMPLETED' ? u.transactionsExtracted ?? 0 : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {rows.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-white/10">
            <p className="text-xs text-fg/50">
              Page {filters.page + 1} of {totalPages} {isFetching && '· updating…'}
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
        )}
      </Card>
    </div>
  )
}