import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  accounts: [],
  summary: null,
  loading: false,
  error: null,
};

const accountSlice = createSlice({
  name: 'account',
  initialState,
  reducers: {
    setAccounts: (state, action) => {
      state.accounts = action.payload;
    },
    setSummary: (state, action) => {
      state.summary = action.payload;
    },
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    setError: (state, action) => {
      state.error = action.payload;
    },
    updateAccountBalance: (state, action) => {
      const { accountId, newBalance } = action.payload;
      const account = state.accounts.find(a => a.id === accountId);
      if (account) {
        account.balance = newBalance;
      }
      if (state.summary) {
        // Recalculate total balance if needed or wait for poll
      }
    }
  },
});

export const { setAccounts, setSummary, setLoading, setError, updateAccountBalance } = accountSlice.actions;
export default accountSlice.reducer;
