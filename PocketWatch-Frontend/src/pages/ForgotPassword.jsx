import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CurrencyRainHero } from '@/components/ui/currency-rain-hero'
import { Button } from '@/components/ui/button'
import { useForgotPassword } from '@/api/queries'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const forgotPassword = useForgotPassword()

  const onSubmit = async (e) => {
    e.preventDefault()
    await forgotPassword.mutateAsync(email)
    setSent(true)
  }

  return (
    <CurrencyRainHero>
      <div className="max-w-md mx-auto pt-40 sm:pt-36">
        <h1 className="font-display text-4xl text-fg text-center mb-8">Reset your password</h1>
        <div className="bg-card rounded-card shadow-card p-7 space-y-4">
          {sent ? (
            <p className="text-sm text-fg/70">
              If an account exists for <strong>{email}</strong>, a reset link has been sent. Check your inbox.
            </p>
          ) : (
            <form onSubmit={onSubmit} className="space-y-4">
              <div>
                <label className="text-sm font-medium text-fg/70 mb-1 block">Email</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
                />
              </div>
              <Button type="submit" variant="brass" size="lg" className="w-full" disabled={forgotPassword.isPending}>
                {forgotPassword.isPending ? 'Sending…' : 'Send reset link'}
              </Button>
            </form>
          )}
          <p className="text-sm text-center text-fg/60">
            <Link to="/login" className="text-fg font-medium underline underline-offset-4">
              Back to sign in
            </Link>
          </p>
        </div>
      </div>
    </CurrencyRainHero>
  )
}