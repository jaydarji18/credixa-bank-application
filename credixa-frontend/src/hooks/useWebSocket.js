import { useEffect, useRef, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { store } from '../store';
import { addNotification } from '../store/slices/notificationSlice';
import { updateAccountBalance } from '../store/slices/accountSlice';
import { formatCurrency } from '../utils/formatters';
import { toast } from 'react-hot-toast';

const SOCKET_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export const useWebSocket = () => {
  const dispatch = useDispatch();
  const { user, isAuthenticated } = useSelector((state) => state.auth);
  const stompClientRef = useRef(null);

  const connect = useCallback(() => {
    if (!isAuthenticated || !user) return;

    const socket = new SockJS(SOCKET_URL);
    const accessToken = store.getState().auth.accessToken || localStorage.getItem('accessToken');
    
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      debug: (str) => {
        // console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = (frame) => {
      // console.log('Connected: ' + frame);
      
      // Personal notifications
      client.subscribe(`/user/${user.id}/queue/notifications`, (message) => {
        const notification = JSON.parse(message.body);
        dispatch(addNotification(notification));
      });

      // Balance updates
      client.subscribe(`/user/queue/balance`, (message) => {
        const update = JSON.parse(message.body);
        dispatch(updateAccountBalance(update));
        toast.success(`Balance updated — ${formatCurrency(update.newBalance)}`, {
          icon: '💰',
          style: {
            borderRadius: '12px',
            background: '#111827',
            color: '#fff',
            border: '1px solid rgba(59, 130, 246, 0.2)'
          }
        });
      });

      // Public notifications
      client.subscribe('/topic/public', (message) => {
        const notification = JSON.parse(message.body);
        dispatch(addNotification(notification));
      });
    };

    client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    client.activate();
    stompClientRef.current = client;
  }, [user, isAuthenticated, dispatch]);

  const disconnect = useCallback(() => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
    }
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return { connect, disconnect };
};

export default useWebSocket;
