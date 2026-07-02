import React, { useState, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  User, 
  Mail, 
  Phone, 
  MapPin, 
  Shield, 
  Camera, 
  Lock, 
  Smartphone,
  CheckCircle2,
  Calendar,
  Hash,
  Loader2,
  Save
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import axiosInstance from '../../api/axios';
import { USERS } from '../../api/endpoints';
import { setUser as updateProfile } from '../../store/slices/authSlice';
import useToast from '../../hooks/useToast';

const personalInfoSchema = z.object({
  firstName: z.string().min(2, 'First name too short'),
  lastName: z.string().min(2, 'Last name too short'),
  phoneNumber: z.string().min(10, 'Invalid phone number'),
  address: z.string().min(5, 'Address too short'),
  city: z.string().min(2, 'City required'),
  state: z.string().min(2, 'State required'),
  pincode: z.string().length(6, 'Pincode must be 6 digits'),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password required'),
  newPassword: z.string().min(8, 'Min 8 characters'),
  confirmNewPassword: z.string()
}).refine((data) => data.newPassword === data.confirmNewPassword, {
  message: "Passwords don't match",
  path: ["confirmNewPassword"],
});

const ProfilePage = () => {
  const dispatch = useDispatch();
  const toast = useToast();
  const { user } = useSelector((state) => state.auth);
  const [activeTab, setActiveTab] = useState('personal');
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);

  const {
    register: regInfo,
    handleSubmit: handleInfoSubmit,
    formState: { errors: infoErrors, isSubmitting: infoSubmitting }
  } = useForm({
    resolver: zodResolver(personalInfoSchema),
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      phoneNumber: user?.phoneNumber || '',
      address: user?.address || '',
      city: user?.city || '',
      state: user?.state || '',
      pincode: user?.pincode || '',
    }
  });

  const {
    register: regPass,
    handleSubmit: handlePassSubmit,
    reset: resetPass,
    formState: { errors: passErrors, isSubmitting: passSubmitting }
  } = useForm({
    resolver: zodResolver(passwordSchema)
  });

  const onUpdateInfo = async (data) => {
    try {
      const response = await axiosInstance.put(USERS.UPDATE_PROFILE, data);
      dispatch(updateProfile(response.data));
      toast.success('Profile updated successfully');
    } catch (err) {
      toast.error(err.message || 'Update failed');
    }
  };

  const onChangePassword = async (data) => {
    try {
      await axiosInstance.post(USERS.CHANGE_PASSWORD, data);
      toast.success('Password changed successfully');
      resetPass();
    } catch (err) {
      toast.error(err.message || 'Password change failed');
    }
  };

  const handlePhotoUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('photo', file);

    try {
      setUploading(true);
      const response = await axiosInstance.post(USERS.UPLOAD_PHOTO, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      dispatch(updateProfile({ ...user, profilePhoto: response.data.photoUrl }));
      toast.success('Profile photo updated');
    } catch (err) {
      toast.error('Failed to upload photo');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-slide-in pb-20">
       <div className="grid lg:grid-cols-12 gap-8">
          
          {/* LEFT: PROFILE CARD */}
          <div className="lg:col-span-4 space-y-6">
             <div className="glass-card flex flex-col items-center text-center p-8 relative overflow-hidden">
                <div className="absolute top-0 left-0 w-full h-24 bg-gradient-to-br from-primary-600/20 to-secondary-600/20" />
                
                <div className="relative mt-8 group">
                   <div className="w-32 h-32 rounded-[40px] bg-surface-2 border-4 border-surface overflow-hidden shadow-2xl relative">
                      {uploading ? (
                        <div className="absolute inset-0 flex items-center justify-center bg-black/40 backdrop-blur-sm">
                           <Loader2 size={32} className="text-primary-500 animate-spin" />
                        </div>
                      ) : user?.profilePhoto ? (
                        <img src={user.profilePhoto} alt="Profile" className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center bg-gradient-to-tr from-primary-600 to-secondary-500 text-4xl font-black text-white">
                          {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                        </div>
                      )}
                   </div>
                   <button 
                      onClick={() => fileInputRef.current?.click()}
                      className="absolute bottom-0 right-0 w-10 h-10 rounded-2xl bg-primary-500 text-white flex items-center justify-center shadow-lg hover:scale-110 transition-transform"
                   >
                      <Camera size={20} />
                   </button>
                   <input type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={handlePhotoUpload} />
                </div>

                <div className="mt-6 space-y-1">
                   <h3 className="text-2xl font-black text-app-text tracking-tight">{user?.firstName} {user?.lastName}</h3>
                   <p className="text-sm font-bold text-muted-text font-mono tracking-wider">{user?.userCode || 'CRD-000000'}</p>
                </div>

                <div className="flex gap-2 mt-4">
                   <Badge variant={user?.kycStatus === 'VERIFIED' ? 'success' : 'warning'}>{user?.kycStatus || 'PENDING'}</Badge>
                   <Badge variant={user?.status === 'ACTIVE' ? 'success' : 'danger'}>{user?.status || 'INACTIVE'}</Badge>
                </div>

                <div className="w-full grid grid-cols-1 gap-4 mt-8 pt-8 border-t border-border-subtle rtl:text-right">
                   <div className="flex items-center gap-3">
                      <Mail size={16} className="text-muted-text" />
                      <span className="text-xs font-bold text-app-text">{user?.email}</span>
                   </div>
                   <div className="flex items-center gap-3">
                      <Phone size={16} className="text-muted-text" />
                      <span className="text-xs font-bold text-app-text">{user?.phoneNumber || 'Not provided'}</span>
                   </div>
                   <div className="flex items-center gap-3">
                      <Calendar size={16} className="text-muted-text" />
                      <span className="text-xs font-bold text-app-text italic">Member since {user?.createdAt ? new Date(user.createdAt).getFullYear() : '2024'}</span>
                   </div>
                </div>
             </div>

             <div className="glass-card bg-primary-500/5 border-primary-500/10 p-6 flex flex-col gap-3">
                <div className="flex items-center gap-3 text-primary-500 mb-1">
                   <Shield size={20} />
                   <h4 className="text-sm font-black uppercase tracking-widest">Security Advisory</h4>
                </div>
                <p className="text-xs font-medium text-muted-text leading-relaxed">
                  Never share your OTP or secondary passwords with anyone. Credixa staff will never ask for your authorization codes via call or email.
                </p>
             </div>
          </div>

          {/* RIGHT: TABS AND FORMS */}
          <div className="lg:col-span-8 space-y-6">
             <div className="p-1.5 bg-surface-2 border border-border-subtle rounded-2xl flex gap-1 w-fit">
                {['personal', 'verification', 'security', '2fa'].map(t => (
                  <button
                    key={t}
                    onClick={() => setActiveTab(t)}
                    className={`px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === t ? 'bg-primary-500 text-white shadow-lg shadow-primary-500/20' : 'text-muted-text hover:text-app-text'}`}
                  >
                    {t === 'personal' ? 'Personal Info' : t === 'verification' ? 'Verification' : t === 'security' ? 'Security' : 'Two-Factor'}
                  </button>
                ))}
             </div>

             <AnimatePresence mode="wait">
                {activeTab === 'personal' && (
                  <motion.div 
                    key="personal"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                  >
                    <form onSubmit={handleInfoSubmit(onUpdateInfo)} className="glass-card p-8 space-y-8">
                       <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
                          <Input label="First Name" {...regInfo('firstName')} error={infoErrors.firstName} />
                          <Input label="Last Name" {...regInfo('lastName')} error={infoErrors.lastName} />
                          <Input label="Phone Number" {...regInfo('phoneNumber')} error={infoErrors.phoneNumber} />
                          <div className="md:col-span-2">
                             <Input label="Street Address" {...regInfo('address')} error={infoErrors.address} />
                          </div>
                          <Input label="City" {...regInfo('city')} error={infoErrors.city} />
                          <Input label="State" {...regInfo('state')} error={infoErrors.state} />
                          <Input label="Pincode" {...regInfo('pincode')} error={infoErrors.pincode} maxLength={6} />
                       </div>
                       
                       <div className="pt-6 border-t border-border-subtle flex justify-end">
                          <Button 
                             type="submit" 
                             isLoading={infoSubmitting}
                             leftIcon={<Save size={18} />}
                          >
                            Save Profile Changes
                          </Button>
                       </div>
                    </form>
                  </motion.div>
                )}

                {activeTab === 'verification' && (
                  <motion.div
                    key="verification"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    className="space-y-6"
                  >
                    <div className="glass-card p-8 space-y-8">
                       <div className="flex items-center gap-4 mb-2">
                          <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${user?.kycStatus === 'VERIFIED' ? 'bg-success-500/10 text-success-500' : 'bg-warning-500/10 text-warning-500'}`}>
                             <Shield size={24} />
                          </div>
                          <div>
                             <h3 className="text-xl font-black text-app-text tracking-tight">KYC Verification</h3>
                             <p className="text-sm font-medium text-muted-text">Identity verification is required for all banking operations.</p>
                          </div>
                       </div>

                       <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                          <div className="p-4 rounded-2xl bg-surface-2 border border-border-subtle">
                             <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Status</p>
                             <Badge variant={user?.kycStatus === 'VERIFIED' ? 'success' : 'warning'}>{user?.kycStatus || 'NOT_SUBMITTED'}</Badge>
                          </div>
                          <div className="p-4 rounded-2xl bg-surface-2 border border-border-subtle">
                             <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">Aadhaar</p>
                             <p className="text-sm font-bold text-app-text">•••• •••• {user?.aadhaarNumber?.slice(-4) || '0000'}</p>
                          </div>
                          <div className="p-4 rounded-2xl bg-surface-2 border border-border-subtle">
                             <p className="text-[10px] font-bold text-muted-text uppercase tracking-widest mb-1">PAN</p>
                             <p className="text-sm font-bold text-app-text">{user?.panNumber?.replace(/.(?=.{2})/g, '•') || 'XXXXXX00X'}</p>
                          </div>
                       </div>

                       <div className="pt-8 border-t border-border-subtle space-y-6">
                          {user?.kycStatus === 'NOT_SUBMITTED' && (
                            <div className="space-y-4">
                               <p className="text-sm text-muted-text">Your documents are ready for submission. Click below to start the verification process.</p>
                               <Button 
                                 onClick={async () => {
                                   try {
                                     await axiosInstance.post(USERS.KYC_SUBMIT);
                                     dispatch(updateProfile({ ...user, kycStatus: 'PENDING' }));
                                     toast.success('KYC submitted for review');
                                   } catch (e) { toast.error(e.message); }
                                 }}
                                 leftIcon={<CheckCircle2 size={18} />}
                               >
                                 Submit for Verification
                               </Button>
                            </div>
                          )}

                          {user?.kycStatus === 'PENDING' && (
                            <div className="p-6 bg-warning-500/5 border border-warning-500/10 rounded-3xl space-y-4">
                               <p className="text-sm text-warning-600">Your application is currently being reviewed by our compliance team. This typically takes 24-48 hours.</p>
                               <div className="flex flex-wrap gap-3">
                                  <Badge variant="warning">UNDER REVIEW</Badge>
                                  <Button 
                                    size="sm"
                                    variant="ghost"
                                    className="text-primary-500 hover:bg-primary-500/10"
                                    onClick={async () => {
                                      try {
                                        await axiosInstance.post(USERS.KYC_VERIFY);
                                        dispatch(updateProfile({ ...user, kycStatus: 'VERIFIED', status: 'ACTIVE' }));
                                        toast.success('KYC simulation successful! Account is now ACTIVE.');
                                      } catch (e) { toast.error(e.message); }
                                    }}
                                  >
                                    [Demo] Simulate Instant Approval
                                  </Button>
                               </div>
                            </div>
                          )}

                          {user?.kycStatus === 'VERIFIED' && (
                            <div className="p-6 bg-success-500/5 border border-success-500/10 rounded-3xl flex items-center gap-6">
                               <div className="w-16 h-16 rounded-full bg-success-500/20 flex items-center justify-center text-success-500 shrink-0">
                                  <CheckCircle2 size={32} />
                               </div>
                               <div className="space-y-1">
                                  <h4 className="text-lg font-black text-app-text tracking-tight">Identity Verified</h4>
                                  <p className="text-sm font-medium text-muted-text">All banking features are unlocked. Your account status is <span className="text-success-500 font-bold">ACTIVE</span>.</p>
                               </div>
                            </div>
                          )}
                       </div>
                    </div>
                  </motion.div>
                )}

                {activeTab === 'security' && (
                  <motion.div 
                    key="security"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                  >
                    <form onSubmit={handlePassSubmit(onChangePassword)} className="glass-card p-8 space-y-8 max-w-2xl">
                       <div className="space-y-6">
                          <Input label="Current Password" type="password" {...regPass('currentPassword')} error={passErrors.currentPassword} />
                          <div className="h-px bg-border-subtle my-2" />
                          <Input label="New Password" type="password" {...regPass('newPassword')} error={passErrors.newPassword} />
                          <Input label="Confirm New Password" type="password" {...regPass('confirmNewPassword')} error={passErrors.confirmNewPassword} />
                       </div>

                       <div className="pt-6 border-t border-white/5 flex justify-end">
                          <Button 
                             type="submit" 
                             variant="secondary"
                             isLoading={passSubmitting}
                             leftIcon={<Lock size={18} />}
                          >
                            Update Credentials
                          </Button>
                       </div>
                    </form>
                  </motion.div>
                )}

                {activeTab === '2fa' && (
                  <motion.div 
                    key="2fa"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    className="glass-card p-12 text-center"
                  >
                    <div className="w-20 h-20 rounded-[32px] bg-surface-2 border border-border-subtle flex items-center justify-center text-muted-text mx-auto mb-6">
                       <Smartphone size={32} />
                    </div>
                    <h3 className="text-2xl font-black text-app-text tracking-tight mb-2">Two-Factor Authentication</h3>
                    <p className="text-sm font-medium text-muted-text max-w-sm mx-auto mb-8">
                       Add an extra layer of security to your account. We'll send an OTP to your registered phone for every login.
                    </p>
                    <div className="flex flex-col items-center gap-3">
                       <Badge variant="warning" className="mb-4">Feature Locked</Badge>
                       <p className="text-[10px] font-black text-muted-text uppercase tracking-widest">Coming soon in Credixa v2.0</p>
                    </div>
                  </motion.div>
                )}
             </AnimatePresence>
          </div>
       </div>
    </div>
  );
};

export default ProfilePage;
