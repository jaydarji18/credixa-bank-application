import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useDispatch, useSelector } from 'react-redux';
import { ArrowDownCircle, Info } from 'lucide-react';

import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import axiosInstance from '../../api/axios';
import { TRANSACTIONS } from '../../api/endpoints';
import { fetchAccounts, fetchAccountSummary } from '../../store/thunks/accountThunks';
import { fetchTransactions } from '../../store/thunks/transactionThunks';
import useToast from '../../hooks/useToast';

const depositSchema = z.object({
  accountId: z.string().min(1, 'Please select an account'),
  amount: z.coerce.number()
    .min(1, 'Minimum deposit is ₹1')
    .max(1000000, 'Maximum deposit is ₹10,00,000'),
  description: z.string().min(1, 'Description is required').max(50, 'Too long'),
});

const DepositModal = ({ isOpen, onClose }) => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { accounts } = useSelector((state) => state.account);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(depositSchema),
  });

  const onSubmit = async (data) => {
    try {
      await axiosInstance.post(TRANSACTIONS.DEPOSIT, data);
      toast.success(`Successfully deposited ₹${data.amount}`);
      
      // Refresh data
      dispatch(fetchAccounts());
      dispatch(fetchAccountSummary());
      dispatch(fetchTransactions());
      
      reset();
      onClose();
    } catch (err) {
      toast.error(err.message || 'Deposit failed');
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Deposit Funds"
      size="sm"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="p-4 bg-primary-500/5 border border-primary-500/10 rounded-2xl flex items-start gap-4">
          <Info size={20} className="text-primary-500 shrink-0 mt-1" />
          <p className="text-xs font-medium text-muted-text leading-relaxed">
            Funds will be credited to your selected account instantly after processing. 
            Maximum limit per transaction is ₹10 Lacs.
          </p>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-semibold text-muted-text ml-1">Select Account</label>
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
          label="Deposit Amount"
          type="number"
          placeholder="0.00"
          leftIcon={<span className="text-dark-400 font-bold">₹</span>}
          error={errors.amount}
          {...register('amount')}
        />

        <Input
          label="Description"
          placeholder="e.g. Monthly Savings"
          error={errors.description}
          {...register('description')}
        />

        <div className="pt-2">
          <Button
            type="submit"
            className="w-full"
            isLoading={isSubmitting}
            leftIcon={<ArrowDownCircle size={20} />}
          >
            Confirm Deposit
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default DepositModal;
