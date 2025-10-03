import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { motion, AnimatePresence } from 'framer-motion';
import { Activity, Users, TrendingUp, Clock, AlertCircle, CheckCircle } from 'lucide-react';

import { useWebSocket } from './hooks/useWebSocket';
import { MetricCard } from './components/MetricCard';
import { RealTimeChart } from './components/RealTimeChart';
import { AlertsPanel } from './components/AlertsPanel';
import { PerformanceMetrics } from './components/PerformanceMetrics';
import { ConnectionStatus } from './components/ConnectionStatus';

const AppContainer = styled.div`
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
`;

const Header = styled(motion.header)`
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
`;

const Title = styled.h1`
  margin: 0;
  font-size: 2.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-align: center;
`;

const Subtitle = styled.p`
  margin: 8px 0 0 0;
  font-size: 1.1rem;
  color: #6b7280;
  text-align: center;
`;

const MetricsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
`;

const ChartsGrid = styled.div`
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  margin-bottom: 24px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
`;

const ChartCard = styled(motion.div)`
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
`;

const ChartTitle = styled.h3`
  margin: 0 0 20px 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const StatusBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 16px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
`;

const StatusItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #6b7280;
`;

function App() {
  const { metrics, isConnected, connectionStatus } = useWebSocket();
  const [alerts, setAlerts] = useState([]);
  const [performanceData, setPerformanceData] = useState({
    latency: 0,
    throughput: 0,
    errorRate: 0
  });

  // Mock performance data updates
  useEffect(() => {
    const interval = setInterval(() => {
      setPerformanceData(prev => ({
        latency: Math.random() * 200 + 100, // 100-300ms
        throughput: Math.random() * 500 + 8000, // 8000-8500 events/min
        errorRate: Math.random() * 0.5 // 0-0.5%
      }));
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  // Mock alerts
  useEffect(() => {
    const mockAlerts = [
      {
        id: 1,
        type: 'HIGH_BOUNCE_RATE',
        severity: 'MEDIUM',
        title: 'High Bounce Rate Detected',
        description: 'User user_1234 viewed only one page in 5-minute window',
        timestamp: Date.now() - 120000,
        resolved: false
      },
      {
        id: 2,
        type: 'CONVERSION_FUNNEL_BREACH',
        severity: 'HIGH',
        title: 'Conversion Funnel Breach',
        description: 'Conversion rate dropped below 2% threshold',
        timestamp: Date.now() - 300000,
        resolved: false
      },
      {
        id: 3,
        type: 'TRAFFIC_ANOMALY',
        severity: 'LOW',
        title: 'Traffic Spike Detected',
        description: 'Page views increased by 150% in the last hour',
        timestamp: Date.now() - 600000,
        resolved: true
      }
    ];
    setAlerts(mockAlerts);
  }, []);

  const metricCards = [
    {
      title: 'Page Views/sec',
      value: metrics.pageViewsPerSecond || 0,
      icon: Activity,
      color: '#3b82f6',
      format: (value) => value.toFixed(1),
      suffix: '/sec'
    },
    {
      title: 'Active Users',
      value: metrics.activeUsers || 0,
      icon: Users,
      color: '#10b981',
      format: (value) => value.toLocaleString(),
      suffix: ''
    },
    {
      title: 'Conversion Rate',
      value: (metrics.conversionRate || 0) * 100,
      icon: TrendingUp,
      color: '#f59e0b',
      format: (value) => value.toFixed(2),
      suffix: '%'
    },
    {
      title: 'Avg Session Duration',
      value: metrics.averageSessionDuration || 0,
      icon: Clock,
      color: '#8b5cf6',
      format: (value) => {
        const minutes = Math.floor(value / 60);
        const seconds = Math.floor(value % 60);
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
      },
      suffix: ''
    }
  ];

  return (
    <AppContainer>
      <Header
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        <Title>Real-Time Analytics Dashboard</Title>
        <Subtitle>
          Live website analytics with sub-500ms latency • 10K+ events/minute processing
        </Subtitle>
      </Header>

      <MetricsGrid>
        <AnimatePresence>
          {metricCards.map((card, index) => (
            <MetricCard
              key={card.title}
              {...card}
              index={index}
              isConnected={isConnected}
            />
          ))}
        </AnimatePresence>
      </MetricsGrid>

      <ChartsGrid>
        <ChartCard
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <ChartTitle>
            <Activity size={20} />
            Real-Time Metrics
          </ChartTitle>
          <RealTimeChart metrics={metrics} />
        </ChartCard>

        <ChartCard
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
        >
          <ChartTitle>
            <AlertCircle size={20} />
            Alerts & Notifications
          </ChartTitle>
          <AlertsPanel alerts={alerts} />
        </ChartCard>
      </ChartsGrid>

      <ChartCard
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.6 }}
      >
        <ChartTitle>
          <CheckCircle size={20} />
          Pipeline Performance
        </ChartTitle>
        <PerformanceMetrics data={performanceData} />
      </ChartCard>

      <StatusBar
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.8 }}
      >
        <StatusItem>
          <ConnectionStatus isConnected={isConnected} status={connectionStatus} />
        </StatusItem>
        <StatusItem>
          Last Update: {new Date(metrics.timestamp || Date.now()).toLocaleTimeString()}
        </StatusItem>
        <StatusItem>
          Latency: {performanceData.latency.toFixed(0)}ms
        </StatusItem>
        <StatusItem>
          Throughput: {performanceData.throughput.toFixed(0)} events/min
        </StatusItem>
      </StatusBar>
    </AppContainer>
  );
}

export default App;
