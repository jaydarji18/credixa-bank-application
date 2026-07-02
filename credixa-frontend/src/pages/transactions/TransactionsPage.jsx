import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  Search, 
  Filter, 
  ArrowDownCircle, 
  ArrowUpCircle, 
  ArrowLeftRight, 
  Eye, 
  Download,
  X,
  ChevronLeft,
  ChevronRight,
  FileSpreadsheet,
  FileText,
  Landmark
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import { fetchTransactions } from '../../store/thunks/transactionThunks';
import { fetchAccounts } from '../../store/thunks/accountThunks';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import axiosInstance from '../../api/axios';
import { TRANSACTIONS } from '../../api/endpoints';
import useToast from '../../hooks/useToast';
import useDebounce from '../../hooks/useDebounce';
import SpinModal from '../../components/common/SpinModal';

const TransactionRow = React.memo(({ tx, onSelect, userAccounts }) => {
  const isIncoming = useMemo(() => {
    const type = tx.transactionType || tx.type;
    if (['DEPOSIT', 'CREDIT'].includes(type)) return true;
    if (['WITHDRAWAL', 'DEBIT'].includes(type)) return false;
    
    // For transfers, check if any of the user's accounts is the receiver
    return userAccounts.some(acc => acc.accountNumber === tx.receiverAccountNumber);
  }, [tx, userAccounts]);

  const getTypeIcon = () => {
    const type = tx.transactionType || tx.type;
    if (isIncoming) return <ArrowDownCircle className="text-success-500" size={20} />;
    if (['WITHDRAWAL', 'DEBIT'].includes(type) || type?.startsWith('TRANSFER_')) {
       return <ArrowUpCircle className="text-danger-500" size={20} />;
    }
    return <ArrowLeftRight className="text-primary-500" size={20} />;
  };

  return (
    <tr className="hover:bg-surface-2 transition-colors group">
      <td className="px-6 py-5">
        <span className="text-xs font-mono font-bold text-muted-text">{tx.referenceNumber || tx.reference}</span>
      </td>
      <td className="px-6 py-5">
        <div className="flex items-center gap-3">
          {getTypeIcon()}
          <span className="text-xs font-bold text-app-text tracking-tight capitalize">
            {(tx.transactionType || tx.type)?.replace('TRANSFER_', '')}
          </span>
        </div>
      </td>
      <td className="px-6 py-5">
        <span className="text-sm font-bold text-app-text uppercase tracking-tight">{tx.description}</span>
      </td>
      <td className="px-6 py-5">
        <span className="text-xs font-semibold text-muted-text">{formatDateTime(tx.initiatedAt || tx.createdAt)}</span>
      </td>
      <td className="px-6 py-5">
        <div className="flex flex-col">
          <span className={`text-sm font-black ${isIncoming ? 'text-success-500' : 'text-danger-500'}`}>
            {isIncoming ? '+' : '-'}{formatCurrency(tx.amount)}
          </span>
          <span className="text-[10px] font-bold text-muted-text">{tx.fee > 0 ? `Fee: ${formatCurrency(tx.fee)}` : 'FREE'}</span>
        </div>
      </td>
      <td className="px-6 py-5">
        <Badge variant={(tx.transactionStatus || tx.status) === 'SUCCESS' ? 'success' : (tx.transactionStatus || tx.status) === 'PENDING' ? 'warning' : 'danger'}>
          {tx.transactionStatus || tx.status}
        </Badge>
      </td>
      <td className="px-6 py-5 text-right">
        <button 
          onClick={() => onSelect(tx)}
          className="p-2 rounded-lg bg-surface-2 text-muted-text hover:text-primary-500 transition-colors"
        >
          <Eye size={18} />
        </button>
      </td>
    </tr>
  );
});

TransactionRow.displayName = 'TransactionRow';

