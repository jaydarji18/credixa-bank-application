import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { ShieldAlert, RefreshCcw, CheckCircle2, ChevronLeft } from 'lucide-react';
import { motion } from 'framer-motion';

import AuthLayout from '../../components/layout/AuthLayout';
import Button from '../../components/common/Button';
import axiosInstance from '../../api/axios';
import { AUTH } from '../../api/endpoints';
import { setCredentials } from '../../store/slices/authSlice';
import useToast from '../../hooks/useToast';

const OtpVerifyPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const toast = useToast();
  
  const { email, sessionToken, purpose } = location.state || {};

  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [timer, setTimer] = useState(60);
  const [canResend, setCanResend] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const inputRefs = [useRef(), useRef(), useRef(), useRef(), useRef(), useRef()];

  useEffect(() => {
    if (!email) {
      navigate('/login');
      return;
    }

    if (timer > 0) {
      const interval = setInterval(() => setTimer(t => t - 1), 1000);
      return () => clearInterval(interval);
    } else {
      setCanResend(true);
    }
  }, [timer, email, navigate]);

  const handleChange = (index, value) => {
    if (isNaN(value)) return;
    
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    // Auto-advance
    if (value && index < 5) {
      inputRefs[index + 1].current.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs[index - 1].current.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').slice(0, 6).split('');
    const newOtp = [...otp];
    pastedData.forEach((char, i) => {
      if (!isNaN(char)) newOtp[i] = char;
    });
    setOtp(newOtp);
    const lastIndex = Math.min(pastedData.length, 5);
    inputRefs[lastIndex].current.focus();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const otpString = otp.join('');
    if (otpString.length < 6) {
      setError('Please enter all 6 digits');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      let response;
      if (purpose === 'REGISTRATION') {
        response = await axiosInstance.post(AUTH.VERIFY_OTP, { 
          email, 
          otp: otpString,
          channel: 'EMAIL'
        });
        toast.success('Email verified successfully! You can now login.');
        navigate('/login');
      } else if (purpose === '2FA') {
        const data = await axiosInstance.post(AUTH.LOGIN, { email, otp: otpString, sessionToken });
        dispatch(setCredentials(data));
        toast.success('Identity verified!');
        navigate('/dashboard');
      } else if (purpose === 'PASSWORD_RESET') {
        // Just verify and go to reset page
        navigate('/reset-password', { state: { email, otp: otpString } });
      } else if (purpose === 'PHONE_VERIFICATION') {
        response = await axiosInstance.post(AUTH.VERIFY_OTP, { 
          email, 
          otp: otpString,
          channel: 'SMS'
        });
        toast.success('Phone verified successfully!');
        dispatch(setCredentials(response.data));
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.message || 'Invalid OTP. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    try {
      setCanResend(false);
      setTimer(60);
      setTimer(60);
      await axiosInstance.post(AUTH.RESEND_OTP, { 
        email,
        channel: purpose === 'PHONE_VERIFICATION' ? 'SMS' : 'EMAIL'
      });
      toast.success(`New OTP sent to your ${purpose === 'PHONE_VERIFICATION' ? 'phone' : 'email'}`);
    } catch (err) {
      toast.error(err.message || 'Failed to resend OTP');
      setCanResend(true);
      setTimer(0);
    }
  };

  const getHeader = () => {
    switch (purpose) {
      case 'REGISTRATION': return { title: 'Verify Email', sub: `Enter the code sent to ${email}` };
      case '2FA': return { title: 'Two-Factor Auth', sub: 'Confirm your identity with the security code' };
      case 'PASSWORD_RESET': return { title: 'Reset Password', sub: 'Verify your email to continue' };
      case 'PHONE_VERIFICATION': return { title: 'Verify Phone', sub: 'Enter the code sent to your mobile' };
      default: return { title: 'Verification', sub: 'Security code required' };
    }
  };

  const { title, sub } = getHeader();

  return (
    <AuthLayout title={title} subtitle={sub}>
      <form onSubmit={handleSubmit} className="space-y-8">
        <div className="flex justify-between gap-2 sm:gap-4 pt-4">
          {otp.map((digit, i) => (
            <input
              key={i}
              ref={inputRefs[i]}
              type="text"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(i, e.target.value)}
              onKeyDown={(e) => handleKeyDown(i, e)}
              onPaste={handlePaste}
              className={`
                w-full aspect-square text-center text-2xl font-black rounded-2xl
                bg-dark-950 border border-white/5 focus:border-primary-500
                focus:ring-2 focus:ring-primary-500/20 transition-all duration-300
                text-white outline-none
                ${error ? 'border-danger-500/50' : ''}
              `}
            />
          ))}
        </div>

        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex items-center gap-2 text-danger-500 bg-danger-500/10 p-4 rounded-xl border border-danger-500/20"
          >
            <ShieldAlert size={18} />
            <span className="text-xs font-semibold">{error}</span>
          </motion.div>
        )}

        <div className="space-y-4">
          <Button
            type="submit"
            className="w-full"
            size="lg"
            isLoading={isLoading}
            disabled={otp.some(d => !d)}
            rightIcon={<CheckCircle2 size={18} />}
          >
            Verify & Continue
          </Button>

          <div className="flex flex-col items-center gap-4">
            <p className="text-dark-300 text-sm font-medium">
              Didn't receive the code?
            </p>
            <button
              type="button"
              onClick={handleResend}
              disabled={!canResend}
              className={`
                flex items-center gap-2 px-6 py-2 rounded-full border border-white/5
                transition-all duration-300 font-bold text-xs uppercase tracking-widest
                ${canResend 
                  ? 'text-primary-500 hover:bg-primary-500/10 border-primary-500/20' 
                  : 'text-dark-400 cursor-not-allowed'}
              `}
            >
              <RefreshCcw size={14} className={!canResend && timer > 0 ? '' : 'animate-spin-once'} />
              {canResend ? 'Resend Now' : `Resend in ${timer}s`}
            </button>
          </div>
        </div>

        <button
          type="button"
          onClick={() => navigate('/login')}
          className="w-full flex items-center justify-center gap-2 text-dark-400 hover:text-white transition-colors text-sm font-semibold"
        >
          <ChevronLeft size={16} />
          Back to Login
        </button>
      </form>
    </AuthLayout>
  );
};

export default OtpVerifyPage;
