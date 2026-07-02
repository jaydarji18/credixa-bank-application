import React, { useState, useEffect, useCallback } from 'react';
import { 
  Users, 
  UserPlus, 
  Clock, 
  Search, 
  CheckCircle,
  Activity,
  Database,
  Server,
  Cpu,
  ShieldCheck,
  Briefcase,
  AlertCircle
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import axiosInstance from '../../api/axios';
import { ADMIN } from '../../api/endpoints';
import { formatCurrency } from '../../utils/formatters';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Skeleton from '../../components/common/Skeleton';
import useToast from '../../hooks/useToast';

const BankOperatorDashboard = () => {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('kyc-pending');
  const [stats, setStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(true);

  // User Management State
  const [users, setUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [userFilters, setUserFilters] = useState({
    search: '',
    status: '',
    kyc: 'PENDING', // Default to pending for operator
    sort: 'newest'
  });
  const [userPagination, setUserPagination] = useState({ page: 0, size: 10, totalPages: 0 });

  // Fetch Stats
  const fetchStats = useCallback(async () => {
    try {
      const response = await axiosInstance.get(ADMIN.SYSTEM_SUMMARY);
      setStats(response);
    } catch (err) {
      console.error('Stats fetch failed');
    } finally {
      setLoadingStats(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, [fetchStats]);

  // Fetch Users
  const fetchUsers = useCallback(async () => {
    try {
      setLoadingUsers(true);
      const response = await axiosInstance.get(ADMIN.USERS, {
        params: {
          page: userPagination.page,
          size: userPagination.size,
          search: userFilters.search,
          status: userFilters.status,
          kycStatus: activeTab === 'kyc-pending' ? 'PENDING' : userFilters.kyc,
          sortBy: userFilters.sort === 'newest' ? 'createdAt' : 'firstName'
        }
      });
      setUsers(response.content);
      setUserPagination(prev => ({ ...prev, totalPages: response.totalPages }));
    } catch (err) {
      toast.error('Failed to fetch users');
    } finally {
      setLoadingUsers(false);
    }
  }, [userPagination.page, userPagination.size, userFilters, activeTab, toast]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Actions
  const handleApproveKYC = async (userCode) => {
    try {
      await axiosInstance.patch(`${ADMIN.USERS}/${userCode}/kyc`, { kycStatus: 'VERIFIED' });
      toast.success('KYC Verified successfully');
      fetchUsers();
      fetchStats();
    } catch (err) {
      toast.error('KYC verification failed');
    }
  };

  return (
    <div className="space-y-8 animate-slide-in pb-20">
       <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-3xl font-black text-app-text tracking-tighter">Operations Dashboard</h2>
            <p className="text-muted-text font-medium">Bank Operator Terminal • KYC Verification Priority</p>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 bg-secondary-500/10 border border-secondary-500/20 rounded-2xl">
              <ShieldCheck size={18} className="text-secondary-500" />
              <span className="text-[10px] font-black text-secondary-500 uppercase tracking-widest">Operator Session Active</span>
          </div>
       </div>

       {/* Top Stats Row */}
       <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            { label: 'Total Users', value: stats?.totalUsers || '0', icon: Users, color: 'text-primary-500', bg: 'bg-primary-500/10' },
            { label: 'Pending KYC', value: stats?.pendingKyc || '0', icon: Clock, color: 'text-warning-500', bg: 'bg-warning-500/10' },
            { label: 'New Today', value: stats?.newUsersToday || '0', icon: UserPlus, color: 'text-success-500', bg: 'bg-success-500/10' },
            { label: 'Operational Status', value: 'OPTIMAL', icon: Activity, color: 'text-secondary-500', bg: 'bg-secondary-500/10' },
          ].map((stat, i) => (
            <div key={i} className="glass-card flex flex-col gap-4 p-6 border-l-4 border-l-secondary-500/30 bg-surface shadow-sm">
               <div className={`w-12 h-12 rounded-2xl ${stat.bg} flex items-center justify-center ${stat.color}`}>
                  <stat.icon size={24} />
               </div>
               <div>
                  <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest leading-none mb-1">{stat.label}</p>
                  <p className="text-xl font-black text-app-text tracking-tight">{stat.value}</p>
               </div>
            </div>
          ))}
       </div>

       {/* KYC Focus Area */}
       <div className="bg-secondary-500/5 border border-secondary-500/10 rounded-3xl p-8">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-8">
              <div>
                <h3 className="text-xl font-black text-app-text flex items-center gap-2">
                    <Briefcase className="text-secondary-500" size={24} />
                    KYC Verification Queue
                </h3>
                <p className="text-sm text-muted-text mt-1">Manage and approve pending identity verifications.</p>
              </div>
              
              <div className="flex gap-2 p-1 bg-surface-2 rounded-xl border border-border-subtle">
                <button
                  onClick={() => setActiveTab('kyc-pending')}
                  className={`px-4 py-2 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === 'kyc-pending' ? 'bg-secondary-500 text-white' : 'text-muted-text hover:text-app-text'}`}
                >
                  Pending
                </button>
                <button
                  onClick={() => setActiveTab('all-users')}
                  className={`px-4 py-2 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === 'all-users' ? 'bg-secondary-500 text-white' : 'text-muted-text hover:text-app-text'}`}
                >
                  All Users
                </button>
              </div>
          </div>

          <div className="glass-card p-6 mb-6 flex flex-wrap gap-4 items-center">
              <div className="flex-1 min-w-[200px]">
                <Input 
                  placeholder="Search by name or email..." 
                  leftIcon={<Search size={18} />}
                  value={userFilters.search}
                  onChange={(e) => setUserFilters({ ...userFilters, search: e.target.value })}
                />
              </div>
              {activeTab === 'all-users' && (
                  <select 
                    className="bg-surface-2 border-white/5 rounded-xl px-4 py-3 text-sm text-app-text font-bold outline-none"
                    value={userFilters.status}
                    onChange={(e) => setUserFilters({ ...userFilters, status: e.target.value })}
                  >
                    <option value="">All Status</option>
                    <option value="ACTIVE">Active</option>
                    <option value="BLOCKED">Blocked</option>
                  </select>
              )}
              <select 
                className="bg-surface-2 border border-border-subtle rounded-xl px-4 py-3 text-sm text-app-text font-bold outline-none"
                value={userFilters.sort}
                onChange={(e) => setUserFilters({ ...userFilters, sort: e.target.value })}
              >
                <option value="newest">Newest First</option>
                <option value="name">Name A-Z</option>
              </select>
          </div>

          <div className="glass-card overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="text-left text-xs font-bold text-muted-text uppercase tracking-widest bg-surface-2 border-b border-border-subtle">
                      <th className="px-6 py-4">User</th>
                      <th className="px-6 py-4">Contact Details</th>
                      <th className="px-6 py-4 text-center">KYC Status</th>
                      <th className="px-6 py-4 text-center">Account Status</th>
                      <th className="px-6 py-4 text-right">Verification Action</th>
                    </tr>
                  </thead>
                   <tbody className="divide-y divide-border-subtle">
                    {loadingUsers ? (
                      [...Array(3)].map((_, i) => (
                        <tr key={i}><td colSpan={5} className="px-6 py-4"><Skeleton height={40} /></td></tr>
                      ))
                    ) : users.length === 0 ? (
                      <tr>
                         <td colSpan={5} className="px-6 py-20 text-center">
                          <div className="flex flex-col items-center gap-3">
                            <AlertCircle size={48} className="text-muted-text/20" />
                            <p className="text-muted-text font-bold uppercase tracking-widest text-xs">No pending applications found</p>
                          </div>
                        </td>
                      </tr>
                    ) : users.map((user) => (
                      <tr key={user.userCode} className="hover:bg-white/[0.02] transition-colors group">
                        <td className="px-6 py-5">
                           <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-xl bg-secondary-500/10 flex items-center justify-center text-xs font-black text-secondary-500">
                                 {user.firstName.charAt(0)}{user.lastName.charAt(0)}
                              </div>
                              <div>
                                <span className="text-sm font-bold text-app-text block">{user.firstName} {user.lastName}</span>
                                <span className="text-[10px] font-mono text-muted-text">{user.userCode}</span>
                              </div>
                           </div>
                        </td>
                        <td className="px-6 py-5">
                           <div className="flex flex-col">
                              <span className="text-xs font-medium text-app-text">{user.email}</span>
                              <span className="text-[10px] text-muted-text">{user.phone}</span>
                           </div>
                        </td>
                        <td className="px-6 py-5 text-center">
                           <Badge variant={user.kycStatus === 'VERIFIED' ? 'success' : 'warning'}>{user.kycStatus}</Badge>
                        </td>
                        <td className="px-6 py-5 text-center">
                           <Badge variant={user.status === 'ACTIVE' ? 'success' : 'danger'}>{user.status}</Badge>
                        </td>
                        <td className="px-6 py-5 text-right">
                           {user.kycStatus === 'PENDING' ? (
                             <Button 
                               onClick={() => handleApproveKYC(user.userCode)} 
                               variant="success" 
                               size="sm"
                               className="px-6"
                               leftIcon={<CheckCircle size={14} />}
                             >
                               Approve KYC
                             </Button>
                           ) : (
                             <span className="text-[10px] font-black text-muted-text uppercase">Verified</span>
                           )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              
              <div className="px-6 py-4 bg-surface-2 border-t border-border-subtle flex items-center justify-between font-bold text-[10px] text-muted-text uppercase tracking-widest">
                 <span>Page {userPagination.page + 1} of {userPagination.totalPages}</span>
                 <div className="flex gap-2">
                    <Button variant="ghost" size="sm" onClick={() => setUserPagination(p => ({ ...p, page: Math.max(0, p.page - 1)}))} disabled={userPagination.page === 0}>Prev</Button>
                    <Button variant="ghost" size="sm" onClick={() => setUserPagination(p => ({ ...p, page: Math.min(p.totalPages - 1, p.page + 1)}))} disabled={userPagination.page >= userPagination.totalPages - 1}>Next</Button>
                 </div>
              </div>
          </div>
       </div>

       {/* Other Admin Features Section (Collapsed/Secondary) */}
        <div className="space-y-4">
           <h4 className="text-[10px] font-black text-muted-text uppercase tracking-[0.2em] ml-1">System Infrastructure</h4>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
             {[
               { label: 'API Version', value: 'v1.0.4-stable', icon: Cpu, color: 'text-primary-500' },
               { label: 'Database', value: 'MySQL 8.0.35', icon: Database, color: 'text-success-500' },
               { label: 'Spring Boot', value: '3.2.4 GA', icon: Server, color: 'text-secondary-500' },
             ].map((item, i) => (
                <div key={i} className="glass-card p-6 flex items-center gap-6 opacity-80 hover:opacity-100 transition-opacity bg-surface shadow-sm">
                   <div className="w-12 h-12 rounded-2xl bg-surface-2 border border-border-subtle flex items-center justify-center">
                     <item.icon size={24} className={item.color} />
                  </div>
                   <div>
                      <p className="text-[10px] font-black text-muted-text uppercase tracking-widest mb-1">{item.label}</p>
                     <p className="text-lg font-black text-app-text">{item.value}</p>
                  </div>
               </div>
             ))}
          </div>
       </div>
    </div>
  );
};

export default BankOperatorDashboard;
