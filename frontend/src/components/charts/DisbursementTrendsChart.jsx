import React from 'react';
import './chartSetup';
import { Bar } from 'react-chartjs-2';
import { formatINR } from '../../utils/formatters';
import { Landmark } from 'lucide-react';

export default function DisbursementTrendsChart({ trends = [] }) {
  if (!trends || trends.length === 0) {
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
        <Landmark size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Direct Benefit Transfers Recorded Yet
        </p>
        <p style={{ fontSize: '12px', color: '#64748b', maxWidth: '380px' }}>
          When payment schedules are authorized and released by the Finance Approver, real-time monthly disbursement velocity appears here.
        </p>
      </div>
    );
  }

  const labels = trends.map((t) => t.month || t.label || t.period);
  const values = trends.map((t) => Number(t.amount || t.disbursed) || 0);

  const data = {
    labels,
    datasets: [
      {
        label: 'Direct Benefit Transfer (DBT)',
        data: values,
        backgroundColor: '#059669',
        borderRadius: 6,
        hoverBackgroundColor: '#047857',
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
          label: (context) => ` Disbursed: ${formatINR(context.parsed.y)}`,
        },
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', font: { family: 'Inter', size: 11 }, maxRotation: 45, minRotation: 0 },
        grid: { display: false },
      },
      y: {
        ticks: {
          color: '#64748b',
          font: { family: 'Inter', size: 11 },
          callback: (val) => formatINR(val),
        },
        grid: { color: '#f1f5f9' },
      },
    },
  };

  return (
    <div style={{ height: '240px', width: '100%', minWidth: 0, maxWidth: '100%', position: 'relative' }}>
      <Bar data={data} options={options} />
    </div>
  );
}
