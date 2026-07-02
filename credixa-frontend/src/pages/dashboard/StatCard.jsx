import React, { useEffect } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';

export const StatCard = React.memo(({ icon: Icon, value, label, trend, color: colorClass = 'bg-primary-500', delay = 0, isCurrency = true, subValue }) => {
  const motionValue = useMotionValue(0);
  const springValue = useSpring(motionValue, { damping: 30, stiffness: 100 });
  const displayValue = useTransform(springValue, (latest) => {
    if (!isCurrency) return Math.floor(latest).toString();
    
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(latest);
  });

  useEffect(() => {
    // Basic numeric extraction for animation
    const numericValue = typeof value === 'number' ? value : parseFloat(value?.toString().replace(/[^0-9.-]+/g, "") || "0");
    motionValue.set(numericValue);
  }, [value, motionValue]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.5 }}
      className="glass-card flex flex-col gap-4 group"
    >
      <div className="flex justify-between items-start">
        <div className={`p-3 rounded-2xl ${colorClass} bg-opacity-10 transition-transform duration-300 group-hover:scale-110`}>
          <Icon size={24} className={colorClass?.replace('bg-', 'text-') || 'text-primary-500'} />
        </div>
        {trend && (
          <span className={`text-xs font-bold px-2 py-1 rounded-lg ${trend.positive ? 'bg-success-500/10 text-success-500' : 'bg-danger-500/10 text-danger-500'}`}>
            {trend.positive ? '+' : '-'}{trend.value}%
          </span>
        )}
      </div>
      <div>
        <p className="text-muted-text text-xs font-bold uppercase tracking-widest mb-1">{label}</p>
        <div className="flex flex-col">
          <motion.h3 className="text-2xl font-black text-app-text tracking-tight">
            <motion.span>{displayValue}</motion.span>
          </motion.h3>
          {subValue && (
            <p className="text-[10px] font-bold text-muted-text uppercase tracking-wider mt-1">{subValue}</p>
          )}
        </div>
      </div>
      <div className="w-full h-1 bg-border-subtle rounded-full overflow-hidden mt-2">
        <motion.div 
          initial={{ width: 0 }}
          animate={{ width: '70%' }}
          transition={{ delay: delay + 0.5, duration: 1 }}
          className={`h-full ${colorClass?.replace('bg-opacity-10', '') || 'bg-primary-500'}`} 
        />
      </div>
    </motion.div>
  );
});

StatCard.displayName = 'StatCard';
