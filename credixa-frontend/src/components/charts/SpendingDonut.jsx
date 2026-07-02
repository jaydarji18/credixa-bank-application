import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { formatCurrency } from '../../utils/formatters';

const SpendingDonut = ({ data, total }) => {
  const COLORS = ['#3b82f6', '#6366f1', '#10b981', '#f59e0b', '#64748b'];

  const defaultData = [
    { name: 'Food', value: 400 },
    { name: 'Shopping', value: 300 },
    { name: 'Utilities', value: 300 },
    { name: 'Transport', value: 200 },
    { name: 'Other', value: 100 },
  ];

  const chartData = data || defaultData;
  const chartTotal = total || chartData.reduce((acc, curr) => acc + curr.value, 0);

  return (
    <div className="h-[350px] w-full relative">
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        <span className="text-muted-text text-[10px] font-bold uppercase tracking-[0.2em] mb-1">Spent</span>
        <span className="text-xl font-black text-app-text">{formatCurrency(chartTotal)}</span>
      </div>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={80}
            outerRadius={100}
            paddingAngle={8}
            dataKey="value"
            animationBegin={200}
            animationDuration={1500}
          >
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="none" />
            ))}
          </Pie>
          <Tooltip 
            contentStyle={{ 
              backgroundColor: 'var(--surface-color)', 
              border: '1px solid var(--border-color)',
              borderRadius: '12px',
              fontSize: '12px',
              fontWeight: 'bold',
              boxShadow: 'var(--card-shadow)'
            }}
            itemStyle={{ color: 'var(--text-color)' }}
            formatter={(value) => formatCurrency(value)}
          />
          <Legend 
            verticalAlign="bottom" 
            height={36}
            iconType="circle"
            formatter={(value) => <span className="text-xs font-bold text-muted-text ml-2 uppercase tracking-widest">{value}</span>}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};

export default SpendingDonut;
