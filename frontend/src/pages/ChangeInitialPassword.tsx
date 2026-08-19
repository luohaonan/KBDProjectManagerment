import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import api from '../lib/api';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { toast } from 'sonner';

const ChangeInitialPassword: React.FC = () => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { isAuthenticated, mustChangePassword, logout } = useAuth();
  const navigate = useNavigate();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!mustChangePassword) return <Navigate to="/dashboard" replace />;

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (newPassword.length < 6 || newPassword.length > 64) {
      toast.error('新密码长度必须为 6-64 位');
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error('两次输入的新密码不一致');
      return;
    }
    if (newPassword === '123456') {
      toast.error('新密码不能与初始密码相同');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/auth/change-initial-password', { currentPassword, newPassword, confirmPassword });
      toast.success('密码修改成功，请使用新密码重新登录');
      logout();
      navigate('/login', { replace: true });
    } catch (error: any) {
      toast.error(error.response?.data?.message || '密码修改失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-65px)] items-center justify-center bg-slate-50">
      <Card className="mx-4 w-full max-w-md shadow-lg">
        <CardHeader>
          <CardTitle>首次登录，请修改密码</CardTitle>
          <CardDescription>当前账号正在使用初始密码。修改成功并重新登录后，才能进入系统。</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <label htmlFor="currentPassword" className="text-sm font-medium">当前密码</label>
              <Input id="currentPassword" type="password" value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)} required autoComplete="current-password" />
            </div>
            <div className="space-y-2">
              <label htmlFor="newPassword" className="text-sm font-medium">新密码（6-64 位）</label>
              <Input id="newPassword" type="password" value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)} required minLength={6} maxLength={64} autoComplete="new-password" />
            </div>
            <div className="space-y-2">
              <label htmlFor="confirmPassword" className="text-sm font-medium">确认新密码</label>
              <Input id="confirmPassword" type="password" value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)} required minLength={6} maxLength={64} autoComplete="new-password" />
            </div>
            <CardFooter className="px-0 pb-0">
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? '修改中...' : '修改密码'}
              </Button>
            </CardFooter>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default ChangeInitialPassword;