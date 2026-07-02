import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  Plus, 
  Building2,
  Eye,
  EyeOff,
  Info,
  TrendingUp,
  Landmark
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { fetchAccounts } from '../../store/thunks/accountThunks';
import axiosInstance from '../../api/axios';
import { ACCOUNTS } from '../../api/endpoints';
import { formatCurrency, maskAccountNumber } from '../../utils/formatters';
import { openAccountSchema } from '../../utils/validators';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import Skeleton from '../../components/common/Skeleton';
import useToast from '../../hooks/useToast';
import DepositModal from '../dashboard/DepositModal';
import WithdrawModal from '../dashboard/WithdrawModal';

const AccountCard = React.memo(({ account, onDeposit, onWithdraw }) => {
  const [showFull, setShowFull] = useState(false);
  
  const gradients = {
    SAVINGS: 'from-primary-600 to-primary-700',
    CURRENT: 'from-secondary-600 to-secondary-700',
    FD: 'from-amber-500 to-amber-600',
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="relative overflow-hidden group"
    >
      <div className={`
        relative z-10 p-8 rounded-[32px] bg-gradient-to-br ${gradients[account.accountType] || 'from-dark-800 to-dark-900'}
        shadow-2xl shadow-primary-900/20 border border-white/10
      `}>
        <div className="flex justify-between items-start mb-12">
           <div className="flex flex-col gap-1">
              <Badge variant="dark" className="bg-white/10 border-none text-white font-black text-[10px]">
                {account.accountType} ACCOUNT
              </Badge>
              <div className="w-12 h-8 rounded-lg bg-white/10 border border-white/10 mt-2 relative overflow-hidden">
                <div className="absolute top-2 left-2 w-3 h-2 bg-amber-400 rounded-sm opacity-60" />
              </div>
           </div>
           <Building2 size={32} className="text-white opacity-20" />
        </div>

        <div className="mb-8 flex items-center gap-4">
           <p className="text-xl lg:text-2xl font-mono font-bold text-white tracking-[0.15em]">
             {showFull ? account.accountNumber : maskAccountNumber(account.accountNumber)}
           </p>
           <button 
             onClick={() => setShowFull(!showFull)}
             className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 transition-all text-white/60"
           >
             {showFull ? <EyeOff size={16} /> : <Eye size={16} />}
           </button>
        </div>

        <div className="mb-10">
           <p className="text-[10px] font-bold text-white/50 uppercase tracking-[0.3em] mb-1">Available Balance</p>
           <h3 className="text-4xl font-black text-white tracking-tighter">
             {formatCurrency(account.balance)}
           </h3>
        </div>

        <div className="flex items-center justify-between pt-6 border-t border-white/10">
           <div className="space-y-1">
              <p className="text-[10px] font-bold text-white/40 uppercase tracking-widest">Branch IFSC</p>
              <p className="text-xs font-bold text-white">{account.ifscCode || 'CRDX000101'}</p>
           </div>
           <Badge variant={account.status === 'ACTIVE' ? 'success' : 'danger'} className="bg-white/10">
             {account.status}
           </Badge>
        </div>
      </div>

       <div className="absolute inset-x-4 bottom-4 z-20 translate-y-full group-hover:translate-y-0 transition-transform duration-500">
          <div className="glass-card p-3 flex gap-2 justify-center shadow-2xl">
              <Button size="sm" variant="ghost" className="text-app-text hover:bg-primary-500/10" onClick={() => onDeposit(account)}>Deposit</Button>
              <Button size="sm" variant="ghost" className="text-app-text hover:bg-primary-500/10" onClick={() => onWithdraw(account)}>Withdraw</Button>
          </div>
       </div>

      <div className="absolute -top-10 -right-10 w-40 h-40 bg-white/10 rounded-full blur-[60px] pointer-events-none" />
      <div className="absolute -bottom-10 -left-10 w-32 h-32 bg-black/20 rounded-full blur-[40px] pointer-events-none" />
    </motion.div>
  );
});

AccountCard.displayName = 'AccountCard';

const INTEREST_RATES = {
  6: 5.5,
  12: 6.5,
  24: 7.2,
  36: 7.5
};

