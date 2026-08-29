import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  AreaChart,
  Area,
} from 'recharts'
import {
  Sparkles,
  Loader2,
  ArrowUpRight,
  TrendingUp,
  TrendingDown,
  Wallet,
} from 'lucide-react'

import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

import {
  useAnalyticsSummary,
  useAnalyticsByCategory,
  useAnalyticsMonthly,
  useAiInsights,
  useBankAccounts,
} from '@/api/queries'

import { useAuth } from '@/context/AuthContext'
import api from '@/api/axios'
import { parseDate } from '@/utils/date'

const PIE_COLORS = [
  '#F5B942',
  '#22C55E',
  '#3B82F6',
  '#EF4444',
  '#A855F7',
  '#06B6D4',
  '#F97316',
  '#EC4899',
  '#84CC16',
  '#14B8A6',
  '#6366F1',
  '#EAB308',
  '#8B5CF6',
  '#10B981',
  '#F43F5E',
  '#78716C',
]

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

  return {
    startDate,
    endDate,
  }
}

function greeting() {
  const h = new Date().getHours()

  if (h < 12) return 'Good morning'
  if (h < 18) return 'Good afternoon'

  return 'Good evening'
}

const fadeUp = {
  hidden: {
    opacity: 0,
    y: 14,
  },

  show: (i = 0) => ({
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.45,
      delay: i * 0.06,
      ease: 'easeOut',
    },
  }),
}

const VIEWS = [
  {
    id: 'category',
    label: 'Category breakdown',
  },
  {
    id: 'trend',
    label: 'Monthly trend',
  },
]

