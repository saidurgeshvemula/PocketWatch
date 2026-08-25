import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Landmark, Plus, Pencil, Trash2 } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Modal } from '@/components/ui/modal'
import { ConfirmDialog, EmptyState, ErrorState, SkeletonRows } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast'
import {
  useBankAccounts,
  useCreateBankAccount,
  useUpdateBankAccount,
  useDeleteBankAccount,
} from '@/api/queries'

const ACCOUNT_TYPES = ['CHECKING', 'SAVINGS', 'CREDIT_CARD']

function AccountForm({ initial, onSubmit, submitting }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ defaultValues: initial || { nickname: '', bankName: '', accountNumber: '', accountType: 'CHECKING' } })

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
      <div>
        <label className="text-sm font-medium text-fg/70 mb-1 block">Nickname</label>
        <input
          className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          placeholder="e.g. Everyday spending"
          {...register('nickname', { required: 'Nickname is required' })}
        />
        {errors.nickname && <p className="text-xs text-debit mt-1">{errors.nickname.message}</p>}
      </div>

      <div>
        <label className="text-sm font-medium text-fg/70 mb-1 block">Bank name</label>
        <input
          className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          placeholder="e.g. HDFC Bank"
          {...register('bankName', { required: 'Bank name is required' })}
        />
        {errors.bankName && <p className="text-xs text-debit mt-1">{errors.bankName.message}</p>}
      </div>

      <div>
        <label className="text-sm font-medium text-fg/70 mb-1 block">Account number</label>
        <input
          className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          placeholder="Full or last 4 digits"
          {...register('accountNumber', { required: 'Account number is required' })}
        />
        {errors.accountNumber && <p className="text-xs text-debit mt-1">{errors.accountNumber.message}</p>}
      </div>

      <div>
        <label className="text-sm font-medium text-fg/70 mb-1 block">Account type</label>
        <select
          className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
          {...register('accountType')}
        >
          {ACCOUNT_TYPES.map((t) => (
            <option key={t} value={t}>
              {t.replace('_', ' ')}
            </option>
          ))}
        </select>
      </div>

      <Button type="submit" variant="brass" className="w-full mt-1" disabled={submitting}>
        {submitting ? 'Saving…' : initial ? 'Save changes' : 'Add account'}
      </Button>
    </form>
  )
}

export default function BankAccounts() {
  const { toast } = useToast()
  const { data, isLoading, isError, refetch } = useBankAccounts()
  const createAccount = useCreateBankAccount()
  const updateAccount = useUpdateBankAccount()
  const deleteAccount = useDeleteBankAccount()

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [pendingDelete, setPendingDelete] = useState(null)

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
  }

  const handleSubmit = async (values) => {
    try {
      if (editing) {
        await updateAccount.mutateAsync({ id: editing.id, ...values })
        toast({ title: 'Account updated', variant: 'success' })
      } else {
        await createAccount.mutateAsync(values)
        toast({ title: 'Account added', variant: 'success' })
      }
      closeForm()
    } catch (err) {
      toast({
        title: 'Could not save account',
        description: err.response?.data?.message || 'Please try again.',
        variant: 'error',
      })
    }
  }

  const handleDelete = async () => {
    try {
      await deleteAccount.mutateAsync(pendingDelete.id)
      toast({ title: 'Account removed', variant: 'success' })
      setPendingDelete(null)
    } catch (err) {
      toast({
        title: 'Could not remove account',
        description: err.response?.data?.message || 'It may have transactions linked to it.',
        variant: 'error',
      })
    }
  }

  const accounts = data ?? []

  return (
    <div className="mx-auto w-full max-w-[1600px]">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl">Bank accounts</h1>
          <p className="text-fg/50 text-sm mt-1">Connect the accounts you want to track statements for.</p>
        </div>
        <Button variant="brass" className="gap-2" onClick={() => setFormOpen(true)}>
          <Plus size={16} /> Add account
        </Button>
      </div>

      <Card>
        {isLoading ? (
          <SkeletonRows rows={3} cols={4} />
        ) : isError ? (
          <ErrorState message="Couldn't load bank accounts." onRetry={refetch} />
        ) : accounts.length === 0 ? (
          <EmptyState
            icon={Landmark}
            title="No bank accounts yet"
            description="Add one so you can pick it when uploading a statement."
            action={
              <Button variant="brass" size="sm" className="gap-2" onClick={() => setFormOpen(true)}>
                <Plus size={14} /> Add account
              </Button>
            }
          />
        ) : (
          <div className="divide-y divide-white/8">
            {accounts.map((acc) => (
              <div key={acc.id} className="flex items-center justify-between py-3.5 first:pt-0 last:pb-0">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="h-9 w-9 rounded-md bg-brass/15 text-brass flex items-center justify-center shrink-0">
                    <Landmark size={16} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-fg truncate">{acc.nickname}</p>
                    <p className="text-xs text-fg/50 truncate">
                      {acc.bankName} · {acc.accountNumber} · {acc.accountType?.replace('_', ' ')}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => {
                      setEditing(acc)
                      setFormOpen(true)
                    }}
                  >
                    <Pencil size={15} />
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => setPendingDelete(acc)}>
                    <Trash2 size={15} className="text-debit" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Modal open={formOpen} title={editing ? 'Edit account' : 'Add bank account'} onClose={closeForm}>
        <AccountForm
          initial={editing || undefined}
          onSubmit={handleSubmit}
          submitting={createAccount.isPending || updateAccount.isPending}
        />
      </Modal>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Remove this account?"
        description={`"${pendingDelete?.nickname}" will no longer be selectable for new uploads. Existing transactions are kept.`}
        confirmLabel="Remove"
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
        loading={deleteAccount.isPending}
      />
    </div>
  )
}
