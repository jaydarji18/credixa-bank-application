import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { 
  Bell, 
  Menu, 
  Moon, 
  Sun, 
  User as UserIcon, 
  Settings, 
  LogOut,
  ChevronDown
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { toggleSidebar, setSidebarOpen, setTheme } from '../../store/slices/uiSlice';
import { useAuth } from '../../hooks/useAuth';
import { logoutUser } from '../../store/thunks/authThunks';

function TopBar() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const { theme, sidebarOpen } = useSelector((state) => state.ui);
  const { unreadCount } = useSelector((state) => state.notification);
  const [profileOpen, setProfileOpen] = useState(false);

  // Auto-generate title from path
  const getPageTitle = () => {
    const path = location.pathname.split('/')[1];
    if (!path) return 'Dashboard';
    return path.charAt(0).toUpperCase() + path.slice(1);
  };

  const handleLogout = async () => {
    await dispatch(logoutUser());
    navigate('/login');
  };

  return (
    <header className="h-20 sticky top-0 z-[45] bg-surface/80 backdrop-blur-xl border-b border-border-subtle flex items-center justify-between px-6 lg:px-10">
      <div className="flex items-center gap-4">
        <button
          onClick={() => dispatch(toggleSidebar())}
          className="p-2 rounded-xl hover:bg-surface-2 text-dark-400 lg:flex items-center hidden"
        >
          <Menu size={24} />
        </button>
        <button
          onClick={() => dispatch(setSidebarOpen(true))}
          className="p-2 rounded-xl hover:bg-surface-2 text-dark-400 flex lg:hidden"
        >
          <Menu size={24} />
        </button>
        <h1 className="text-xl font-black text-app-text tracking-tight">{getPageTitle()}</h1>
      </div>

      <div className="flex items-center gap-3 lg:gap-6">
        {/* Theme Toggle */}
        <button 
          onClick={() => dispatch(setTheme(theme === 'dark' ? 'light' : 'dark'))}
          className="p-2.5 rounded-xl bg-surface-2 border border-border-subtle text-muted-text hover:text-primary-500 transition-all shadow-sm"
        >
          {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
        </button>

        {/* Notifications */}
        <Link 
          to="/notifications"
          className="relative p-2.5 rounded-xl bg-surface-2 border border-border-subtle text-muted-text hover:text-primary-500 transition-all group shadow-sm"
        >
          <Bell size={20} />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 min-w-[20px] h-5 bg-danger-500 text-white text-[10px] font-black rounded-full flex items-center justify-center border-2 border-surface px-1">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Link>

        {/* User Dropdown */}
        <div className="relative">
          <button 
            onClick={() => setProfileOpen(!profileOpen)}
            className="flex items-center gap-3 p-1.5 lg:pr-3 rounded-full bg-surface-2 hover:bg-surface transition-all border border-border-subtle shadow-sm"
          >
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-primary-600 to-secondary-500 flex items-center justify-center text-xs font-black shadow-lg">
              {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
            </div>
            <span className="hidden lg:block text-sm font-bold text-app-text max-w-[120px] truncate">{user?.firstName}</span>
            <ChevronDown size={14} className={`text-dark-400 transition-transform ${profileOpen ? 'rotate-180' : ''}`} />
          </button>

          <AnimatePresence>
            {profileOpen && (
              <>
                <div className="fixed inset-0 z-0" onClick={() => setProfileOpen(false)} />
                <motion.div
                  initial={{ opacity: 0, y: 10, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 10, scale: 0.95 }}
                  className="absolute right-0 mt-3 w-56 bg-surface border border-border-subtle rounded-2xl shadow-2xl p-2 z-50 overflow-hidden"
                >
                  <div className="px-3 py-2 border-b border-border-subtle mb-2">
                    <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest leading-loose">Access Control</p>
                  </div>
                  
                  <Link 
                    to="/profile" 
                    className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-surface-2 text-sm font-semibold transition-all group"
                    onClick={() => setProfileOpen(false)}
                  >
                    <UserIcon size={18} className="text-dark-400 group-hover:text-primary-500" />
                    My Account
                  </Link>
                  
                  <button 
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-surface-2 text-sm font-semibold transition-all group"
                  >
                    <Settings size={18} className="text-dark-400 group-hover:text-primary-500" />
                    Settings
                  </button>

                  <div className="h-px bg-white/5 my-2" />

                  <button 
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-danger-500/10 text-sm font-bold text-danger-500 transition-all"
                  >
                    <LogOut size={18} />
                    Logout Session
                  </button>
                </motion.div>
              </>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}

export default React.memo(TopBar);
