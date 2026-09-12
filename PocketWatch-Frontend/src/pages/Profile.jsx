import { CalendarDays, Mail, Receipt, Tags, LogOut } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useToast } from '@/components/ui/toast'
import { useProfile, useTransactions, useCategories, useLogoutAllDevices } from '@/api/queries'
import { useAuth } from '@/context/AuthContext'
import { formatDate } from '@/utils/date'

const initials = (fullName) => {
  if (!fullName) return '?'
  const parts = fullName.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase()
}

export default function Profile() {
  const { data: profile, isLoading } = useProfile()
  const { data: categories } = useCategories()
  const { logout } = useAuth()
  const { toast } = useToast()
  const navigate = useNavigate()

  const { data: txPage } = useTransactions({ page: 0, size: 1 })

  const logoutAllDevices = useLogoutAllDevices()

  const handleLogoutAllDevices = async () => {
    const confirmed = window.confirm(
      'This will sign you out on every device, including this one. Continue?'
    )
    if (!confirmed) return

    try {
      await logoutAllDevices.mutateAsync()
      toast({ title: 'Logged out from all devices', variant: 'success' })
      logout()
      navigate('/login')
    } catch (err) {
      toast({
        title: 'Something went wrong',
        description: err.response?.data?.message || 'Please try again.',
        variant: 'error',
      })
    }
  }

  return (
    <div className="mx-auto w-full max-w-4xl">
      <div>
        <h1 className="font-display text-3xl">Profile</h1>
        <p className="text-fg/50 text-sm mt-1">Your account details.</p>
      </div>

      <Card className="p-6">
        {isLoading ? (
          <p className="text-fg/40 text-sm">Loading profile…</p>
        ) : (
          <div className="flex items-start gap-4">
            <div className="flex items-center justify-center w-14 h-14 rounded-full bg-brass/15 text-brass font-display text-xl shrink-0">
              {initials(profile?.fullName)}
            </div>

            <div className="flex-1 min-w-0">
              <h2 className="font-display text-xl truncate">{profile?.fullName || '—'}</h2>
              <p className="flex items-center gap-1.5 text-fg/50 text-sm mt-1">
                <Mail size={13} />
                {profile?.email || '—'}
              </p>
            </div>
          </div>
        )}
      </Card>

      <Card className="p-6">
        <h3 className="font-display text-lg mb-4">Account overview</h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-9 h-9 rounded-full bg-white/5 text-fg/50 shrink-0">
              <CalendarDays size={16} />
            </div>
            <div>
              <p className="text-fg/45 text-xs uppercase tracking-wide">Member since</p>
              <p className="text-sm font-medium">
                {profile?.createdAt ? formatDate(profile.createdAt) : '—'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-9 h-9 rounded-full bg-white/5 text-fg/50 shrink-0">
              <Receipt size={16} />
            </div>
            <div>
              <p className="text-fg/45 text-xs uppercase tracking-wide">Transactions</p>
              <p className="text-sm font-medium tabular">
                {txPage?.totalElements ?? '—'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-9 h-9 rounded-full bg-white/5 text-fg/50 shrink-0">
              <Tags size={16} />
            </div>
            <div>
              <p className="text-fg/45 text-xs uppercase tracking-wide">Categories</p>
              <p className="text-sm font-medium tabular">{categories?.length ?? '—'}</p>
            </div>
          </div>
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="font-display text-lg mb-1">Security</h3>
        <p className="text-fg/50 text-sm mb-4">
          Signed in somewhere you don't recognize, or just lost your phone?
          This signs you out everywhere at once, including this device.
        </p>
        <Button
          variant="destructive"
          onClick={handleLogoutAllDevices}
          disabled={logoutAllDevices.isPending}
        >
          <LogOut size={16} className="mr-2" />
          {logoutAllDevices.isPending ? 'Logging out…' : 'Log out of all devices'}
        </Button>
      </Card>
    </div>
  )
}