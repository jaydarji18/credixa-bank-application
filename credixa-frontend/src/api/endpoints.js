export const AUTH = {
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  VERIFY_OTP: '/auth/verify-otp',
  RESEND_OTP: '/auth/resend-otp',
  FORGOT_PASSWORD: '/auth/forgot-password',
  RESET_PASSWORD: '/auth/reset-password',
  REFRESH_TOKEN: '/auth/refresh',
  LOGOUT: '/auth/logout',
  ME: '/users/me'
};

export const ACCOUNTS = {
  LIST: '/accounts',
  DETAILS: (id) => `/accounts/${id}`,
  SUMMARY: '/accounts/summary',
  CREATE: '/accounts',
  MY_ACCOUNTS: '/accounts'
};

export const TRANSACTIONS = {
  LIST: '/transactions',
  HISTORY: (accountId) => `/transactions/history/${accountId}`,
  DEPOSIT: '/transactions/deposit',
  WITHDRAW: '/transactions/withdraw',
  TRANSFER: '/transactions/transfer',
  SUMMARY: '/transactions/summary',
  STATEMENT: '/transactions/statement'
};

export const BENEFICIARIES = {
  LIST: '/beneficiaries',
  ADD: '/beneficiaries',
  DELETE: (id) => `/beneficiaries/${id}`
};

export const LOANS = {
  LIST: '/loans',
  APPLY: '/loans/apply',
  REPAY: (id) => `/loans/${id}/repay`,
  MY_LOANS: '/loans'
};

export const NOTIFICATIONS = {
  LIST: '/notifications',
  MARK_READ: (id) => `/notifications/${id}/read`,
  MARK_ALL_READ: '/notifications/read-all'
};

export const PROFILE = {
  GET: '/users/profile',
  UPDATE: '/users/profile',
  CHANGE_PASSWORD: '/users/change-password'
};

export const USERS = {
  ME: '/users/me',
  UPDATE_PROFILE: '/users/me',
  CHANGE_PASSWORD: '/users/me/change-password',
  UPLOAD_PHOTO: '/users/me/photo',
  KYC_SUBMIT: '/users/me/kyc/submit',
  KYC_VERIFY: '/users/me/kyc/verify',
  SET_SPIN: '/users/me/spin',
  VERIFY_SPIN: '/users/me/spin/verify'
};

export const ADMIN_AUTH = {
  LOGIN: '/admin/auth/login',
  LOGOUT: '/admin/auth/logout'
};

export const ADMIN = {
  USERS: '/admin/users',
  TRANSACTIONS: '/admin/transactions',
  SYSTEM_SUMMARY: '/admin/stats',
  LOANS_PENDING: '/admin/loans/pending',
  LOAN_APPROVE: (id) => `/admin/loans/${id}/approve`,
  BRANCHES: '/admin/branches',
  LOAN_PRODUCTS: '/admin/loan-products'
};
