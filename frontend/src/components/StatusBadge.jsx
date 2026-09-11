import React from 'react';
import { APP_STATUS_INFO } from '../utils/constants';
import { CheckCircle2, Clock, XCircle, FileText, ArrowRight } from 'lucide-react';

export default function StatusBadge({ status }) {
  const info = APP_STATUS_INFO[status] || {
    badge: 'neutral',
    label: String(status || 'Unknown'),
  };

  const getIcon = () => {
    switch (info.badge) {
      case 'success': return <CheckCircle2 size={13} />;
      case 'warning': return <Clock size={13} />;
      case 'danger': return <XCircle size={13} />;
      case 'primary': return <ArrowRight size={13} />;
      default: return <FileText size={13} />;
    }
  };

  return (
    <span className={`badge badge-${info.badge}`}>
      {getIcon()}
      <span>{info.label}</span>
    </span>
  );
}
