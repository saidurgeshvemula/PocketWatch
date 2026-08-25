import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { CurrencyRainHero } from '@/components/ui/currency-rain-hero'
import { Button } from '@/components/ui/button'
import { useResetPassword } from '@/api/queries'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const resetPassword = useResetPassword()

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await resetPassword.mutateAsync({ token, newPassword: password })
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message || 'That link is invalid or has expired.')
    }
  }

  if (!token) {
    return (
      <CurrencyRainHero>
        <div className="max-w-md mx-auto pt-40 sm:pt-36 text-center">
          <p className="bg-card rounded-card shadow-card p-7 text-sm text-debit">
            Missing reset token. Please use the link from your email.
          </p>
        </div>
      </CurrencyRainHero>
    )
  }

  return (
    <CurrencyRainHero>
      <div className="max-w-md mx-auto pt-40 sm:pt-36">
        <h1 className="font-display text-4xl text-fg text-center mb-8">Choose a new password</h1>
        <form onSubmit={onSubmit} className="bg-card rounded-card shadow-card p-7 space-y-4">
          {error && <p className="text-sm text-debit bg-debit-light rounded-md px-3 py-2">{error}</p>}
          <div>
            <label className="text-sm font-medium text-fg/70 mb-1 block">New password</label>
            <input
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            />
          </div>
          <Button type="submit" variant="brass" size="lg" className="w-full" disabled={resetPassword.isPending}>
            {resetPassword.isPending ? 'Saving…' : 'Reset password'}
          </Button>
          <p className="text-sm text-center text-fg/60">
            <Link to="/login" className="text-fg font-medium underline underline-offset-4">
              Back to sign in
            </Link>
          </p>
        </form>
      </div>
    </CurrencyRainHero>
  )
}