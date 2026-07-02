import { z } from 'zod';

export const openAccountSchema = z.object({
  type: z.enum(['SAVINGS', 'CURRENT', 'FIXED_DEPOSIT']),
  branch: z.string().min(1, 'Please select a branch'),
  tenure: z.coerce.number().optional(), // For FD
  initialDeposit: z.coerce.number().min(500, 'Minimum initial deposit is ₹500'),
});

export const loginSchema = z.object({
  email: z.string().email('Enter a valid business email'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

export const registerSchema = z.object({
  firstName: z.string().min(2, 'Min 2 characters'),
  lastName: z.string().min(2, 'Min 2 characters'),
  email: z.string().email('Invalid email address'),
  phone: z
    .string()
    .regex(/^[0-9]{10}$/, 'Enter a valid 10-digit mobile number'),
  dateOfBirth: z
    .string()
    .min(1, 'Date of birth is required')
    .refine((val) => {
      const dob = new Date(val);
      return dob < new Date();
    }, 'Date of birth must be in the past'),
  panNumber: z
    .string()
    .regex(/^[A-Z]{5}[0-9]{4}[A-Z]$/, 'Invalid PAN format (e.g. ABCDE1234F)'),
  aadhaarNumber: z
    .string()
    .regex(/^[0-9]{12}$/, 'Aadhaar must be 12 digits'),
  address: z.string().min(5, 'Address must be at least 5 characters'),
  city: z.string().min(2, 'City name too short'),
  state: z.string().min(2, 'State name too short'),
  pincode: z
    .string()
    .regex(/^[0-9]{6}$/, 'Pincode must be 6 digits'),
  password: z
    .string()
    .min(8, 'Min 8 characters')
    .regex(
      /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/,
      'Must include uppercase, number & special character'
    ),
  confirmPassword: z.string(),
}).refine(data => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

export const otpSchema = z.object({
  otp: z.string().length(6, 'OTP must be 6 digits'),
});

export const forgotPasswordSchema = z.object({
  email: z.string().email('Invalid email'),
});

export const resetPasswordSchema = z.object({
  password: z.string().min(8, 'Min 8 characters'),
  confirmPassword: z.string(),
}).refine(data => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

export const depositSchema = z.object({
  accountId: z.string().min(1, 'Select an account'),
  amount: z.coerce.number().min(10, 'Min deposit is ₹10').max(1000000, 'Max single deposit ₹10L'),
});

export const withdrawSchema = z.object({
  accountId: z.string().min(1, 'Select an account'),
  amount: z.coerce.number().min(1, 'Min amount ₹1'),
});

export const transferSchema = z.object({
  fromAccountId: z.string().min(1, 'Select source account'),
  amount: z.coerce.number().min(1, 'Amount required'),
  type: z.enum(['NEFT', 'RTGS', 'IMPS', 'UPI', 'INTERNAL']),
  remarks: z.string().max(100).optional(),
});

export const addBeneficiarySchema = z.object({
  beneficiaryName: z.string().min(3, 'Name required'),
  accountNumber: z.string().min(10, 'Invalid account number'),
  ifscCode: z.string().regex(/^[A-Z0-9]{11}$/, 'IFSC must be exactly 11 alphanumeric characters'),
  bankName: z.string().min(2, 'Bank name required'),
  nickname: z.string().optional(),
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password required'),
  newPassword: z.string().min(8, 'Min 8 characters'),
  confirmPassword: z.string(),
}).refine(data => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

export const profileUpdateSchema = z.object({
  firstName: z.string().min(2),
  lastName: z.string().min(2),
  phoneNumber: z.string().min(10),
  address: z.string().min(5),
  city: z.string().min(2),
  state: z.string().min(2),
  pincode: z.string().length(6),
});

export const loanApplySchema = z.object({
  amount: z.coerce.number().min(1000, 'Min amount is ₹1,000'),
  tenure: z.coerce.number().min(6, 'Min tenure is 6 months'),
  productId: z.coerce.number().min(1, 'Product required'),
  linkedAccountId: z.coerce.number().min(1, 'Select a linked account')
});
export const spinSchema = z.object({
  spin: z.string().length(6, 'PIN must be exactly 6 digits').regex(/^[0-9]+$/, 'PIN must contain only numbers'),
});
