import React from 'react';

export default function MetricCard({ title, value, subtext, icon: Icon, trend, color = 'primary' }) {
  const colorMap = {
    primary: { bg: '#dbeafe', text: '#1d4ed8', border: '#bfdbfe' },
    success: { bg: '#d1fae5', text: '#059669', border: '#a7f3d0' },
    warning: { bg: '#fef3c7', text: '#d97706', border: '#fde68a' },
    danger: { bg: '#fee2e2', text: '#dc2626', border: '#fecaca' },
    neutral: { bg: '#f1f5f9', text: '#475569', border: '#e2e8f0' },
  };

  const scheme = colorMap[color] || colorMap.primary;

  return (
    <div className="card card-hover" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>
          {title}
        </span>
        {Icon && (
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              backgroundColor: scheme.bg,
              color: scheme.text,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Icon size={18} />
          </div>
        )}
      </div>

      <div>
        <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          {value}
        </div>
        {subtext && (
          <div style={{ fontSize: '12.5px', color: 'var(--text-muted)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            {trend && (
              <span style={{ color: trend > 0 ? '#059669' : '#dc2626', fontWeight: 600 }}>
                {trend > 0 ? `+${trend}%` : `${trend}%`}
              </span>
            )}
            <span>{subtext}</span>
          </div>
        )}
      </div>
    </div>
  );
}
