import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CurrencyRainHero } from '@/components/ui/currency-rain-hero'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/context/AuthContext'

export default function Register() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()

  const [serverError, setServerError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm()

  const onSubmit = async (values) => {
    setServerError('')

    try {
      await registerUser(values)

      navigate('/login', {
        replace: true,
        state: {
          message:
            'Registration successful! Please verify your email using the link sent to your inbox before signing in.',
        },
      })
    } catch (err) {
      setServerError(
        err.response?.data?.message ||
          'Could not create your account. Please try again.'
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
          <h1 className="font-display text-4xl text-fg font-bold text-center mb-8 tracking-tight">
            Open an account
          </h1>

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="glass-panel rounded-card shadow-pop p-7 space-y-4"
          >
            {serverError && (
              <div className="bg-debit-light text-debit px-3 py-2 text-sm border border-debit/20 rounded-control">
                {serverError}
              </div>
            )}

            <div>
              <label className="text-sm font-medium text-fg-muted mb-1 block">
                Full name
              </label>

              <input
                type="text"
                autoComplete="name"
                className="w-full h-11 rounded-control border border-white/10 bg-elevated text-fg placeholder:text-fg-subtle px-3 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass focus-visible:border-brass/50"
                {...register('fullName', {
                  required: 'Full name is required',
                })}
              />

              {errors.fullName && (
                <p className="text-xs text-debit mt-1">
                  {errors.fullName.message}
                </p>
              )}
            </div>

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
                autoComplete="new-password"
                className="w-full h-11 rounded-control border border-white/10 bg-elevated text-fg placeholder:text-fg-subtle px-3 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass focus-visible:border-brass/50"
                {...register('password', {
                  required: 'Password is required',
                  minLength: {
                    value: 8,
                    message: 'Password must be at least 8 characters',
                  },
                })}
              />

              {errors.password && (
                <p className="text-xs text-debit mt-1">
                  {errors.password.message}
                </p>
              )}
            </div>

            <Button
              type="submit"
              variant="brass"
              size="lg"
              className="w-full"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Creating account...' : 'Create Account'}
            </Button>

            <p className="text-sm text-center text-fg-muted">
              Already have an account?{' '}
              <Link
                to="/login"
                className="font-medium text-brass underline underline-offset-4"
              >
                Sign In
              </Link>
            </p>
          </form>
        </motion.div>
      </div>
    </CurrencyRainHero>
  )
}