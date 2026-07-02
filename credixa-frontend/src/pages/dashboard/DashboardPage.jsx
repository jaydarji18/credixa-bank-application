import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  Wallet, 
  TrendingUp, 
  TrendingDown, 
  Landmark, 
  Send, 
  ArrowDownCircle, 
  ArrowUpCircle,
  Plus
} from 'lucide-react';
import { motion } from 'framer-motion';

import { StatCard } from './StatCard';
import TradingChart from '../../components/charts/TradingChart';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import DepositModal from './DepositModal';
import WithdrawModal from './WithdrawModal';

import { fetchAccountSummary, fetchAccounts } from '../../store/thunks/accountThunks';
import { fetchNotifications } from '../../store/thunks/notificationThunks';
import { formatCurrency, maskAccountNumber } from '../../utils/formatters';

const DashboardPage = () => {
  const dispatch = useDispatch();
  const { summary, accounts, loading: accLoading } = useSelector((state) => state.account);
  
  const [isDepositOpen, setIsDepositOpen] = useState(false);
  const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);

  useEffect(() => {
    dispatch(fetchAccountSummary());
    dispatch(fetchAccounts());
    dispatch(fetchNotifications());
  }, [dispatch]);

  const stats = [
    { 
      label: 'Total Balance', 
      value: summary?.totalBalance || 0, 
      icon: Wallet, 
      color: 'bg-primary-500', 
      trend: { value: 2.5, positive: true } 
    },
    { 
      label: 'Monthly Income', 
      value: summary?.monthlyIncome || 0, 
      icon: TrendingUp, 
      color: 'bg-success-500', 
      trend: { value: 12, positive: true } 
    },
    { 
      label: 'Monthly Expenses', 
      value: summary?.monthlyExpenses || 0, 
      icon: TrendingDown, 
      color: 'bg-danger-500', 
      trend: { value: 8, positive: false } 
    },
    { 
      label: 'Loan Balance', 
      value: summary?.totalLoanBalance || 0, 
      icon: Landmark, 
      color: 'bg-secondary-500', 
      trend: { value: 0.5, positive: false },
      subValue: summary?.activeLoanCount > 0 ? `${summary.activeLoanCount} Active Loans` : 'No Active Loans'
    },
  ];

  const container = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };

  return (
    <div className="space-y-8 animate-slide-in">
      {/* Quick Actions */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Button 
          variant="secondary" 
          className="h-16 rounded-2xl border-border-subtle bg-surface shadow-sm"
          leftIcon={<Send size={20} className="text-primary-500" />}
          onClick={() => window.location.href = '/transfer'}
        >
          Send Money
        </Button>
        <Button 
          variant="secondary" 
          className="h-16 rounded-2xl border-border-subtle bg-surface shadow-sm"
          leftIcon={<ArrowDownCircle size={20} className="text-success-500" />}
          onClick={() => setIsDepositOpen(true)}
        >
          Deposit
        </Button>
        <Button 
          variant="secondary" 
          className="h-16 rounded-2xl border-border-subtle bg-surface shadow-sm"
          leftIcon={<ArrowUpCircle size={20} className="text-danger-500" />}
          onClick={() => setIsWithdrawOpen(true)}
        >
          Withdraw
        </Button>
        <Button 
          variant="secondary" 
          className="h-16 rounded-2xl border-border-subtle bg-surface shadow-sm"
          leftIcon={<Landmark size={20} className="text-secondary-500" />}
          onClick={() => window.location.href = '/loans'}
        >
          My Loans
        </Button>
      </section>

      {/* Stats Grid */}
      <motion.section 
        variants={container}
        initial="hidden"
        animate="show"
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
      >
        {accLoading && !summary ? (
          [...Array(4)].map((_, i) => <Skeleton key={i} variant="card" height={160} />)
        ) : (
          stats.map((stat, i) => (
            <StatCard key={i} {...stat} delay={i * 0.1} />
          ))
        )}
      </motion.section>

      {/* Horizontal Accounts List */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-xl font-black text-app-text tracking-tight">Your Accounts</h3>
          <button className="p-2 rounded-xl bg-primary-600/10 text-primary-500 hover:bg-primary-600/20 transition-all">
            <Plus size={20} />
          </button>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {accLoading ? (
            [...Array(3)].map((_, i) => <Skeleton key={i} variant="card" height={120} />)
          ) : accounts.length > 0 ? (
            accounts.map((acc) => (
              <div key={acc.id} className="glass-card flex flex-col gap-4 group hover:border-primary-500/30 transition-all cursor-pointer">
                <div className="flex justify-between items-start">
                  <Badge variant={acc.type === 'SAVINGS' ? 'primary' : 'dark'}>
                    {acc.type}
                  </Badge>
                  <Badge variant={acc.status === 'ACTIVE' ? 'success' : 'danger'}>
                    {acc.status}
                  </Badge>
                </div>
                <div>
                  <span className="text-2xl font-black text-app-text block mb-1">{formatCurrency(acc.balance)}</span>
                  <span className="text-xs font-bold text-muted-text font-mono italic">
                    {maskAccountNumber(acc.accountNumber)}
                  </span>
                </div>
              </div>
            ))
          ) : (
            <div className="glass-card p-8 text-center text-muted-text font-bold text-sm lg:col-span-3">
              No accounts found.
            </div>
          )}
        </div>
      </section>

      {/* Trading Spend Chart */}
      <section className="glass-card p-8">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
          <div>
            <h3 className="text-xl font-black text-app-text tracking-tight">Spending Analytics</h3>
            <p className="text-xs font-bold text-muted-text uppercase tracking-widest mt-1 italic">Real-time financial trend</p>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-xs font-bold text-muted-text uppercase">Total Expenses</span>
            <span className="text-2xl font-black text-danger-500">{formatCurrency(summary?.monthlyExpenses || 0)}</span>
          </div>
        </div>
        <TradingChart data={summary?.spendingTrend} />
      </section>

      {/* Modals */}
      <DepositModal isOpen={isDepositOpen} onClose={() => setIsDepositOpen(false)} />
      <WithdrawModal isOpen={isWithdrawOpen} onClose={() => setIsWithdrawOpen(false)} />
    </div>
  );
};

export default DashboardPage;
