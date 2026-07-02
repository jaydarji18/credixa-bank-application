import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X } from 'lucide-react';

const SidePanel = ({ isOpen, onClose, title, children }) => {
  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100]"
          />
          
          {/* Panel */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="fixed top-0 right-0 h-full w-full md:w-[480px] bg-dark-950 border-l border-white/5 z-[101] flex flex-col shadow-2xl"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-6 border-b border-white/5 bg-dark-900/50">
               <div>
                  <h3 className="text-xl font-black text-white uppercase tracking-tight">{title}</h3>
                  <div className="w-8 h-1 bg-primary-500 mt-1 rounded-full" />
               </div>
               <button 
                  onClick={onClose}
                  className="p-2 rounded-xl bg-surface-2 text-dark-300 hover:text-white hover:bg-dark-800 transition-all border border-white/5"
               >
                  <X size={20} />
               </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6 custom-scrollbar">
               {children}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default SidePanel;