function Stat({
  label,
  value,
  icon: Icon,
  tone,
  loading,
}) {
  return (
    <div className="flex items-center gap-3">
      <div
        className={`w-9 h-9 rounded-control bg-white/[0.04] flex items-center justify-center ${tone}`}
      >
        <Icon size={16} />
      </div>

      <div>
        <p className="text-[11px] uppercase tracking-wider text-fg-subtle">
          {label}
        </p>

        <p
          className={`font-mono tabular text-lg font-semibold ${tone}`}
        >
          {loading
            ? '—'
            : formatCurrency(value)}
        </p>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()

  /*
   * ============================================================
   * DASHBOARD SELECTION
   * ============================================================
   *
   * These values are also stored in localStorage so that
   * AI Chat can use the exact same month and account.
   */

  const [month, setMonth] = useState(() =>
    localStorage.getItem('dashboardMonth') ||
    new Date().toISOString().slice(0, 7)
  )

  const [view, setView] = useState('category')

  const [bankAccountId, setBankAccountId] = useState(() =>
    localStorage.getItem('dashboardBankAccountId') || ''
  )

  /*
   * ============================================================
   * INITIAL MONTH
   * ============================================================
   *
   * If a Dashboard month was already selected previously,
   * keep it.
   *
   * Only determine the latest transaction month when there
   * is no saved Dashboard month.
   */

  useEffect(() => {
    const savedMonth =
      localStorage.getItem('dashboardMonth')

    if (savedMonth) {
      setMonth(savedMonth)
      return
    }

    let cancelled = false

    api
      .get('/transactions', {
        params: {
          page: 0,
          size: 1,
          sort: 'transactionDate,desc',
        },
      })
      .then((res) => {
        const latest =
          res.data.data?.content?.[0]

        if (latest && !cancelled) {
          const latestMonth =
            parseDate(
              latest.transactionDate
            ).slice(0, 7)

          setMonth(latestMonth)

          localStorage.setItem(
            'dashboardMonth',
            latestMonth
          )
        }
      })
      .catch(() => {})

    return () => {
      cancelled = true
    }
  }, [])

  /*
   * ============================================================
   * SAVE MONTH
   * ============================================================
   */

  useEffect(() => {
    if (month) {
      localStorage.setItem(
        'dashboardMonth',
        month
      )
    }
  }, [month])

  /*
   * ============================================================
   * SAVE BANK ACCOUNT
   * ============================================================
   */

  useEffect(() => {
    localStorage.setItem(
      'dashboardBankAccountId',
      bankAccountId
    )
  }, [bankAccountId])

  /*
   * ============================================================
   * ANALYTICS
   * ============================================================
   */

  const range = monthToRange(month)

  const { data: bankAccounts } =
    useBankAccounts()

  const {
    data: summary,
    isLoading: loadingSummary,
  } = useAnalyticsSummary({
    ...range,
    bankAccountId,
  })

  const {
    data: byCategory,
    isLoading: loadingCategory,
  } = useAnalyticsByCategory({
    ...range,
    bankAccountId,
  })

  const {
    data: monthly,
    isLoading: loadingMonthly,
  } = useAnalyticsMonthly(
    bankAccountId
  )

  /*
   * ============================================================
   * AI INSIGHTS
   * ============================================================
   */

  const {
    mutate: generateInsight,
    data: insight,
    isPending: insightPending,
  } = useAiInsights()

  const firstName =
    user?.fullName?.split(' ')[0] ||
    'there'

  return (
    <div className="max-w-[1600px] mx-auto space-y-6">

      {/* ======================================================
          HEADER
          ====================================================== */}

      <motion.div
        variants={fadeUp}
        initial="hidden"
        animate="show"
        custom={0}
        className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4"
      >
        <div>
          <p className="text-white text-sm mb-1">
            {greeting()}, {firstName}
          </p>

          <h1 className="font-display text-3xl font-bold text-fg tracking-tight">
            {summary?.month
              ? `Your ${summary.month} in numbers`
              : 'Your accounts at a glance'}
          </h1>
        </div>

        {/* ==================================================
            DASHBOARD FILTERS
            ================================================== */}

        <div className="flex items-center gap-2">

          <select
            value={bankAccountId}
            onChange={(e) => {
              const value = e.target.value

              setBankAccountId(value)

              localStorage.setItem(
                'dashboardBankAccountId',
                value
              )
            }}
            className="h-10 rounded-control border border-white/10 bg-elevated text-fg px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          >
            <option value="">
              All accounts
            </option>

            {(bankAccounts ?? []).map((a) => (
              <option
                key={a.id}
                value={a.id}
              >
                {a.nickname || a.bankName}
              </option>
            ))}
          </select>

          <input
            type="month"
            value={month}
            onChange={(e) => {
              const value = e.target.value

              setMonth(value)

              localStorage.setItem(
                'dashboardMonth',
                value
              )
            }}
            className="h-10 rounded-control border border-white/10 bg-elevated text-fg px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          />
        </div>
      </motion.div>

      {/* ======================================================
          MAIN GRID
          ====================================================== */}

      <div className="grid grid-cols-1 xl:grid-cols-[1fr_400px] gap-6 items-start">

        {/* ====================================================
            ANALYTICS CARD
            ==================================================== */}

        <motion.div
          variants={fadeUp}
          initial="hidden"
          animate="show"
          custom={1}
        >
          <Card className="relative overflow-hidden">

            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">

              <div className="flex items-center gap-6 flex-wrap">

                <Stat
                  label="Net savings"
                  value={summary?.netSavings}
                  icon={Wallet}
                  tone="text-fg"
                  loading={loadingSummary}
                />

                <Stat
                  label="Income"
                  value={summary?.totalIncome}
                  icon={TrendingUp}
                  tone="text-credit"
                  loading={loadingSummary}
                />

                <Stat
                  label="Expenses"
                  value={summary?.totalExpenses}
                  icon={TrendingDown}
                  tone="text-debit"
                  loading={loadingSummary}
                />

              </div>

              <div className="flex gap-1 bg-elevated rounded-control p-1 self-start sm:self-auto">

                {VIEWS.map((v) => (
                  <button
                    key={v.id}
                    onClick={() =>
                      setView(v.id)
                    }
                    className={`px-3 py-1.5 rounded-[8px] text-xs font-medium transition-colors ${
                      view === v.id
                        ? 'bg-gold-gradient text-ink'
                        : 'text-fg-muted hover:text-fg'
                    }`}
                  >
                    {v.label}
                  </button>
                ))}

              </div>
            </div>

            <div className="h-[380px]">

              <AnimatePresence mode="wait">

                {view === 'category' ? (

                  <motion.div
                    key="category"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.25 }}
                    className="h-full flex flex-col sm:flex-row items-center gap-6"
                  >

                    <div className="w-full sm:w-1/2 h-full">

                      {loadingCategory ? (

                        <div className="h-full flex items-center justify-center text-fg-subtle text-sm">
                          Loading…
                        </div>

                      ) : !byCategory?.length ? (

                        <div className="h-full flex items-center justify-center text-fg-subtle text-sm text-center px-6">
                          No categorized transactions for {month}.
                        </div>

                      ) : (

                        <ResponsiveContainer
                          width="100%"
                          height="100%"
                        >
                          <PieChart>

                            <Pie
                              data={byCategory}
                              dataKey="totalAmount"
                              nameKey="categoryName"
                              innerRadius={78}
                              outerRadius={128}
                              paddingAngle={3}
                              stroke="none"
                              animationDuration={700}
                            >

                              {byCategory.map(
                                (_, i) => (
                                  <Cell
                                    key={i}
                                    fill={
                                      PIE_COLORS[
                                        i %
                                        PIE_COLORS.length
                                      ]
                                    }
                                  />
                                )
                              )}

                            </Pie>

                            <Tooltip
                              formatter={(v) =>
                                formatCurrency(v)
                              }
                              contentStyle={{
                                background:
                                  '#18181B',
                                border:
                                  '1px solid rgba(255,255,255,0.1)',
                                borderRadius: 10,
                                color: '#FFFFFF',
                              }}
                              labelStyle={{
                                color: '#FFFFFF',
                              }}
                              itemStyle={{
                                color: '#FFFFFF',
                              }}
                            />

                          </PieChart>
                        </ResponsiveContainer>
                      )}

                    </div>

                    <div className="w-full sm:w-1/2 space-y-2.5 max-h-full overflow-y-auto ledger-scrollbar pr-1">

                      {byCategory?.map(
                        (c, i) => (
                          <div
                            key={c.categoryName}
                            className="flex items-center justify-between text-sm py-1.5 border-b border-white/[0.04]"
                          >

                            <div className="flex items-center gap-2 min-w-0">

                              <span
                                className="w-2.5 h-2.5 rounded-full shrink-0"
                                style={{
                                  background:
                                    PIE_COLORS[
                                      i %
                                      PIE_COLORS.length
                                    ],
                                }}
                              />

                              <span className="text-fg-muted truncate">
                                {c.categoryName}
                              </span>

                            </div>

                            <span className="font-mono tabular text-fg shrink-0 ml-3">
                              {formatCurrency(
                                c.totalAmount
                              )}
                            </span>

                          </div>
                        )
                      )}

                    </div>

                  </motion.div>

                ) : (

                  <motion.div
                    key="trend"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.25 }}
                    className="h-full"
                  >

                    {loadingMonthly ? (

                      <div className="h-full flex items-center justify-center text-fg-subtle text-sm">
                        Loading…
                      </div>

                    ) : (

                      <ResponsiveContainer
                        width="100%"
                        height="100%"
                      >

                        <AreaChart
                          data={monthly ?? []}
                        >

                          <defs>
                            <linearGradient
                              id="areaGold"
                              x1="0"
                              y1="0"
                              x2="0"
                              y2="1"
                            >
                              <stop
                                offset="0%"
                                stopColor="#F5B942"
                                stopOpacity={0.35}
                              />

                              <stop
                                offset="100%"
                                stopColor="#F5B942"
                                stopOpacity={0}
                              />
                            </linearGradient>
                          </defs>

                          <CartesianGrid
                            vertical={false}
                            stroke="rgba(255,255,255,0.06)"
                          />

                          <XAxis
                            dataKey="month"
                            tick={{
                              fontSize: 12,
                              fill: '#9CA3AF',
                            }}
                            axisLine={false}
                            tickLine={false}
                          />

                          <YAxis
                            tick={{
                              fontSize: 12,
                              fill: '#9CA3AF',
                            }}
                            axisLine={false}
                            tickLine={false}
                          />

                          <Tooltip
                            formatter={(v) =>
                              formatCurrency(v)
                            }
                            cursor={{
                              stroke:
                                'rgba(245,185,66,0.3)',
                            }}
                            contentStyle={{
                              background:
                                '#1F2228',
                              border:
                                '1px solid rgba(255,255,255,0.1)',
                              borderRadius: 10,
                              color: '#FAFAFA',
                            }}
                            labelStyle={{
                              color: '#9CA3AF',
                            }}
                          />

                          <Area
                            type="monotone"
                            dataKey="totalExpenses"
                            stroke="#F5B942"
                            strokeWidth={2}
                            fill="url(#areaGold)"
                            name="Expenses"
                            animationDuration={700}
                          />

                        </AreaChart>

                      </ResponsiveContainer>
                    )}

                  </motion.div>
                )}

              </AnimatePresence>

            </div>

          </Card>
        </motion.div>

        {/* ====================================================
            AI SUMMARY
            ==================================================== */}

        <motion.div
          variants={fadeUp}
          initial="hidden"
          animate="show"
          custom={2}
        >
          <Card
            glass
            className="h-full flex flex-col min-h-[460px]"
          >

            <div className="flex items-center gap-2.5 mb-4">

              <div className="w-8 h-8 rounded-control bg-gold-gradient flex items-center justify-center shadow-glow shrink-0">
                <Sparkles
                  size={14}
                  className="text-ink"
                />
              </div>

              <div>
                <p className="text-sm font-semibold text-fg">
                  Executive summary
                </p>

                <p className="text-[11px] text-fg-subtle">
                  Generated from your transactions
                </p>
              </div>

            </div>

            <div className="flex-1 overflow-y-auto ledger-scrollbar pr-1">

              {insight ? (

                <motion.p
                  initial={{
                    opacity: 0,
                    y: 6,
                  }}
                  animate={{
                    opacity: 1,
                    y: 0,
                  }}
                  className="text-[15px] leading-relaxed text-fg/90 whitespace-pre-line font-display"
                >
                  {insight}
                </motion.p>

              ) : (

                <p className="text-sm text-fg-muted leading-relaxed">
                   No summary yet.
                </p>

              )}

            </div>

            <div className="pt-4 mt-4 border-t border-white/[0.06] space-y-2">

              {/* ==================================================
                  AI INSIGHTS
                  ================================================== */}

              <Button
                variant="brass"
                className="w-full gap-2"
                onClick={() =>
                  generateInsight({
                    month,
                    bankAccountId:
                      bankAccountId || null,
                  })
                }
                disabled={insightPending}
              >

                {insightPending ? (
                  <Loader2
                    size={14}
                    className="animate-spin"
                  />
                ) : (
                  <Sparkles size={14} />
                )}

                {insight
                  ? 'Generate new summary'
                  : 'Generate summary'}

              </Button>

              {/* ==================================================
                  AI CHAT
                  ================================================== */}

              <button
                onClick={() => {
                  /*
                   * Make absolutely sure the latest Dashboard
                   * selection is available to Chat before navigating.
                   */

                  localStorage.setItem(
                    'dashboardMonth',
                    month
                  )

                  localStorage.setItem(
                    'dashboardBankAccountId',
                    bankAccountId
                  )

                  navigate('/chat')
                }}
                className="w-full flex items-center justify-center gap-1.5 text-xs text-fg-muted hover:text-brass py-2 transition-colors"
              >
                Open AI Copilot
                <ArrowUpRight size={12} />
              </button>

            </div>

          </Card>
        </motion.div>

      </div>
    </div>
  )
}