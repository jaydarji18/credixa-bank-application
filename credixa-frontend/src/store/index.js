import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import uiReducer from './slices/uiSlice';
import accountReducer from './slices/accountSlice';
import transactionReducer from './slices/transactionSlice';
import notificationReducer from './slices/notificationSlice';
import beneficiaryReducer from './slices/beneficiarySlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    ui: uiReducer,
    account: accountReducer,
    transaction: transactionReducer,
    notification: notificationReducer,
    beneficiary: beneficiaryReducer,
  },
});

export default store;
