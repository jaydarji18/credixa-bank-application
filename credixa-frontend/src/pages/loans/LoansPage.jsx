import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  Landmark, 
  TrendingUp, 
  Calendar, 
  ArrowRight,
  Info,
  Briefcase,
  Home,
  Car
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import axiosInstance from '../../api/axios';
import { LOANS } from '../../api/endpoints';
import { fetchAccounts } from '../../store/thunks/accountThunks';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { loanApplySchema } from '../../utils/validators';
import useToast from '../../hooks/useToast';
import SpinModal from '../../components/common/SpinModal';

const LoanProductCard = React.memo(({ product, onApply }) => (
  <div className="glass-card hover:border-primary-500/30 transition-all flex flex-col group">
    <div className="w-14 h-14 rounded-[20px] bg-primary-600/10 border border-primary-500/10 flex items-center justify-center text-primary-500 mb-6 group-hover:scale-110 transition-transform">
      <product.icon size={28} />
    </div>
    <h3 className="text-xl font-black text-app-text uppercase tracking-tight mb-2">{product.name}</h3>
    <div className="flex items-baseline gap-1 mb-6">
      <span className="text-4xl font-black text-primary-500 tracking-tighter">{product.rate}</span>
      <span className="text-lg font-bold text-primary-500/60">% APR</span>
    </div>
    <div className="space-y-3 mb-8">
      <div className="flex justify-between text-xs font-bold">
        <span className="text-muted-text uppercase tracking-widest">Min Amount</span>
        <span className="text-app-text">₹{product.min.toLocaleString()}</span>
      </div>
      <div className="flex justify-between text-xs font-bold">
        <span className="text-muted-text uppercase tracking-widest">Max Amount</span>
        <span className="text-app-text">₹{product.max / 100000}L+</span>
      </div>
    </div>
    <Button 
      className="mt-auto w-full" 
      rightIcon={<ArrowRight size={18} />}
      onClick={() => onApply(product)}
    >
      Apply Now
    </Button>
  </div>
));

LoanProductCard.displayName = 'LoanProductCard';

const MyLoanCard = React.memo(({ loan }) => (
  <div className="glass-card hover:border-primary-500/30 transition-all group">
    <div className="flex justify-between items-start mb-6">
      <div className="flex items-center gap-4">
        <div className="w-12 h-12 rounded-2xl bg-primary-500/10 flex items-center justify-center text-primary-500">
          <Landmark size={24} />
        </div>
        <div>
          <p className="text-xs font-bold text-muted-text uppercase tracking-widest">{loan.loanNumber}</p>
          <h3 className="text-lg font-black text-app-text uppercase tracking-tight">{loan.productName}</h3>
        </div>
      </div>
      <Badge variant={loan.status === 'ACTIVE' ? 'success' : 'warning'}>{loan.status}</Badge>
    </div>
    
    <div className="space-y-1 mb-6">
      <div className="flex justify-between text-xs font-bold mb-1">
        <span className="text-muted-text">Repayment Progress</span>
        <span className="text-app-text">{loan.principalAmount > 0 ? Math.round(((loan.paidAmount || 0) / loan.principalAmount) * 100) : 0}%</span>
      </div>
      <div className="h-2 bg-surface-2 rounded-full overflow-hidden border border-border-subtle">
        <motion.div 
          initial={{ width: 0 }}
          animate={{ width: `${loan.principalAmount > 0 ? ((loan.paidAmount || 0) / loan.principalAmount) * 100 : 0}%` }}
          className="h-full bg-gradient-to-r from-primary-600 to-secondary-500"
        />
      </div>
    </div>

    <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border-subtle">
      <div>
        <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Monthly EMI</p>
        <p className="text-sm font-black text-app-text">{formatCurrency(loan.emiAmount)}</p>
      </div>
      <div className="text-right">
        <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Outstanding Balance</p>
        <p className="text-sm font-black text-danger-500">{formatCurrency(loan.outstandingBalance)}</p>
      </div>
    </div>
  </div>
));

MyLoanCard.displayName = 'MyLoanCard';