const AccountsPage = () => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { accounts, loading } = useSelector((state) => state.account);
  
  const [isDepositOpen, setIsDepositOpen] = useState(false);
  const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [isNewAccountOpen, setIsNewAccountOpen] = useState(false);

  useEffect(() => {
    dispatch(fetchAccounts());
  }, [dispatch]);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting }
  } = useForm({
    resolver: zodResolver(openAccountSchema),
    defaultValues: { type: 'SAVINGS' }
  });

  const watchType = watch('type');
  const watchTenure = watch('tenure');
  const watchInitial = watch('initialDeposit');

  const maturityAmount = useMemo(() => {
    if (watchType !== 'FD') return 0;
    const rate = INTEREST_RATES[watchTenure] || 0;
    const principal = Number(watchInitial) || 0;
    const interest = (principal * rate * (Number(watchTenure) / 12)) / 100;
    return principal + interest;
  }, [watchType, watchTenure, watchInitial]);

  const onOpenAccount = useCallback(async (data) => {
    try {
      // Map frontend form data to Backend DTO fields
      const payload = {
        accountType: data.type,
        branchId: Number(data.branch),
        fdTenureMonths: data.type === 'FIXED_DEPOSIT' ? Number(data.tenure) : null,
        initialDeposit: Number(data.initialDeposit)
      };

      await axiosInstance.post(ACCOUNTS.CREATE, payload);
      toast.success('Your new account was successfully created');
      dispatch(fetchAccounts());
      setIsNewAccountOpen(false);
      reset();
    } catch (err) {
      toast.error(err.message || 'Failed to open account');
    }
  }, [dispatch, toast, reset]);

  const handleDeposit = useCallback((acc) => {
    setSelectedAccount(acc);
    setIsDepositOpen(true);
  }, []);

  const handleWithdraw = useCallback((acc) => {
    setSelectedAccount(acc);
    setIsWithdrawOpen(true);
  }, []);

  return (
    <div className="space-y-8 animate-slide-in">
       <div className="flex items-center justify-between">
          <div className="space-y-1">
            <h2 className="text-3xl font-black text-app-text tracking-tighter">Your Financial Portfolio</h2>
            <p className="text-muted-text font-medium text-sm">Manage your savings, current, and investment accounts in one place.</p>
          </div>
          <Button 
            className="hidden md:flex" 
            leftIcon={<Plus size={20} />}
            onClick={() => setIsNewAccountOpen(true)}
          >
            Open New Account
          </Button>
       </div>

       <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {loading ? (
             [...Array(2)].map((_, i) => <Skeleton key={i} variant="card" height={360} className="rounded-[32px]" />)
          ) : (
            <>
              {accounts.map((acc) => (
                <AccountCard 
                  key={acc.id} 
                  account={acc} 
                  onDeposit={handleDeposit}
                  onWithdraw={handleWithdraw}
                />
              ))}
              
              <motion.button
                onClick={() => setIsNewAccountOpen(true)}
                whileHover={{ scale: 0.99 }}
                className="group p-8 rounded-[32px] border-2 border-dashed border-border-subtle bg-surface-2/50 flex flex-col items-center justify-center gap-6 min-h-[360px] transition-all hover:bg-surface-2 hover:border-primary-500/40"
              >
                 <div className="w-16 h-16 rounded-[24px] bg-surface border border-border-subtle flex items-center justify-center text-muted-text group-hover:text-primary-500 group-hover:border-primary-500/20 transition-all">
                    <Plus size={32} strokeWidth={3} />
                 </div>
                 <div className="text-center">
                    <h3 className="text-xl font-black text-app-text tracking-tight mb-1" id="add-account-card">Add Another Account</h3>
                    <p className="text-sm font-medium text-muted-text max-w-[200px]">Unlock more benefits with specialized account types.</p>
                 </div>
              </motion.button>
            </>
          )}
       </div>

       <Modal
         isOpen={isNewAccountOpen}
         onClose={() => setIsNewAccountOpen(false)}
         title="Expansion Control Center"
         size="md"
       >
          <form onSubmit={handleSubmit(onOpenAccount)} className="space-y-8">
             <div className="p-4 bg-primary-500/5 border border-primary-500/10 rounded-2xl flex items-start gap-4">
               <Info size={20} className="text-primary-500 shrink-0 mt-1" />
               <p className="text-[10px] font-bold text-muted-text leading-relaxed uppercase tracking-wider">
                 Selecting an account type determines your interest yield and daily transaction thresholds.
               </p>
             </div>

             <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                   <label className="text-xs font-bold text-muted-text uppercase tracking-widest ml-1">Account Type</label>
                    <select 
                       {...register('type')}
                       className="w-full bg-surface-2 border border-border-subtle rounded-xl px-4 py-3.5 text-sm outline-none focus:ring-2 ring-primary-500/20 text-app-text font-bold transition-all focus:border-primary-500/50"
                    >
                       <option value="SAVINGS">SAVINGS PRO</option>
                       <option value="CURRENT">ELITE CURRENT</option>
                       <option value="FIXED_DEPOSIT">FIXED DEPOSIT</option>
                    </select>
                </div>

                <div className="space-y-2">
                   <label className="text-xs font-bold text-muted-text uppercase tracking-widest ml-1">Branch Selection</label>
                     <select 
                        {...register('branch')}
                        className="w-full bg-surface-2 border border-border-subtle rounded-xl px-4 py-3.5 text-sm outline-none focus:ring-2 ring-primary-500/20 text-app-text font-bold transition-all focus:border-primary-500/50"
                     >
                        <option value="">Choose Branch...</option>
                        <option value="1">Mumbai Headquarters (Main)</option>
                        <option value="2">Delhi Tech Center</option>
                        <option value="3">Bengalaru Hub</option>
                     </select>
                   {errors.branch && <p className="text-xs text-danger-500 mt-1">{errors.branch.message}</p>}
                </div>

                {watchType === 'FIXED_DEPOSIT' && (
                  <div className="md:col-span-2 space-y-4">
                     <div className="space-y-2">
                        <label className="text-xs font-bold text-dark-400 uppercase tracking-widest ml-1">Fixed Tenure (Months)</label>
                        <div className="grid grid-cols-4 gap-3">
                           {[6, 12, 24, 36].map(m => (
                             <label key={m} className={`
                                flex flex-col items-center p-3 rounded-2xl border cursor-pointer transition-all
                                 ${Number(watchTenure) === m ? 'bg-amber-500/10 border-amber-500 shadow-lg shadow-amber-500/10' : 'bg-surface-2 border-border-subtle hover:border-primary-500/20'}
                              `}>
                                <input type="radio" value={m} {...register('tenure')} className="hidden" />
                                <span className={`text-sm font-black ${Number(watchTenure) === m ? 'text-amber-600' : 'text-muted-text'}`}>{m}m</span>
                                <span className="text-[10px] font-bold text-muted-text">{INTEREST_RATES[m]}%</span>
                              </label>
                           ))}
                        </div>
                     </div>

                     <div className="p-6 bg-amber-500/5 rounded-[32px] border border-amber-500/10 flex items-center justify-between">
                         <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-2xl bg-amber-500/20 flex items-center justify-center text-amber-500">
                               <TrendingUp size={24} />
                            </div>
                             <div>
                                <p className="text-[10px] font-bold text-amber-500/60 uppercase tracking-widest leading-none mb-1">Maturity Estimate</p>
                                <p className="text-2xl font-black text-amber-600 tracking-tighter">{formatCurrency(maturityAmount)}</p>
                             </div>
                         </div>
                         <div className="text-right">
                             <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-1">Final APR</p>
                             <p className="text-lg font-black text-amber-500">{INTEREST_RATES[watchTenure] || 0}%</p>
                         </div>
                     </div>
                  </div>
                )}

                <div className="md:col-span-2">
                   <Input 
                      label="Initial Deposit"
                      type="number"
                      placeholder="Min. ₹500"
                      leftIcon={<span className="font-bold text-dark-400">₹</span>}
                      {...register('initialDeposit')}
                      error={errors.initialDeposit}
                   />
                </div>
             </div>

             <Button type="submit" className="w-full h-14" isLoading={isSubmitting}>
               Authorize Account Creation
             </Button>
          </form>
       </Modal>

       <DepositModal isOpen={isDepositOpen} onClose={() => setIsDepositOpen(false)} initialAccount={selectedAccount?.id} />
       <WithdrawModal isOpen={isWithdrawOpen} onClose={() => setIsWithdrawOpen(false)} initialAccount={selectedAccount?.id} />
    </div>
  );
};

export default React.memo(AccountsPage);
