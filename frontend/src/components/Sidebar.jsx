import React from 'react';
import { LogOut, X, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar({ items = [], activeTab, onSelectTab, isOpen = false, onClose }) {
  const { logout } = useAuth();

  const handleSelect = (key) => {
    onSelectTab(key);
    if (onClose) onClose();
  };

  return (
    <>
      {/* Mobile Backdrop Overlay */}
      <div
        className={`sidebar-overlay ${isOpen ? 'active' : ''}`}
        onClick={onClose}
      />

      <aside className={`sidebar-container ${isOpen ? 'open' : ''}`}>
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
          {onClose && (
            <button
              onClick={onClose}
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
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', flex: 1, overflowY: 'auto' }}>
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

        <div style={{ paddingTop: '10px', borderTop: '1px solid var(--border-main)' }}>
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
