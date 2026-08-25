import { NavLink, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  LayoutDashboard,
  UploadCloud,
  MessageCircle,
  Tags,
  Landmark,
  ArrowLeftRight,
  History,
  User,
  LogOut,
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { cn } from '@/lib/utils'
import pocketWatchIcon from '@/assets/pocketwatch-icon.png'

const dockLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight},
  { to: '/upload', label: 'Upload', icon: UploadCloud },
  { to: '/chat', label: 'PocketWatch AI', icon: MessageCircle },
  { to: '/categories', label: 'Categories', icon: Tags },
  { to: '/bank-accounts', label: 'Accounts', icon: Landmark },
  { to: '/upload-history', label: 'History', icon: History },
]

function DockItem({ to, label, icon: Icon }) {
  return (
    <NavLink to={to} className="group relative flex flex-col items-center">
      {({ isActive }) => (
        <motion.div
          whileHover={{ y: -6, scale: 1.08 }}
          transition={{ type: 'spring', stiffness: 400, damping: 22 }}
          className="relative flex flex-col items-center"
        >
          <div
            className={cn(
              'relative flex items-center justify-center w-11 h-11 rounded-2xl transition-colors duration-200',
              isActive
                ? 'bg-gold-gradient shadow-glow'
                : 'bg-white/[0.04] group-hover:bg-white/[0.09]',
            )}
          >
            <Icon
              size={18}
              strokeWidth={2.2}
              className={
                isActive
                  ? 'text-ink'
                  : 'text-fg-muted group-hover:text-fg'
              }
            />

            {isActive && (
              <motion.span
                layoutId="dock-active-dot"
                className="absolute -bottom-2 w-1 h-1 rounded-full bg-brass"
                transition={{
                  type: 'spring',
                  stiffness: 500,
                  damping: 40,
                }}
              />
            )}
          </div>

          <span className="pointer-events-none absolute -top-9 whitespace-nowrap rounded-md bg-elevated border border-white/[0.08] px-2 py-1 text-[11px] text-fg opacity-0 shadow-pop transition-opacity duration-150 group-hover:opacity-100">
            {label}
          </span>
        </motion.div>
      )}
    </NavLink>
  )
}

export function Dock() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <>
      {}
      <div className="fixed top-5 left-0 right-0 z-40">
        <div className="flex w-full items-center justify-between px-6 sm:px-10">

          {}
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl overflow-hidden shadow-glow shrink-0">
              <img src={pocketWatchIcon} alt="PocketWatch" className="w-full h-full object-cover" />
            </div>

            <p className="font-display text-base font-bold tracking-tight text-fg">
              PocketWatch
            </p>
          </div>

          {}
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/profile')}
              className="flex items-center gap-2 pl-1 pr-3 py-1 rounded-full bg-surface/70 backdrop-blur-xl border border-white/[0.07] hover:border-brass/30 transition-colors"
            >
              <span className="w-6 h-6 rounded-full bg-elevated flex items-center justify-center">
                <User size={12} className="text-fg" />
              </span>

              <span className="text-xs text-fg max-w-[120px] truncate">
                {user?.fullName || user?.email || 'Account'}
              </span>
            </button>

            <button
              onClick={logout}
              className="w-8 h-8 rounded-full bg-surface/70 backdrop-blur-xl border border-white/[0.07] hover:border-debit/40 hover:text-debit flex items-center justify-center text-fg transition-colors"
              title="Sign out"
            >
              <LogOut size={14} />
            </button>
          </div>

        </div>
      </div>

      {}
      <motion.nav
        initial={{ y: 40, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{
          type: 'spring',
          stiffness: 260,
          damping: 26,
          delay: 0.1,
        }}
        className="fixed bottom-6 left-0 right-0 z-40"
      >
        <div className="mx-auto flex w-fit items-end gap-2 rounded-[26px] border border-white/[0.08] bg-surface/70 px-4 py-2.5 backdrop-blur-2xl shadow-pop">
          {dockLinks.map((link) => (
            <DockItem key={link.to} {...link} />
          ))}
        </div>
      </motion.nav>
    </>
  )
}