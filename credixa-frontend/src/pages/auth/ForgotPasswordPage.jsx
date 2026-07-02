import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, ChevronRight, CheckCircle, ArrowLeft } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { forgotPasswordSchema } from '../../utils/validators';
import axiosInstance from '../../api/axios';
import { AUTH } from '../../api/endpoints';
import useToast from '../../hooks/useToast';

const ForgotPasswordPage = () => {
  const navigate = useNavigate();
  const toast = useToast();
  const [isSent, setIsSent] = useState(false);
  const [email, setEmail] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(forgotPasswordSchema),
  });

  const onSubmit = async (data) => {
    try {
      await axiosInstance.post(AUTH.FORGOT_PASSWORD, data);
      setEmail(data.email);
      setIsSent(true);
      toast.success('Reset code sent to your email');
    } catch (err) {
      toast.error(err.message || 'Failed to send reset code');
    }
  };

  return (
    <AuthLayout 
      title="Access Recovery" 
      subtitle={isSent ? "Verification code sent" : "Enter your email to receive a recovery code"}
    >
      <AnimatePresence mode="wait">
        {!isSent ? (
          <motion.form
            key="form"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-6"
          >
            <Input
              label="Recovery Email"
              type="email"
              placeholder="name@example.com"
              leftIcon={<Mail size={18} />}
              error={errors.email}
              {...register('email')}
            />

            <Button
              type="submit"
              className="w-full"
              size="lg"
              isLoading={isSubmitting}
              rightIcon={<ChevronRight size={18} />}
            >
              Send Recovery Code
            </Button>

            <Link
              to="/login"
              className="flex items-center justify-center gap-2 text-dark-400 hover:text-white transition-all text-sm font-semibold pt-4"
            >
              <ArrowLeft size={16} />
              Return to Sign In
            </Link>
          </motion.form>
        ) : (
          <motion.div
            key="success"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="text-center space-y-8 py-4"
          >
            <div className="w-20 h-20 bg-success-500/10 border border-success-500/20 rounded-3xl flex items-center justify-center mx-auto shadow-2xl shadow-success-900/10">
              <CheckCircle size={40} className="text-success-500" />
            </div>
            
            <div className="space-y-3">
              <h3 className="text-xl font-bold text-white">Check Your Inbox</h3>
              <p className="text-dark-300 text-sm leading-relaxed max-w-[280px] mx-auto">
                We've sent a 6-digit security code to <br/>
                <span className="text-white font-bold">{email}</span>
              </p>
            </div>

            <Button
              onClick={() => navigate('/verify-otp', { state: { email, purpose: 'PASSWORD_RESET' } })}
              className="w-full"
              size="lg"
              rightIcon={<ChevronRight size={18} />}
            >
              Enter Security Code
            </Button>
            
            <button
              onClick={() => setIsSent(false)}
              className="text-primary-500 text-xs font-bold uppercase tracking-widest hover:text-primary-400"
            >
              Try another email
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </AuthLayout>
  );
};

export default ForgotPasswordPage;
