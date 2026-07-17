import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import SpinModal from '../common/SpinModal';
import { setGlobalLoading } from '../../store/slices/uiSlice';
import { setUser } from '../../store/slices/authSlice';

// Placeholder for unread count fetch - would be in notificationThunks
// export const fetchUnreadCount = createAsyncThunk(...)

export default function AppLayout() {
  const dispatch = useDispatch();
  const { sidebarOpen, theme } = useSelector((state) => state.ui);
  const { user } = useSelector((state) => state.auth);
  const [showSetSpin, setShowSetSpin] = React.useState(false);

  React.useEffect(() => {
    if (user && user.role === 'USER' && !user.spinSet) {
      setShowSetSpin(true);
    }
  }, [user]);

  React.useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  // Initialization
  useEffect(() => {
    // Poll for notifications as fallback
    const interval = setInterval(() => {
      // dispatch(fetchUnreadCount());
    }, 60000);

    return () => {
      clearInterval(interval);
      // disconnect(); 
    };
  }, [dispatch]);

  return (
    <div className="min-h-screen bg-app-bg flex transition-colors duration-300">
      <Sidebar />
      
      <div 
        className={`
          flex-1 flex flex-col min-w-0 transition-all duration-300
          ${sidebarOpen ? 'lg:ml-[260px]' : 'lg:ml-20'}
        `}
      >
        <TopBar />
        
        <main className="flex-1 p-6 lg:p-10 overflow-y-auto overflow-x-hidden relative">
          {/* Subtle background glow */}
          <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-primary-600/5 rounded-full blur-[120px] -z-10 pointer-events-none" />
          <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-secondary-600/5 rounded-full blur-[120px] -z-10 pointer-events-none" />
          
          <div className="max-w-7xl mx-auto w-full">
            <Outlet />
          </div>
        </main>
      </div>

      <SpinModal 
        isOpen={showSetSpin}
        onClose={() => setShowSetSpin(false)}
        mode="set"
        mandatory={true}
        title="Initialize Security Protocol"
        description="To ensure your assets remain protected, please initialize your 6-digit Secret PIN (sPin) before proceeding."
      />
    </div>
  );
}
