import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import api from '../lib/api';

interface User {
  id?: number;
  username: string;
  roles: string[];
  permissions?: string[];
  departments?: string[];
}

interface AuthContextType {
  user: User | null;
  login: (token: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
  refreshUserInfo: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

const parseJwt = (token: string) => {
  try {
    const payload = token.split('.')[1];
    // JWT 使用 base64url 编码，需要转换为标准 base64 才能用 atob 解码
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    // 添加缺失的 padding
    const padding = '='.repeat((4 - (base64.length % 4)) % 4);
    return JSON.parse(atob(base64 + padding));
  } catch {
    return null;
  }
};

const buildUserFromToken = (token: string): User | null => {
  const payload = parseJwt(token);
  if (!payload || typeof payload !== 'object' || !payload.sub) {
    return null;
  }

  const roles = Array.isArray(payload.roles)
    ? payload.roles
    : typeof payload.roles === 'string'
    ? [payload.roles]
    : [];

  const permissions = Array.isArray(payload.permissions)
    ? payload.permissions
    : [];

  return {
    username: payload.sub,
    roles,
    permissions,
    departments: Array.isArray(payload.depts) ? payload.depts : [],
    id: typeof payload.id === 'number' ? payload.id : undefined,
  };
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      const parsedUser = buildUserFromToken(token);
      if (parsedUser) {
        setUser(parsedUser);
        // 异步从服务器获取最新权限信息
        fetchUserInfo();
      } else {
        localStorage.removeItem('token');
      }
    }
  }, []);

  const fetchUserInfo = async () => {
    try {
      const response = await api.get('/api/users/me');
      const data = response.data?.data;
      if (data) {
        setUser(prev => prev ? {
          ...prev,
          id: data.id,
          username: data.username,
          roles: data.roles || [],
          permissions: data.permissions || [],
          departments: data.departmentNames || [],
        } : prev);
      }
    } catch (error) {
      console.error('Failed to fetch user info:', error);
    }
  };

  const login = (token: string) => {
    localStorage.setItem('token', token);
    const parsedUser = buildUserFromToken(token);
    if (parsedUser) {
      setUser(parsedUser);
      // 异步获取完整信息
      fetchUserInfo();
    } else {
      // token 解析失败，清除并登出
      localStorage.removeItem('token');
      setUser(null);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };

  const refreshUserInfo = useCallback(async () => {
    await fetchUserInfo();
  }, []);

  const isAuthenticated = user !== null;

  const hasRole = (role: string) => {
    return user?.roles.includes(role) || false;
  };

  const hasPermission = (permission: string) => {
    return user?.permissions?.includes(permission) || false;
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated, hasRole, hasPermission, refreshUserInfo }}>
      {children}
    </AuthContext.Provider>
  );
};