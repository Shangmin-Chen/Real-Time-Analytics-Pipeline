import React from 'react';
import styled from 'styled-components';
import { motion, AnimatePresence } from 'framer-motion';
import { AlertCircle, AlertTriangle, Info, CheckCircle } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

const AlertsContainer = styled.div`
  max-height: 400px;
  overflow-y: auto;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
  }
`;

const AlertItem = styled(motion.div)`
  background: ${props => {
    if (props.resolved) return '#f0fdf4';
    switch (props.severity) {
      case 'CRITICAL': return '#fef2f2';
      case 'HIGH': return '#fef3c7';
      case 'MEDIUM': return '#fef3c7';
      case 'LOW': return '#f0f9ff';
      default: return '#f9fafb';
    }
  }};
  border-left: 4px solid ${props => {
    if (props.resolved) return '#10b981';
    switch (props.severity) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f59e0b';
      case 'MEDIUM': return '#f59e0b';
      case 'LOW': return '#3b82f6';
      default: return '#6b7280';
    }
  }};
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
`;

const AlertHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
`;

const AlertTitle = styled.h4`
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const AlertTime = styled.span`
  font-size: 0.75rem;
  color: #6b7280;
`;

const AlertDescription = styled.p`
  margin: 0 0 8px 0;
  font-size: 0.85rem;
  color: #4b5563;
  line-height: 1.4;
`;

const AlertType = styled.span`
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 2px 8px;
  border-radius: 12px;
  background: ${props => {
    switch (props.severity) {
      case 'CRITICAL': return '#fecaca';
      case 'HIGH': return '#fed7aa';
      case 'MEDIUM': return '#fed7aa';
      case 'LOW': return '#bfdbfe';
      default: return '#e5e7eb';
    }
  }};
  color: ${props => {
    switch (props.severity) {
      case 'CRITICAL': return '#dc2626';
      case 'HIGH': return '#d97706';
      case 'MEDIUM': return '#d97706';
      case 'LOW': return '#2563eb';
      default: return '#6b7280';
    }
  }};
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
`;

const EmptyIcon = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #9ca3af;
`;

const EmptyTitle = styled.h3`
  margin: 0 0 8px 0;
  font-size: 1rem;
  font-weight: 600;
  color: #4b5563;
`;

const EmptyDescription = styled.p`
  margin: 0;
  font-size: 0.85rem;
  color: #6b7280;
`;

const getSeverityIcon = (severity, resolved) => {
  if (resolved) return CheckCircle;
  switch (severity) {
    case 'CRITICAL': return AlertCircle;
    case 'HIGH': return AlertTriangle;
    case 'MEDIUM': return AlertTriangle;
    case 'LOW': return Info;
    default: return Info;
  }
};

export function AlertsPanel({ alerts }) {
  const sortedAlerts = alerts.sort((a, b) => {
    // Unresolved alerts first, then by timestamp
    if (a.resolved !== b.resolved) {
      return a.resolved ? 1 : -1;
    }
    return b.timestamp - a.timestamp;
  });

  if (sortedAlerts.length === 0) {
    return (
      <EmptyState>
        <EmptyIcon>
          <CheckCircle size={24} />
        </EmptyIcon>
        <EmptyTitle>All Clear</EmptyTitle>
        <EmptyDescription>No active alerts at this time</EmptyDescription>
      </EmptyState>
    );
  }

  return (
    <AlertsContainer>
      <AnimatePresence>
        {sortedAlerts.map((alert) => {
          const Icon = getSeverityIcon(alert.severity, alert.resolved);
          
          return (
            <AlertItem
              key={alert.id}
              severity={alert.severity}
              resolved={alert.resolved}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
              whileHover={{ scale: 1.02 }}
            >
              <AlertHeader>
                <AlertTitle>
                  <Icon size={16} />
                  {alert.title}
                </AlertTitle>
                <AlertTime>
                  {formatDistanceToNow(new Date(alert.timestamp), { addSuffix: true })}
                </AlertTime>
              </AlertHeader>
              
              <AlertDescription>
                {alert.description}
              </AlertDescription>
              
              <AlertType severity={alert.severity}>
                {alert.type.replace(/_/g, ' ').toLowerCase()}
              </AlertType>
            </AlertItem>
          );
        })}
      </AnimatePresence>
    </AlertsContainer>
  );
}
