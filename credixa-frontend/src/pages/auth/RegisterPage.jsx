import React, { useState, useEffect } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { 
  User, 
  Mail, 
  Phone, 
  Lock, 
  Calendar, 
  CreditCard, 
  ChevronRight,
  ShieldCheck,
  Check,
  X,
  MapPin,
  Home,
  Hash
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { registerSchema } from '../../utils/validators';
import { registerUser } from '../../store/thunks/authThunks';
import useToast from '../../hooks/useToast';

const RegisterPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const toast = useToast();
  const [apiError, setApiError] = useState(null);
  
  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(registerSchema),
  });

  const password = useWatch({ control, name: 'password', defaultValue: '' });
  const [strength, setStrength] = useState(0);

  useEffect(() => {
    let score = 0;
    if (password.length > 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    setStrength(score);
  }, [password]);

  const onSubmit = async (data) => {
    try {
      setApiError(null);

      // Map frontend fields → backend RegisterRequestDTO
      // Backend expects: firstName, lastName, email, phone,
      //                  password, dateOfBirth (YYYY-MM-DD), panNumber,
      //                  aadhaarNumber, address, city, state, pincode
      // confirmPassword is FE-only and must NOT be sent
      const { confirmPassword, ...rest } = data;

      const submissionData = {
        ...rest,
      };

      await dispatch(registerUser(submissionData)).unwrap();

      toast.success('Registration successful! Please verify your email.');
      navigate('/verify-otp', {
        state: {
          email: data.email,
          purpose: 'REGISTRATION',
        },
      });
    } catch (err) {
      setApiError(err.message || 'Registration failed. Please try again.');
    }
  };

  const handlePanChange = (e) => {
    const value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    setValue('panNumber', value, { shouldValidate: true });
  };

  const handleAadhaarChange = (e) => {
    // Remove all spaces and non-digits
    const value = e.target.value.replace(/\s/g, '').replace(/[^0-9]/g, '');
    setValue('aadhaarNumber', value, { shouldValidate: true });
  };

  return (
    <AuthLayout 
      title="Create Account" 
      subtitle="Join the elite banking experience today"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 max-h-[500px] overflow-y-auto px-1 custom-scrollbar pb-4">
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="First Name"
            placeholder="John"
            leftIcon={<User size={16} />}
            error={errors.firstName}
            {...register('firstName')}
          />
          <Input
            label="Last Name"
            placeholder="Doe"
            leftIcon={<User size={16} />}
            error={errors.lastName}
            {...register('lastName')}
          />
        </div>

        <Input
          label="Email Address"
          type="email"
          placeholder="john.doe@example.com"
          leftIcon={<Mail size={16} />}
          error={errors.email}
          {...register('email')}
        />

        <Input
          label="Phone Number"
          placeholder="9876543210"
          type="tel"
          leftIcon={<span className="text-xs font-bold text-muted-text">+91</span>}
          error={errors.phone}
          {...register('phone')}
        />

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Date of Birth"
            type="date"
            leftIcon={<Calendar size={16} />}
            error={errors.dateOfBirth}
            {...register('dateOfBirth')}
          />
          <Input
            label="PAN Number"
            placeholder="ABCDE1234F"
            leftIcon={<CreditCard size={16} />}
            error={errors.panNumber}
            {...register('panNumber', {
              onChange: handlePanChange
            })}
          />
        </div>

        <Input
          label="Aadhaar Number"
          placeholder="5555 6666 7777"
          leftIcon={<ShieldCheck size={16} />}
          error={errors.aadhaarNumber}
          {...register('aadhaarNumber', {
            onChange: handleAadhaarChange
          })}
        />

        <Input
          label="Residential Address"
          placeholder="Building 404, Main Rd, MG Road"
          leftIcon={<Home size={16} />}
          error={errors.address}
          {...register('address')}
        />

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="City"
            placeholder="Mumbai"
            leftIcon={<MapPin size={16} />}
            error={errors.city}
            {...register('city')}
          />
          <Input
            label="State"
            placeholder="Maharashtra"
            leftIcon={<MapPin size={16} />}
            error={errors.state}
            {...register('state')}
          />
        </div>

        <Input
          label="Pincode"
          placeholder="400001"
          leftIcon={<Hash size={16} />}
          error={errors.pincode}
          {...register('pincode')}
        />

        <div className="space-y-4">
          <Input
            label="Password"
            type="password"
            placeholder="••••••••"
            leftIcon={<Lock size={16} />}
            error={errors.password}
            {...register('password')}
          />
          
          {/* Strength Meter */}
          <div className="space-y-1.5 px-1">
            <div className="flex justify-between items-center">
              <span className="text-[10px] font-bold text-muted-text uppercase tracking-widest leading-none">Security Level</span>
              <span className={`text-[10px] font-black uppercase tracking-widest leading-none ${
                strength === 0 ? 'text-muted-text' :
                strength <= 2 ? 'text-warning-500' : 'text-success-500'
              }`}>
                {strength === 0 ? 'None' : strength <= 2 ? 'Medium' : 'Strong'}
              </span>
            </div>
            <div className="h-1.5 w-full bg-surface-2 border border-border-subtle rounded-full flex gap-1 overflow-hidden">
              {[1, 2, 3, 4].map((step) => (
                <div 
                  key={step}
                  className={`flex-1 transition-all duration-300 rounded-full ${
                    strength >= step 
                      ? (strength <= 2 ? 'bg-warning-500 shadow-[0_0_8px_rgba(245,158,11,0.3)]' : 'bg-success-500 shadow-[0_0_8px_rgba(16,185,129,0.3)]') 
                      : 'bg-transparent'
                  }`}
                />
              ))}
            </div>
          </div>

          <Input
            label="Confirm Password"
            type="password"
            placeholder="••••••••"
            leftIcon={<Lock size={16} />}
            error={errors.confirmPassword}
            {...register('confirmPassword')}
          />
        </div>

        {apiError && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="p-4 bg-danger-500/10 border border-danger-500/20 rounded-xl flex items-start gap-3"
          >
            <X size={18} className="text-danger-500 shrink-0 mt-0.5" />
            <p className="text-danger-500 text-xs font-semibold leading-relaxed">
              {apiError}
            </p>
          </motion.div>
        )}

        <div className="pt-2">
          <Button
            type="submit"
            className="w-full"
            size="lg"
            isLoading={isSubmitting}
            rightIcon={<ChevronRight size={18} />}
          >
            Create My Account
          </Button>
        </div>

        <div className="text-center pt-2 pb-2">
          <p className="text-muted-text text-sm font-medium">
            Already have an account?{' '}
            <Link 
              to="/login" 
              className="text-primary-500 font-bold hover:underline decoration-2 underline-offset-4"
            >
              Sign In
            </Link>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};

export default RegisterPage;
