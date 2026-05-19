import React, { useState } from 'react';
import { Navigate, useNavigate, useLocation } from 'react-router-dom';
import api from '../lib/api';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from '../components/ui/card';
import { toast } from 'sonner';

const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as any)?.from?.pathname || '/dashboard';

  if (isAuthenticated) {
    return React.createElement(Navigate, { to: "/dashboard", replace: true });
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);

    try {
      const response = await api.post('/api/auth/login', {
        username,
        password,
      });

      const token = response.data?.token;
      if (!token || typeof token !== 'string') {
        throw new Error('登录失败');
      }

      login(token);
      toast.success('登录成功');
      navigate(from, { replace: true });
    } catch (error) {
      toast.error('登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <Card className="w-full max-w-md shadow-lg mx-4">
        <CardHeader>
          <CardTitle>项目管理系统登录</CardTitle>
          <CardDescription>使用你的账号密码登录系统，查看仪表盘和项目详情。</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <label className="block text-sm font-medium text-slate-700" htmlFor="username">
                用户名
              </label>
              <Input
                id="username"
                name="username"
                type="text"
                placeholder="请输入用户名"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <label className="block text-sm font-medium text-slate-700" htmlFor="password">
                密码
              </label>
              <Input
                id="password"
                name="password"
                type="password"
                placeholder="请输入密码"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </div>

            <CardFooter className="justify-end">
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? '登录中...' : '登录'}
              </Button>
            </CardFooter>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default Login;
