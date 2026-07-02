import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { 
  LayoutDashboard, 
  CreditCard, 
  ArrowLeftRight, 
  Send, 
  Landmark, 
  Bell, 
  User, 
  Shield, 
  LogOut,
  TrendingDown,
  ChevronRight,
  Briefcase
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { logoutUser } from '../../store/thunks/authThunks';
import { setSidebarOpen } from '../../store/slices/uiSlice';
import Modal from '../common/Modal';
import Button from '../common/Button';

const NavSection = ({ title, items, sidebarOpen }) => (
  <div className="mb-8">
    <h3 className={`px-6 text-[10px] font-bold text-muted-text uppercase tracking-[0.2em] mb-4 ${!sidebarOpen && 'hidden'}`}>
      {title}
    </h3>
    <ul className="space-y-1 px-3">
      {items.map((item) => (
        <li key={item.path}>
          <NavLink
            to={item.path}
            className={({ isActive }) => `
              group flex items-center px-4 py-3 rounded-xl transition-all duration-200
              ${isActive 
                ? 'bg-primary-500/10 text-primary-500 border-l-[3px] border-primary-500 rounded-l-none' 
                : 'text-muted-text hover:bg-surface-2 hover:text-primary-500'}
              ${!sidebarOpen ? 'justify-center px-0' : ''}
            `}
          >
            <item.icon size={sidebarOpen ? 20 : 24} className={sidebarOpen ? 'mr-3' : ''} />
            {sidebarOpen && (
              <span className="text-sm font-bold tracking-tight">{item.name}</span>
            )}
            {sidebarOpen && item.badge > 0 && (
              <span className="ml-auto w-5 h-5 bg-danger-500 text-white text-[10px] font-black rounded-full flex items-center justify-center">
                {item.badge > 9 ? '9+' : item.badge}
              </span>
            )}
          </NavLink>
        </li>
      ))}
    </ul>
  </div>
);

function Sidebar() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();
  const { sidebarOpen } = useSelector((state) => state.ui);
  const { unreadCount } = useSelector((state) => state.notification);

  const [showLogoutConfirm, setShowLogoutConfirm] = React.useState(false);

  const handleLogout = async () => {
    setShowLogoutConfirm(false);
    await dispatch(logoutUser());
    navigate('/login');
  };

  const isBankOperator = user?.role === 'BANK_OPERATOR' || user?.role === 'ROLE_BANK_OPERATOR';
  let sections = [];

  if (isAdmin || isBankOperator) {
    // Admin/Operator Specific Sections
    if (isAdmin) {
      sections.push({
        title: 'System',
        items: [
          { name: 'Admin Panel', path: '/admin', icon: Shield },
        ]
      });
    }

    if (isBankOperator) {
      sections.push({
        title: 'Operations',
        items: [
          { name: 'Operator Panel', path: '/bank-operator', icon: Briefcase },
        ]
      });
    }

    // Common Admin/Operator Account Sections
    sections.push({
      title: 'Account',
      items: [
        { name: 'Notifications', path: '/notifications', icon: Bell, badge: unreadCount },
        { name: 'Profile', path: '/profile', icon: User },
      ]
    });
  } else {
    // Normal User Sections
    sections = [
      {
        title: 'Main',
        items: [
          { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
          { name: 'Accounts', path: '/accounts', icon: CreditCard },
          { name: 'Transactions', path: '/transactions', icon: ArrowLeftRight },
          { name: 'Transfer', path: '/transfer', icon: Send },
        ]
      },
      {
        title: 'Banking',
        items: [
          { name: 'Loans', path: '/loans', icon: Landmark },
        ]
      },
      {
        title: 'Account',
        items: [
          { name: 'Notifications', path: '/notifications', icon: Bell, badge: unreadCount },
          { name: 'Profile', path: '/profile', icon: User },
        ]
      }
    ];
  }

  return (
    <>
      {/* Backdrop for mobile */}
      <div 
        className={`fixed inset-0 bg-dark-950/40 backdrop-blur-sm z-40 lg:hidden transition-opacity duration-300 ${sidebarOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
        onClick={() => dispatch(setSidebarOpen(false))}
      />

      <aside 
        className={`
          fixed left-0 top-0 bottom-0 z-50 bg-surface border-r border-border-subtle 
          transition-all duration-300 ease-in-out flex flex-col
          ${sidebarOpen ? 'w-[260px]' : 'w-20 -translate-x-full lg:translate-x-0'}
        `}
      >
        {/* Logo */}
        <div className="h-20 flex items-center px-6 gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-primary-600 to-secondary-500 flex items-center justify-center shadow-lg shadow-primary-900/40 shrink-0">
            <TrendingDown size={22} className="text-white" />
          </div>
          {sidebarOpen && (
            <div className="flex flex-col">
              <span className="text-lg font-black text-app-text leading-none">Credixa</span>
               <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest mt-0.5">Pro</span>
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto custom-scrollbar py-4">
          {sections.map(section => (
            <NavSection 
              key={section.title} 
              title={section.title} 
              items={section.items} 
              sidebarOpen={sidebarOpen} 
            />
          ))}
        </nav>

        {/* User Card */}
        <div className="p-4 border-t border-border-subtle bg-surface-2/50">
          <div className={`flex items-center ${sidebarOpen ? 'gap-3' : 'justify-center'}`}>
            <div className="w-10 h-10 rounded-full bg-surface-2 border border-border-subtle flex items-center justify-center font-bold text-primary-500 shrink-0">
              {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
            </div>
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-bold text-app-text truncate">{user?.firstName} {user?.lastName}</p>
                <p className="text-[10px] font-medium text-muted-text truncate">{user?.email}</p>
              </div>
            )}
          </div>
          
          <button
            onClick={() => setShowLogoutConfirm(true)}
            className={`
              mt-4 w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-danger-500 
              hover:bg-danger-500/10 transition-all font-bold text-xs uppercase tracking-widest
              ${!sidebarOpen ? 'justify-center px-0' : ''}
            `}
          >
            <LogOut size={18} />
            {sidebarOpen && <span>Logout</span>}
          </button>
        </div>
      </aside>
      
      <Modal
        isOpen={showLogoutConfirm}
        onClose={() => setShowLogoutConfirm(false)}
        title="Session Termination"
        size="sm"
      >
        <div className="flex flex-col items-center text-center space-y-6 py-4">
          <div className="w-20 h-20 rounded-3xl bg-danger-500/10 flex items-center justify-center text-danger-500 shadow-xl shadow-danger-500/5">
            <LogOut size={40} />
          </div>
          <div className="space-y-2">
            <h3 className="text-xl font-black text-app-text tracking-tight">Ready to leave?</h3>
            <p className="text-sm font-medium text-muted-text max-w-[240px]">
              Securely sign out of your Credixa Pro session. All active operations will be synchronized.
            </p>
          </div>
          <div className="flex flex-col w-full gap-3 pt-4">
            <Button variant="danger" fullWidth onClick={handleLogout} className="py-4">
              Confirm Logout
            </Button>
            <Button variant="ghost" fullWidth onClick={() => setShowLogoutConfirm(false)}>
              Stay Connected
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

export default React.memo(Sidebar);
