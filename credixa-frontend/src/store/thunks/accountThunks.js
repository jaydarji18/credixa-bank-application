import { createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axios';
import { ACCOUNTS } from '../../api/endpoints';
import { setAccounts, setSummary, setLoading, setError } from '../slices/accountSlice';

export const fetchAccounts = createAsyncThunk(
  'account/fetchAll',
  async (_, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.get(ACCOUNTS.MY_ACCOUNTS);
      dispatch(setAccounts(data));
      return data;
    } catch (err) {
      dispatch(setError(err.message));
      return rejectWithValue(err.message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const fetchAccountSummary = createAsyncThunk(
  'account/fetchSummary',
  async (_, { dispatch, rejectWithValue }) => {
    try {
      const data = await axiosInstance.get(ACCOUNTS.SUMMARY);
      dispatch(setSummary(data));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);
