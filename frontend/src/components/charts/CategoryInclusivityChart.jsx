import React from 'react';
import './chartSetup';
import { Bar } from 'react-chartjs-2';
import { Users } from 'lucide-react';

export default function CategoryInclusivityChart({ categories = [] }) {
  if (!categories || categories.length === 0) {
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
        <Users size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Category Distribution Data Available
        </p>
        <p style={{ fontSize: '12px', color: '#64748b' }}>
          Beneficiary social category distribution will appear as citizens register on the DBT platform.
        </p>
      </div>
    );
  }

  const labels = categories.map((c) => c.category || 'Unknown');
  const counts = categories.map((c) => Number(c.count || c.beneficiaryCount || 0));
  const percentages = categories.map((c) => Number(c.percent || c.percentage || 0));

  const palette = [
    'rgba(30, 64, 175, 0.85)',  // Deep Blue (GENERAL)
    'rgba(5, 150, 105, 0.85)',  // Emerald (OBC)
    'rgba(124, 58, 237, 0.85)', // Purple (SC)
    'rgba(217, 119, 6, 0.85)',  // Amber (ST)
    'rgba(14, 165, 233, 0.85)',  // Sky (EWS)
  ];

  const borderPalette = [
    '#1e40af',
    '#059669',
    '#7c3aed',
    '#d97706',
    '#0ea5e9',
  ];

  const data = {
    labels,
    datasets: [
      {
        label: 'Enrolled Citizens (Count)',
        data: counts,
        backgroundColor: palette.slice(0, labels.length),
        borderColor: borderPalette.slice(0, labels.length),
        borderWidth: 1.5,
        borderRadius: 6,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: '#0f172a',
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (context) => {
            const idx = context.dataIndex;
            const count = context.parsed.y;
            const pct = percentages[idx] !== undefined ? percentages[idx].toFixed(1) : 0;
            return ` ${count} Citizens (${pct}% of Total DBT Enrollment)`;
          },
        },
      },
    },
    scales: {
      x: {
        ticks: {
          color: '#475569',
          font: { family: 'Inter', size: 11.5, weight: '600' },
          maxRotation: 45,
          minRotation: 0,
        },
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        ticks: { color: '#64748b', font: { family: 'Inter', size: 11 }, precision: 0, stepSize: 1 },
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
