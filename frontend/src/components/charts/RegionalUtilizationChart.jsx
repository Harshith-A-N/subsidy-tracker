import React from 'react';
import './chartSetup';
import { Bar } from 'react-chartjs-2';
import { Percent } from 'lucide-react';

export default function RegionalUtilizationChart({ regions = [] }) {
  if (!regions || regions.length === 0) {
    return (
      <div
        style={{
          height: '260px',
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f8fafc',
          borderRadius: '8px',
          border: '1px dashed #cbd5e1',
          padding: '24px',
          textAlign: 'center',
        }}
      >
        <Percent size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Regional Budget Utilization Data
        </p>
        <p style={{ fontSize: '12px', color: '#64748b' }}>
          Fund absorption percentages by state and district will display here from live treasury accounts.
        </p>
      </div>
    );
  }

  const labels = regions.map((r) => r.regionName || r.region || r.state || 'Region');
  const utilizationValues = regions.map((r) => Math.round(Number(r.utilizationPercent) || 0));

  // Threshold colors: >80% Red (exhaustion risk), >50% Amber, <=50% Emerald Green
  const bgColors = utilizationValues.map((pct) =>
    pct > 80 ? '#dc2626' : pct > 50 ? '#d97706' : '#059669'
  );

  const data = {
    labels,
    datasets: [
      {
        label: 'Budget Utilization %',
        data: utilizationValues,
        backgroundColor: bgColors,
        borderRadius: 5,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#0f172a',
        padding: 10,
        callbacks: {
          label: (context) => ` Utilization: ${context.parsed.y}%`,
        },
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', font: { family: 'Inter', size: 11 }, maxRotation: 45, minRotation: 0 },
        grid: { display: false },
      },
      y: {
        max: 100,
        ticks: {
          color: '#64748b',
          font: { family: 'Inter', size: 11 },
          callback: (val) => `${val}%`,
        },
        grid: { color: '#f1f5f9' },
      },
    },
  };

  return (
    <div style={{ height: '260px', width: '100%', minWidth: 0, maxWidth: '100%', position: 'relative' }}>
      <Bar data={data} options={options} />
    </div>
  );
}
