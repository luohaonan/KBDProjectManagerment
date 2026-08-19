/// <reference types="vite/client" />
import axios from 'axios';
import { toast } from 'sonner';

// 开发默认同源（走 Vite /api 代理）；生产通过 VITE_API_BASE_URL 指定后端地址
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

const api = axios.create({
  baseURL,
  withCredentials: false,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('mustChangePassword');
      toast.error('登录已失效，请重新登录');
      window.location.href = '/login';
    } else if (status === 403) {
      const message = error?.response?.data?.message || '当前操作无权限';
      toast.error(message);
      if (message === '请先修改初始密码') {
        localStorage.setItem('mustChangePassword', 'true');
        if (window.location.pathname !== '/change-initial-password') {
          window.location.href = '/change-initial-password';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
