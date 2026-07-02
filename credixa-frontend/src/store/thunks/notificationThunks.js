import { createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axios';
import { NOTIFICATIONS } from '../../api/endpoints';
import { setNotifications, setLoading } from '../slices/notificationSlice';

export const fetchNotifications = createAsyncThunk(
  'notification/fetchAll',
  async (_, { dispatch, rejectWithValue }) => {
    try {
      dispatch(setLoading(true));
      const data = await axiosInstance.get(NOTIFICATIONS.LIST);
      dispatch(setNotifications(data));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    } finally {
      dispatch(setLoading(false));
    }
  }
);

export const markNotificationRead = createAsyncThunk(
  'notification/markRead',
  async (id, { dispatch, rejectWithValue }) => {
    try {
      await axiosInstance.put(NOTIFICATIONS.MARK_READ(id));
      return id;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);
