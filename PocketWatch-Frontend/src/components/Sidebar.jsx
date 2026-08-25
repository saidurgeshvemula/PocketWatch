import { NavLink } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  LayoutDashboard,
  Receipt,
  UploadCloud,
  Sparkles,
  MessageCircle,
  LogOut,
  Landmark,
  History,
  Tags,
  User,
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { cn } from '@/lib/utils'
import pocketWatchIcon from '@/assets/pocketwatch-icon.png'

const primaryLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/transactions', label: 'Transactions', icon: Receipt },
  { to: '/upload', label: 'Upload', icon: UploadCloud },
  { to: '/insights', label: 'AI Insights', icon: Sparkles },
  { to: '/chat', label: 'AI Chat', icon: MessageCircle },
]

const managementLinks = [
  { to: '/bank-accounts', label: 'Bank accounts', icon: Landmark },
  { to: '/upload-history', label: 'Upload history', icon: History },
  { to: '/categories', label: 'Categories', icon: Tags },
]

function NavItem({ to, label, icon: Icon }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'group relative flex items-center gap-3 px-3 py-2.5 rounded-control text-sm transition-all duration-200',
          isActive
            ? 'text-fg font-medium bg-white/[0.06]'
            : 'text-fg-muted hover:bg-white/[0.04] hover:text-fg',
        )
      }
    >
      {({ isActive }) => (
        <>
          {isActive && (
            <motion.span
              layoutId="sidebar-active-rail"
              className="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-full bg-gold-gradient"
              transition={{ type: 'spring', stiffness: 500, damping: 40 }}
            />
          )}
          <Icon size={17} strokeWidth={2} className={cn('shrink-0', isActive ? 'text-brass' : 'text-fg-subtle group-hover:text-fg-muted')} />
          <span className="truncate">{label}</span>
        </>
      )}
    </NavLink>
  )
}

export function Sidebar() {
  const { user, logout } = useAuth()

  return (
    <aside className="w-64 shrink-0 bg-surface/80 backdrop-blur-xl border-r border-white/[0.06] text-fg flex flex-col min-h-screen">
      <div className="px-6 py-6 border-b border-white/[0.06] flex items-center gap-2.5">
        <div className="w-9 h-9 rounded-xl overflow-hidden shadow-glow shrink-0">
          <img src={pocketWatchIcon} alt="PocketWatch" className="w-full h-full object-cover" />
        </div>
        <div className="min-w-0">
          <p className="font-display text-base font-bold tracking-tight text-fg leading-none">PocketWatch</p>
          <p className="text-[11px] text-fg-subtle mt-1">Know where it goes</p>
        </div>
      </div>

      <nav className="flex-1 px-3 py-5 space-y-6 overflow-y-auto ledger-scrollbar">
        <div className="space-y-1">
          {primaryLinks.map((link) => (
            <NavItem key={link.to} {...link} />
          ))}
        </div>

        <div>
          <p className="px-3 pb-2 text-[10px] uppercase tracking-widest text-fg-subtle font-semibold">Manage</p>
          <div className="space-y-1">
            {managementLinks.map((link) => (
              <NavItem key={link.to} {...link} />
            ))}
          </div>
        </div>
      </nav>

      <div className="px-3 py-4 border-t border-white/[0.06] space-y-1">
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            cn(
              'flex items-center gap-2.5 px-3 py-2 rounded-control text-xs transition-colors truncate',
              isActive ? 'bg-white/[0.06] text-brass font-medium' : 'text-fg-subtle hover:bg-white/[0.04] hover:text-fg',
            )
          }
        >
          <span className="w-6 h-6 rounded-full bg-elevated flex items-center justify-center shrink-0">
            <User size={12} />
          </span>
          <span className="truncate">{user?.fullName || user?.email || 'Signed in'}</span>
        </NavLink>
        <button
          onClick={logout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-control text-sm text-fg-muted hover:bg-debit/10 hover:text-debit transition-colors"
        >
          <LogOut size={17} />
          Sign out
        </button>
      </div>
    </aside>
  )
}
