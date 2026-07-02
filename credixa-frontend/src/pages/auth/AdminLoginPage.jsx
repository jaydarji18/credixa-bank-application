import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Eye, EyeOff, Mail, Lock, LogIn, ShieldAlert } from 'lucide-react';
import { motion } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { loginSchema } from '../../utils/validators';
import axiosInstance from '../../api/axios';
import { ADMIN_AUTH } from '../../api/endpoints';
import { setCredentials } from '../../store/slices/authSlice';
import useToast from '../../hooks/useToast';

const AdminLoginPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [showPassword, setShowPassword] = useState(false);
  const [apiError, setApiError] = useState(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data) => {
    try {
      setApiError(null);
      const result = await axiosInstance.post(ADMIN_AUTH.LOGIN, data);
      dispatch(setCredentials(result));
      toast.success('Login successful!');
      
      const userRole = result.user?.role;
      let defaultPath = '/admin';
      if (userRole === 'BANK_OPERATOR' || userRole === 'ROLE_BANK_OPERATOR') {
        defaultPath = '/bank-operator';
      }

      const from = location.state?.from || defaultPath;
      navigate(from, { replace: true });
    } catch (err) {
      setApiError(err.message || 'Invalid admin credentials');
    }
  };

  return (
    <AuthLayout 
      title="Admin Portal" 
      subtitle="Sign in with your administrative credentials"
    >
      <div className="p-4 mb-6 bg-primary-500/10 border border-primary-500/20 rounded-xl flex items-center justify-center gap-2 text-primary-500 font-bold text-sm">
        <ShieldAlert size={18} />
        <span>Restricted Access Panel</span>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Input
          label="Admin Email Address"
          id="email"
          type="email"
          placeholder="admin@credixa.in"
          autoComplete="email"
          leftIcon={<Mail size={18} />}
          error={errors.email}
          {...register('email')}
        />

        <div className="relative">
          <Input
            label="Security Password"
            id="password"
            type={showPassword ? 'text' : 'password'}
            placeholder="••••••••"
            autoComplete="current-password"
            leftIcon={<Lock size={18} />}
            rightIcon={
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="focus:outline-none hover:text-primary-500 transition-colors"
                tabIndex="-1"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            }
            error={errors.password}
            {...register('password')}
          />
        </div>

        {apiError && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="p-4 bg-danger-500/10 border border-danger-500/20 rounded-xl text-danger-500 text-sm font-semibold"
          >
            {apiError}
          </motion.div>
        )}

        <Button
          type="submit"
          className="w-full h-12"
          size="lg"
          isLoading={isSubmitting}
          rightIcon={<LogIn size={18} />}
        >
          Access Portal
        </Button>
      </form>
    </AuthLayout>
  );
};

export default AdminLoginPage;
