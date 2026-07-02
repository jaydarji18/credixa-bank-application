import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import Button from './common/Button';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-dark-950 flex items-center justify-center p-6">
          <div className="glass-card max-w-md w-full p-10 text-center space-y-6 border-danger-500/20 shadow-2xl shadow-danger-500/10">
            <div className="w-20 h-20 bg-danger-500/10 rounded-[32px] flex items-center justify-center mx-auto text-danger-500 mb-2">
               <AlertTriangle size={40} />
            </div>
            
            <div className="space-y-2">
               <h2 className="text-2xl font-black text-white uppercase tracking-tight">System Violation</h2>
               <p className="text-dark-400 text-sm font-medium leading-relaxed">
                 An unexpected runtime error has occurred. Our security protocols have intercepted the failure to protect your session.
               </p>
            </div>

            <div className="p-4 bg-dark-900 rounded-2xl border border-white/5 text-left">
               <p className="text-[10px] font-black text-dark-500 uppercase tracking-widest mb-1">Error Signature</p>
               <p className="text-xs font-mono text-danger-400 truncate">{this.state.error?.message || 'Unknown exception'}</p>
            </div>

            <Button 
               className="w-full h-12" 
               variant="danger" 
               leftIcon={<RefreshCw size={18} />}
               onClick={() => window.location.reload()}
            >
              Initialize Reboot
            </Button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
