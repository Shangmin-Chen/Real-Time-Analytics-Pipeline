import React from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { Gauge, Activity, Zap, AlertTriangle } from 'lucide-react';

const MetricsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
`;

const MetricItem = styled(motion.div)`
  background: linear-gradient(135deg, ${props => props.color}20, ${props => props.color}10);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid ${props => props.color}30;
`;

const MetricIcon = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: ${props => props.color};
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  color: white;
`;

const MetricValue = styled.div`
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 4px;
`;

const MetricLabel = styled.div`
  font-size: 0.9rem;
  color: #6b7280;
  font-weight: 500;
`;

const MetricUnit = styled.span`
  font-size: 1.2rem;
  font-weight: 500;
  color: #9ca3af;
`;

const StatusIndicator = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 0.8rem;
  font-weight: 500;
  color: ${props => props.status === 'good' ? '#10b981' : props.status === 'warning' ? '#f59e0b' : '#ef4444'};
`;

const StatusDot = styled.div`
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: ${props => props.status === 'good' ? '#10b981' : props.status === 'warning' ? '#f59e0b' : '#ef4444'};
  animation: ${props => props.status === 'good' ? 'pulse 2s infinite' : 'none'};

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
`;

const GaugeContainer = styled.div`
  position: relative;
  width: 120px;
  height: 60px;
  margin: 0 auto 12px;
`;

const GaugeSvg = styled.svg`
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
`;

const GaugeBackground = styled.circle`
  fill: none;
  stroke: #e5e7eb;
  stroke-width: 8;
`;

const GaugeFill = styled.circle`
  fill: none;
  stroke: ${props => props.color};
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
`;

const GaugeText = styled.text`
  font-size: 1.2rem;
  font-weight: 700;
  fill: #1f2937;
  text-anchor: middle;
  dominant-baseline: middle;
`;

function CircularGauge({ value, max, color, unit }) {
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const percentage = Math.min(value / max, 1);
  const strokeDasharray = `${circumference * percentage} ${circumference}`;

  return (
    <GaugeContainer>
      <GaugeSvg>
        <GaugeBackground cx="60" cy="60" r={radius} />
        <GaugeFill
          cx="60"
          cy="60"
          r={radius}
          strokeDasharray={strokeDasharray}
          color={color}
        />
      </GaugeSvg>
      <div style={{
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        fontSize: '1.2rem',
        fontWeight: '700',
        color: '#1f2937'
      }}>
        {value.toFixed(0)}{unit}
      </div>
    </GaugeContainer>
  );
}

function getLatencyStatus(latency) {
  if (latency < 200) return 'good';
  if (latency < 400) return 'warning';
  return 'critical';
}

function getThroughputStatus(throughput) {
  if (throughput > 9500) return 'good';
  if (throughput > 8500) return 'warning';
  return 'critical';
}

function getErrorRateStatus(errorRate) {
  if (errorRate < 0.1) return 'good';
  if (errorRate < 0.5) return 'warning';
  return 'critical';
}

export function PerformanceMetrics({ data }) {
  const { latency, throughput, errorRate } = data;

  const metrics = [
    {
      key: 'latency',
      label: 'End-to-End Latency',
      value: latency,
      unit: 'ms',
      color: '#3b82f6',
      icon: Zap,
      max: 500,
      status: getLatencyStatus(latency),
      target: '< 500ms'
    },
    {
      key: 'throughput',
      label: 'Events/Minute',
      value: throughput,
      unit: '',
      color: '#10b981',
      icon: Activity,
      max: 12000,
      status: getThroughputStatus(throughput),
      target: '> 10K/min'
    },
    {
      key: 'errorRate',
      label: 'Error Rate',
      value: errorRate,
      unit: '%',
      color: '#f59e0b',
      icon: AlertTriangle,
      max: 2,
      status: getErrorRateStatus(errorRate),
      target: '< 1%'
    }
  ];

  return (
    <MetricsGrid>
      {metrics.map((metric, index) => {
        const Icon = metric.icon;
        
        return (
          <MetricItem
            key={metric.key}
            color={metric.color}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: index * 0.1 }}
            whileHover={{ scale: 1.05, transition: { duration: 0.2 } }}
          >
            <MetricIcon color={metric.color}>
              <Icon size={24} />
            </MetricIcon>
            
            <CircularGauge
              value={metric.value}
              max={metric.max}
              color={metric.color}
              unit={metric.unit}
            />
            
            <MetricLabel>{metric.label}</MetricLabel>
            
            <StatusIndicator status={metric.status}>
              <StatusDot status={metric.status} />
              {metric.status === 'good' ? 'Optimal' : metric.status === 'warning' ? 'Warning' : 'Critical'}
            </StatusIndicator>
          </MetricItem>
        );
      })}
    </MetricsGrid>
  );
}
