import { useState, useEffect, useRef } from 'react';

/**
 * Custom hook for managing WebSocket connection to the analytics server
 * Provides real-time metrics updates with automatic reconnection
 */
export function useWebSocket() {
  const [metrics, setMetrics] = useState({});
  const [isConnected, setIsConnected] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  useEffect(() => {
    const connect = () => {
      try {
        const wsUrl = process.env.REACT_APP_WEBSOCKET_URL || 'ws://localhost:8082';
        wsRef.current = new WebSocket(wsUrl);

        wsRef.current.onopen = () => {
          console.log('WebSocket connected');
          setIsConnected(true);
          setConnectionStatus('connected');
          reconnectAttempts.current = 0;
        };

        wsRef.current.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            setMetrics(prevMetrics => ({
              ...prevMetrics,
              ...data,
              timestamp: Date.now()
            }));
          } catch (error) {
            console.error('Error parsing WebSocket message:', error);
          }
        };

        wsRef.current.onclose = () => {
          console.log('WebSocket disconnected');
          setIsConnected(false);
          setConnectionStatus('disconnected');
          
          // Attempt to reconnect
          if (reconnectAttempts.current < maxReconnectAttempts) {
            reconnectAttempts.current++;
            const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000);
            
            setConnectionStatus(`reconnecting (${reconnectAttempts.current}/${maxReconnectAttempts})`);
            
            reconnectTimeoutRef.current = setTimeout(() => {
              connect();
            }, delay);
          } else {
            setConnectionStatus('failed');
          }
        };

        wsRef.current.onerror = (error) => {
          console.error('WebSocket error:', error);
          setConnectionStatus('error');
        };

      } catch (error) {
        console.error('Error creating WebSocket connection:', error);
        setConnectionStatus('error');
      }
    };

    connect();

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  const sendMessage = (message) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    }
  };

  const subscribeToMetrics = (metricTypes) => {
    sendMessage({
      action: 'subscribe',
      metrics: metricTypes
    });
  };

  return {
    metrics,
    isConnected,
    connectionStatus,
    sendMessage,
    subscribeToMetrics
  };
}
