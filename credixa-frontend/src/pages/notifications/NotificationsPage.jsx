import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  Bell, 
  CheckCheck, 
  ArrowDownCircle, 
  ArrowUpCircle, 
  ArrowLeftRight, 
  ShieldAlert,
  Clock,
  ChevronDown,
  Info
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { formatDistanceToNow } from 'date-fns';

import { fetchNotifications, markNotificationRead } from '../../store/thunks/notificationThunks';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import useToast from '../../hooks/useToast';
import axiosInstance from '../../api/axios';
import { NOTIFICATIONS } from '../../api/endpoints';

const NotificationsPage = () => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { notifications, loading, unreadCount } = useSelector((state) => state.notification);
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    dispatch(fetchNotifications());
  }, [dispatch]);

  const handleMarkAllRead = async () => {
    try {
      await axiosInstance.put(NOTIFICATIONS.MARK_ALL_READ);
      dispatch(fetchNotifications());
      toast.success('All notifications marked as read');
    } catch (err) {
      toast.error('Failed to mark all as read');
    }
  };

  const handleToggleRead = async (notif) => {
    setExpandedId(expandedId === notif.id ? null : notif.id);
    if (!notif.isRead) {
      dispatch(markNotificationRead(notif.id));
    }
  };

  const getIcon = (type) => {
    switch(type) {
      case 'AMOUNT_CREDITED': 
      case 'TRANSACTION_SUCCESS': return <ArrowDownCircle className="text-success-500" size={20} />;
      case 'AMOUNT_DEBITED': 
      case 'TRANSACTION_FAILED': return <ArrowUpCircle className="text-danger-500" size={20} />;
      case 'LOGIN_ALERT': return <ShieldAlert className="text-warning-500" size={20} />;
      case 'LOAN_APPLIED':
      case 'LOAN_APPROVED': return <Clock className="text-secondary-500" size={20} />;
      case 'KYC_SUBMITTED':
      case 'KYC_APPROVED': return <CheckCheck className="text-primary-500" size={20} />;
      default: return <Info className="text-primary-500" size={20} />;
    }
  };

  const filteredNotifications = notifications.filter(n => {
    if (activeFilter === 'UNREAD') return !n.isRead;
    if (activeFilter === 'TRANSACTION') return n.notificationType.includes('TRANSACTION') || n.notificationType.includes('AMOUNT');
    if (activeFilter === 'ALERTS') return n.notificationType.includes('ALERT') || n.notificationType.includes('KYC');
    return true;
  });

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-slide-in">
       <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-3xl font-black text-app-text tracking-tighter">Notifications</h2>
            <p className="text-muted-text font-medium">Stay updated with your account activity and security alerts.</p>
          </div>
          <Button 
            variant="outline" 
            size="sm" 
            disabled={unreadCount === 0}
            onClick={handleMarkAllRead}
            leftIcon={<CheckCheck size={18} />}
          >
            Mark All as Read
          </Button>
       </div>

       <div className="flex gap-2 p-1.5 bg-surface-2 border border-border-subtle rounded-2xl w-fit">
          {['ALL', 'UNREAD', 'TRANSACTION', 'ALERTS'].map(f => (
            <button
              key={f}
              onClick={() => setActiveFilter(f)}
              className={`px-5 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${activeFilter === f ? 'bg-primary-500 text-white shadow-lg' : 'text-muted-text hover:text-primary-500'}`}
            >
              {f}
            </button>
          ))}
       </div>

       <div className="space-y-4">
          {loading ? (
             [...Array(5)].map((_, i) => <Skeleton key={i} height={80} variant="card" />)
          ) : filteredNotifications.length > 0 ? (
            filteredNotifications.map((notif) => (
              <motion.div
                key={notif.id}
                layout
                onClick={() => handleToggleRead(notif)}
                className={`
                  glass-card p-0 cursor-pointer overflow-hidden transition-all border-l-4
                  ${notif.isRead ? 'border-l-transparent' : 'border-l-primary-500 bg-primary-500/[0.02] shadow-lg shadow-primary-500/5'}
                `}
              >
                <div className="p-5 flex items-start gap-4">
                    <div className={`w-12 h-12 rounded-2xl bg-surface-2 border border-border-subtle flex items-center justify-center shrink-0`}>
                       {getIcon(notif.notificationType)}
                    </div>
                   <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-start gap-4">
                          <h4 className={`text-sm tracking-tight ${notif.isRead ? 'text-muted-text font-bold' : 'text-app-text font-black'}`}>
                            {notif.title}
                          </h4>
                         <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest flex items-center gap-1 whitespace-nowrap">
                            <Clock size={12} />
                            {formatDistanceToNow(new Date(notif.createdAt), { addSuffix: true })}
                         </span>
                      </div>
                       <p className={`text-xs mt-1 leading-relaxed ${notif.isRead ? 'text-muted-text' : 'text-app-text font-medium opacity-80'} ${expandedId === notif.id ? '' : 'truncate max-w-xl'}`}>
                        {notif.body}
                      </p>
                   </div>
                   <div className={`transition-transform duration-300 ${expandedId === notif.id ? 'rotate-180' : ''}`}>
                      <ChevronDown size={14} className="text-muted-text" />
                   </div>
                </div>
              </motion.div>
            ))
          ) : (
            <EmptyState 
              icon={Bell}
              title="You're all caught up! 🎉"
              message="No new notifications to display. We'll let you know when something important happens."
            />
          )}
       </div>
    </div>
  );
};

export default NotificationsPage;
