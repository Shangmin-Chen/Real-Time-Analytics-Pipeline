import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { format, subMinutes } from 'date-fns';

const ChartContainer = styled.div`
  height: 300px;
  width: 100%;
`;

const Legend = styled.div`
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  flex-wrap: wrap;
`;

const LegendItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
`;

const LegendDot = styled.div`
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: ${props => props.color};
`;

export function RealTimeChart({ metrics }) {
  const [chartData, setChartData] = useState([]);
  const [maxDataPoints] = useState(20); // Keep last 20 data points

  useEffect(() => {
    if (metrics.timestamp && metrics.pageViewsPerSecond !== undefined) {
      const newDataPoint = {
        timestamp: format(new Date(metrics.timestamp), 'HH:mm:ss'),
        pageViews: metrics.pageViewsPerSecond || 0,
        activeUsers: metrics.activeUsers || 0,
        conversionRate: (metrics.conversionRate || 0) * 100,
        sessionDuration: metrics.averageSessionDuration || 0
      };

      setChartData(prev => {
        const updated = [...prev, newDataPoint];
        return updated.slice(-maxDataPoints);
      });
    }
  }, [metrics, maxDataPoints]);

  // Initialize with empty data if no data yet
  useEffect(() => {
    if (chartData.length === 0) {
      const emptyData = Array.from({ length: 10 }, (_, i) => ({
        timestamp: format(subMinutes(new Date(), 9 - i), 'HH:mm:ss'),
        pageViews: 0,
        activeUsers: 0,
        conversionRate: 0,
        sessionDuration: 0
      }));
      setChartData(emptyData);
    }
  }, [chartData.length]);

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{
          background: 'rgba(255, 255, 255, 0.95)',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: '12px',
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)'
        }}>
          <p style={{ margin: '0 0 8px 0', fontWeight: '600', color: '#1f2937' }}>
            {label}
          </p>
          {payload.map((entry, index) => (
            <p key={index} style={{ margin: '4px 0', color: entry.color, fontSize: '0.9rem' }}>
              {`${entry.dataKey}: ${entry.value.toFixed(2)}`}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <>
      <ChartContainer>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis 
              dataKey="timestamp" 
              stroke="#6b7280"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="#6b7280"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip content={<CustomTooltip />} />
            <Line 
              type="monotone" 
              dataKey="pageViews" 
              stroke="#3b82f6" 
              strokeWidth={3}
              dot={false}
              activeDot={{ r: 6, fill: '#3b82f6' }}
            />
            <Line 
              type="monotone" 
              dataKey="activeUsers" 
              stroke="#10b981" 
              strokeWidth={3}
              dot={false}
              activeDot={{ r: 6, fill: '#10b981' }}
            />
            <Line 
              type="monotone" 
              dataKey="conversionRate" 
              stroke="#f59e0b" 
              strokeWidth={3}
              dot={false}
              activeDot={{ r: 6, fill: '#f59e0b' }}
            />
            <Line 
              type="monotone" 
              dataKey="sessionDuration" 
              stroke="#8b5cf6" 
              strokeWidth={3}
              dot={false}
              activeDot={{ r: 6, fill: '#8b5cf6' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartContainer>
      
      <Legend>
        <LegendItem>
          <LegendDot color="#3b82f6" />
          Page Views/sec
        </LegendItem>
        <LegendItem>
          <LegendDot color="#10b981" />
          Active Users
        </LegendItem>
        <LegendItem>
          <LegendDot color="#f59e0b" />
          Conversion Rate (%)
        </LegendItem>
        <LegendItem>
          <LegendDot color="#8b5cf6" />
          Session Duration (s)
        </LegendItem>
      </Legend>
    </>
  );
}
