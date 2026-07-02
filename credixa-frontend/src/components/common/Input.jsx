import React, { forwardRef } from 'react';
import { motion } from 'framer-motion';

export const Input = forwardRef(({ 
  label, 
  error, 
  className = '', 
  leftIcon, 
  rightIcon, 
  id, 
  ...props 
}, ref) => {
  return (
    <div className={`flex flex-col gap-2 ${className}`}>
      {label && (
        <label 
          htmlFor={id} 
          className="text-sm font-semibold text-muted-text ml-1"
        >
          {label}
        </label>
      )}
      <div className="relative group">
        {leftIcon && (
          <div className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-text group-focus-within:text-primary-500 transition-colors duration-200">
            {leftIcon}
          </div>
        )}
        <input
          id={id}
          ref={ref}
          className={`
            form-input py-3.5 bg-surface border border-border-subtle text-app-text
            ${leftIcon ? 'pl-12' : ''}
            ${rightIcon ? 'pr-12' : ''}
            ${error ? 'border-danger-500/50 focus:ring-danger-500/50' : ''}
          `}
          {...props}
        />
        {rightIcon && (
          <div className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-text">
            {rightIcon}
          </div>
        )}
      </div>
      {error && (
        <motion.p 
          initial={{ opacity: 0, x: -10 }} 
          animate={{ opacity: 1, x: 0 }} 
          className="text-xs text-danger-500 font-medium ml-1 mt-0.5"
        >
          {error.message || error}
        </motion.p>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
