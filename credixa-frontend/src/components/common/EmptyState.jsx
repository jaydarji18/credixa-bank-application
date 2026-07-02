import React from 'react';
import { motion } from 'framer-motion';

const EmptyState = ({ 
  title = "No data available", 
  message = "There's nothing to show here at the moment.",
  icon: Icon
}) => {
  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="flex flex-col items-center justify-center py-16 px-4 text-center"
    >
      <div className="w-24 h-24 bg-surface-2 rounded-[40px] flex items-center justify-center mb-6 border border-border-subtle shadow-inner">
        {Icon ? <Icon size={40} className="text-muted-text" /> : (
          <div className="relative">
             <div className="w-12 h-1 bg-muted-text/30 rounded-full mb-1" />
             <div className="w-8 h-1 bg-muted-text/10 rounded-full" />
          </div>
        )}
      </div>
      <h3 className="text-xl font-black text-app-text tracking-tight mb-2 uppercase">{title}</h3>
      <p className="text-sm font-medium text-muted-text max-w-xs">{message}</p>
    </motion.div>
  );
};

export default EmptyState;
