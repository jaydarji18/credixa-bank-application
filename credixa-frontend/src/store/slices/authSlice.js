import { createSlice } from '@reduxjs/toolkit';

const safeJSONParse = (key) => {
  try {
    const val = localStorage.getItem(key);
    return val && val !== 'undefined' ? JSON.parse(val) : null;
  } catch (e) {
    localStorage.removeItem(key);
    return null;
  }
};

const getToken = (key) => {
  const val = localStorage.getItem(key);
  return val && val !== 'undefined' && val !== 'null' ? val : null;
};

const initialState = {
  user: safeJSONParse('user'),
  accessToken: getToken('accessToken'),
  refreshToken: getToken('refreshToken'),
  isAuthenticated: !!getToken('accessToken'),
  loading: false,
  error: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    setCredentials: (state, action) => {
      if (!action.payload) return;
      
      const { user, accessToken, refreshToken } = action.payload;
      state.user = user;
      state.accessToken = accessToken;
      state.refreshToken = refreshToken;
      state.isAuthenticated = true;
      state.error = null;

      if (accessToken) localStorage.setItem('accessToken', accessToken);
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
      if (user) localStorage.setItem('user', JSON.stringify(user));
    },
    setUser: (state, action) => {
      state.user = action.payload;
      localStorage.setItem('user', JSON.stringify(action.payload));
    },
    setError: (state, action) => {
      state.error = action.payload;
      state.loading = false;
    },
    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.error = null;

      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    },
  },
});

export const { setLoading, setCredentials, setUser, setError, logout } = authSlice.actions;

export default authSlice.reducer;
