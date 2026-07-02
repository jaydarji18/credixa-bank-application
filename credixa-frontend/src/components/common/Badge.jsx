import React from 'react';

const Badge = ({ children, variant = 'primary', className = '' }) => {
  const variants = {
    primary: 'bg-primary-500/10 text-primary-500',
    secondary: 'bg-secondary-500/10 text-secondary-500',
    success: 'bg-success-500/10 text-success-500',
    danger: 'bg-danger-500/10 text-danger-500',
    warning: 'bg-warning-500/10 text-warning-500',
    info: 'bg-info-500/10 text-info-500',
    dark: 'bg-surface-2 text-muted-text border border-border-subtle',
    outline: 'border border-border-subtle text-muted-text bg-transparent',
  };

  return (
    <span className={`px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest inline-flex items-center justify-center ${variants[variant]} ${className}`}>
      {children}
    </span>
  );
};

export default Badge;
