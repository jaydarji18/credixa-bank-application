import { Navigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';

export const ProtectedRoute = ({ children, adminOnly = false }) => {
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const location = useLocation();

  if (!isAuthenticated) {
    // Save current location to redirect after login
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  const isAdmin = user?.userCode?.startsWith('ADMIN_') || 
                  ['SUPER_ADMIN', 'BANK_OPERATOR', 'BANK_MANAGER', 'TELLER', 'COMPLIANCE_OFFICER', 'SUPPORT_AGENT'].includes(user?.role);

  if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};

export default ProtectedRoute;
