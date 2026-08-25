import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from './axios'
import { apiDate } from '@/utils/date'

const unwrap = (res) => res.data.data

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: async () => unwrap(await api.get('/categories')),
  })
}

export function useAnalyticsSummary(range = {}) {
  return useQuery({
    queryKey: ['analytics', 'summary', range],
    queryFn: async () =>
      unwrap(
        await api.get('/analytics/summary', {
          params: {
            ...range,
            startDate: apiDate(range.startDate),
            endDate: apiDate(range.endDate),
          },
        }),
      ),
  })
}

export function useAnalyticsMonthly(bankAccountId) {
  return useQuery({
    queryKey: ['analytics', 'monthly', bankAccountId],
    queryFn: async () =>
      unwrap(
        await api.get('/analytics/monthly', {
          params: { bankAccountId: bankAccountId || undefined },
        }),
      ),
  })
}

export function useAnalyticsByCategory(range = {}) {
  return useQuery({
    queryKey: ['analytics', 'by-category', range],
    queryFn: async () =>
      unwrap(
        await api.get('/analytics/by-category', {
          params: {
            ...range,
            startDate: apiDate(range.startDate),
            endDate: apiDate(range.endDate),
          },
        }),
      ),
  })
}

export function useTransactions(filters) {
  const { page, size, search, categoryId, type, source, bankAccountId, startDate, endDate, sort } = filters
  return useQuery({
    queryKey: ['transactions', filters],
    queryFn: async () =>
      unwrap(
        await api.get('/transactions', {
          params: {
            page,
            size,
            sort: sort || 'transactionDate,desc',
            search: search || undefined,
            categoryId: categoryId || undefined,
            type: type && type !== 'ALL' ? type : undefined,
            source: source && source !== 'ALL' ? source : undefined,
            bankAccountId: bankAccountId || undefined,
            startDate: startDate ? apiDate(startDate) : undefined,
            endDate: endDate ? apiDate(endDate) : undefined,
          },
        }),
      ),
    keepPreviousData: true,
  })
}

export function useDeleteTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (id) => unwrap(await api.delete(`/transactions/${id}`)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  })
}

export function useUpdateTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, ...body }) => unwrap(await api.put(`/transactions/${id}`, body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  })
}

export function useDeleteTransactionsByMonth() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ year, month }) =>
      unwrap(await api.delete('/transactions/month', { data: { year, month } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  })
}

export function useDeleteTransactionsByDateRange() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ startDate, endDate }) =>
      unwrap(
        await api.delete('/transactions/date-range', {
          data: {
            startDate: apiDate(startDate),
            endDate: apiDate(endDate),
          },
        }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  })
}

export function useDeleteSelectedTransactions() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (ids) =>
      unwrap(await api.delete('/transactions/bulk', { data: { transactionIds: ids } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  })
}

export function useCreateCashExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body) => unwrap(await api.post('/transactions/cash', body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  })
}

export function useUploadCsv() {
  return useMutation({
    mutationFn: async ({ file, bankAccountId }) => {
      const form = new FormData()
      form.append('file', file)
      if (bankAccountId) form.append('bankAccountId', bankAccountId)
      return unwrap(
        await api.post('/uploads', form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        }),
      )
    },
  })
}

export function useUploadStatus(uploadId, { enabled }) {
  return useQuery({
    queryKey: ['uploads', uploadId, 'status'],
    queryFn: async () => unwrap(await api.get(`/uploads/${uploadId}/status`)),
    enabled: Boolean(uploadId) && enabled,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'COMPLETED' || status === 'FAILED' ? false : 2000
    },
  })
}

export function useAiInsights() {
  return useMutation({
    mutationFn: async ({ month, bankAccountId }) =>
      unwrap(
        await api.get('/ai/insights', {
          params: {
            month,
            bankAccountId: bankAccountId || null,
          },
        })
      ),
  })
}
export function useChatHistory() {
  return useQuery({
    queryKey: ['ai', 'chat', 'history'],
    queryFn: async () => unwrap(await api.get('/ai/chat/history')),
  })
}

export function useSendChatMessage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ message, bankAccountId }) =>
      unwrap(
        await api.post('/ai/chat', {
          message,
          bankAccountId: bankAccountId || null,
        }),
      ),

    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ['ai', 'chat', 'history'],
      }),
  })
}

export function useBankAccounts() {
  return useQuery({
    queryKey: ['bank-accounts'],
    queryFn: async () => unwrap(await api.get('/accounts')),
  })
}

export function useCreateBankAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body) => unwrap(await api.post('/accounts', body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }),
  })
}

export function useUpdateBankAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, ...body }) => unwrap(await api.put(`/accounts/${id}`, body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }),
  })
}

export function useDeleteBankAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (id) => unwrap(await api.delete(`/accounts/${id}`)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }),
  })
}

export function useUploadHistory(filters) {
  const { page = 0, size = 10, status } = filters
  return useQuery({
    queryKey: ['uploads', 'history'],
    queryFn: async () => unwrap(await api.get('/uploads')),
    keepPreviousData: true,
    select: (uploads) => {
      let rows = [...uploads].sort(
        (a, b) => new Date(b.uploadedAt) - new Date(a.uploadedAt),
      )
      if (status && status !== 'ALL') rows = rows.filter((u) => u.status === status)
      const start = page * size
      return {
        content: rows.slice(start, start + size),
        totalElements: rows.length,
        totalPages: Math.max(1, Math.ceil(rows.length / size)),
        number: page,
      }
    },
  })
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: async (email) => unwrap(await api.post('/auth/forgot-password', { email })),
  })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: async ({ token, newPassword }) =>
      unwrap(await api.post('/auth/reset-password', { token, newPassword })),
  })
}

export function useVerifyEmail() {
  return useMutation({
    mutationFn: async (token) => unwrap(await api.post('/auth/verify-email', { token })),
  })
}

export function useResendVerification() {
  return useMutation({
    mutationFn: async (email) => unwrap(await api.post('/auth/resend-verification', { email })),
  })
}

export function useProfile() {
  return useQuery({
    queryKey: ['profile'],
    queryFn: async () => unwrap(await api.get('/auth/me')),
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body) => unwrap(await api.put('/auth/me', body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['profile'] }),
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: async (body) => unwrap(await api.put('/auth/me/password', body)),
  })
}