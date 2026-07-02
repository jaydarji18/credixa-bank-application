import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Plus,
  ArrowRight,
  Check,
  ArrowLeft,
  Building2,
  Zap,
  ShieldCheck,
  Send,
  Info,
  Clock,
  Landmark
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { fetchBeneficiaries, addBeneficiary } from '../../store/thunks/beneficiaryThunks';
import { fetchAccounts } from '../../store/thunks/accountThunks';
import axiosInstance from '../../api/axios';
import { TRANSACTIONS } from '../../api/endpoints';
import { formatCurrency, maskAccountNumber } from '../../utils/formatters';
import { transferSchema, addBeneficiarySchema } from '../../utils/validators';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Skeleton from '../../components/common/Skeleton';
import Modal from '../../components/common/Modal';
import SpinModal from '../../components/common/SpinModal';
import useToast from '../../hooks/useToast';

const BeneficiaryBtn = React.memo(({ ben, isSelected, onSelect }) => (
  <button
    onClick={() => onSelect(ben)}
    className={`
      text-left p-4 rounded-[24px] border transition-all group flex items-center gap-4
      ${isSelected ? 'bg-primary-500/10 border-primary-500 ring-1 ring-primary-500 shadow-xl shadow-primary-500/5' : 'bg-surface border-white/5 hover:border-white/10 hover:bg-white/5'}
    `}
  >
    <div className="w-12 h-12 rounded-2xl bg-surface-2 border border-border-subtle flex items-center justify-center font-bold text-primary-500 group-hover:scale-105 transition-transform">
      {ben.beneficiaryName.charAt(0)}
    </div>
    <div className="flex-1 min-w-0">
      <div className="flex items-center gap-1.5">
        <p className="font-black text-app-text truncate text-sm uppercase tracking-tight">{ben.beneficiaryName}</p>
        {(ben.verified || true) && <ShieldCheck size={14} className="text-success-500" />}
      </div>
      <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest">{ben.bankName}</p>
      <p className="text-xs font-mono text-muted-text mt-0.5">{ben.accountNumber}</p>
    </div>
    {isSelected && (
      <div className="w-6 h-6 rounded-full bg-primary-500 flex items-center justify-center text-white scale-110">
        <Check size={14} />
      </div>
    )}
  </button>
));

BeneficiaryBtn.displayName = 'BeneficiaryBtn';

const TransferPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const toast = useToast();

  const { beneficiaries, loading: benLoading } = useSelector((state) => state.beneficiary);
  const { accounts } = useSelector((state) => state.account);

  const [step, setStep] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedBeneficiary, setSelectedBeneficiary] = useState(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [transferData, setTransferData] = useState(null);
  const [transactionResult, setTransactionResult] = useState(null);
  const [isSpinModalOpen, setIsSpinModalOpen] = useState(false);

  useEffect(() => {
    dispatch(fetchBeneficiaries());
    dispatch(fetchAccounts());
  }, [dispatch]);

  const {
    register: regBen,
    handleSubmit: handleBenSubmit,
    reset: resetBen,
    formState: { errors: benErrors, isSubmitting: benSubmitting }
  } = useForm({ resolver: zodResolver(addBeneficiarySchema) });

  const {
    register: regTx,
    handleSubmit: handleTxSubmit,
    watch: watchTx,
    formState: { errors: txErrors }
  } = useForm({
    resolver: zodResolver(transferSchema),
    defaultValues: { type: 'IMPS' }
  });

  const watchAmount = watchTx('amount');
  const watchFromAccount = watchTx('fromAccountId');
  const watchType = watchTx('type');

  const selectedAccount = useMemo(() => accounts.find(a => a.id === watchFromAccount), [accounts, watchFromAccount]);

  const FEES = useMemo(() => ({
    NEFT: 5,
    RTGS: 25,
    IMPS: 10,
    UPI: 0,
    INTERNAL: 0
  }), []);

  const currentFee = FEES[watchType] || 0;
  const totalDeduction = useMemo(() => (Number(watchAmount) || 0) + currentFee, [watchAmount, currentFee]);

  const onAddBeneficiary = useCallback(async (data) => {
    try {
      await dispatch(addBeneficiary(data)).unwrap();
      toast.success('Beneficiary registered successfully');
      setShowAddForm(false);
      resetBen();
    } catch (err) {
      toast.error(err || 'Failed to register beneficiary');
    }
  }, [dispatch, resetBen, toast]);

  const onReviewTransfer = useCallback((data) => {
    if (selectedAccount && totalDeduction > selectedAccount.balance) {
      toast.error('Insufficient funds in selected account');
      return;
    }
    setTransferData({ ...data, beneficiary: selectedBeneficiary, fee: currentFee, total: totalDeduction });
    setStep(3);
  }, [selectedAccount, totalDeduction, selectedBeneficiary, currentFee, toast]);

  const onConfirmTransfer = useCallback(async () => {
    setIsSpinModalOpen(true);
  }, []);

  const handleAuthorizedTransfer = async () => {
    try {
      const payload = {
        senderAccountId: Number(transferData.fromAccountId),
        beneficiaryId: Number(transferData.beneficiary.id),
        amount: Number(transferData.amount),
        transferType: `TRANSFER_${transferData.type}`,
        remarks: transferData.remarks
      };

      const response = await axiosInstance.post(TRANSACTIONS.TRANSFER, payload);
      setTransactionResult(response.data);
      setStep(4);
      toast.success('Funds authorization processed successfully');
    } catch (err) {
      toast.error(err.message || 'Transfer failed');
    }
  };

  const filteredBeneficiaries = useMemo(() => (
    beneficiaries.filter(b =>
      b.beneficiaryName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      b.accountNumber.includes(searchQuery)
    )
  ), [beneficiaries, searchQuery]);

  const handleSelectBen = useCallback((ben) => setSelectedBeneficiary(ben), []);

  return (
    <div className="max-w-4xl mx-auto pb-20 animate-slide-in">
      {/* Step Indicator */}
      {step < 4 && (
        <div className="flex items-center justify-center mb-12">
          {[1, 2, 3].map((s) => (
            <React.Fragment key={s}>
              <div className={`
                w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-all duration-500
                ${step === s ? 'bg-primary-500 text-white shadow-lg shadow-primary-500/20 ring-4 ring-primary-500/10' :
                  step > s ? 'bg-success-500 text-white' : 'bg-surface-2 text-muted-text border border-border-subtle'}
              `}>
                {step > s ? <Check size={18} /> : s}
              </div>
              {s < 3 && (
                <div className={`w-16 lg:w-32 h-1 mx-2 rounded-full transition-colors duration-500 ${step > s ? 'bg-success-500' : 'bg-surface-2 border border-border-subtle'}`} />
              )}
            </React.Fragment>
          ))}
        </div>
      )}

      <AnimatePresence mode="wait">
        {step === 1 && (
          <motion.div
            key="step1"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="space-y-6"
          >
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 className="text-3xl font-black text-app-text tracking-tighter">Transfer Funds</h2>
                <p className="text-muted-text font-medium">Select a secure verified beneficiary to proceed.</p>
              </div>
              <Button variant="outline" size="sm" leftIcon={<Plus size={18} />} onClick={() => setShowAddForm(true)}>New Payee</Button>
            </div>

            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-text" size={20} />
              <input
                type="text"
                placeholder="Search by identity or bank reference..."
                className="w-full bg-surface-2 border border-border-subtle rounded-2xl pl-12 pr-4 py-4 text-app-text outline-none focus:ring-2 ring-primary-500/20 transition-all font-bold placeholder:text-muted-text/50"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              {benLoading ? (
                [...Array(4)].map((_, i) => <Skeleton key={i} variant="card" height={100} />)
              ) : (
                filteredBeneficiaries.map((ben) => (
                  <BeneficiaryBtn
                    key={ben.id}
                    ben={ben}
                    isSelected={selectedBeneficiary?.id === ben.id}
                    onSelect={handleSelectBen}
                  />
                ))
              )}
            </div>

            <div className="pt-8 flex justify-end gap-4 border-t border-border-subtle">
              <Button
                disabled={!selectedBeneficiary}
                onClick={() => setStep(2)}
                rightIcon={<ArrowRight size={20} />}
              >
                Configure Transfer
              </Button>
            </div>
          </motion.div>
        )}

        {step === 2 && (
          <motion.div
            key="step2"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="space-y-8"
          >
            <div className="glass-card flex items-center gap-4 p-5 border-primary-500/20 bg-primary-500/5 shadow-2xl">
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-primary-600 to-secondary-500 flex items-center justify-center text-white font-black text-2xl">
                {selectedBeneficiary.beneficiaryName.charAt(0)}
              </div>
              <div className="flex-1">
                <p className="text-[10px] font-bold text-primary-500/60 uppercase tracking-[0.3em] leading-none mb-1">Target Identity</p>
                <p className="text-xl font-black text-app-text uppercase tracking-tight">{selectedBeneficiary.beneficiaryName}</p>
                <p className="text-xs font-bold text-muted-text font-mono tracking-wider">{selectedBeneficiary.bankName} • {maskAccountNumber(selectedBeneficiary.accountNumber)}</p>
              </div>
              <Button variant="ghost" size="sm" onClick={() => setStep(1)} className="text-muted-text hover:text-white">Switch</Button>
            </div>

            <form onSubmit={handleTxSubmit(onReviewTransfer)} className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="space-y-6">
                <div>
                  <label className="text-xs font-bold text-muted-text uppercase tracking-widest ml-1 mb-2 block">Source Account</label>
                  <select
                    {...regTx('fromAccountId')}
                    className="w-full bg-surface-2 border border-border-subtle rounded-2xl px-4 py-4 text-app-text text-sm outline-none focus:ring-2 ring-primary-500/20 transition-all font-black focus:border-primary-500/50"
                  >
                    <option value="">Choose an account...</option>
                    {accounts.map(acc => (
                      <option key={acc.id} value={acc.id}>
                        {acc.type} • {acc.accountNumber} (₹{acc.balance.toLocaleString()})
                      </option>
                    ))}
                  </select>
                  {txErrors.fromAccountId && <p className="text-xs text-danger-500 mt-2 font-bold">{txErrors.fromAccountId.message}</p>}
                </div>

                <Input
                  label="Transfer Volume"
                  type="number"
                  placeholder="0.00"
                  leftIcon={<span className="font-black text-muted-text text-xs">₹</span>}
                  {...regTx('amount')}
                  error={txErrors.amount}
                />

                <Input
                  label="Operation Remarks"
                  placeholder="Purpose of authorization"
                  {...regTx('remarks')}
                  error={txErrors.remarks}
                />
              </div>

              <div className="space-y-6">
                <div>
                  <label className="text-xs font-bold text-muted-text uppercase tracking-widest ml-1 mb-4 block">Transfer Protocol</label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { id: 'IMPS', icon: Zap, label: 'IMPS' },
                      { id: 'NEFT', icon: Clock, label: 'NEFT' },
                      { id: 'RTGS', icon: Building2, label: 'RTGS' },
                      { id: 'UPI', icon: ShieldCheck, label: 'UPI' },
                      { id: 'INTERNAL', icon: Landmark, label: 'INTERNAL' }
                    ].map(t => (
                      <label key={t.id} className={`
                           relative flex flex-col items-center justify-center p-4 rounded-2xl border cursor-pointer transition-all aspect-square
                           ${watchType === t.id ? 'bg-primary-500/10 border-primary-500 shadow-xl' : 'bg-surface border-border-subtle hover:border-border-subtle'}
                         `}>
                        <input type="radio" value={t.id} {...regTx('type')} className="hidden" />
                        <t.icon size={20} className={watchType === t.id ? 'text-primary-500' : 'text-muted-text'} />
                        <span className={`text-xs font-black mt-2 ${watchType === t.id ? 'text-primary-600' : 'text-muted-text'}`}>{t.label}</span>
                        <span className="text-[10px] font-bold text-muted-text mt-1">₹{FEES[t.id]} FEE</span>
                      </label>
                    ))}
                  </div>
                </div>

                <div className="p-8 bg-primary-500/5 rounded-[40px] border border-primary-500/10 space-y-4 shadow-inner">
                  <div className="flex justify-between text-xs font-bold">
                    <span className="text-muted-text uppercase tracking-widest">Protocol Fee</span>
                    <span className="text-app-text">₹{currentFee}.00</span>
                  </div>
                  <div className="h-px bg-border-subtle" />
                  <div className="flex justify-between items-baseline">
                    <span className="text-sm font-black text-app-text uppercase tracking-tight">Net Amount</span>
                    <span className="text-3xl font-black text-primary-500 tracking-tighter">₹{totalDeduction.toLocaleString()}</span>
                  </div>
                </div>
              </div>

              <div className="md:col-span-2 pt-8 flex justify-between gap-4 border-t border-border-subtle">
                <Button variant="ghost" type="button" onClick={() => setStep(1)} leftIcon={<ArrowLeft size={18} />}>Abandom</Button>
                <Button type="submit" rightIcon={<ArrowRight size={18} />} className="shadow-lg shadow-primary-500/20">Review Transaction</Button>
              </div>
            </form>
          </motion.div>
        )}

        {step === 3 && (
          <motion.div
            key="step3"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="max-w-xl mx-auto space-y-8"
          >
            <div className="text-center space-y-1">
              <h2 className="text-3xl font-black text-app-text tracking-tighter">Authorize Transfer</h2>
              <p className="text-muted-text font-medium">Global ledger synchronization verify protocol.</p>
            </div>

            <div className="glass-card p-0 overflow-hidden border-primary-500/20 shadow-2xl">
              <div className="bg-primary-500/5 p-10 flex flex-col items-center border-b border-border-subtle relative overflow-hidden">
                <div className="absolute top-0 right-0 p-4 opacity-5">
                  <Landmark size={120} />
                </div>
                <p className="text-[10px] font-black text-primary-500 uppercase tracking-[0.4em] mb-4">Total Disbursement</p>
                <h3 className="text-6xl font-black text-app-text tracking-tighter">₹{transferData.amount.toLocaleString()}</h3>
                <div className="mt-6 px-6 py-2 rounded-full bg-surface border border-border-subtle text-xs font-black text-primary-500 uppercase tracking-widest">
                  PROCESSED VIA {transferData.type}
                </div>
              </div>

              <div className="p-8 space-y-8">
                <div className="flex justify-between gap-8 items-center">
                  <div className="text-left space-y-1">
                    <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1 leading-none">ORIGIN</p>
                    <p className="text-sm font-black text-app-text uppercase tracking-tight leading-none">
                      {accounts.find(a => a.id === transferData.fromAccountId)?.type} SAVINGS
                    </p>
                    <p className="text-xs font-mono font-bold text-muted-text">
                      {accounts.find(a => a.id === transferData.fromAccountId)?.accountNumber}
                    </p>
                  </div>
                  <ArrowRight size={24} className="text-primary-500 animate-pulse" />
                  <div className="text-right space-y-1">
                    <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1 leading-none">DESTINATION</p>
                    <p className="text-sm font-black text-app-text uppercase tracking-tight leading-none">{transferData.beneficiary.beneficiaryName}</p>
                    <p className="text-xs font-mono font-bold text-muted-text italic">{transferData.beneficiary.bankName}</p>
                  </div>
                </div>

                <div className="pt-6 border-t border-border-subtle space-y-4">
                  <div className="flex justify-between text-xs font-bold font-mono">
                    <span className="text-muted-text">NETWORK FEE</span>
                    <span className="text-app-text">₹{transferData.fee}.00</span>
                  </div>
                  <div className="flex justify-between text-xs font-bold font-mono">
                    <span className="text-muted-text">TOTAL DEBIT</span>
                    <span className="text-app-text font-black text-lg">₹{transferData.total.toLocaleString()}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex gap-4">
              <Button variant="ghost" className="flex-1" onClick={() => setStep(2)}>Abort</Button>
              <Button className="flex-1 shadow-2xl shadow-primary-500/30" onClick={onConfirmTransfer} leftIcon={<ShieldCheck size={20} />}>Authorize Now</Button>
            </div>

            <p className="text-[10px] text-center text-dark-500 font-bold px-12 leading-relaxed italic opacity-60">
              BY AUTHORIZING THIS DISBURSEMENT, YOU ADHERE TO THE BANKING ACT REGULATIONS AND CREDIXA PRO CORE PROTOCOLS.
              IRREVERSIBLE OPERATION WARNING ACTIVATED.
            </p>
          </motion.div>
        )}

        {step === 4 && (
          <motion.div
            key="step4"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="max-w-md mx-auto text-center space-y-8 py-10"
          >
            <div className="relative">
              <div className="absolute inset-0 bg-success-500/30 blur-[120px] -z-10" />
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", damping: 12 }}
                className="w-28 h-28 bg-success-500 rounded-[40px] flex items-center justify-center text-white mx-auto shadow-2xl shadow-success-500/50"
              >
                <Check size={56} strokeWidth={4} />
              </motion.div>
            </div>

            <div className="space-y-3">
              <h2 className="text-4xl font-black text-app-text tracking-tighter">Transaction Success</h2>
              <p className="text-muted-text font-medium px-4">The funds have been authorized and are propagating through the banking network.</p>
            </div>

            <div className="glass-card p-8 divide-y divide-border-subtle space-y-5 bg-surface-2/50 border-success-500/20">
              <div className="flex justify-between items-center">
                <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest">Global Reference</span>
                <span className="text-xs font-mono font-black text-app-text">{transactionResult?.reference || 'CTX-' + Math.random().toString(36).substr(2, 9).toUpperCase()}</span>
              </div>
              <div className="flex justify-between items-center py-5">
                <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest">Disbursed Value</span>
                <span className="text-xl font-black text-success-500">₹{transferData.amount.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center pt-5">
                <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest">Network Timestamp</span>
                <span className="text-xs font-bold text-app-text uppercase tracking-tight">{new Date().toLocaleString()}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Button variant="outline" onClick={() => { setStep(1); setSelectedBeneficiary(null); }} leftIcon={<Send size={18} />}>Repeat Process</Button>
              <Button variant="secondary" onClick={() => navigate('/transactions')} rightIcon={<ArrowRight size={18} />}>View Ledger</Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <Modal
        isOpen={showAddForm}
        onClose={() => setShowAddForm(false)}
        title="Register Verified Payee"
        size="md"
      >
        <form onSubmit={handleBenSubmit(onAddBeneficiary)} className="space-y-8 p-1">
          <div className="p-5 bg-primary-500/5 border border-primary-500/10 rounded-[24px] flex items-start gap-4">
            <Info size={20} className="text-primary-500 shrink-0 mt-0.5" />
            <p className="text-[10px] font-bold text-muted-text leading-relaxed uppercase tracking-wider">
              PAYEE DISCOVERY PROTOCOL: Ensure the target account number and IFSC coordinates are precisely mapped to prevent authorization failure.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
            <div className="md:col-span-12">
              <Input
                label="Beneficiary Identity"
                placeholder="Legal Name as per Banking Records"
                {...regBen('beneficiaryName')}
                error={benErrors.beneficiaryName}
              />
            </div>
            <div className="md:col-span-12">
              <Input
                label="Account Number"
                placeholder="Global Identity Reference"
                {...regBen('accountNumber')}
                error={benErrors.accountNumber}
              />
            </div>
            <div className="md:col-span-6">
              <Input
                label="IFSC Code"
                placeholder="Branch Coordinate"
                {...regBen('ifscCode')}
                error={benErrors.ifscCode}
              />
            </div>
            <div className="md:col-span-6">
              <Input
                label="Bank Name"
                placeholder="Financial Institution"
                {...regBen('bankName')}
                error={benErrors.bankName}
              />
            </div>
            <div className="md:col-span-12">
              <Input
                label="Local Alias (Optional)"
                placeholder="Internal reference tag"
                {...regBen('nickname')}
                error={benErrors.nickname}
              />
            </div>
          </div>

          <div className="pt-4">
            <Button type="submit" className="w-full h-14" isLoading={benSubmitting}>Establish Payee Connection</Button>
          </div>
        </form>
      </Modal>

      <SpinModal 
        isOpen={isSpinModalOpen}
        onClose={() => setIsSpinModalOpen(false)}
        onSuccess={handleAuthorizedTransfer}
        mode="verify"
        title="Authorize Disbursement"
        description={`Confirm 6-digit sPin to authorize ₹${transferData?.amount.toLocaleString()} transfer to ${transferData?.beneficiary.beneficiaryName}`}
      />
    </div>
  );
};

export default React.memo(TransferPage);
