import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CurrencyRainHero } from '@/components/ui/currency-rain-hero'
import { useVerifyEmail } from '@/api/queries'

export default function VerifyEmail() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const verifyEmail = useVerifyEmail()
  const [status, setStatus] = useState('pending')

  useEffect(() => {
    if (!token) {
      setStatus('error')
      return
    }
    verifyEmail
      .mutateAsync(token)
      .then(() => setStatus('success'))
      .catch(() => setStatus('error'))

  }, [token])

  return (
    <CurrencyRainHero>
      <div className="max-w-md mx-auto pt-40 sm:pt-36 text-center">
        <div className="bg-card rounded-card shadow-card p-7">
          {status === 'pending' && <p className="text-sm text-fg/70">Verifying your email…</p>}
          {status === 'success' && (
            <p className="text-sm text-fg/70">Your email is verified. You're all set.</p>
          )}
          {status === 'error' && (
            <p className="text-sm text-debit">That verification link is invalid or has expired.</p>
          )}
          <Link to="/login" className="text-fg font-medium underline underline-offset-4 text-sm block mt-4">
            Go to login page
          </Link>
        </div>
      </div>
    </CurrencyRainHero>
  )
}