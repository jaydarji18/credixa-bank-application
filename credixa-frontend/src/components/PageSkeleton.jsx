import React from 'react';
import Skeleton from './common/Skeleton';

const PageSkeleton = () => {
  return (
    <div className="flex flex-col h-full space-y-8 animate-pulse">
      {/* Top Bar Area */}
      <div className="flex items-center justify-between mb-8">
        <div className="space-y-2">
          <Skeleton variant="line" width={200} height={32} />
          <Skeleton variant="line" width={300} height={16} />
        </div>
        <div className="flex gap-4">
           <Skeleton variant="circle" width={40} height={40} />
           <Skeleton variant="circle" width={40} height={40} />
        </div>
      </div>

      {/* Grid Content */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
         {[...Array(4)].map((_, i) => (
           <Skeleton key={i} variant="card" height={120} />
         ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
         <div className="lg:col-span-2 space-y-6">
            <Skeleton variant="card" height={400} />
         </div>
         <div className="space-y-6">
            <Skeleton variant="card" height={400} />
         </div>
      </div>
    </div>
  );
};

export default PageSkeleton;
