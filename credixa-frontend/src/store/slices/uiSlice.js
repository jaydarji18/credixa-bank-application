import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  sidebarOpen: false,
  theme: localStorage.getItem('theme') || 'light', // Default to light by default
  loading: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action) => {
      state.sidebarOpen = action.payload;
    },
    toggleTheme: (state) => {
      state.theme = state.theme === 'light' ? 'dark' : 'light';
      localStorage.setItem('theme', state.theme);
      
      // Side effect: update document class
      if (state.theme === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    },
    setTheme: (state, action) => {
      state.theme = action.payload;
      localStorage.setItem('theme', action.payload);
      if (action.payload === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    },
    setGlobalLoading: (state, action) => {
      state.loading = action.payload;
    },
  },
});

export const { toggleSidebar, setSidebarOpen, toggleTheme, setTheme, setGlobalLoading } = uiSlice.actions;
export default uiSlice.reducer;
