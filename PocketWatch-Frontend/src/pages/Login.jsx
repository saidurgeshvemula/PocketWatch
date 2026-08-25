import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CurrencyRainHero } from '@/components/ui/currency-rain-hero'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/context/AuthContext'

export default function Login() {
  const { login } = useAuth()

  const navigate = useNavigate()
  const location = useLocation()

  const [serverError, setServerError] = useState('')

  const successMessage = location.state?.message

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm()

  const onSubmit = async (values) => {
    setServerError('')

    try {
      await login(values)
      navigate('/dashboard')
    } catch (err) {
      setServerError(
        err.response?.data?.message ||
          'Could not sign in. Check your credentials.'
      )
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
