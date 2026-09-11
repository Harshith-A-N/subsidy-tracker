import React from 'react';
import './chartSetup';
import { Bar } from 'react-chartjs-2';
import { FileText } from 'lucide-react';

export default function PipelineStageChart({ pipeline = [] }) {
  if (!pipeline || pipeline.length === 0) {
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
        <FileText size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Application Pipeline Data
        </p>
        <p style={{ fontSize: '12px', color: '#64748b' }}>
          Applications will be automatically categorized as they move through verification stages.
        </p>
      </div>
    );
  }

  const labels = pipeline.map((item) => item.label || item.status);
  const dataValues = pipeline.map((item) => Number(item.count) || 0);

  const colors = [
    '#3b82f6', // Submitted (Blue)
    '#f59e0b', // Field Verification (Amber)
    '#8b5cf6', // District Review (Purple)
    '#0284c7', // Finance Review (Sky Blue)
    '#059669', // Disbursed / Completed (Emerald)
    '#ef4444', // Rejected (Red)
  ];

  const data = {
    labels,
    datasets: [
      {
        label: 'Applications',
        data: dataValues,
        backgroundColor: colors.slice(0, labels.length),
        borderRadius: 6,
        borderSkipped: false,
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
        titleFont: { family: 'Inter', size: 12 },
        bodyFont: { family: 'Inter', size: 13, weight: 'bold' },
        cornerRadius: 6,
        callbacks: {
          label: (context) => ` ${context.parsed.y} Applications in this stage`,
        },
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', font: { family: 'Inter', size: 11, weight: '500' }, maxRotation: 45, minRotation: 0 },
        grid: { display: false },
      },
      y: {
        ticks: { color: '#64748b', font: { family: 'Inter', size: 11 }, precision: 0, stepSize: 1 },
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
