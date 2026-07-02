import { useSelector, useDispatch } from 'react-redux';
import { logout as logoutAction, setCredentials } from '../store/slices/authSlice';
import axiosInstance from '../api/axios';
import { AUTH } from '../api/endpoints';

export const useAuth = () => {
  const dispatch = useDispatch();
  const { user, isAuthenticated, loading, error } = useSelector((state) => state.auth);

  const login = async (credentials) => {
    try {
      const response = await axiosInstance.post(AUTH.LOGIN, credentials);
      dispatch(setCredentials(response.data));
      return response.data;
    } catch (err) {
      throw err;
    }
  };

  const logout = () => {
    dispatch(logoutAction());
  };

  const isAdmin = user?.userCode?.startsWith('ADMIN_') || 
                  ['SUPER_ADMIN', 'BANK_OPERATOR', 'BANK_MANAGER', 'TELLER', 'COMPLIANCE_OFFICER', 'SUPPORT_AGENT'].includes(user?.role);

  return {
    user,
    isAuthenticated,
    loading,
    error,
    login,
    logout,
    isAdmin,
  };
};

export default useAuth;
