import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { formatCurrency } from '../../utils/formatters';

const TradingChart = ({ data }) => {
  const defaultData = [
    { name: 'Mon', value: 4000 },
    { name: 'Tue', value: 3000 },
    { name: 'Wed', value: 2000 },
    { name: 'Thu', value: 2780 },
    { name: 'Fri', value: 1890 },
    { name: 'Sat', value: 2390 },
    { name: 'Sun', value: 3490 },
  ];

  const chartData = data || defaultData;

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div className="glass-card p-4 border border-primary-500/20 shadow-2xl">
          <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">{label}</p>
          <p className="text-lg font-black text-primary-500">{formatCurrency(payload[0].value)}</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="h-[350px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="var(--primary-color)" stopOpacity={0.3} />
              <stop offset="95%" stopColor="var(--primary-color)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-color)" />
          <XAxis 
            dataKey="name" 
            axisLine={false} 
            tickLine={false} 
            tick={{ fill: 'var(--muted-text)', fontSize: 10, fontWeight: 700 }}
            dy={10}
          />
          <YAxis 
            hide={true} 
            domain={['auto', 'auto']}
          />
          <Tooltip content={<CustomTooltip />} cursor={{ stroke: 'var(--primary-color)', strokeWidth: 1, strokeDasharray: '4 4' }} />
          <Area
            type="monotone"
            dataKey="value"
            stroke="var(--primary-color)"
            strokeWidth={3}
            fillOpacity={1}
            fill="url(#colorValue)"
            animationDuration={2000}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TradingChart;
