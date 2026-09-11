/**
 * Indian Rupee Short Formatter (e.g. ₹ 1.5 Cr, ₹ 25 L, ₹ 5 K)
 */
export function formatINR(amount) {
  const num = Number(amount) || 0;
  if (num >= 10000000) return '₹ ' + (num / 10000000).toFixed(2) + ' Cr';
  if (num >= 100000) return '₹ ' + (num / 100000).toFixed(2) + ' L';
  if (num >= 1000) return '₹ ' + (num / 1000).toFixed(1) + ' K';
  return '₹ ' + num.toLocaleString('en-IN');
}

/**
 * Indian Rupee Full Formatter (e.g. ₹ 1,50,000)
 */
export function formatINRFull(amount) {
  const num = Number(amount) || 0;
  return '₹ ' + num.toLocaleString('en-IN');
}

/**
 * Clean Date Formatter
 */
export function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return String(dateStr);
    return d.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return String(dateStr);
  }
}

/**
 * Relative or Detailed Date Time
 */
export function formatDateTime(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return String(dateStr);
    return d.toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
    });
  } catch {
    return String(dateStr);
  }
}
