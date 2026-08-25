import { useState } from 'react'
import * as Icons from 'lucide-react'
import { Tags } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { EmptyState, ErrorState, Skeleton } from '@/components/ui/feedback'
import { useCategories, useAnalyticsByCategory } from '@/api/queries'

const formatCurrency = (value) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'INR' }).format(value ?? 0)

export default function Categories() {
  const [month, setMonth] = useState(() => new Date().toISOString().slice(0, 7))
  const range = (() => {
    const [year, m] = month.split('-').map(Number)
    const lastDay = new Date(year, m, 0).getDate()
    return { startDate: `${month}-01`, endDate: `${month}-${String(lastDay).padStart(2, '0')}` }
  })()

  const { data: categories, isLoading: loadingCategories, isError, refetch } = useCategories()
  const { data: breakdown, isLoading: loadingBreakdown } = useAnalyticsByCategory(range)

  const isLoading = loadingCategories || loadingBreakdown
  const statsByCategory = new Map((breakdown ?? []).map((b) => [b.categoryId, b]))

  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl">Categories</h1>
          <p className="text-fg/50 text-sm mt-1">How your spending is grouped, and how much each one costs you.</p>
        </div>
        <input
          type="month"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
          className="h-9 rounded-md border border-white/15 bg-elevated px-3 text-sm"
        />
      </div>

      {isError ? (
        <Card>
          <ErrorState message="Couldn't load categories." onRetry={refetch} />
        </Card>
      ) : isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <Card key={i} className="space-y-3">
              <Skeleton className="h-9 w-9 rounded-md" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-6 w-1/2" />
            </Card>
          ))}
        </div>
      ) : !categories?.length ? (
        <Card>
          <EmptyState icon={Tags} title="No categories yet" description="Categories are created by the AI categorization engine as transactions come in." />
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {categories.map((c) => {
            const stat = statsByCategory.get(c.id)
            const Icon = (c.icon && Icons[c.icon]) || Tags
            return (
              <Card key={c.id} className="space-y-3">
                <div
                  className="h-9 w-9 rounded-md flex items-center justify-center"
                  style={{ backgroundColor: `${c.color || '#C9A227'}22`, color: c.color || '#C9A227' }}
                >
                  <Icon size={17} />
                </div>
                <div>
                  <p className="font-medium text-fg">{c.name}</p>
                  <p className="text-xs text-fg/45 mt-0.5">
                    {stat?.transactionCount ?? 0} transaction{stat?.transactionCount === 1 ? '' : 's'} this month
                  </p>
                </div>
                <p className="font-mono tabular text-xl font-semibold text-fg">
                  {formatCurrency(stat?.totalAmount ?? 0)}
                </p>
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}
