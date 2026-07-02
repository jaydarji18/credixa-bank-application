import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Toaster } from 'react-hot-toast';

// Layouts & Protected Routes
import AppLayout from './components/layout/AppLayout';
import ProtectedRoute from './components/layout/ProtectedRoute';
import ErrorBoundary from './components/ErrorBoundary';
import PageSkeleton from './components/PageSkeleton';

// Lazy Loaded Pages
const LoginPage = lazy(() => import('./pages/auth/LoginPage'));
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'));
const OtpVerifyPage = lazy(() => import('./pages/auth/OtpVerifyPage'));
const ForgotPasswordPage = lazy(() => import('./pages/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('./pages/auth/ResetPasswordPage'));

const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const AccountsPage = lazy(() => import('./pages/accounts/AccountsPage'));
const TransactionsPage = lazy(() => import('./pages/transactions/TransactionsPage'));
const TransferPage = lazy(() => import('./pages/transfer/TransferPage'));
const LoansPage = lazy(() => import('./pages/loans/LoansPage'));
const NotificationsPage = lazy(() => import('./pages/notifications/NotificationsPage'));
const ProfilePage = lazy(() => import('./pages/profile/ProfilePage'));
const AdminPage = lazy(() => import('./pages/admin/AdminPage'));
const AdminLoginPage = lazy(() => import('./pages/auth/AdminLoginPage'));
const AdminRegisterPage = lazy(() => import('./pages/auth/AdminRegisterPage'));
const BankOperatorDashboard = lazy(() => import('./pages/bank-operator/BankOperatorDashboard'));

// Redirect to dashboard if authenticated, else login
const HomeRedirect = () => {
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  
  if (!isAuthenticated) return <Navigate to="/login" replace />;

  const isAdmin = user?.userCode?.startsWith('ADMIN_') || 
                  ['SUPER_ADMIN', 'BANK_OPERATOR', 'BANK_MANAGER', 'TELLER', 'COMPLIANCE_OFFICER', 'SUPPORT_AGENT'].includes(user?.role);

  if (isAdmin) {
    if (user?.role === 'BANK_OPERATOR') return <Navigate to="/bank-operator" replace />;
    return <Navigate to="/admin" replace />;
  }

  return <Navigate to="/dashboard" replace />;
};

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Toaster 
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#111827',
              color: '#fff',
              border: '1px solid rgba(255,255,255,0.05)',
              borderRadius: '16px',
              padding: '12px 24px',
            },
            success: {
              iconTheme: {
                primary: '#10b981',
                secondary: '#fff',
              },
            },
            error: {
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />
        
        <Suspense fallback={<PageSkeleton />}>
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-otp" element={<OtpVerifyPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/admin/login" element={<AdminLoginPage />} />
            <Route path="/admin/register" element={<AdminRegisterPage />} />

            {/* Protected Routes */}
            <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/accounts" element={<AccountsPage />} />
              <Route path="/transactions" element={<TransactionsPage />} />
              <Route path="/transfer" element={<TransferPage />} />
              <Route path="/loans" element={<LoansPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              
              {/* Admin Protected Routes */}
              <Route path="/admin" element={<ProtectedRoute adminOnly={true}><AdminPage /></ProtectedRoute>} />
              <Route path="/bank-operator" element={<ProtectedRoute adminOnly={true}><BankOperatorDashboard /></ProtectedRoute>} />
            </Route>

            {/* Catch-all - redirect to landing/dashboard */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
