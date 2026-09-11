import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ROLE_INFO } from '../utils/constants';
import { Shield } from 'lucide-react';

export default function Navbar() {
  const { user, getDashboardPath } = useAuth();
  const navigate = useNavigate();

  const roleMeta = (user && ROLE_INFO[user.role]) || { label: 'Citizen', badge: 'neutral' };

  return (
    <header style={{
      backgroundColor: '#ffffff',
      borderBottom: '1px solid var(--border-main)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
      width: '100%',
    }}>
      {/* Main Navbar */}
      <div style={{
        maxWidth: '1440px',
        margin: '0 auto',
        padding: 'clamp(8px, 1.2vw, 12px) clamp(12px, 2vw, 24px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '12px',
        flexWrap: 'wrap',
      }}>
        {/* Brand Logo */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none', minWidth: 0 }}>
          <div style={{
            width: '38px',
            height: '38px',
            minWidth: '38px',
            borderRadius: '10px',
            background: 'linear-gradient(135deg, #1d4ed8 0%, #1e3a8a 100%)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 6px -1px rgba(29, 78, 216, 0.3)',
          }}>
            <Shield size={20} />
          </div>
          <div style={{ minWidth: 0 }}>
            <div style={{ fontSize: 'clamp(15px, 1.8vw, 18px)', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.02em', lineHeight: 1.1 }}>
              GovGrant <span style={{ color: '#1d4ed8', fontWeight: 600, fontSize: 'clamp(12px, 1.3vw, 14px)' }}>Portal</span>
            </div>
            <div style={{ fontSize: '10.5px', color: '#64748b', fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              Direct Benefit Transfer & Subsidies
            </div>
          </div>
        </Link>

        {/* Right Nav Options */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'clamp(8px, 1.5vw, 16px)', flexWrap: 'wrap' }}>
          {user ? (
            <>
              <button
                onClick={() => navigate(getDashboardPath(user.role))}
                className="btn btn-ghost btn-sm"
                style={{ fontWeight: 600 }}
              >
                Dashboard
              </button>

              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '4px 8px',
                borderRadius: '8px',
                backgroundColor: 'var(--bg-subtle)',
                border: '1px solid var(--border-main)',
                maxWidth: '220px',
              }}>
                <div style={{
                  width: '28px',
                  height: '28px',
                  minWidth: '28px',
                  borderRadius: '50%',
                  backgroundColor: '#1d4ed8',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 700,
                  fontSize: '12px',
                }}>
                  {user.name ? user.name[0].toUpperCase() : 'U'}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                  <span style={{ fontSize: '12px', fontWeight: 700, color: '#0f172a', lineHeight: 1.1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.name}
                  </span>
                  <span className={`badge badge-${roleMeta.badge}`} style={{ fontSize: '9.5px', padding: '1px 5px', marginTop: '1px', width: 'fit-content' }}>
                    {roleMeta.label}
                  </span>
                </div>
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
              <Link to="/login" className="btn btn-secondary btn-sm" style={{ padding: '6px 12px', fontSize: '12px' }}>
                Officer / Staff Login
              </Link>
              <Link to="/login" className="btn btn-primary btn-sm" style={{ padding: '6px 12px', fontSize: '12px' }}>
                Citizen Portal
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
