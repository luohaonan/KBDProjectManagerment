import React from 'react';
import { useAuth } from '../contexts/AuthContext';

interface HasPermissionProps {
  roles: string[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export const HasPermission: React.FC<HasPermissionProps> = ({ roles, children, fallback = null }) => {
  const { hasRole } = useAuth();

  const hasAnyRole = roles.some(role => hasRole(role));

  return hasAnyRole ? <>{children}</> : <>{fallback}</>;
};