const TransactionsPage = () => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { transactions, loading } = useSelector((state) => state.transaction);
  const { accounts } = useSelector((state) => state.account);

  const [filters, setFilters] = useState({
    search: '',
    accountId: '',
    type: '',
    status: '',
    fromDate: '',
    toDate: ''
  });

  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalPages: 0, // Placeholder
    totalItems: 0
  });

  const [selectedTx, setSelectedTx] = useState(null);
  const [downloading, setDownloading] = useState(false);
  const [spinModal, setSpinModal] = useState({ isOpen: false, format: 'csv' });
  const debouncedFilters = useDebounce(filters, 500);

  useEffect(() => {
    dispatch(fetchAccounts());
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchTransactions({
      ...debouncedFilters,
      page: pagination.page,
      size: pagination.size
    }));
  }, [dispatch, pagination.page, pagination.size, debouncedFilters]);

  const handleApplyFilters = useCallback(() => {
    setPagination(p => ({ ...p, page: 0 }));
    dispatch(fetchTransactions({
      ...filters,
      page: 0,
      size: pagination.size
    }));
  }, [dispatch, filters, pagination.size]);

  const handleClearFilters = useCallback(() => {
    const clearedFilters = { search: '', accountId: '', type: '', status: '', fromDate: '', toDate: '' };
    setFilters(clearedFilters);
    setPagination(p => ({ ...p, page: 0 }));
    dispatch(fetchTransactions({
      ...clearedFilters,
      page: 0,
      size: pagination.size
    }));
  }, [dispatch, pagination.size]);

  const handleDownloadStatement = useCallback(async (format = 'csv') => {
    if (!filters.accountId) {
      toast.error('Please select an account first');
      return;
    }
    setSpinModal({ isOpen: true, format });
  }, [filters.accountId, toast]);

  const onAuthorizedDownload = async () => {
    const format = spinModal.format;
    try {
      setDownloading(true);
      
      const to = filters.toDate ? `${filters.toDate}T23:59:59` : new Date().toISOString().split('.')[0];
      const from = filters.fromDate ? `${filters.fromDate}T00:00:00` : new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('.')[0];

      const rawParams = {
        accountId: filters.accountId,
        fromDate: from,
        toDate: to,
        search: filters.search || '',
        type: filters.type || '',
        status: filters.status || '',
        format
      };

      const response = await axiosInstance.get(TRANSACTIONS.STATEMENT, {
        params: rawParams,
        responseType: 'blob'
      });
      
      let mimeType = 'text/csv';
      if (format === 'xlsx') {
        mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      } else if (format === 'pdf') {
        mimeType = 'application/pdf';
      }
      
      const url = window.URL.createObjectURL(new Blob([response], { type: mimeType }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `statement-${filters.accountId}-${Date.now()}.${format}`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success(`${format.toUpperCase()} statement downloaded successfully`);
    } catch (err) {
      console.error('Download error:', err);
      toast.error(err.message || 'Failed to download statement');
    } finally {
      setDownloading(false);
    }
  };

  const handleSelectTx = useCallback((tx) => setSelectedTx(tx), []);

  const handleDownloadExcel = useCallback(() => handleDownloadStatement('xlsx'), [handleDownloadStatement]);
  const handleDownloadPdf = useCallback(() => handleDownloadStatement('pdf'), [handleDownloadStatement]);

  return (
    <div className="space-y-6 animate-slide-in">
      {/* Filter Bar */}
      <div className="glass-card sticky top-20 z-30 p-6 space-y-4 shadow-2xl">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Input 
            placeholder="Search reference or description..." 
            leftIcon={<Search size={18} />}
            value={filters.search}
            onChange={(e) => setFilters({...filters, search: e.target.value})}
            onKeyDown={(e) => e.key === 'Enter' && handleApplyFilters()}
          />
          
          <select 
            className="bg-surface-2 border border-border-subtle rounded-xl px-4 py-3 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500/50 transition-all text-app-text outline-none font-bold"
            value={filters.accountId}
            onChange={(e) => setFilters({...filters, accountId: e.target.value})}
          >
            <option value="">All Accounts</option>
            {accounts.map(acc => (
              <option key={acc.id} value={acc.id}>{acc.type} - {acc.accountNumber}</option>
            ))}
          </select>

          <select 
            className="bg-surface-2 border border-border-subtle rounded-xl px-4 py-3 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500/50 transition-all text-app-text outline-none font-bold"
            value={filters.type}
            onChange={(e) => setFilters({...filters, type: e.target.value})}
          >
            <option value="">All Types</option>
            <option value="DEPOSIT">Deposit</option>
            <option value="WITHDRAWAL">Withdrawal</option>
            <option value="TRANSFER_INTERNAL">Internal Transfer</option>
            <option value="TRANSFER_NEFT">NEFT</option>
            <option value="TRANSFER_RTGS">RTGS</option>
            <option value="TRANSFER_IMPS">IMPS</option>
            <option value="TRANSFER_UPI">UPI</option>
          </select>

          <select 
            className="bg-surface-2 border border-border-subtle rounded-xl px-4 py-3 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500/50 transition-all text-app-text outline-none font-bold"
            value={filters.status}
            onChange={(e) => setFilters({...filters, status: e.target.value})}
          >
            <option value="">All Statuses</option>
            <option value="SUCCESS">Success</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-border-subtle">
          <div className="flex items-center gap-4">
             <div className="flex items-center gap-2">
                <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest">From</span>
                <input 
                  type="date" 
                  className="bg-surface-2 border border-border-subtle rounded-lg px-3 py-1.5 text-xs text-app-text outline-none focus:border-primary-500 font-bold"
                  value={filters.fromDate}
                  onChange={(e) => setFilters({...filters, fromDate: e.target.value})}
                />
             </div>
             <div className="flex items-center gap-2">
                <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest">To</span>
                <input 
                  type="date" 
                  className="bg-surface-2 border border-border-subtle rounded-lg px-3 py-1.5 text-xs text-app-text outline-none focus:border-primary-500 font-bold"
                  value={filters.toDate}
                  onChange={(e) => setFilters({...filters, toDate: e.target.value})}
                />
             </div>
          </div>
          
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="sm" onClick={handleClearFilters} leftIcon={<X size={16}/>}>Clear</Button>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={handleDownloadExcel} isLoading={downloading} leftIcon={<FileSpreadsheet size={16}/>}>Download Statement</Button>
              <Button variant="outline" size="sm" onClick={handleDownloadPdf} isLoading={downloading} leftIcon={<FileText size={16}/>}>Save Data as PDF</Button>
            </div>
            <Button size="sm" onClick={handleApplyFilters} leftIcon={<Filter size={16}/>}>Apply</Button>
          </div>
        </div>
      </div>

      {/* Results Table */}
      <div className="glass-card overflow-hidden shadow-2xl">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-xs font-bold text-muted-text uppercase tracking-widest bg-surface-2/50 border-b border-border-subtle">
                <th className="px-6 py-4">Reference</th>
                <th className="px-6 py-4">Type</th>
                <th className="px-6 py-4">Description</th>
                <th className="px-6 py-4">Timestamp</th>
                <th className="px-6 py-4">Amount</th>
                <th className="px-6 py-4 text-right">Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-subtle">
              {loading ? (
                [...Array( pagination.size || 5)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={7} className="px-6 py-4"><Skeleton variant="line" height={40}/></td>
                  </tr>
                ))
              ) : transactions.length > 0 ? (
                transactions.map((tx) => (
                  <TransactionRow key={tx.id} tx={tx} onSelect={handleSelectTx} userAccounts={accounts} />
                ))
              ) : (
                <tr>
                   <td colSpan={7}>
                      <EmptyState 
                        title="No matching transactions" 
                        message="Try adjusting your filters or search terms." 
                      />
                   </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Placeholder */}
        <div className="px-6 py-4 bg-surface-2/50 flex items-center justify-between border-t border-border-subtle">
           <p className="text-xs font-bold text-muted-text uppercase tracking-widest">Page {pagination.page + 1}</p>
           <div className="flex gap-2">
              <Button size="sm" variant="ghost" disabled={pagination.page === 0} onClick={() => setPagination(p => ({...p, page: p.page - 1 }))}><ChevronLeft size={16}/></Button>
              <Button size="sm" variant="ghost" disabled={transactions.length < pagination.size} onClick={() => setPagination(p => ({...p, page: p.page + 1 }))}><ChevronRight size={16}/></Button>
           </div>
        </div>
      </div>

      <Modal
        isOpen={!!selectedTx}
        onClose={() => setSelectedTx(null)}
        title="Ledger Entry Detail"
        size="md"
      >
        {selectedTx && (
          <div className="space-y-8">
            <div className="flex flex-col items-center justify-center p-8 bg-surface-2/50 rounded-[32px] border border-border-subtle">
               {(() => {
                 const type = selectedTx.transactionType || selectedTx.type;
                 const isIncoming = ['DEPOSIT', 'CREDIT'].includes(type) || 
                                   accounts.some(acc => acc.accountNumber === selectedTx.receiverAccountNumber);
                 
                 return (
                   <>
                     <div className={`w-16 h-16 rounded-[24px] flex items-center justify-center mb-4 ${
                       isIncoming ? 'bg-success-500/10 text-success-500' : 'bg-danger-500/10 text-danger-500'
                     }`}>
                       <Landmark size={32} />
                     </div>
                     <h2 className={`text-3xl font-black ${isIncoming ? 'text-success-500' : 'text-danger-500'}`}>
                       {isIncoming ? '+' : '-'}{formatCurrency(selectedTx.amount)}
                     </h2>
                   </>
                 );
               })()}
               <Badge variant={(selectedTx.transactionStatus || selectedTx.status) === 'SUCCESS' ? 'success' : 'warning'} className="mt-2 text-[10px] tracking-[0.2em]">
                 {selectedTx.transactionStatus || selectedTx.status}
               </Badge>
            </div>

            <div className="grid grid-cols-2 gap-y-8 gap-x-12 px-2">
              <div className="space-y-1">
                <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">External ID</p>
                <p className="text-sm font-mono font-bold text-app-text italic">{selectedTx.referenceNumber || selectedTx.reference}</p>
              </div>
              <div className="space-y-1">
                <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Operation</p>
                <p className="text-sm font-bold text-app-text uppercase">{selectedTx.transactionType || selectedTx.type}</p>
              </div>
              <div className="space-y-1">
                <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Timestamp</p>
                <p className="text-sm font-bold text-app-text">{formatDateTime(selectedTx.initiatedAt || selectedTx.createdAt)}</p>
              </div>
              <div className="space-y-1">
                <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Fee Applied</p>
                <p className="text-sm font-bold text-app-text">{formatCurrency(selectedTx.fee)}</p>
              </div>
            </div>

            <div className="p-6 bg-surface-2 rounded-2xl border border-border-subtle space-y-4">
               <div>
                  <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1 font-mono">Description</p>
                  <p className="text-sm font-black text-app-text uppercase tracking-tight">{selectedTx.description}</p>
               </div>
               {selectedTx.remarks && (
                 <div>
                    <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1 font-mono">Remarks</p>
                    <p className="text-sm font-medium text-muted-text italic opacity-80">"{selectedTx.remarks}"</p>
                 </div>
               )}
            </div>
          </div>
        )}
      </Modal>

      <SpinModal 
        isOpen={spinModal.isOpen}
        onClose={() => setSpinModal({ ...spinModal, isOpen: false })}
        onSuccess={onAuthorizedDownload}
        mode="verify"
        title="Verify Identity"
        description="Enter your 6-digit sPin to authorize ledger download"
      />
    </div>
  );
};

export default React.memo(TransactionsPage);
