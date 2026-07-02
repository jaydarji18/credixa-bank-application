import React from 'react';

/**
 * Skeleton loader with custom shimmer effect
 * @param {Object} props
 * @param {'line' | 'card' | 'circle'} props.variant - The shape of the skeleton
 * @param {string | number} props.width - Width of the skeleton
 * @param {string | number} props.height - Height of the skeleton
 * @param {string} props.className - Additional classes
 */
export const Skeleton = ({ variant = 'line', width, height, className = '' }) => {
  const baseStyles = "relative overflow-hidden bg-white/5 rounded-lg before:absolute before:inset-0 before:-translate-x-full before:animate-[shimmer_2s_infinite] before:bg-gradient-to-r before:from-transparent before:via-white/[0.05] before:to-transparent";
  
  const variants = {
    line: 'rounded-md',
    card: 'rounded-2xl',
    circle: 'rounded-full',
  };

  return (
    <div 
      className={`${baseStyles} ${variants[variant]} ${className}`}
      style={{ 
        width: typeof width === 'number' ? `${width}px` : width, 
        height: typeof height === 'number' ? `${height}px` : height 
      }}
    />
  );
};

export default Skeleton;
