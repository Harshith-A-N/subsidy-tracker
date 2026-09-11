import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiClient, setToken, clearToken, getToken } from '../api/client';
import { useToast } from './ToastContext';

const AuthContext = createContext(null);
const USER_KEY = 'govgrant_auth_user';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  useEffect(() => {
    const token = getToken();
    if (token) {
      // Validate token silently with /users/me
      apiClient.get('/api/v1/users/me')
        .then((res) => {
          if (res.success && res.data) {
            const updated = {
              id: res.data.id,
              name: res.data.fullName || res.data.name || 'User',
              email: res.data.email,
              role: (res.data.role || '').toUpperCase(),
              region: res.data.region || 'ALL',
            };
            setUser(updated);
            localStorage.setItem(USER_KEY, JSON.stringify(updated));
          }
        })
        .catch(() => {
          // Token expired or invalid
          clearToken();
          localStorage.removeItem(USER_KEY);
          setUser(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (email, password, remember = true) => {
    const res = await apiClient.post('/api/v1/auth/login', { email, password });
    if (!res.success) {
      toast.error(res.error || 'Login failed. Please check your credentials.');
      return { success: false, error: res.error };
    }

    const data = res.data;
    const token = data.token;
    if (token) setToken(token, remember);

    const role = (data.role || '').toUpperCase();
    const userInfo = {
      id: data.id || data.userId,
      email: data.email || email,
      name: data.fullName || data.name || email.split('@')[0],
      role: role,
      region: data.region || 'ALL',
    };

    setUser(userInfo);
    if (remember) {
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
    } else {
      sessionStorage.setItem(USER_KEY, JSON.stringify(userInfo));
    }

    toast.success(`Welcome back, ${userInfo.name}!`);
    return { success: true, user: userInfo };
  };

  const registerCitizen = async (formData) => {
    const res = await apiClient.post('/api/v1/auth/register', formData);
    if (!res.success) {
      toast.error(res.error || 'Registration failed.');
      return { success: false, error: res.error };
    }
    toast.success('Registration successful! Please login with your credentials.');
    return { success: true, data: res.data };
  };

  const submitOfficerRequest = async (formData) => {
    const res = await apiClient.post('/api/v1/auth/officer-register', formData);
    if (!res.success) {
      toast.error(res.error || 'Request submission failed.');
      return { success: false, error: res.error };
    }
    toast.success('Officer registration request submitted! Awaiting administrator approval.');
    return { success: true, data: res.data };
  };

  const logout = () => {
    clearToken();
    localStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(USER_KEY);
    setUser(null);
    toast.info('Logged out successfully.');
  };

  const getDashboardPath = (role) => {
    const r = (role || (user && user.role) || '').toUpperCase();
    switch (r) {
      case 'BENEFICIARY': return '/portal/beneficiary';
      case 'FIELD_OFFICER': return '/portal/field-officer';
      case 'DISTRICT_OFFICER': return '/portal/district-officer';
      case 'FINANCE_APPROVER': return '/portal/finance';
      case 'ADMIN': return '/portal/admin';
      default: return '/';
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        registerCitizen,
        submitOfficerRequest,
        logout,
        getDashboardPath,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};
