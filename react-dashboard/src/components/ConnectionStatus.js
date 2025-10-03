import React from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { Wifi, WifiOff, RotateCcw } from 'lucide-react';

const StatusContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 500;
`;

const StatusIcon = styled(motion.div)`
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${props => props.color};
`;

const StatusText = styled.span`
  color: ${props => props.color};
`;

const getStatusConfig = (isConnected, connectionStatus) => {
  if (isConnected) {
    return {
      icon: Wifi,
      color: '#10b981',
      text: 'Connected'
    };
  }
  
  switch (connectionStatus) {
    case 'reconnecting':
      return {
        icon: RotateCcw,
        color: '#f59e0b',
        text: connectionStatus
      };
    case 'failed':
      return {
        icon: WifiOff,
        color: '#ef4444',
        text: 'Failed'
      };
    case 'error':
      return {
        icon: WifiOff,
        color: '#ef4444',
        text: 'Error'
      };
    default:
      return {
        icon: WifiOff,
        color: '#6b7280',
        text: 'Disconnected'
      };
  }
};

export function ConnectionStatus({ isConnected, status }) {
  const config = getStatusConfig(isConnected, status);
  const Icon = config.icon;

  return (
    <StatusContainer>
      <StatusIcon
        color={config.color}
        animate={isConnected ? { scale: [1, 1.2, 1] } : {}}
        transition={{ duration: 2, repeat: Infinity }}
      >
        <Icon size={16} />
      </StatusIcon>
      <StatusText color={config.color}>
        {config.text}
      </StatusText>
    </StatusContainer>
  );
}
