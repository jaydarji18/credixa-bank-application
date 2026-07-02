import React from 'react';
import { motion } from 'framer-motion';

export const Button = ({ 
  children, 
  variant = 'primary', 
  size = 'md', 
  isLoading = false, 
  leftIcon, 
  rightIcon, 
  fullWidth = false,
  className = '', 
  ...props 
}) => {
  const variants = {
    primary: 'bg-gradient-to-r from-primary-600 to-secondary-600 text-white shadow-primary-900/10 hover:shadow-primary-600/30',
    secondary: 'bg-surface-2 text-app-text hover:bg-surface border border-border-subtle shadow-sm',
    danger: 'bg-gradient-to-r from-danger-600 to-danger-500 text-white shadow-danger-900/10',
    ghost: 'bg-transparent text-app-text hover:bg-surface-2',
    outline: 'bg-transparent text-primary-500 border border-primary-500/30 hover:bg-primary-500/10'
  };

  const sizes = {
    sm: 'px-3 py-1.5 text-xs font-semibold rounded-lg',
    md: 'px-5 py-2.5 text-sm font-semibold rounded-xl',
    lg: 'px-8 py-4 text-base font-bold rounded-2xl'
  };

  return (
    <motion.button
      whileTap={{ scale: 0.98 }}
      whileHover={{ y: -2, scale: 1.01 }}
      className={`
        inline-flex items-center justify-center transition-all duration-300
        disabled:opacity-50 disabled:pointer-events-none disabled:grayscale
        relative overflow-hidden group
        ${variants[variant]} 
        ${sizes[size]} 
        ${fullWidth ? 'w-full' : ''}
        ${className}
      `}
      {...props}
    >
      {isLoading && (
        <svg className="animate-spin -ml-1 mr-3 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      )}
      {!isLoading && leftIcon && <span className="mr-2.5">{leftIcon}</span>}
      <span className={isLoading ? 'opacity-0' : 'opacity-100'}>{children}</span>
      {!isLoading && rightIcon && <span className="ml-2.5">{rightIcon}</span>}
    </motion.button>
  );
};

export default Button;
