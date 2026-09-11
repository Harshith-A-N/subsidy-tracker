import React from 'react';
import './chartSetup';
import { Doughnut } from 'react-chartjs-2';
import { formatINR } from '../../utils/formatters';
import { Layers } from 'lucide-react';

export default function SchemeDistributionChart({ schemes = [] }) {
  if (!schemes || schemes.length === 0) {
    return (
      <div
        style={{
          height: '240px',
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
        <Layers size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Scheme Budget Data Available
        </p>
        <p style={{ fontSize: '12px', color: '#64748b' }}>
          Scheme allocations will appear dynamically as schemes and regional funding plans are created.
        </p>
      </div>
    );
  }

  const labels = schemes.map((s) => s.schemeName || s.name || 'Scheme');
  const values = schemes.map((s) => Number(s.totalBudget || s.utilizedBudget) || 0);

  const colors = [
    '#1d4ed8', // Royal Blue
    '#059669', // Emerald
    '#d97706', // Amber
    '#7c3aed', // Purple
    '#0284c7', // Sky Blue
    '#dc2626', // Crimson
    '#4f46e5', // Indigo
    '#0891b2', // Cyan
  ];

  const data = {
    labels,
    datasets: [
      {
        data: values,
        backgroundColor: colors.slice(0, labels.length),
        borderWidth: 2,
        borderColor: '#ffffff',
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '65%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#475569',
          font: { family: 'Inter', size: 11.5, weight: '500' },
          boxWidth: 10,
          padding: 10,
        },
      },
      tooltip: {
        backgroundColor: '#0f172a',
        padding: 10,
        callbacks: {
          label: (context) => ` ${context.label}: ${formatINR(context.parsed)}`,
        },
      },
    },
  };

  return (
    <div style={{ height: '240px', width: '100%', minWidth: 0, maxWidth: '100%', position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Doughnut data={data} options={options} />
    </div>
  );
}
