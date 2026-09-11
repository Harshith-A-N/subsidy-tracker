import React from 'react';
import './chartSetup';
import { Bar } from 'react-chartjs-2';
import { MapPin } from 'lucide-react';

export default function RegionalApplicationsChart({ regions = [] }) {
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
        <MapPin size={36} color="#94a3b8" style={{ marginBottom: '10px' }} />
        <p style={{ fontSize: '13.5px', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
          No Regional Application Data Available
        </p>
        <p style={{ fontSize: '12px', color: '#64748b' }}>
          Regional distribution will populate as citizens apply across designated states and districts.
        </p>
      </div>
    );
  }

  const labels = regions.map((r) => r.regionName || r.region || r.state || 'Region');
  const totalApps = regions.map((r) => Number(r.applicationCount || r.totalApplications) || 0);
  const approvedApps = regions.map((r) => Number(r.approvedCount || r.approvedApplications) || 0);

  const data = {
    labels,
    datasets: [
      {
        label: 'Total Applications',
        data: totalApps,
        backgroundColor: '#1d4ed8',
        borderRadius: 5,
      },
      {
        label: 'Approved Subsidies',
        data: approvedApps,
        backgroundColor: '#059669',
        borderRadius: 5,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        align: 'end',
        labels: {
          color: '#475569',
          font: { family: 'Inter', size: 12, weight: '600' },
          boxWidth: 12,
        },
      },
      tooltip: {
        backgroundColor: '#0f172a',
        padding: 10,
        cornerRadius: 6,
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', font: { family: 'Inter', size: 11 }, maxRotation: 45, minRotation: 0 },
        grid: { display: false },
      },
      y: {
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
