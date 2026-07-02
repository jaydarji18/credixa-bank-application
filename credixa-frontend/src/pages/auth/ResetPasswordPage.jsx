import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Lock, CheckCircle, ChevronRight, AlertTriangle } from 'lucide-react';
import { motion } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { resetPasswordSchema } from '../../utils/validators';
import axiosInstance from '../../api/axios';
import { AUTH } from '../../api/endpoints';
import useToast from '../../hooks/useToast';

const ResetPasswordPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const { email, otp } = location.state || {};
  const [isSuccess, setIsSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { email }
  });

  const onSubmit = async (data) => {
    if (!email || !otp) {
      toast.error('Missing verification data. Please restart the process.');
      navigate('/forgot-password');
      return;
    }

    try {
      await axiosInstance.post(AUTH.RESET_PASSWORD, {
        email,
        otp,
        newPassword: data.newPassword
      });
      
      setIsSuccess(true);
      toast.success('Password reset successfully!');
      
      // Auto-redirect after 3 seconds
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err) {
      toast.error(err.message || 'Failed to reset password');
    }
  };

  if (!email || !otp) {
    return (
      <AuthLayout title="Security Error" subtitle="Verification session expired">
        <div className="text-center py-8 space-y-6">
          <div className="w-16 h-16 bg-danger-500/10 rounded-full flex items-center justify-center mx-auto">
            <AlertTriangle className="text-danger-500" size={32} />
          </div>
          <p className="text-dark-300 text-sm font-medium">
            We couldn't find a valid verification session. Please request a new recovery code.
          </p>
          <Button onClick={() => navigate('/forgot-password')} className="w-full">
            Restart Recovery
          </Button>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout 
      title="Create New Password" 
      subtitle="Ensure your new password is secure and unique"
    >
      {!isSuccess ? (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 pt-4">
          <div className="p-4 bg-primary-500/5 border border-primary-500/10 rounded-xl flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary-600/10 flex items-center justify-center shrink-0">
               <span className="text-xs font-black text-primary-500">ID</span>
            </div>
            <div className="flex flex-col">
              <span className="text-[10px] font-bold text-dark-400 uppercase tracking-widest leading-none mb-1">Account Verified</span>
              <span className="text-sm font-bold text-white leading-none">{email}</span>
            </div>
          </div>

          <Input
            label="New Password"
            type="password"
            placeholder="••••••••"
            leftIcon={<Lock size={18} />}
            error={errors.newPassword}
            {...register('newPassword')}
          />

          <Input
            label="Confirm New Password"
            type="password"
            placeholder="••••••••"
            leftIcon={<Lock size={18} />}
            error={errors.confirmNewPassword}
            {...register('confirmNewPassword')}
          />

          <Button
            type="submit"
            className="w-full"
            size="lg"
            isLoading={isSubmitting}
            rightIcon={<ChevronRight size={18} />}
          >
            Update Password
          </Button>
        </form>
      ) : (
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center space-y-8 py-8"
        >
          <div className="w-24 h-24 bg-success-500/10 border border-success-500/20 rounded-[40px] flex items-center justify-center mx-auto shadow-2xl shadow-success-900/10">
            <CheckCircle size={48} className="text-success-500" />
          </div>
          
          <div className="space-y-3">
            <h3 className="text-2xl font-black text-white tracking-tight">Access Restored</h3>
            <p className="text-dark-300 text-sm font-medium leading-relaxed max-w-[280px] mx-auto">
              Your password has been successfully updated. Redirecting to sign in momentarily...
            </p>
          </div>

          <Link to="/login" className="block">
            <Button variant="secondary" className="w-full">
              Sign In Now
            </Button>
          </Link>
        </motion.div>
      )}
    </AuthLayout>
  );
};

export default ResetPasswordPage;
