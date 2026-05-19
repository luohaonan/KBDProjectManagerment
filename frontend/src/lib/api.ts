/// <reference types="vite/client" />
import axios from 'axios';
import { toast } from 'sonner';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

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
    if (status === 401 || status === 403) {
      localStorage.removeItem('token');
      toast.error('登录已失效，请重新登录');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
