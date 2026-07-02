import React, { useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X } from 'lucide-react';

export const Modal = ({ 
  isOpen, 
  onClose, 
  title, 
  children, 
  size = 'md', 
  className = '', 
  footer 
}) => {
  // Handle Escape key and body scroll
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };

    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleEscape);
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      window.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  const sizes = {
    sm: 'max-w-md',
    md: 'max-w-2xl',
    lg: 'max-w-4xl',
    xl: 'max-w-6xl',
    full: 'max-w-[95%]',
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-dark-950/80 backdrop-blur-sm z-[100] cursor-pointer"
          />
          <div className="fixed inset-0 z-[101] flex items-center justify-center p-4 pointer-events-none">
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className={`
                w-full ${sizes[size]} bg-surface border border-border-subtle 
                rounded-[32px] shadow-2xl pointer-events-auto overflow-hidden
                flex flex-col max-h-[90vh] ${className}
              `}
            >
              {/* Header */}
              {title && (
                <div className="flex items-center justify-between px-8 py-6 border-b border-border-subtle">
                  <h3 className="text-xl font-bold text-app-text tracking-tight">{title}</h3>
                  <button
                    onClick={onClose}
                    className="p-2.5 rounded-xl hover:bg-white/5 text-dark-400 hover:text-white transition-all duration-200"
                  >
                    <X size={20} />
                  </button>
                </div>
              )}

              {/* Body */}
              <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
                {children}
              </div>

              {/* Footer */}
              {footer && (
                <div className="px-8 py-6 border-t border-border-subtle bg-surface-2/50">
                  {footer}
                </div>
              )}
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
};

export default Modal;
