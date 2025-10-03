import React from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { format } from 'date-fns';

const Card = styled(motion.div)`
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 2px solid ${props => props.isConnected ? props.color : '#e5e7eb'};
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, ${props => props.color}, ${props => props.color}88);
    opacity: ${props => props.isConnected ? 1 : 0.3};
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
`;

const Title = styled.h3`
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
`;

const IconContainer = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: ${props => props.color}20;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${props => props.color};
`;

const Value = styled.div`
  font-size: 2.5rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
  display: flex;
  align-items: baseline;
  gap: 4px;
`;

const Suffix = styled.span`
  font-size: 1.2rem;
  font-weight: 500;
  color: #6b7280;
`;

const StatusIndicator = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: #6b7280;
`;

const StatusDot = styled.div`
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: ${props => props.isConnected ? '#10b981' : '#ef4444'};
  animation: ${props => props.isConnected ? 'pulse 2s infinite' : 'none'};

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
`;

export function MetricCard({ 
  title, 
  value, 
  icon: Icon, 
  color, 
  format, 
  suffix, 
  index, 
  isConnected 
}) {
  const formattedValue = format ? format(value) : value;
  const [displayValue, setDisplayValue] = React.useState(0);

  // Animate value changes
  React.useEffect(() => {
    const targetValue = typeof value === 'number' ? value : 0;
    const startValue = displayValue;
    const duration = 1000;
    const startTime = Date.now();

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // Easing function for smooth animation
      const easeOutCubic = 1 - Math.pow(1 - progress, 3);
      const currentValue = startValue + (targetValue - startValue) * easeOutCubic;
      
      setDisplayValue(currentValue);
      
      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };

    requestAnimationFrame(animate);
  }, [value]);

  return (
    <Card
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: index * 0.1 }}
      color={color}
      isConnected={isConnected}
      whileHover={{ y: -4, transition: { duration: 0.2 } }}
    >
      <Header>
        <Title>{title}</Title>
        <IconContainer color={color}>
          <Icon size={20} />
        </IconContainer>
      </Header>
      
      <Value>
        {typeof displayValue === 'number' ? displayValue.toLocaleString() : displayValue}
        {suffix && <Suffix>{suffix}</Suffix>}
      </Value>
      
      <StatusIndicator>
        <StatusDot isConnected={isConnected} />
        {isConnected ? 'Live' : 'Disconnected'}
      </StatusIndicator>
    </Card>
  );
}
