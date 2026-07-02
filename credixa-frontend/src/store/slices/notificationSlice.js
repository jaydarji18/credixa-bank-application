import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  notifications: [],
  unreadCount: 0,
  loading: false,
};

const notificationSlice = createSlice({
  name: 'notification',
  initialState,
  reducers: {
    setNotifications: (state, action) => {
      state.notifications = action.payload.notifications;
      state.unreadCount = action.payload.unreadCount;
    },
    addNotification: (state, action) => {
      state.notifications.unshift(action.payload);
      if (!action.payload.isRead) state.unreadCount += 1;
    },
    markAsRead: (state, action) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && !notification.isRead) {
        notification.isRead = true;
        state.unreadCount -= 1;
      }
    },
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
  },
});

export const { setNotifications, addNotification, markAsRead, setLoading } = notificationSlice.actions;
export default notificationSlice.reducer;
