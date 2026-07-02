import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Eye, EyeOff, Mail, Lock, LogIn } from 'lucide-react';
import { motion } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { loginSchema } from '../../utils/validators';
import { loginUser } from '../../store/thunks/authThunks';
import useToast from '../../hooks/useToast';

const LoginPage = () => {
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
      const result = await dispatch(loginUser(data)).unwrap();
      
      if (result.twoFaRequired) {
        if (result.message === 'PHONE_VERIFICATION_REQUIRED') {
          toast.info('Phone verification required');
          navigate('/verify-otp', { 
            state: { 
              email: data.email, 
              sessionToken: result.sessionToken, 
              purpose: 'PHONE_VERIFICATION' 
            } 
          });
        } else {
          toast.info('Two-factor authentication required');
          navigate('/verify-otp', { 
            state: { 
              email: data.email, 
              sessionToken: result.sessionToken, 
              purpose: '2FA' 
            } 
          });
        }
      } else {
        toast.success('Login successful!');
        const from = location.state?.from || '/dashboard';
        navigate(from, { replace: true });
      }
    } catch (err) {
      setApiError(err.message || 'Invalid credentials');
    }
  };

  return (
    <AuthLayout 
      title="Welcome Back" 
      subtitle="Sign in to your account to continue"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Input
          label="Email Address"
          id="email"
          type="email"
          placeholder="name@example.com"
          autoComplete="email"
          leftIcon={<Mail size={18} />}
          error={errors.email}
          {...register('email')}
        />

        <div className="relative">
          <Input
            label="Password"
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
          <div className="flex justify-end mt-2">
            <Link 
              to="/forgot-password" 
              className="text-xs font-bold text-primary-500 hover:text-primary-400 transition-colors uppercase tracking-wider"
            >
              Forgot Password?
            </Link>
          </div>
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
          className="w-full"
          size="lg"
          isLoading={isSubmitting}
          rightIcon={<LogIn size={18} />}
        >
          Sign In
        </Button>

        <div className="text-center pt-4">
          <p className="text-muted-text text-sm font-medium">
            New here?{' '}
            <Link 
              to="/register" 
              className="text-primary-500 font-bold hover:underline decoration-2 underline-offset-4 transition-all"
            >
              Create an account
            </Link>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};

export default LoginPage;
