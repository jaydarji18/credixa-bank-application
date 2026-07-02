import { createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axios';
import { TRANSACTIONS } from '../../api/endpoints';
import { setTransactions, setLoading, setError } from '../slices/transactionSlice';

export const fetchTransactions = createAsyncThunk(
  'transaction/fetchAll',
  async (params = {}, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      
      // Clean up params: remove empty strings and nulls
      const cleanParams = Object.entries(params).reduce((acc, [key, value]) => {
        if (value !== '' && value !== null && value !== undefined) {
          acc[key] = value;
        }
        return acc;
      }, {});

      // Format dates
      if (cleanParams.fromDate && !cleanParams.fromDate.includes('T')) cleanParams.fromDate = `${cleanParams.fromDate}T00:00:00`;
      if (cleanParams.toDate && !cleanParams.toDate.includes('T')) cleanParams.toDate = `${cleanParams.toDate}T23:59:59`;

      const data = await axiosInstance.get(TRANSACTIONS.LIST, { 
        params: cleanParams
      });
      dispatch(setTransactions(data.content || data));
      return data;
    } catch (err) {
      dispatch(setError(err.message));
      return rejectWithValue(err.message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const performTransfer = createAsyncThunk(
  'transaction/transfer',
  async (transferData, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.post(TRANSACTIONS.TRANSFER, transferData);
      return data;
    } catch (err) {
      dispatch(setError(err.message));
      return rejectWithValue(err.message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);
