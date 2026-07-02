import { createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axios';
import { AUTH } from '../../api/endpoints';
import { setCredentials, setLoading, setError, logout } from '../slices/authSlice';

export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.post(AUTH.LOGIN, credentials);
      dispatch(setCredentials(data));
      return data;
    } catch (err) {
      const message = err.message || 'Login failed';
      dispatch(setError(message));
      return rejectWithValue(message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const registerUser = createAsyncThunk(
  'auth/register',
  async (userData, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.post(AUTH.REGISTER, userData);
      return data;
    } catch (err) {
      const message = err.message || 'Registration failed';
      dispatch(setError(message));
      return rejectWithValue(message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const logoutUser = createAsyncThunk(
  'auth/logout',
  async (_, { dispatch }) => {
    try {
      await axiosInstance.post(AUTH.LOGOUT);
    } catch (err) {
      console.warn('Logout request failed', err);
    } finally {
      dispatch(logout());
    }
  }
);

export const fetchCurrentUser = createAsyncThunk(
  'auth/fetchMe',
  async (_, { dispatch, rejectWithValue }) => {
    try {
      const data = await axiosInstance.get(AUTH.ME);
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);
