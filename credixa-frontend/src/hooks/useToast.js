import { useCallback, useMemo } from 'react';
import { toast as hotToast } from 'react-hot-toast';

export const useToast = () => {
  const success = useCallback((message) => {
    hotToast.success(message, {
      style: {
        background: '#1a2235',
        color: '#fff',
        border: '1px solid rgba(16, 185, 129, 0.2)',
        borderRadius: '12px',
        padding: '12px 20px',
        fontSize: '14px',
        fontWeight: '500',
      },
      iconTheme: {
        primary: '#10b981',
        secondary: '#fff',
      },
    });
  }, []);

  const error = useCallback((message) => {
    hotToast.error(message, {
      style: {
        background: '#1a2235',
        color: '#fff',
        border: '1px solid rgba(239, 68, 68, 0.2)',
        borderRadius: '12px',
        padding: '12px 20px',
        fontSize: '14px',
        fontWeight: '500',
      },
      iconTheme: {
        primary: '#ef4444',
        secondary: '#fff',
      },
    });
  }, []);

  const loading = useCallback((message) => {
    return hotToast.loading(message, {
      style: {
        background: '#1a2235',
        color: '#fff',
        border: '1px solid rgba(59, 130, 246, 0.2)',
        borderRadius: '12px',
        padding: '12px 20px',
        fontSize: '14px',
        fontWeight: '500',
      },
    });
  }, []);

  const info = useCallback((message) => {
    hotToast(message, {
      icon: 'ℹ️',
      style: {
        background: '#1a2235',
        color: '#fff',
        border: '1px solid rgba(59, 130, 246, 0.2)',
        borderRadius: '12px',
        padding: '12px 20px',
        fontSize: '14px',
        fontWeight: '500',
      },
    });
  }, []);

  const dismiss = useCallback((toastId) => {
    hotToast.dismiss(toastId);
  }, []);

  return useMemo(() => ({
    success,
    error,
    loading,
    info,
    dismiss,
    toast: hotToast
  }), [success, error, loading, info, dismiss]);
};

export default useToast;
