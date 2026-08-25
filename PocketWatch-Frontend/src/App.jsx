import { Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/AppLayout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import ForgotPassword from '@/pages/ForgotPassword'
import ResetPassword from '@/pages/ResetPassword'
import VerifyEmail from '@/pages/VerifyEmail'
import Dashboard from '@/pages/Dashboard'
import Transactions from '@/pages/Transactions'
import CashExpenseModal from './pages/Cashexpensemodal'
import Upload from '@/pages/Upload'
import Insights from '@/pages/Insights'
import Chat from '@/pages/Chat'
import BankAccounts from '@/pages/BankAccounts'
import UploadHistory from '@/pages/UploadHistory'
import Categories from '@/pages/Categories'
import Profile from '@/pages/Profile'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/verify-email" element={<VerifyEmail />} />

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/upload" element={<Upload />} />
        <Route path="/insights" element={<Insights />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/bank-accounts" element={<BankAccounts />} />
        <Route path="/upload-history" element={<UploadHistory />} />
        <Route path="/categories" element={<Categories />} />
        <Route path="/profile" element={<Profile />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}