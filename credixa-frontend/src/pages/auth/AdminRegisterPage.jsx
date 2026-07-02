import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { Plus, User, Mail, Lock, ShieldAlert } from 'lucide-react';
import { motion } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import axiosInstance from '../../api/axios';
import { ADMIN_AUTH } from '../../api/endpoints';
import useToast from '../../hooks/useToast';
import { z } from 'zod';

const adminRegisterSchema = z.object({
  firstName: z.string().min(2, 'Min 2 characters'),
  lastName: z.string().min(2, 'Min 2 characters'),
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Min 8 characters'),
  role: z.enum(['SUPER_ADMIN', 'BANK_OPERATOR'], {
    errorMap: () => ({ message: 'Please select a valid role' })
  }),
});

const AdminRegisterPage = () => {
  const navigate = useNavigate();
  const toast = useToast();
  const [showPassword, setShowPassword] = useState(false);
  const [apiError, setApiError] = useState(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(adminRegisterSchema),
  });

  const onSubmit = async (data) => {
    try {
      setApiError(null);
      // NOTE: /admin/auth/register may not exist yet on backend, 
      // Ensure backend handles this if you want self-service admin registration
      // Usually, admin registration is blocked or protected.
      await axiosInstance.post('/admin/auth/register', data);
      toast.success('Admin registration request submitted!');
      navigate('/admin/login');
    } catch (err) {
      setApiError(err.message || 'Registration failed');
    }
  };

  return (
    <AuthLayout 
      title="Admin Application" 
      subtitle="Register for an administrative account"
    >
      <div className="p-4 mb-6 bg-warning-500/10 border border-warning-500/20 rounded-xl flex items-center justify-center gap-2 text-warning-500 font-bold text-sm">
        <ShieldAlert size={18} />
        <span>Applications require Super Admin approval</span>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <Input label="First Name" id="firstName" placeholder="John" leftIcon={<User size={18} />} error={errors.firstName} {...register('firstName')} />
          <Input label="Last Name" id="lastName" placeholder="Doe" leftIcon={<User size={18} />} error={errors.lastName} {...register('lastName')} />
        </div>

        <Input label="Admin Email Address" id="email" type="email" placeholder="admin@credixa.in" leftIcon={<Mail size={18} />} error={errors.email} {...register('email')} />

        <div className="space-y-1.5">
          <label className="text-xs font-bold text-dark-400 uppercase tracking-widest ml-1">Assigned Role</label>
          <select 
            className={`w-full bg-dark-950 border ${errors.role ? 'border-danger-500' : 'border-white/5'} rounded-xl px-4 py-3 text-sm text-white font-bold outline-none focus:ring-2 ring-primary-500/20`}
            {...register('role')}
          >
            <option value="">Select Role</option>
            <option value="SUPER_ADMIN">System Administrator</option>
            <option value="BANK_OPERATOR">Bank Operator</option>
          </select>
          {errors.role && <p className="text-[10px] text-danger-500 font-bold ml-1">{errors.role.message}</p>}
        </div>

        <div className="relative">
          <Input
            label="Security Password"
            id="password"
            type={showPassword ? 'text' : 'password'}
            placeholder="••••••••"
            leftIcon={<Lock size={18} />}
            error={errors.password}
            {...register('password')}
          />
        </div>

        {apiError && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} className="p-4 bg-danger-500/10 border border-danger-500/20 rounded-xl text-danger-500 text-sm font-semibold">
            {apiError}
          </motion.div>
        )}

        <Button type="submit" className="w-full h-12" size="lg" isLoading={isSubmitting} rightIcon={<Plus size={18} />}>
          Submit Application
        </Button>
        <button type="button" onClick={() => navigate('/admin/login')} className="w-full text-center text-xs text-dark-300 hover:text-white pt-2">
           Already have access? Login here
        </button>
      </form>
    </AuthLayout>
  );
};

export default AdminRegisterPage;
