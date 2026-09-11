import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import Navbar from './components/Navbar';

// Pages
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import BeneficiaryPortal from './pages/BeneficiaryPortal';
import FieldOfficerPortal from './pages/FieldOfficerPortal';
import DistrictOfficerPortal from './pages/DistrictOfficerPortal';
import FinanceApproverPortal from './pages/FinanceApproverPortal';
import AdminPortal from './pages/AdminPortal';

// Initialize React Query Client with 5-minute cache time for 0ms buffering
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function ProtectedRoute({ children, allowedRoles = [] }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
        <div style={{ textAlign: 'center', color: '#64748b' }}>
          <div style={{ fontSize: '18px', fontWeight: 700 }}>Loading Portal...</div>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

function PublicRoute({ children }) {
  const { user, loading, getDashboardPath } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
        <div style={{ textAlign: 'center', color: '#64748b' }}>
          <div style={{ fontSize: '18px', fontWeight: 700 }}>Loading Portal...</div>
        </div>
      </div>
    );
  }

  if (user) {
    return <Navigate to={getDashboardPath(user.role)} replace />;
  }

  return children;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Navbar />
            <div style={{ flex: 1 }}>
              <Routes>
                {/* Public Routes - Auto-redirects to portal dashboard if logged in */}
                <Route path="/" element={<PublicRoute><LandingPage /></PublicRoute>} />
                <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
                <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

                {/* Role-Protected Portals */}
                <Route
                  path="/portal/beneficiary"
                  element={
                    <ProtectedRoute allowedRoles={['BENEFICIARY']}>
                      <BeneficiaryPortal />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/portal/field-officer"
                  element={
                    <ProtectedRoute allowedRoles={['FIELD_OFFICER']}>
                      <FieldOfficerPortal />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/portal/district-officer"
                  element={
                    <ProtectedRoute allowedRoles={['DISTRICT_OFFICER']}>
                      <DistrictOfficerPortal />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/portal/finance"
                  element={
                    <ProtectedRoute allowedRoles={['FINANCE_APPROVER']}>
                      <FinanceApproverPortal />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/portal/admin"
                  element={
                    <ProtectedRoute allowedRoles={['ADMIN']}>
                      <AdminPortal />
                    </ProtectedRoute>
                  }
                />

                {/* Fallback to Home */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </div>
          </div>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}
