import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useDispatch, useSelector } from 'react-redux';
import { ArrowUpCircle, AlertTriangle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import axiosInstance from '../../api/axios';
import { TRANSACTIONS } from '../../api/endpoints';
import { fetchAccounts, fetchAccountSummary } from '../../store/thunks/accountThunks';
import { fetchTransactions } from '../../store/thunks/transactionThunks';
import useToast from '../../hooks/useToast';

const withdrawSchema = z.object({
  accountId: z.string().min(1, 'Please select an account'),
  amount: z.coerce.number()
    .min(1, 'Minimum withdrawal is ₹1'),
  description: z.string().min(1, 'Description is required').max(50, 'Too long'),
});

const WithdrawModal = ({ isOpen, onClose }) => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { accounts } = useSelector((state) => state.account);
  const [errorMessage, setErrorMessage] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(withdrawSchema),
  });

  const selectedAccountId = watch('accountId');
  const selectedAccount = accounts.find(a => a.id === selectedAccountId);

  const onSubmit = async (data) => {
    try {
      setErrorMessage(null);
      
      // Basic client-side check before API
      if (selectedAccount && data.amount > selectedAccount.balance) {
        setErrorMessage('Insufficient balance in selected account');
        return;
      }

      await axiosInstance.post(TRANSACTIONS.WITHDRAW, data);
      toast.success(`Successfully withdrawn ₹${data.amount}`);
      
      // Refresh data
      dispatch(fetchAccounts());
      dispatch(fetchAccountSummary());
      dispatch(fetchTransactions());
      
      reset();
      onClose();
    } catch (err) {
      setErrorMessage(err.message || 'Withdrawal failed');
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Withdraw Funds"
      size="sm"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="p-4 bg-danger-500/5 border border-danger-500/10 rounded-2xl flex items-start gap-4">
          <AlertTriangle size={20} className="text-danger-500 shrink-0 mt-1" />
          <p className="text-xs font-medium text-muted-text leading-relaxed">
            Ensure you have sufficient funds before proceeding. 
            Withdrawals typically reflect in your account transactions immediately.
          </p>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-semibold text-muted-text ml-1">Select Source Account</label>
          <select
            {...register('accountId')}
            className={`
              w-full bg-surface-2 border border-border-subtle rounded-xl px-4 py-3.5 text-sm 
              focus:border-primary-500 focus:ring-1 focus:ring-primary-500/50 
              transition-all duration-300 text-app-text outline-none appearance-none
              ${errors.accountId ? 'border-danger-500/50' : ''}
            `}
          >
            <option value="">Choose an account...</option>
            {accounts.map((acc) => (
              <option key={acc.id} value={acc.id}>
                {acc.type} - {acc.accountNumber} (₹{acc.balance})
              </option>
            ))}
          </select>
          {errors.accountId && <p className="text-xs text-danger-500 font-medium ml-1">{errors.accountId.message}</p>}
        </div>

        <Input
          label="Withdrawal Amount"
          type="number"
          placeholder="0.00"
          leftIcon={<span className="text-dark-400 font-bold">₹</span>}
          error={errors.amount}
          {...register('amount')}
        />

        <Input
          label="Description"
          placeholder="e.g. Cash withdrawal"
          error={errors.description}
          {...register('description')}
        />

        <AnimatePresence>
          {errorMessage && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="p-3 bg-danger-500/10 border border-danger-500/20 rounded-xl flex items-center gap-2 text-danger-500 text-xs font-bold"
            >
              <AlertTriangle size={14} />
              {errorMessage}
            </motion.div>
          )}
        </AnimatePresence>

        <div className="pt-2">
          <Button
            type="submit"
            variant="danger"
            className="w-full"
            isLoading={isSubmitting}
            leftIcon={<ArrowUpCircle size={20} />}
          >
            Confirm Withdrawal
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default WithdrawModal;
