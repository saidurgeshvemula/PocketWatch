import { useMemo } from 'react'
import { Sparkles, Loader2 } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useAiInsights, useTransactions } from '@/api/queries'

const formatCurrency = (value) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value ?? 0)

function monthToRange(monthStr) {
  const [year, month] = monthStr.split('-').map(Number)
  const startDate = `${monthStr}-01`
  const lastDay = new Date(year, month, 0).getDate()
  const endDate = `${monthStr}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
}

export default function Insights() {
  const { mutate, data, isPending, isError } = useAiInsights()

  const month =
    localStorage.getItem('dashboardMonth') ||
    new Date().toISOString().slice(0, 7)

  const bankAccountId =
    localStorage.getItem('dashboardBankAccountId') || null

  const range = monthToRange(month)

  /*
   * Cash is a separate entity from the selected bank account.
   * Reuses the existing transactions endpoint (source=CASH) and
   * sums client-side, so no new backend endpoint is needed.
   */
  const { data: cashTxns, isLoading: loadingCash } = useTransactions({
    page: 0,
    size: 1000,
    source: 'CASH',
    startDate: range.startDate,
    endDate: range.endDate,
  })

  const cashSummary = useMemo(() => {
    const rows = cashTxns?.content ?? []

    const income = rows
      .filter((t) => t.type === 'INCOME')
      .reduce((sum, t) => sum + Number(t.amount), 0)

    const expenses = rows
      .filter((t) => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + Number(t.amount), 0)

    return {
      income,
      expenses,
      net: income - expenses,
      count: rows.length,
    }
  }, [cashTxns])

  const handleGenerate = () => {
    mutate({ month, bankAccountId })
  }

  return (
    <div className="max-w-5xl mx-auto w-full space-y-6">
      <div>
        <h1 className="font-display text-3xl">AI Insights</h1>
        <p className="text-fg/50 text-sm mt-1">A plain-language read on your spending, generated from your recent transactions.</p>
      </div>

      <Card className="p-6">
        <p className="text-xs uppercase tracking-wider text-brass font-medium mb-3">
          Cash — {month} (separate from bank accounts)
        </p>

        {loadingCash ? (
          <p className="text-sm text-fg/50">Loading cash data…</p>
        ) : (
          <div className="grid grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-fg/50">Cash Income</p>
              <p className="text-fg font-semibold">{formatCurrency(cashSummary.income)}</p>
            </div>
            <div>
              <p className="text-fg/50">Cash Expenses</p>
              <p className="text-debit font-semibold">{formatCurrency(cashSummary.expenses)}</p>
            </div>
            <div>
              <p className="text-fg/50">Cash Net</p>
              <p className="text-fg font-semibold">{formatCurrency(cashSummary.net)}</p>
            </div>
          </div>
        )}
      </Card>

      <Card className="p-6 text-center">
        <Button variant="brass" size="lg" onClick={handleGenerate} disabled={isPending} className="gap-2">
          {isPending ? <Loader2 size={18} className="animate-spin" /> : <Sparkles size={18} />}
          {isPending ? 'Reading your ledger…' : 'Generate insights'}
        </Button>
      </Card>

      {isError && (
        <Card className="p-5 border-debit/30">
          <p className="text-sm text-debit">Couldn't generate insights right now. Try again in a moment.</p>
        </Card>
      )}

      {data && (
        <Card torn className="p-6 mb-3">
          <p className="text-xs uppercase tracking-wider text-brass font-medium mb-3">Your insights</p>
          <p className="text-fg/80 leading-relaxed whitespace-pre-line">{data}</p>
        </Card>
      )}
    </div>
  )
}