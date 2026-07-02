import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, AlertCircle, Info, X, Zap } from 'lucide-react';
import { toast } from 'react-hot-toast';

export const Toast = ({ t, type = 'success', message, title }) => {
  const icons = {
    success: <CheckCircle className="text-success-500" size={24} />,
    error: <AlertCircle className="text-danger-500" size={24} />,
    info: <Info className="text-primary-500" size={24} />,
    loading: <Zap className="text-warning-500 animate-pulse" size={24} />,
  };

  const colors = {
    success: 'border-success-500/20 shadow-success-900/10',
    error: 'border-danger-500/20 shadow-danger-900/10',
    info: 'border-primary-500/20 shadow-primary-900/10',
    loading: 'border-warning-500/20 shadow-warning-900/10',
  };

  return (
    <div
      className={`
        ${t.visible ? 'animate-enter' : 'animate-leave'}
        max-w-md w-full bg-surface-2 border border-white/5 shadow-2xl rounded-2xl pointer-events-auto flex ring-1 ring-black ring-opacity-5 overflow-hidden
        ${colors[type]}
      `}
    >
      <div className="flex-1 w-0 p-4">
        <div className="flex items-start">
          <div className="flex-shrink-0 pt-0.5">
            {icons[type]}
          </div>
          <div className="ml-3 flex-1">
            <p className="text-sm font-bold text-white tracking-tight">
              {title || type.charAt(0).toUpperCase() + type.slice(1)}
            </p>
            <p className="mt-1 text-xs font-medium text-dark-300 leading-relaxed">
              {message}
            </p>
          </div>
        </div>
      </div>
      <div className="flex border-l border-white/5">
        <button
          onClick={() => toast.dismiss(t.id)}
          className="w-full border border-transparent rounded-none rounded-r-2xl p-4 flex items-center justify-center text-sm font-medium text-dark-400 hover:text-white hover:bg-white/5 transition-all focus:outline-none"
        >
          <X size={18} />
        </button>
      </div>
    </div>
  );
};

export default Toast;
