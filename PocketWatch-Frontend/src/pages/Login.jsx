import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CurrencyRainHero } from '@/components/ui/currency-rain-hero'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/context/AuthContext'
import { useResendVerification } from '@/api/queries'

const RESEND_COOLDOWN_SECONDS = 60

export default function Login() {
  const { login } = useAuth()

  const navigate = useNavigate()
  const location = useLocation()

  const [serverError, setServerError] = useState('')
  const [unverifiedEmail, setUnverifiedEmail] = useState('')
  const [resendMessage, setResendMessage] = useState('')
  const [cooldown, setCooldown] = useState(0)

  const resendVerification = useResendVerification()
  const cooldownRef = useRef(null)

  const successMessage = location.state?.message

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm()

  useEffect(() => {
    return () => clearInterval(cooldownRef.current)
  }, [])

  const startCooldown = () => {
    setCooldown(RESEND_COOLDOWN_SECONDS)
    clearInterval(cooldownRef.current)
    cooldownRef.current = setInterval(() => {
      setCooldown((prev) => {
        if (prev <= 1) {
          clearInterval(cooldownRef.current)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  const onSubmit = async (values) => {
    setServerError('')
    setUnverifiedEmail('')
    setResendMessage('')

    try {
      await login(values)
      navigate('/dashboard')
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Could not sign in. Check your credentials.'

      setServerError(message)

      if (message.toLowerCase().includes('verify your email')) {
        setUnverifiedEmail(values.email)
      }
    }
  }

  const handleResend = async () => {
    if (!unverifiedEmail || cooldown > 0) return

    setResendMessage('')

    try {
      await resendVerification.mutateAsync(unverifiedEmail)
      setResendMessage(
        "Sent! Check your inbox (and spam folder) for the link."
      )
      startCooldown()
    } catch {
      setResendMessage('Could not resend right now. Please try again shortly.')
    }
  }

  return (
    <CurrencyRainHero>
      <div className="max-w-md mx-auto pt-40 sm:pt-36">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <h1 className="font-display text-4xl font-bold text-fg text-center mb-8 tracking-tight">
            Welcome Back
          </h1>

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="glass-panel rounded-card shadow-pop p-7 space-y-4"
          >
            {successMessage && (
              <div className="rounded-control bg-credit-light text-credit px-3 py-2 text-sm border border-credit/20">
                {successMessage}
              </div>
            )}

            {serverError && (
              <div className="rounded-control bg-debit-light text-debit px-3 py-2 text-sm border border-debit/20">
                {serverError}

                {unverifiedEmail && (
                  <div className="mt-2">
                    <button
                      type="button"
                      onClick={handleResend}
                      disabled={resendVerification.isPending || cooldown > 0}
                      className="text-xs font-medium text-brass underline underline-offset-4 disabled:opacity-50 disabled:no-underline disabled:cursor-not-allowed"
                    >
                      {cooldown > 0
                        ? `Resend available in ${cooldown}s`
                        : resendVerification.isPending
                        ? 'Sending...'
                        : "Didn't get the link? Resend it"}
                    </button>
                  </div>
                )}
              </div>
            )}

            {resendMessage && (
              <div className="rounded-control bg-credit-light text-credit px-3 py-2 text-sm border border-credit/20">
                {resendMessage}
              </div>
            )}

            <div>
              <label className="text-sm font-medium text-fg-muted mb-1 block">
                Email
              </label>

              <input
                type="email"
                autoComplete="email"
                className="w-full h-11 rounded-control border border-white/10 bg-elevated text-fg placeholder:text-fg-subtle px-3 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass focus-visible:border-brass/50"
                {...register('email', {
                  required: 'Email is required',
                })}
              />

              {errors.email && (
                <p className="text-xs text-debit mt-1">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium text-fg-muted mb-1 block">
                Password
              </label>

              <input
                type="password"
                autoComplete="current-password"
                className="w-full h-11 rounded-control border border-white/10 bg-elevated text-fg placeholder:text-fg-subtle px-3 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass focus-visible:border-brass/50"
                {...register('password', {
                  required: 'Password is required',
                })}
              />

              {errors.password && (
                <p className="text-xs text-debit mt-1">
                  {errors.password.message}
                </p>
              )}

              <Link
                to="/forgot-password"
                className="text-xs text-fg-subtle hover:text-brass underline underline-offset-4 mt-1 inline-block transition-colors"
              >
                Forgot Password?
              </Link>
            </div>

            <Button
              type="submit"
              variant="brass"
              size="lg"
              className="w-full"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Signing In...' : 'Sign In'}
            </Button>

            <p className="text-sm text-center text-fg-muted">
              Don't have an account?{' '}
              <Link
                to="/register"
                className="font-medium text-brass underline underline-offset-4"
              >
                Create One
              </Link>
            </p>
          </form>
        </motion.div>
      </div>
    </CurrencyRainHero>
  )

}