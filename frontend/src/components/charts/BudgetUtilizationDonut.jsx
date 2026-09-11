import React from 'react';
import './chartSetup';
import { Doughnut } from 'react-chartjs-2';
import { formatINR } from '../../utils/formatters';
import { PieChart } from 'lucide-react';

export default function BudgetUtilizationDonut({ utilized = 0, total = 0 }) {
  const utilizedNum = Math.max(0, Number(utilized) || 0);
  const totalNum = Math.max(0, Number(total) || 0);
  const remainingNum = Math.max(0, totalNum - utilizedNum);
  const percent = totalNum > 0 ? ((utilizedNum / totalNum) * 100).toFixed(1) : '0.0';

  if (totalNum === 0) {
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
        <PieChart size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Budget Allocation Recorded
        </p>
        <p style={{ fontSize: '12px', color: '#64748b' }}>
          Allocated and utilized funds will appear here when regional budgets are configured.
        </p>
      </div>
    );
  }

  const data = {
    labels: ['Utilized Budget', 'Remaining Allocation'],
    datasets: [
      {
        data: [utilizedNum, remainingNum],
        backgroundColor: ['#059669', '#e2e8f0'],
        hoverBackgroundColor: ['#047857', '#cbd5e1'],
        borderWidth: 2,
        borderColor: '#ffffff',
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '70%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#475569',
          font: { family: 'Inter', size: 12, weight: '500' },
          boxWidth: 12,
          padding: 14,
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
      <div
        style={{
          position: 'absolute',
          top: '42%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          textAlign: 'center',
          pointerEvents: 'none',
        }}
      >
        <div style={{ fontSize: '20px', fontWeight: 800, color: '#0f172a', lineHeight: 1 }}>
          {percent}%
        </div>
        <div style={{ fontSize: '10.5px', color: '#64748b', fontWeight: 600, marginTop: '2px' }}>
          Utilized
        </div>
      </div>
    </div>
  );
}
