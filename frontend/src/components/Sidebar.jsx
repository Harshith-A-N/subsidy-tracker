import React from 'react';
import { LogOut, X, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar({ items = [], activeTab, onSelectTab, isOpen, onClose }) {
  const { logout, mobileNavOpen, setMobileNavOpen } = useAuth();
  const isNavOpen = isOpen !== undefined ? isOpen : mobileNavOpen;
  const handleClose = onClose || (() => setMobileNavOpen(false));

  const handleSelect = (key) => {
    onSelectTab(key);
    handleClose();
  };

  return (
    <>
      {/* Mobile Backdrop Overlay */}
      <div
        className={`sidebar-overlay ${isNavOpen ? 'active' : ''}`}
        onClick={handleClose}
      />

      <aside className={`sidebar-container ${isNavOpen ? 'open' : ''}`}>
        {/* Mobile-only header inside drawer */}
        <div
          className="sidebar-mobile-header"
          style={{
            display: 'none',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '4px 8px 12px',
            borderBottom: '1px solid var(--border-main)',
            marginBottom: '6px',
          }}
        >
          <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-primary)' }}>
            Navigation Menu
          </span>
          <button
            onClick={handleClose}
            aria-label="Close navigation menu"
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: 'var(--text-dim)',
              padding: '4px',
              borderRadius: '6px',
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <X size={18} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', flex: 1, overflowY: 'auto', minHeight: 0 }}>
          {items.map((item, idx) => {
            if (item.type === 'heading') {
              return (
                <div
                  key={idx}
                  style={{
                    fontSize: '10.5px',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    color: 'var(--text-dim)',
                    padding: '10px 10px 3px',
                    letterSpacing: '0.05em',
                  }}
                >
                  {item.label}
                </div>
              );
            }

            const isActive = activeTab === item.key;
            const Icon = item.icon;

            return (
              <button
                key={item.key || idx}
                onClick={() => handleSelect(item.key)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '9px 12px',
                  borderRadius: '8px',
                  border: 'none',
                  background: isActive ? 'var(--primary-light)' : 'transparent',
                  color: isActive ? 'var(--primary-dark)' : 'var(--text-secondary)',
                  fontWeight: isActive ? 700 : 500,
                  fontSize: '13px',
                  cursor: 'pointer',
                  textAlign: 'left',
                  width: '100%',
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'var(--bg-subtle)';
                    e.currentTarget.style.color = 'var(--text-primary)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'transparent';
                    e.currentTarget.style.color = 'var(--text-secondary)';
                  }
                }}
              >
                {Icon && <Icon size={17} color={isActive ? 'var(--primary)' : 'var(--text-muted)'} />}
                <span style={{ flex: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {item.label}
                </span>
                {item.badge !== undefined && item.badge !== null && item.badge > 0 && (
                  <span style={{
                    fontSize: '10.5px',
                    fontWeight: 700,
                    padding: '1px 6px',
                    borderRadius: '999px',
                    backgroundColor: isActive ? 'var(--primary)' : '#e2e8f0',
                    color: isActive ? '#ffffff' : '#334155',
                  }}>
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        <div style={{ paddingTop: '10px', borderTop: '1px solid var(--border-main)', flexShrink: 0 }}>
          <button
            onClick={logout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '8px 12px',
              borderRadius: '8px',
              border: 'none',
              background: 'transparent',
              color: '#dc2626',
              fontWeight: 600,
              fontSize: '13px',
              cursor: 'pointer',
              width: '100%',
              textAlign: 'left',
            }}
            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#fee2e2'}
            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
          >
            <LogOut size={16} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  );
}