const LoansPage = () => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { accounts } = useSelector((state) => state.account);
  
  const [activeTab, setActiveTab] = useState('loans');
  const [myLoans, setMyLoans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState(null);
  
  const [loanProducts, setLoanProducts] = useState([]);
  const [isSpinModalOpen, setIsSpinModalOpen] = useState(false);
  const [pendingLoanData, setPendingLoanData] = useState(null);

  const fetchLoanProducts = useCallback(async () => {
    try {
      setLoading(true);
      const data = await axiosInstance.get(LOANS.LIST + '/products');
      // Access data field - axios interceptor already unwraps to response.data.data
      const productsData = data || [];
      
      // Map icons based on type (Backend uses PERSONAL_LOAN, HOME_LOAN, etc.)
      const iconMap = {
        PERSONAL_LOAN: Briefcase,
        HOME_LOAN: Home,
        VEHICLE_LOAN: Car,
        PERSONAL: Briefcase, // Fallback for old data
        HOME: Home,
        VEHICLE: Car
      };
      const products = productsData.map(p => ({
        ...p,
        name: p.productName,
        type: p.loanType,
        icon: iconMap[p.loanType] || Landmark,
        rate: p.interestRate,
        min: p.minAmount,
        max: p.maxAmount || 99000000
      }));
      setLoanProducts(products);
    } catch (err) {
      toast.error('Failed to load loan products');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  const fetchMyLoans = useCallback(async () => {
    try {
      setLoading(true);
      const data = await axiosInstance.get(LOANS.MY_LOANS);
      // Access data from axios interceptor unwrapped result
      setMyLoans(data || []);
    } catch (err) {
      toast.error('Failed to load your loans');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchMyLoans();
    fetchLoanProducts();
    dispatch(fetchAccounts());
  }, [fetchMyLoans, fetchLoanProducts, dispatch]);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting }
  } = useForm({
    resolver: zodResolver(loanApplySchema)
  });

  const watchAmount = watch('amount');
  const watchTenure = watch('tenure');

  const calculatedEMI = useMemo(() => {
    const amount = Number(watchAmount) || 0;
    const tenure = Number(watchTenure) || 0;
    const rate = selectedProduct?.rate || 0;
    if (!amount || !tenure || !rate) return 0;
    const r = rate / 12 / 100;
    const n = tenure;
    return (amount * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
  }, [watchAmount, watchTenure, selectedProduct]);

  const handleApplyClick = useCallback((product) => {
    setSelectedProduct(product);
    reset({ productId: product.id, amount: product.min, tenure: 12 });
  }, [reset]);

  const onApply = useCallback(async (data) => {
    setPendingLoanData(data);
    setIsSpinModalOpen(true);
  }, []);

  const handleAuthorizedApply = async () => {
    const data = pendingLoanData;
    try {
      // Map frontend fields to backend DTO
      const requestPayload = {
        requestedAmount: data.amount,
        tenureMonths: data.tenure,
        loanProductId: data.productId,
        linkedAccountId: data.linkedAccountId
      };
      
      await axiosInstance.post(LOANS.APPLY, requestPayload);
      toast.success('Loan application submitted successfully');
      reset();
      setSelectedProduct(null);
      setActiveTab('loans');
      fetchMyLoans();
    } catch (err) {
      toast.error(err.message || 'Application failed');
    }
  };

  return (
    <div className="space-y-8 animate-slide-in">
       <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-3xl font-black text-app-text tracking-tighter">Loans & Credit</h2>
            <p className="text-muted-text font-medium">Empower your dreams with our flexible credit solutions.</p>
          </div>
          
          <div className="p-1.5 bg-surface-2 border border-border-subtle rounded-2xl flex gap-1">
             <button 
                onClick={() => setActiveTab('loans')}
                className={`px-6 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === 'loans' ? 'bg-primary-500 text-white shadow-lg' : 'text-muted-text hover:text-app-text'}`}
             >
                My Loans
             </button>
             <button 
                onClick={() => setActiveTab('products')}
                className={`px-6 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === 'products' ? 'bg-primary-500 text-white shadow-lg' : 'text-muted-text hover:text-app-text'}`}
             >
                Loan Products
             </button>
          </div>
       </div>

       <AnimatePresence mode="wait">
          {activeTab === 'loans' ? (
            <motion.div 
               key="my-loans"
               initial={{ opacity: 0, x: -20 }}
               animate={{ opacity: 1, x: 0 }}
               exit={{ opacity: 0, x: 20 }}
               className="space-y-6"
            >
               {loading ? (
                  <div className="grid md:grid-cols-2 gap-6">
                    {[...Array(2)].map((_, i) => <Skeleton key={i} variant="card" height={200} />)}
                  </div>
               ) : myLoans.length > 0 ? (
                 <div className="grid md:grid-cols-2 gap-6">
                    {myLoans.map(loan => <MyLoanCard key={loan.id} loan={loan} />)}
                 </div>
               ) : (
                 <EmptyState 
                    icon={Landmark}
                    title="No active debt detected"
                    message="You don't have any active loans. Explore our premium loan products to start your journey."
                 />
               )}
            </motion.div>
          ) : (
            <motion.div 
               key="products"
               initial={{ opacity: 0, x: 20 }}
               animate={{ opacity: 1, x: 0 }}
               exit={{ opacity: 0, x: -20 }}
               className="space-y-6"
            >
               {loading ? (
                  <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {[...Array(3)].map((_, i) => <Skeleton key={i} variant="card" height={300} />)}
                  </div>
               ) : loanProducts.length > 0 ? (
                  <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {loanProducts.map(product => (
                      <LoanProductCard key={product.id} product={product} onApply={handleApplyClick} />
                    ))}
                  </div>
               ) : (
                  <EmptyState 
                    icon={Briefcase}
                    title="No loan products available"
                    message="Check back soon for our latest financial offerings."
                  />
               )}
            </motion.div>
          )}
       </AnimatePresence>

       <Modal
          isOpen={!!selectedProduct}
          onClose={() => setSelectedProduct(null)}
          title={`Applying for ${selectedProduct?.name}`}
          size="md"
       >
          <form onSubmit={handleSubmit(onApply)} className="space-y-8">
             <input type="hidden" {...register('productId')} />
             <div className="p-4 bg-primary-500/5 border border-primary-500/10 rounded-2xl flex items-start gap-4">
                <Info size={20} className="text-primary-500 shrink-0 mt-1" />
                <p className="text-[10px] font-bold text-dark-300 leading-relaxed uppercase tracking-wider">
                   Approved funds will be disbursed to your linked account within 24-48 business hours.
                </p>
             </div>

             <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="text-xs font-bold text-dark-400 uppercase tracking-widest ml-1 mb-2 block">Linked Account</label>
                  <select
                    {...register('linkedAccountId')}
                    className="w-full bg-surface-2 border border-border-subtle rounded-xl px-4 py-3.5 text-sm text-app-text font-bold outline-none focus:ring-2 ring-primary-500/20 transition-all focus:border-primary-500/50"
                  >
                    <option value="">Select account...</option>
                    {accounts.map(acc => (
                      <option key={acc.id} value={acc.id}>{acc.accountNumber}</option>
                    ))}
                  </select>
                  {errors.linkedAccountId && <p className="text-xs text-danger-500 mt-1 font-bold">{errors.linkedAccountId.message}</p>}
                </div>

                <Input 
                   label="Loan Amount"
                   type="number"
                   placeholder={`Min. ₹${selectedProduct?.min.toLocaleString()}`}
                   leftIcon={<span className="font-bold text-dark-400 text-xs">₹</span>}
                   {...register('amount')}
                   error={errors.amount}
                />

                <div>
                   <label className="text-xs font-bold text-dark-400 uppercase tracking-widest ml-1 mb-2 block">Tenure (Months)</label>
                   <select 
                      {...register('tenure')}
                      className="w-full bg-surface-2 border border-border-subtle rounded-xl px-4 py-3.5 text-sm text-app-text font-bold outline-none focus:ring-2 ring-primary-500/20 transition-all focus:border-primary-500/50"
                   >
                      {[12, 24, 36, 48, 60].map(m => <option key={m} value={m}>{m} Months</option>)}
                   </select>
                </div>

                <div className="p-6 bg-primary-500/5 rounded-[32px] border border-primary-500/10 flex flex-col justify-center">
                   <p className="text-[10px] font-bold text-primary-500/60 uppercase tracking-widest leading-none mb-2 text-center italic">Projected Monthly EMI</p>
                   <p className="text-3xl font-black text-app-text tracking-tighter text-center">
                     {formatCurrency(calculatedEMI)}
                   </p>
                </div>
             </div>

             <Button type="submit" className="w-full h-14" isLoading={isSubmitting}>
                Authorize Disbursement Application
             </Button>
          </form>
       </Modal>

        <SpinModal 
          isOpen={isSpinModalOpen}
          onClose={() => setIsSpinModalOpen(false)}
          onSuccess={handleAuthorizedApply}
          mode="verify"
          title="Confirm Loan Application"
          description={`Enter 6-digit sPin to authorize ₹${pendingLoanData?.amount.toLocaleString()} loan request`}
        />
    </div>
  );
};

export default React.memo(LoansPage);
