import React, { useState, useEffect, useCallback } from 'react';
import { 
  Users, 
  UserPlus, 
  ArrowLeftRight, 
  AlertTriangle, 
  Clock, 
  Search, 
  Filter, 
  Eye, 
  Ban, 
  CheckCircle,
  X,
  ChevronLeft,
  ChevronRight,
  Activity,
  Database,
  Server,
  Cpu,
  RefreshCw,
  MoreVertical,
  ShieldCheck,
  CreditCard,
  Landmark,
  Edit,
  Trash2
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import axiosInstance from '../../api/axios';
import { ADMIN } from '../../api/endpoints';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import SidePanel from '../../components/common/SidePanel';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import useToast from '../../hooks/useToast';

const AdminPage = () => {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('users');
  const [stats, setStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(true);

  // User Management State
  const [users, setUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [userFilters, setUserFilters] = useState({
    search: '',
    status: '',
    kyc: '',
    sort: 'newest'
  });
  const [userPagination, setUserPagination] = useState({ page: 0, size: 10, totalPages: 0 });
  const [selectedUser, setSelectedUser] = useState(null);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [blockReason, setBlockReason] = useState('');
  const [userToBlock, setUserToBlock] = useState(null);
  
  // Edit/Delete State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [userToEdit, setUserToEdit] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);
  const [editFormData, setEditFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    city: '',
    state: '',
    pincode: ''
  });

  // Transactions State
  const [transactions, setTransactions] = useState([]);
  const [loadingTx, setLoadingTx] = useState(true);
  const [txFilters, setTxFilters] = useState({
    userCode: '',
    type: '',
    status: '',
    minAmount: '',
    maxAmount: '',
    fromDate: '',
    toDate: ''
  });
  
  // Loans Management State
  const [pendingLoans, setPendingLoans] = useState([]);
  const [loadingLoans, setLoadingLoans] = useState(true);

  // Branch Management State
  const [branches, setBranches] = useState([]);
  const [loadingBranches, setLoadingBranches] = useState(true);
  const [isBranchModalOpen, setIsBranchModalOpen] = useState(false);
  const [branchToEdit, setBranchToEdit] = useState(null);
  const [branchFormData, setBranchFormData] = useState({
    branchName: '',
    branchCode: '',
    ifscCode: '',
    address: '',
    city: '',
    state: '',
    pincode: '',
    phone: ''
  });

  // Loan Product Management State
  const [loanProducts, setLoanProducts] = useState([]);
  const [loadingLoanProducts, setLoadingLoanProducts] = useState(true);
  const [isLoanProductModalOpen, setIsLoanProductModalOpen] = useState(false);
  const [loanProductToEdit, setLoanProductToEdit] = useState(null);
  const [loanProductFormData, setLoanProductFormData] = useState({
    productCode: '',
    productName: '',
    loanType: 'PERSONAL_LOAN',
    interestRate: '',
    minAmount: '',
    maxAmount: '',
    minTenureMonths: '',
    maxTenureMonths: '',
    isActive: true
  });

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
          ...userFilters
        }
      });
      setUsers(response.content);
      setUserPagination(prev => ({ ...prev, totalPages: response.totalPages }));
    } catch (err) {
      toast.error('Failed to fetch users');
    } finally {
      setLoadingUsers(false);
    }
  }, [userPagination.page, userPagination.size, userFilters, toast]);

  // Debounced user search
  useEffect(() => {
    const timer = setTimeout(fetchUsers, userFilters.search ? 400 : 0);
    return () => clearTimeout(timer);
  }, [userFilters.search, userFilters.status, userFilters.kyc, userFilters.sort, userPagination.page]);

  // Actions
  const handleApproveKYC = async (userCode) => {
    try {
      await axiosInstance.patch(`${ADMIN.USERS}/${userCode}/kyc`, { kycStatus: 'VERIFIED' });
      toast.success('KYC Verified successfully');
      fetchUsers();
    } catch (err) {
      toast.error('KYC verification failed');
    }
  };

  const handleBlockUser = async () => {
    if (blockReason.length < 10) {
      toast.error('Reason must be at least 10 characters');
      return;
    }
    try {
      await axiosInstance.patch(`${ADMIN.USERS}/${userToBlock.userCode}/status`, { 
        status: 'BLOCKED', 
        reason: blockReason 
      });
      toast.success('User blocked successfully');
      setIsBlockModalOpen(false);
      setBlockReason('');
      fetchUsers();
    } catch (err) {
      toast.error('Operation failed');
    }
  };

  const handleEditUser = async (e) => {
    e.preventDefault();
    try {
      await axiosInstance.put(`${ADMIN.USERS}/${userToEdit.userCode}`, editFormData);
      toast.success('User updated successfully');
      setIsEditModalOpen(false);
      fetchUsers();
    } catch (err) {
      toast.error(err.message || 'Update failed');
    }
  };

  const handleDeleteUser = async () => {
    try {
      await axiosInstance.delete(`${ADMIN.USERS}/${userToDelete.userCode}`);
      toast.success('User deleted successfully');
      setIsDeleteModalOpen(false);
      fetchUsers();
      fetchStats();
    } catch (err) {
      toast.error('Deletion failed');
    }
  };

  const openEditModal = (user) => {
    setUserToEdit(user);
    setEditFormData({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone,
      address: user.address || '',
      city: user.city || '',
      state: user.state || '',
      pincode: user.pincode || ''
    });
    setIsEditModalOpen(true);
  };

  const fetchPendingLoans = useCallback(async () => {
    try {
      setLoadingLoans(true);
      const response = await axiosInstance.get(ADMIN.LOANS_PENDING);
      setPendingLoans(response);
    } catch (err) {
      toast.error('Failed to fetch pending loans');
    } finally {
      setLoadingLoans(false);
    }
  }, [toast]);

  const handleApproveLoan = async (loanId) => {
    try {
      await axiosInstance.post(ADMIN.LOAN_APPROVE(loanId));
      toast.success('Loan approved and disbursed');
      fetchPendingLoans();
      fetchStats(); // Update stats since volume changed
    } catch (err) {
      toast.error(err.message || 'Loan approval failed');
    }
  };

  // Branch Actions
  const fetchBranches = useCallback(async () => {
    try {
      setLoadingBranches(true);
      const response = await axiosInstance.get(ADMIN.BRANCHES);
      setBranches(response);
    } catch (err) {
      toast.error('Failed to fetch branches');
    } finally {
      setLoadingBranches(false);
    }
  }, [toast]);

  const handleBranchSubmit = async (e) => {
    e.preventDefault();
    try {
      if (branchToEdit) {
        await axiosInstance.put(`${ADMIN.BRANCHES}/${branchToEdit.id}`, branchFormData);
        toast.success('Branch updated successfully');
      } else {
        await axiosInstance.post(ADMIN.BRANCHES, branchFormData);
        toast.success('Branch created successfully');
      }
      setIsBranchModalOpen(false);
      fetchBranches();
    } catch (err) {
      toast.error(err.message || 'Operation failed');
    }
  };

  const handleDeleteBranch = async (id) => {
    if (!window.confirm('Delete this branch?')) return;
    try {
      await axiosInstance.delete(`${ADMIN.BRANCHES}/${id}`);
      toast.success('Branch deleted');
      fetchBranches();
    } catch (err) {
      toast.error('Deletion failed');
    }
  };

  const openBranchModal = (branch = null) => {
    setBranchToEdit(branch);
    setBranchFormData(branch || {
      branchName: '',
      branchCode: '',
      ifscCode: '',
      address: '',
      city: '',
      state: '',
      pincode: '',
      phone: ''
    });
    setIsBranchModalOpen(true);
  };

  // Loan Product Actions
  const fetchLoanProducts = useCallback(async () => {
    try {
      setLoadingLoanProducts(true);
      const response = await axiosInstance.get(ADMIN.LOAN_PRODUCTS);
      setLoanProducts(response);
    } catch (err) {
      toast.error('Failed to fetch loan products');
    } finally {
      setLoadingLoanProducts(false);
    }
  }, [toast]);

  const handleLoanProductSubmit = async (e) => {
    e.preventDefault();
    try {
      if (loanProductToEdit) {
        await axiosInstance.put(`${ADMIN.LOAN_PRODUCTS}/${loanProductToEdit.id}`, loanProductFormData);
        toast.success('Product updated successfully');
      } else {
        await axiosInstance.post(ADMIN.LOAN_PRODUCTS, loanProductFormData);
        toast.success('Product created successfully');
      }
      setIsLoanProductModalOpen(false);
      fetchLoanProducts();
    } catch (err) {
      toast.error(err.message || 'Operation failed');
    }
  };

  const handleDeleteLoanProduct = async (id) => {
    if (!window.confirm('Delete this product?')) return;
    try {
      await axiosInstance.delete(`${ADMIN.LOAN_PRODUCTS}/${id}`);
      toast.success('Product deleted');
      fetchLoanProducts();
    } catch (err) {
      toast.error('Deletion failed');
    }
  };

  const openLoanProductModal = (lp = null) => {
    setLoanProductToEdit(lp);
    setLoanProductFormData(lp || {
      productCode: '',
      productName: '',
      loanType: 'PERSONAL_LOAN',
      interestRate: '',
      minAmount: '',
      maxAmount: '',
      minTenureMonths: '',
      maxTenureMonths: '',
      isActive: true
    });
    setIsLoanProductModalOpen(true);
  };

  const openUserDetail = (user) => {
    setSelectedUser(user);
    setIsPanelOpen(true);
  };

  return (
    <div className="space-y-8 animate-slide-in pb-20">
       <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-3xl font-black text-app-text tracking-tighter">Admin Control Center</h2>
            <p className="text-dark-400 font-medium">Global system overview and user orchestration.</p>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 bg-surface-2 border border-white/5 rounded-2xl">
              <div className="w-2 h-2 rounded-full bg-success-500 animate-pulse" />
              <span className="text-[10px] font-black text-dark-300 uppercase tracking-widest">System Online</span>
          </div>
       </div>

       {/* Top Stats Row */}
       <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
          {[
            { label: 'Total Users', value: stats?.totalUsers || '0', icon: Users, color: 'text-primary-500', bg: 'bg-primary-500/10' },
            { label: 'New Today', value: stats?.newUsersToday || '0', icon: UserPlus, color: 'text-success-500', bg: 'bg-success-500/10' },
            { label: 'Tx Volume Today', value: formatCurrency(stats?.transactionVolumeToday || 0), icon: ArrowLeftRight, color: 'text-secondary-500', bg: 'bg-secondary-500/10' },
            { label: 'Pending Loan Approval', value: stats?.pendingLoans || '0', icon: CreditCard, color: 'text-danger-500', bg: 'bg-danger-500/10' },
            { label: 'Pending KYC', value: stats?.pendingKyc || '0', icon: Clock, color: 'text-warning-500', bg: 'bg-warning-500/10' },
          ].map((stat, i) => (
            <div key={i} className="glass-card flex flex-col gap-4 p-6">
               <div className={`w-12 h-12 rounded-2xl ${stat.bg} flex items-center justify-center ${stat.color}`}>
                  <stat.icon size={24} />
               </div>
               <div>
                  <p className="text-[10px] font-bold text-dark-500 uppercase tracking-widest leading-none mb-1">{stat.label}</p>
                  <p className="text-xl font-black text-app-text tracking-tight">{stat.value}</p>
               </div>
            </div>
          ))}
       </div>

       {/* Tab Navigation */}
        <div className="flex flex-wrap gap-2 p-1.5 bg-surface-2 border border-app-border rounded-2xl w-fit">
          {[
            { id: 'users', label: 'User Management' },
            { id: 'loans', label: 'Loan Requests' },
            { id: 'branches', label: 'Branches' },
            { id: 'loanProducts', label: 'Loan Products' },
            { id: 'transactions', label: 'All Transactions' },
            { id: 'system', label: 'System Info' }
          ].map(t => (
            <button
              key={t.id}
              onClick={() => setActiveTab(t.id)}
              className={`px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === t.id ? 'bg-primary-500 text-white shadow-lg' : 'text-dark-400 hover:text-app-text'}`}
            >
              {t.label}
            </button>
          ))}
       </div>

       <AnimatePresence mode="wait">
          {/* USER MANAGEMENT TAB */}
          {activeTab === 'users' && (
            <motion.div 
               key="users-tab"
               initial={{ opacity: 0, scale: 0.98 }}
               animate={{ opacity: 1, scale: 1 }}
               exit={{ opacity: 0, scale: 0.98 }}
               className="space-y-6"
            >
               <div className="glass-card p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <Input 
                    placeholder="Search name, email, code..." 
                    leftIcon={<Search size={18} />}
                    value={userFilters.search}
                    onChange={(e) => setUserFilters({ ...userFilters, search: e.target.value })}
                  />
                  <select 
                    className="bg-surface-2 border-white/5 rounded-xl px-4 py-3 text-sm text-app-text font-bold outline-none"
                    value={userFilters.status}
                    onChange={(e) => setUserFilters({ ...userFilters, status: e.target.value })}
                  >
                    <option value="">All Status</option>
                    <option value="ACTIVE">Active</option>
                    <option value="BLOCKED">Blocked</option>
                    <option value="PENDING_VERIFICATION">Pending</option>
                  </select>
                  <select 
                    className="bg-surface-2 border-white/5 rounded-xl px-4 py-3 text-sm text-app-text font-bold outline-none"
                    value={userFilters.kyc}
                    onChange={(e) => setUserFilters({ ...userFilters, kyc: e.target.value })}
                  >
                    <option value="">All KYC</option>
                    <option value="VERIFIED">Verified</option>
                    <option value="PENDING">Pending</option>
                    <option value="NOT_SUBMITTED">Not Submitted</option>
                  </select>
                  <select 
                    className="bg-surface-2 border-white/5 rounded-xl px-4 py-3 text-sm text-app-text font-bold outline-none"
                    value={userFilters.sort}
                    onChange={(e) => setUserFilters({ ...userFilters, sort: e.target.value })}
                  >
                    <option value="newest">Newest First</option>
                    <option value="oldest">Oldest First</option>
                    <option value="name_asc">Name A-Z</option>
                  </select>
               </div>

               <div className="glass-card overflow-hidden">
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="text-left text-[10px] font-bold text-dark-400 uppercase tracking-widest bg-dark-900 border-b border-white/5">
                          <th className="px-6 py-4">User Code</th>
                          <th className="px-6 py-4">Name</th>
                          <th className="px-6 py-4">Contact</th>
                          <th className="px-6 py-4">Accounts</th>
                          <th className="px-6 py-4 text-center">KYC</th>
                          <th className="px-6 py-4 text-center">Status</th>
                          <th className="px-6 py-4 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-white/5">
                        {loadingUsers ? (
                          [...Array(5)].map((_, i) => (
                            <tr key={i}><td colSpan={7} className="px-6 py-4"><Skeleton height={40} /></td></tr>
                          ))
                        ) : users.map((user) => (
                          <tr key={user.userCode} className="hover:bg-white/[0.02] transition-colors group">
                            <td className="px-6 py-5">
                               <span className="text-xs font-mono font-bold text-dark-300">{user.userCode}</span>
                            </td>
                            <td className="px-6 py-5">
                               <div className="flex items-center gap-3">
                                  <div className="w-8 h-8 rounded-lg bg-primary-500/10 flex items-center justify-center text-[10px] font-black text-primary-500">
                                     {user.firstName.charAt(0)}{user.lastName.charAt(0)}
                                  </div>
                                  <span className="text-sm font-bold text-white uppercase">{user.firstName} {user.lastName}</span>
                               </div>
                            </td>
                            <td className="px-6 py-5">
                               <div className="flex flex-col">
                                  <span className="text-xs font-medium text-dark-100">{user.email}</span>
                                  <span className="text-[10px] text-dark-500">{user.phone}</span>
                               </div>
                            </td>
                            <td className="px-6 py-5">
                               <Badge variant="dark">{formatCurrency(user.totalBalance || 0)}</Badge>
                            </td>
                            <td className="px-6 py-5 text-center">
                               <Badge variant={user.kycStatus === 'VERIFIED' ? 'success' : 'warning'}>{user.kycStatus}</Badge>
                            </td>
                            <td className="px-6 py-5 text-center">
                               <Badge variant={user.status === 'ACTIVE' ? 'success' : 'danger'}>{user.status}</Badge>
                            </td>
                            <td className="px-6 py-5 text-right">
                               <div className="flex justify-end gap-2">
                                  <button onClick={() => openUserDetail(user)} className="p-2 rounded-lg bg-surface-2 text-dark-300 hover:text-white transition-all"><Eye size={16} /></button>
                                  {user.kycStatus === 'PENDING' && (
                                    <button onClick={() => handleApproveKYC(user.userCode)} className="p-2 rounded-lg bg-success-500/10 text-success-500 hover:bg-success-500 hover:text-white transition-all"><CheckCircle size={16} /></button>
                                  )}
                                  {user.status !== 'BLOCKED' && (
                                    <button onClick={() => { setUserToBlock(user); setIsBlockModalOpen(true); }} className="p-2 rounded-lg bg-danger-500/10 text-danger-500 hover:bg-danger-500 hover:text-white transition-all"><Ban size={16} /></button>
                                  )}
                                  <button onClick={() => openEditModal(user)} className="p-2 rounded-lg bg-primary-500/10 text-primary-500 hover:bg-primary-500 hover:text-white transition-all"><Edit size={16} /></button>
                                  <button onClick={() => { setUserToDelete(user); setIsDeleteModalOpen(true); }} className="p-2 rounded-lg bg-danger-500/10 text-danger-500 hover:bg-danger-500 hover:text-white transition-all"><Trash2 size={16} /></button>
                               </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  
                  {/* Pagination */}
                  <div className="px-6 py-4 bg-dark-900 border-t border-white/5 flex items-center justify-between font-bold text-[10px] text-dark-400 uppercase tracking-widest">
                     <span>Page {userPagination.page + 1} of {userPagination.totalPages}</span>
                     <div className="flex gap-2">
                        <Button variant="ghost" size="sm" onClick={() => setUserPagination(p => ({ ...p, page: Math.max(0, p.page - 1)}))} disabled={userPagination.page === 0}>Prev</Button>
                        <Button variant="ghost" size="sm" onClick={() => setUserPagination(p => ({ ...p, page: Math.min(p.totalPages - 1, p.page + 1)}))} disabled={userPagination.page >= userPagination.totalPages - 1}>Next</Button>
                     </div>
                  </div>
               </div>
            </motion.div>
          )}

          {/* LOANS MANAGEMENT TAB */}
          {activeTab === 'loans' && (
            <motion.div 
               key="loans-tab"
               initial={{ opacity: 0, x: 20 }}
               animate={{ opacity: 1, x: 0 }}
               exit={{ opacity: 0, x: -20 }}
               className="space-y-6"
               onViewportEnter={fetchPendingLoans}
            >
               {/* Same as before but with fetchPendingLoans */}
               <div className="glass-card overflow-hidden">
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="text-left text-[10px] font-bold text-dark-400 uppercase tracking-widest bg-dark-900 border-b border-white/5">
                          <th className="px-6 py-4">Loan ID</th>
                          <th className="px-6 py-4">Product</th>
                          <th className="px-6 py-4">Principal</th>
                          <th className="px-6 py-4">EMI</th>
                          <th className="px-6 py-4 text-center">Status</th>
                          <th className="px-6 py-4 text-right">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-white/5">
                        {loadingLoans ? (
                          [...Array(3)].map((_, i) => (
                            <tr key={i}><td colSpan={6} className="px-6 py-4"><Skeleton height={40} /></td></tr>
                          ))
                        ) : pendingLoans.length > 0 ? (
                          pendingLoans.map((loan) => (
                            <tr key={loan.id} className="hover:bg-white/[0.02] transition-colors group">
                              <td className="px-6 py-5">
                                 <span className="text-xs font-mono font-bold text-dark-300">{loan.loanNumber}</span>
                              </td>
                              <td className="px-6 py-5">
                                 <span className="text-sm font-bold text-white uppercase">{loan.productName}</span>
                              </td>
                              <td className="px-6 py-5">
                                 <span className="text-sm font-black text-primary-500">{formatCurrency(loan.principalAmount)}</span>
                              </td>
                              <td className="px-6 py-5">
                                 <span className="text-xs font-bold text-dark-300">{formatCurrency(loan.emiAmount)} / mo</span>
                              </td>
                              <td className="px-6 py-5 text-center">
                                 <Badge variant="warning">{loan.status}</Badge>
                              </td>
                              <td className="px-6 py-5 text-right">
                                 <Button 
                                    size="sm" 
                                    className="bg-success-500 hover:bg-success-600 text-white"
                                    onClick={() => handleApproveLoan(loan.id)}
                                 >
                                    Approve & Disburse
                                 </Button>
                              </td>
                            </tr>
                          ))
                        ) : (
                          <tr>
                            <td colSpan={6} className="px-6 py-20">
                               <EmptyState 
                                  icon={Landmark}
                                  title="Clear of Requests"
                                  message="All loan applications have been processed. Systems optimal."
                                />
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
               </div>
            </motion.div>
          )}

          {/* BRANCHES TAB */}
          {activeTab === 'branches' && (
            <motion.div 
               key="branches-tab"
               initial={{ opacity: 0, y: 20 }}
               animate={{ opacity: 1, y: 0 }}
               className="space-y-6"
               onViewportEnter={fetchBranches}
            >
               <div className="flex justify-between items-center">
                  <h3 className="text-xl font-black text-white uppercase tracking-tight">Branch Network</h3>
                  <Button size="sm" onClick={() => openBranchModal()}>Add New Branch</Button>
               </div>
               <div className="glass-card overflow-hidden">
                  <table className="w-full">
                    <thead>
                      <tr className="text-left text-[10px] font-bold text-dark-400 uppercase tracking-widest bg-dark-900 border-b border-white/5">
                        <th className="px-6 py-4">Code</th>
                        <th className="px-6 py-4">Name / IFSC</th>
                        <th className="px-6 py-4">Location</th>
                        <th className="px-6 py-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {loadingBranches ? (
                        <tr><td colSpan={4} className="px-6 py-4"><Skeleton height={40} /></td></tr>
                      ) : branches.map(branch => (
                        <tr key={branch.id} className="hover:bg-white/[0.02]">
                          <td className="px-6 py-5 font-mono font-bold text-primary-500">{branch.branchCode}</td>
                          <td className="px-6 py-5">
                            <div className="flex flex-col">
                              <span className="text-sm font-bold text-white uppercase">{branch.branchName}</span>
                              <span className="text-[10px] text-dark-500 font-mono">{branch.ifscCode}</span>
                            </div>
                          </td>
                          <td className="px-6 py-5">
                            <span className="text-xs text-dark-300">{branch.city}, {branch.state}</span>
                          </td>
                          <td className="px-6 py-5 text-right">
                            <div className="flex justify-end gap-2">
                              <button onClick={() => openBranchModal(branch)} className="p-2 rounded-lg bg-primary-500/10 text-primary-500 hover:bg-primary-500 hover:text-white transition-all"><Edit size={14} /></button>
                              <button onClick={() => handleDeleteBranch(branch.id)} className="p-2 rounded-lg bg-danger-500/10 text-danger-500 hover:bg-danger-500 hover:text-white transition-all"><Trash2 size={14} /></button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
               </div>
            </motion.div>
          )}

          {/* LOAN PRODUCTS TAB */}
          {activeTab === 'loanProducts' && (
            <motion.div 
               key="lp-tab"
               initial={{ opacity: 0, y: 20 }}
               animate={{ opacity: 1, y: 0 }}
               className="space-y-6"
               onViewportEnter={fetchLoanProducts}
            >
               <div className="flex justify-between items-center">
                  <h3 className="text-xl font-black text-white uppercase tracking-tight">Loan Portfolio</h3>
                  <Button size="sm" onClick={() => openLoanProductModal()}>Create Product</Button>
               </div>
               <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {loadingLoanProducts ? (
                    <Skeleton height={200} count={3} />
                  ) : loanProducts.map(lp => (
                    <div key={lp.id} className="glass-card p-6 flex flex-col gap-4 border-l-4 border-l-primary-500">
                       <div className="flex justify-between items-start">
                          <div>
                            <span className="text-[10px] font-black text-primary-500 uppercase tracking-widest">{lp.loanType}</span>
                            <h4 className="text-lg font-black text-white uppercase mt-1">{lp.productName}</h4>
                            <p className="text-[10px] font-mono text-dark-500">{lp.productCode}</p>
                          </div>
                          <Badge variant={lp.isActive ? 'success' : 'dark'}>{lp.isActive ? 'Active' : 'Inactive'}</Badge>
                       </div>
                       <div className="grid grid-cols-2 gap-4 py-2">
                          <div>
                             <p className="text-[10px] font-bold text-dark-500 uppercase">Int. Rate</p>
                             <p className="text-sm font-black text-white">{lp.interestRate}%</p>
                          </div>
                          <div>
                             <p className="text-[10px] font-bold text-dark-500 uppercase">Tenure</p>
                             <p className="text-sm font-black text-white">{lp.maxTenureMonths} Mo</p>
                          </div>
                       </div>
                       <div className="pt-4 border-t border-white/5 flex gap-2">
                          <Button variant="outline" size="sm" className="flex-1" onClick={() => openLoanProductModal(lp)}>Edit</Button>
                          <Button variant="danger" size="sm" className="px-3" onClick={() => handleDeleteLoanProduct(lp.id)}><Trash2 size={14} /></Button>
                       </div>
                    </div>
                  ))}
               </div>
            </motion.div>
          )}

          {/* SYSTEM INFO TAB */}
          {activeTab === 'system' && (
            <motion.div 
               key="system-tab"
               initial={{ opacity: 0, y: 10 }}
               animate={{ opacity: 1, y: 0 }}
               className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
            >
               {[
{ label: 'API Version', value: 'v1.0.4-stable', icon: Cpu, color: 'text-primary-500' },
                  { label: 'Database', value: 'MySQL 8.0.35', icon: Database, color: 'text-success-500' },
                  { label: 'Spring Boot', value: '3.2.4 GA', icon: Server, color: 'text-secondary-500' },
                  { label: 'WebSocket', value: 'STOMP Active', icon: Activity, color: 'text-warning-500' },
                  { label: 'Last Deploy', value: 'April 17, 2026', icon: Clock, color: 'text-dark-400' },
               ].map((item, i) => (
                 <div key={i} className="glass-card p-6 flex items-center gap-6">
                    <div className="w-12 h-12 rounded-2xl bg-dark-900 border border-white/5 flex items-center justify-center">
                       <item.icon size={24} className={item.color} />
                    </div>
                    <div>
                       <p className="text-[10px] font-black text-dark-500 uppercase tracking-widest mb-1">{item.label}</p>
                       <p className="text-lg font-black text-white">{item.value}</p>
                    </div>
                 </div>
               ))}
            </motion.div>
          )}
       </AnimatePresence>

       {/* SIDE PANEL: USER DETAIL */}
       <SidePanel 
          isOpen={isPanelOpen} 
          onClose={() => setIsPanelOpen(false)}
          title="Security Intel: User Profile"
       >
          {selectedUser && (
            <div className="space-y-10">
               {/* User Basic Info */}
               <div className="flex flex-col items-center text-center">
                  <div className="w-24 h-24 rounded-[32px] bg-gradient-to-tr from-primary-600 to-secondary-500 flex items-center justify-center text-3xl font-black text-white mb-4">
                     {selectedUser.firstName.charAt(0)}{selectedUser.lastName.charAt(0)}
                  </div>
                  <h3 className="text-2xl font-black text-white uppercase tracking-tight">{selectedUser.firstName} {selectedUser.lastName}</h3>
                  <p className="text-xs font-mono font-bold text-dark-400 mt-1">{selectedUser.userCode}</p>
                  <div className="flex gap-2 mt-4">
                     <Badge variant={selectedUser.kycStatus === 'VERIFIED' ? 'success' : 'warning'}>{selectedUser.kycStatus}</Badge>
                     <Badge variant={selectedUser.status === 'ACTIVE' ? 'success' : 'danger'}>{selectedUser.status}</Badge>
                  </div>
               </div>

               {/* Stats Overview */}
               <div className="grid grid-cols-2 gap-4">
                  <div className="glass-card bg-dark-900/50 p-4 border-none">
                     <p className="text-[10px] font-bold text-dark-500 uppercase tracking-widest mb-1">Total Assets</p>
                     <p className="text-lg font-black text-white tracking-tight">{formatCurrency(selectedUser.totalBalance || 0)}</p>
                  </div>
                  <div className="glass-card bg-dark-900/50 p-4 border-none">
                     <p className="text-[10px] font-bold text-dark-500 uppercase tracking-widest mb-1">Active Accounts</p>
                     <p className="text-lg font-black text-white tracking-tight">{selectedUser.accountsCount || 0}</p>
                  </div>
               </div>

               {/* Information Blocks */}
               <div className="space-y-6">
                  <div>
                    <h4 className="text-[10px] font-black text-primary-500 uppercase tracking-[0.2em] mb-4 flex items-center gap-2">
                       <Activity size={12} /> Contact Information
                    </h4>
                    <div className="space-y-4 p-5 bg-dark-900/30 rounded-2xl border border-white/5">
                       <div className="flex justify-between">
                          <span className="text-xs text-dark-400">Email Address</span>
                          <span className="text-xs font-bold text-white">{selectedUser.email}</span>
                       </div>
                       <div className="flex justify-between">
                          <span className="text-xs text-dark-400">Phone Numer</span>
                          <span className="text-xs font-bold text-white">{selectedUser.phoneNumber}</span>
                       </div>
                    </div>
                  </div>

                  <div>
                    <h4 className="text-[10px] font-black text-secondary-500 uppercase tracking-[0.2em] mb-4 flex items-center gap-2">
                       <Activity size={12} /> Account Intelligence
                    </h4>
                    <div className="space-y-2">
                       {selectedUser.accounts?.map(acc => (
                         <div key={acc.id} className="p-4 bg-dark-900/50 rounded-2xl border border-white/5 flex justify-between items-center group">
                            <div className="flex items-center gap-3">
                               <div className="w-8 h-8 rounded-lg bg-dark-950 flex items-center justify-center text-dark-400">
                                  <CreditCard size={14} />
                               </div>
                               <div>
                                  <p className="text-xs font-bold text-white tracking-tight uppercase">{acc.type}</p>
                                  <p className="text-[10px] font-mono text-dark-500">{acc.accountNumber}</p>
                               </div>
                            </div>
                            <span className="text-sm font-black text-white tracking-tight">{formatCurrency(acc.balance)}</span>
                         </div>
                       )) || <p className="text-xs text-dark-500 px-4">No linked accounts discovered.</p>}
                    </div>
                  </div>
               </div>

               <div className="pt-6">
                  <Button variant="outline" className="w-full" size="sm">Audit Log Export</Button>
               </div>
            </div>
          )}
       </SidePanel>

       {/* MODAL: BLOCK USER */}
       <Modal
          isOpen={isBlockModalOpen}
          onClose={() => setIsBlockModalOpen(false)}
          title="Sanction Protocol: Block Access"
          size="sm"
       >
          <div className="space-y-6">
             <div className="p-4 bg-danger-500/5 border border-danger-500/10 rounded-2xl flex items-start gap-4">
                <AlertTriangle size={20} className="text-danger-500 shrink-0 mt-1" />
                <p className="text-xs font-medium text-dark-300 leading-relaxed uppercase tracking-wide">
                  Blocking this actor will terminate all active sessions and disable transaction authorization immediately.
                </p>
             </div>

             <div className="space-y-2">
                <label className="text-xs font-bold text-dark-400 uppercase tracking-widest ml-1">Reason for Sanction</label>
                <textarea 
                  className="w-full bg-dark-950 border-white/5 rounded-2xl p-4 text-sm text-white font-medium outline-none focus:ring-2 ring-danger-500/20 min-h-[120px] placeholder:text-dark-600"
                  placeholder="Provide incident reference or justification (min 10 chars)..."
                  value={blockReason}
                  onChange={(e) => setBlockReason(e.target.value)}
                />
             </div>

             <div className="flex gap-3">
                <Button variant="ghost" className="flex-1" onClick={() => setIsBlockModalOpen(false)}>Cancel</Button>
                <Button variant="danger" className="flex-1" onClick={handleBlockUser} disabled={blockReason.length < 10}>Execute Block</Button>
             </div>
          </div>
       </Modal>

        {/* MODAL: EDIT USER */}
        <Modal
           isOpen={isEditModalOpen}
           onClose={() => setIsEditModalOpen(false)}
           title="Update Security Records"
           size="md"
        >
           <form onSubmit={handleEditUser} className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                 <Input 
                   label="First Name"
                   value={editFormData.firstName}
                   onChange={(e) => setEditFormData({ ...editFormData, firstName: e.target.value })}
                   required
                 />
                 <Input 
                   label="Last Name"
                   value={editFormData.lastName}
                   onChange={(e) => setEditFormData({ ...editFormData, lastName: e.target.value })}
                   required
                 />
              </div>
              <Input 
                label="Email Address"
                type="email"
                value={editFormData.email}
                onChange={(e) => setEditFormData({ ...editFormData, email: e.target.value })}
                required
              />
              <Input 
                label="Phone Number"
                value={editFormData.phone}
                onChange={(e) => setEditFormData({ ...editFormData, phone: e.target.value })}
                required
              />
              <div className="grid grid-cols-2 gap-4">
                 <Input 
                   label="City"
                   value={editFormData.city}
                   onChange={(e) => setEditFormData({ ...editFormData, city: e.target.value })}
                 />
                 <Input 
                   label="State"
                   value={editFormData.state}
                   onChange={(e) => setEditFormData({ ...editFormData, state: e.target.value })}
                 />
              </div>
              <div className="flex gap-3 pt-4">
                 <Button variant="ghost" className="flex-1" type="button" onClick={() => setIsEditModalOpen(false)}>Cancel</Button>
                 <Button variant="primary" className="flex-1" type="submit">Update Identity</Button>
              </div>
           </form>
        </Modal>

        {/* MODAL: DELETE USER */}
        <Modal
           isOpen={isDeleteModalOpen}
           onClose={() => setIsDeleteModalOpen(false)}
           title="Termination Protocol: Identity Deletion"
           size="sm"
        >
           <div className="space-y-6 text-center">
              <div className="w-20 h-20 rounded-full bg-danger-500/10 flex items-center justify-center text-danger-500 mx-auto mb-4">
                 <Trash2 size={40} />
              </div>
              <div className="space-y-2">
                 <h3 className="text-xl font-black text-white uppercase tracking-tight">Are you absolutely sure?</h3>
                 <p className="text-sm text-dark-400">
                    This will soft-delete user <span className="font-mono text-danger-500 font-bold">{userToDelete?.userCode}</span>. 
                    The identity will be deactivated and marked as CLOSED in the global ledger.
                 </p>
              </div>
              <div className="flex gap-3">
                 <Button variant="ghost" className="flex-1" onClick={() => setIsDeleteModalOpen(false)}>Abort</Button>
                 <Button variant="danger" className="flex-1" onClick={handleDeleteUser}>Confirm Deletion</Button>
              </div>
           </div>
        </Modal>

        {/* MODAL: BRANCH CRUD */}
        <Modal
           isOpen={isBranchModalOpen}
           onClose={() => setIsBranchModalOpen(false)}
           title={branchToEdit ? 'Update Branch Records' : 'Initialize New Branch'}
           size="md"
        >
           <form onSubmit={handleBranchSubmit} className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                 <Input label="Branch Name" value={branchFormData.branchName} onChange={e => setBranchFormData({...branchFormData, branchName: e.target.value})} required />
                 <Input label="Branch Code" value={branchFormData.branchCode} onChange={e => setBranchFormData({...branchFormData, branchCode: e.target.value})} required />
              </div>
              <Input label="IFSC Code" value={branchFormData.ifscCode} onChange={e => setBranchFormData({...branchFormData, ifscCode: e.target.value})} required />
              <Input label="Address" value={branchFormData.address} onChange={e => setBranchFormData({...branchFormData, address: e.target.value})} required />
              <div className="grid grid-cols-3 gap-4">
                 <Input label="City" value={branchFormData.city} onChange={e => setBranchFormData({...branchFormData, city: e.target.value})} required />
                 <Input label="State" value={branchFormData.state} onChange={e => setBranchFormData({...branchFormData, state: e.target.value})} required />
                 <Input label="Pincode" value={branchFormData.pincode} onChange={e => setBranchFormData({...branchFormData, pincode: e.target.value})} required />
              </div>
              <Input label="Phone" value={branchFormData.phone} onChange={e => setBranchFormData({...branchFormData, phone: e.target.value})} />
              <div className="flex gap-3 pt-4">
                 <Button variant="ghost" className="flex-1" type="button" onClick={() => setIsBranchModalOpen(false)}>Cancel</Button>
                 <Button variant="primary" className="flex-1" type="submit">{branchToEdit ? 'Save Changes' : 'Create Branch'}</Button>
              </div>
           </form>
        </Modal>

        {/* MODAL: LOAN PRODUCT CRUD */}
        <Modal
           isOpen={isLoanProductModalOpen}
           onClose={() => setIsLoanProductModalOpen(false)}
           title={loanProductToEdit ? 'Edit Product Parameters' : 'Deploy New Loan Product'}
           size="md"
        >
           <form onSubmit={handleLoanProductSubmit} className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                 <Input label="Product Name" value={loanProductFormData.productName} onChange={e => setLoanProductFormData({...loanProductFormData, productName: e.target.value})} required />
                 <Input label="Product Code" value={loanProductFormData.productCode} onChange={e => setLoanProductFormData({...loanProductFormData, productCode: e.target.value})} required />
              </div>
              <div className="grid grid-cols-2 gap-4">
                 <div className="space-y-2">
                    <label className="text-[10px] font-black text-dark-500 uppercase tracking-widest ml-1">Loan Type</label>
                    <select 
                      className="w-full bg-dark-950 border border-white/5 rounded-2xl px-4 py-3 text-sm text-white font-bold outline-none"
                      value={loanProductFormData.loanType}
                      onChange={e => setLoanProductFormData({...loanProductFormData, loanType: e.target.value})}
                    >
                       <option value="HOME_LOAN">Home Loan</option>
                       <option value="PERSONAL_LOAN">Personal Loan</option>
                       <option value="VEHICLE_LOAN">Vehicle Loan</option>
                       <option value="EDUCATION_LOAN">Education Loan</option>
                       <option value="BUSINESS_LOAN">Business Loan</option>
                    </select>
                 </div>
                 <Input label="Int. Rate (%)" type="number" step="0.01" value={loanProductFormData.interestRate} onChange={e => setLoanProductFormData({...loanProductFormData, interestRate: e.target.value})} required />
              </div>
              <div className="grid grid-cols-2 gap-4">
                 <Input label="Min Amount" type="number" value={loanProductFormData.minAmount} onChange={e => setLoanProductFormData({...loanProductFormData, minAmount: e.target.value})} />
                 <Input label="Max Amount" type="number" value={loanProductFormData.maxAmount} onChange={e => setLoanProductFormData({...loanProductFormData, maxAmount: e.target.value})} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                 <Input label="Min Tenure (Mo)" type="number" value={loanProductFormData.minTenureMonths} onChange={e => setLoanProductFormData({...loanProductFormData, minTenureMonths: e.target.value})} />
                 <Input label="Max Tenure (Mo)" type="number" value={loanProductFormData.maxTenureMonths} onChange={e => setLoanProductFormData({...loanProductFormData, maxTenureMonths: e.target.value})} />
              </div>
              <div className="flex items-center gap-3 p-4 bg-dark-900/50 rounded-2xl border border-white/5">
                 <input 
                    type="checkbox" 
                    checked={loanProductFormData.isActive} 
                    onChange={e => setLoanProductFormData({...loanProductFormData, isActive: e.target.checked})}
                    className="w-5 h-5 rounded-lg accent-primary-500"
                 />
                 <span className="text-xs font-black text-white uppercase tracking-tight">Active for Applications</span>
              </div>
              <div className="flex gap-3 pt-4">
                 <Button variant="ghost" className="flex-1" type="button" onClick={() => setIsLoanProductModalOpen(false)}>Cancel</Button>
                 <Button variant="primary" className="flex-1" type="submit">{loanProductToEdit ? 'Save Product' : 'Create Product'}</Button>
              </div>
           </form>
        </Modal>
     </div>
  );
};

export default AdminPage;
