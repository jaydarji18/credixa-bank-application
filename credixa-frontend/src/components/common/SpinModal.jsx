import React, { useState, useRef, useEffect } from 'react';
import { Shield, Lock, ArrowRight, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import Modal from './Modal';
import Button from './Button';
import useToast from '../../hooks/useToast';
import axiosInstance from '../../api/axios';
import { USERS } from '../../api/endpoints';
import { useDispatch } from 'react-redux';
import { setUser } from '../../store/slices/authSlice';

const SpinModal = ({ 
  isOpen, 
  onClose, 
  onSuccess, 
  mode = 'verify', // 'set' or 'verify'
  title,
  description,
  mandatory = false
}) => {
  const [pin, setPin] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const inputRefs = [useRef(), useRef(), useRef(), useRef(), useRef(), useRef()];
  const toast = useToast();
  const dispatch = useDispatch();

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRefs[0].current?.focus(), 100);
      setPin(['', '', '', '', '', '']);
    }
  }, [isOpen]);

  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;
    
    const newPin = [...pin];
    newPin[index] = value.slice(-1);
    setPin(newPin);

    if (value && index < 5) {
      inputRefs[index + 1].current.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !pin[index] && index > 0) {
      inputRefs[index - 1].current.focus();
    }
  };

  const handleSubmit = async () => {
    const fullPin = pin.join('');
    if (fullPin.length !== 6) {
      toast.error('Please enter a 6-digit PIN');
      return;
    }

    setLoading(true);
    try {
      const endpoint = mode === 'set' ? USERS.SET_SPIN : USERS.VERIFY_SPIN;
      await axiosInstance.post(endpoint, { spin: fullPin });
      
      if (mode === 'set') {
        toast.success('Secret PIN set successfully');
        // Update user profile in state to reflect spinSet: true
        const userResponse = await axiosInstance.get(USERS.ME);
        dispatch(setUser(userResponse));
      } else {
        toast.success('PIN Verified');
      }
      
      onSuccess?.(fullPin);
      onClose();
    } catch (err) {
      toast.error(err.message || 'Invalid PIN');
      setPin(['', '', '', '', '', '']);
      inputRefs[0].current.focus();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal 
      isOpen={isOpen} 
      onClose={mandatory ? () => {} : onClose}
      title={title || (mode === 'set' ? 'Set Secret PIN' : 'Verify Identity')}
      size="sm"
    >
      <div className="space-y-8 py-4">
        <div className="flex flex-col items-center text-center space-y-4">
          <div className="w-16 h-16 rounded-[24px] bg-primary-500/10 flex items-center justify-center text-primary-500 border border-primary-500/10">
            {mode === 'set' ? <Shield size={32} /> : <Lock size={32} />}
          </div>
          <div>
            <p className="text-sm font-bold text-app-text tracking-tight mb-2 uppercase">
              {description || (mode === 'set' ? 'Create a 6-digit PIN for secure transactions' : 'Enter your 6-digit sPin to proceed')}
            </p>
            {mandatory && (
              <p className="text-[10px] font-black text-danger-500 uppercase tracking-widest bg-danger-500/5 px-3 py-1 rounded-full border border-danger-500/10">
                Action Required: Security Protocol
              </p>
            )}
          </div>
        </div>

        <div className="flex justify-center gap-2">
          {pin.map((digit, i) => (
            <input
              key={i}
              ref={inputRefs[i]}
              type="password"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(i, e.target.value)}
              onKeyDown={(e) => handleKeyDown(i, e)}
              className="w-12 h-16 bg-surface-2 border border-border-subtle rounded-2xl text-center text-2xl font-black text-app-text outline-none focus:ring-2 ring-primary-500/20 focus:border-primary-500/50 transition-all"
            />
          ))}
        </div>

        <Button 
          className="w-full h-14" 
          onClick={handleSubmit} 
          isLoading={loading}
          rightIcon={<ArrowRight size={18} />}
        >
          {mode === 'set' ? 'Confirm & Initialize' : 'Verify & Proceed'}
        </Button>

        {mode === 'set' && (
          <p className="text-[10px] font-bold text-dark-400 text-center uppercase tracking-widest leading-relaxed">
            Memorize this PIN carefully. You will need it to authorize transactions and sensitive operations.
          </p>
        )}
      </div>
    </Modal>
  );
};

export default SpinModal;
