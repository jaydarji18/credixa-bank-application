import { createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axios';
import { BENEFICIARIES } from '../../api/endpoints';
import { setBeneficiaries, addBeneficiaryLocally, setLoading, setError } from '../slices/beneficiarySlice';

export const fetchBeneficiaries = createAsyncThunk(
  'beneficiary/fetchAll',
  async (_, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.get(BENEFICIARIES.LIST);
      dispatch(setBeneficiaries(data));
      return data;
    } catch (err) {
      dispatch(setError(err.message));
      return rejectWithValue(err.message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const addBeneficiary = createAsyncThunk(
  'beneficiary/add',
  async (beneficiaryData, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.post(BENEFICIARIES.ADD, beneficiaryData);
      dispatch(addBeneficiaryLocally(data));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const removeBeneficiary = createAsyncThunk(
  'beneficiary/remove',
  async (id, { dispatch, rejectWithValue }) => {
    try {
      await axiosInstance.delete(BENEFICIARIES.DELETE(id));
      dispatch(fetchBeneficiaries());
      return id;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);
