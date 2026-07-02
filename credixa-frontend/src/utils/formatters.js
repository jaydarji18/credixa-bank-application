import { format, formatDistanceToNow } from 'date-fns';

/**
 * Format number as Indian Rupee (INR)
 * @param {number} amount 
 * @param {string} currency 
 * @returns {string}
 */
export const formatCurrency = (amount, currency = 'INR') => {
  if (amount === undefined || amount === null) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(amount);
};

/**
 * Format date to simple "MMM d, yyyy"
 * @param {string | Date} dateStr 
 * @returns {string}
 */
export const formatDate = (dateStr) => {
  if (!dateStr) return '--';
  return format(new Date(dateStr), 'MMM d, yyyy');
};

/**
 * Format date time to "MMM d, yyyy · h:mm a"
 * @param {string | Date} dateStr 
 * @returns {string}
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '--';
  return format(new Date(dateStr), 'MMM d, yyyy · h:mm a');
};

/**
 * Format relative time (e.g. "2 hours ago")
 * @param {string | Date} dateStr 
 * @returns {string}
 */
export const formatRelativeTime = (dateStr) => {
  if (!dateStr) return '';
  return formatDistanceToNow(new Date(dateStr), { addSuffix: true });
};

/**
 * Mask account number with X and spaces
 * @param {string} num 
 * @returns {string}
 */
export const maskAccountNumber = (num) => {
  if (!num) return '';
  const str = String(num);
  // Mask all digits except the last 4
  const visible = str.slice(-4);
  const masked = str.slice(0, -4).replace(/\d/g, 'X');
  // Group by 4
  return (masked + visible).replace(/(.{4})/g, '$1 ').trim();
};
