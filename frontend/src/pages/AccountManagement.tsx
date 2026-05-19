import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { useAuth } from '../contexts/AuthContext';
import api from '../lib/api';
import { toast } from 'sonner';
import { UserPlus, Key, Shield, ShieldCheck, ArrowLeft, Save, X } from 'lucide-react';

interface UserInfo {
  id: number;
  username: string;
  email: string;
  isActive: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

interface RoleInfo {
  id: number;
  name: string;
  description: string;
  permissions: PermissionInfo[];
}

interface PermissionInfo {
  id: number;
  name: string;
  description: string;
}

const AccountManagement: React.FC = () => {
  const navigate = useNavigate();
  const { user, hasRole, hasPermission } = useAuth();
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [permissions, setPermissions] = useState<PermissionInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'users' | 'roles'>('users');
  const [showCreateUser, setShowCreateUser] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState<{ userId: number; username: string } | null>(null);
  const [showRoleModal, setShowRoleModal] = useState<{ userId: number; username: string; currentRoles: string[] } | null>(null);
  const [showPermissionModal, setShowPermissionModal] = useState<RoleInfo | null>(null);

  // 表单状态
  const [newUser, setNewUser] = useState({ username: '', password: '', email: '', roles: [] as string[] });
  const [newPassword, setNewPassword] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);

  const isAdmin = hasRole('ROLE_ADMIN');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [usersRes, rolesRes, permsRes] = await Promise.all([
        api.get('/api/users'),
        api.get('/api/users/roles'),
        api.get('/api/users/permissions'),
      ]);
      setUsers(usersRes.data.data || []);
      setRoles(rolesRes.data.data || []);
      setPermissions(permsRes.data.data || []);
    } catch (error) {
      toast.error('加载数据失败');
      console.error('Load data error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async () => {
    if (!newUser.username || !newUser.password) {
      toast.error('用户名和密码不能为空');
      return;
    }
    try {
      await api.post('/api/users', newUser);
      toast.success('用户创建成功');
      setShowCreateUser(false);
      setNewUser({ username: '', password: '', email: '', roles: [] });
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '创建用户失败');
    }
  };

  const handleUpdatePassword = async () => {
    if (!showPasswordModal || !newPassword) {
      toast.error('密码不能为空');
      return;
    }
    try {
      await api.put(`/api/users/${showPasswordModal.userId}/password`, { newPassword });
      toast.success('密码修改成功');
      setShowPasswordModal(null);
      setNewPassword('');
    } catch (error: any) {
      toast.error(error.response?.data?.message || '修改密码失败');
    }
  };

  const handleUpdateRoles = async () => {
    if (!showRoleModal) return;
    try {
      await api.put(`/api/users/${showRoleModal.userId}/roles`, { roles: selectedRoles });
      toast.success('角色更新成功');
      setShowRoleModal(null);
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '更新角色失败');
    }
  };

  const handleUpdatePermissions = async () => {
    if (!showPermissionModal) return;
    try {
      await api.put(`/api/users/roles/${showPermissionModal.id}/permissions`, { permissions: selectedPermissions });
      toast.success('权限更新成功');
      setShowPermissionModal(null);
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '更新权限失败');
    }
  };

  const openRoleModal = (u: UserInfo) => {
    setSelectedRoles(u.roles);
    setShowRoleModal({ userId: u.id, username: u.username, currentRoles: u.roles });
  };

  const openPermissionModal = (r: RoleInfo) => {
    setSelectedPermissions(r.permissions.map(p => p.name));
    setShowPermissionModal(r);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
        <div className="text-xl">加载中...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      {/* 顶部标题栏 */}
      <div className="border-b border-slate-700 px-6 py-4">
        <div className="flex items-center justify-between max-w-7xl mx-auto">
          <div className="flex items-center gap-4">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/dashboard')}
              className="border-slate-600 text-slate-300 hover:bg-slate-700"
            >
              <ArrowLeft className="w-4 h-4 mr-1" />
              返回首页
            </Button>
            <h1 className="text-2xl font-bold">账号与权限管理</h1>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-6">
        {/* 当前用户信息 */}
        <Card className="bg-slate-800 border-slate-600 mb-6">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-slate-400">当前用户</p>
                <p className="text-lg font-semibold">{user?.username}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-slate-400">角色</p>
                <div className="flex gap-1 mt-1">
                  {user?.roles.map(role => (
                    <Badge key={role} variant="outline" className="bg-blue-900/50 text-blue-300 border-blue-700">
                      {role.replace('ROLE_', '')}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
            {user?.permissions && user.permissions.length > 0 && (
              <div className="mt-3 pt-3 border-t border-slate-700">
                <p className="text-sm text-slate-400 mb-2">拥有的权限</p>
                <div className="flex flex-wrap gap-1">
                  {user.permissions.map(perm => (
                    <Badge key={perm} variant="outline" className="bg-green-900/30 text-green-300 border-green-700 text-xs">
                      {perm}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* 标签切换 */}
        {isAdmin && (
          <div className="flex gap-2 mb-6">
            <Button
              variant={activeTab === 'users' ? 'default' : 'outline'}
              onClick={() => setActiveTab('users')}
              className={activeTab === 'users' ? 'bg-blue-600' : 'border-slate-600 text-slate-300'}
            >
              <Shield className="w-4 h-4 mr-2" />
              用户管理
            </Button>
            <Button
              variant={activeTab === 'roles' ? 'default' : 'outline'}
              onClick={() => setActiveTab('roles')}
              className={activeTab === 'roles' ? 'bg-blue-600' : 'border-slate-600 text-slate-300'}
            >
              <ShieldCheck className="w-4 h-4 mr-2" />
              权限组设置
            </Button>
          </div>
        )}

        {/* 用户管理面板 */}
        {activeTab === 'users' && isAdmin && (
          <div>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">用户列表</h2>
              <Button onClick={() => setShowCreateUser(true)} className="bg-green-600 hover:bg-green-700">
                <UserPlus className="w-4 h-4 mr-2" />
                新建账号
              </Button>
            </div>

            <Card className="bg-slate-800 border-slate-600">
              <CardContent className="p-0">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-slate-700 text-slate-400 text-sm">
                      <th className="text-left p-4">ID</th>
                      <th className="text-left p-4">用户名</th>
                      <th className="text-left p-4">邮箱</th>
                      <th className="text-left p-4">角色</th>
                      <th className="text-left p-4">状态</th>
                      <th className="text-right p-4">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map(u => (
                      <tr key={u.id} className="border-b border-slate-700 hover:bg-slate-700/50">
                        <td className="p-4 text-sm">{u.id}</td>
                        <td className="p-4 font-medium">{u.username}</td>
                        <td className="p-4 text-sm text-slate-400">{u.email || '-'}</td>
                        <td className="p-4">
                          <div className="flex flex-wrap gap-1">
                            {u.roles.map(role => (
                              <Badge key={role} variant="outline" className="bg-purple-900/30 text-purple-300 border-purple-700 text-xs">
                                {role.replace('ROLE_', '')}
                              </Badge>
                            ))}
                          </div>
                        </td>
                        <td className="p-4">
                          <Badge variant="outline" className={u.isActive ? 'bg-green-900/30 text-green-300 border-green-700' : 'bg-red-900/30 text-red-300 border-red-700'}>
                            {u.isActive ? '激活' : '禁用'}
                          </Badge>
                        </td>
                        <td className="p-4 text-right">
                          <div className="flex gap-2 justify-end">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => setShowPasswordModal({ userId: u.id, username: u.username })}
                              className="border-slate-600 text-slate-300 hover:bg-slate-700"
                              title="修改密码"
                            >
                              <Key className="w-3 h-3" />
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => openRoleModal(u)}
                              className="border-slate-600 text-slate-300 hover:bg-slate-700"
                              title="修改角色"
                            >
                              <Shield className="w-3 h-3" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 权限组设置面板 */}
        {activeTab === 'roles' && isAdmin && (
          <div>
            <h2 className="text-xl font-semibold mb-4">权限组设置</h2>
            <div className="grid gap-4">
              {roles.map(role => (
                <Card key={role.id} className="bg-slate-800 border-slate-600">
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <CardTitle className="text-slate-100 text-lg">
                          {role.name.replace('ROLE_', '')}
                        </CardTitle>
                        <p className="text-sm text-slate-400 mt-1">{role.description}</p>
                      </div>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openPermissionModal(role)}
                        className="border-slate-600 text-slate-300 hover:bg-slate-700"
                      >
                        <ShieldCheck className="w-4 h-4 mr-1" />
                        编辑权限
                      </Button>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="flex flex-wrap gap-1">
                      {role.permissions.map(p => (
                        <Badge key={p.name} variant="outline" className="bg-green-900/30 text-green-300 border-green-700 text-xs">
                          {p.name}
                        </Badge>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* 非管理员查看自己的权限 */}
        {!isAdmin && (
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">我的权限</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <p className="text-sm text-slate-400 mb-2">角色</p>
                  <div className="flex flex-wrap gap-2">
                    {user?.roles.map(role => (
                      <Badge key={role} variant="outline" className="bg-blue-900/50 text-blue-300 border-blue-700">
                        {role.replace('ROLE_', '')}
                      </Badge>
                    ))}
                  </div>
                </div>
                <div>
                  <p className="text-sm text-slate-400 mb-2">权限列表</p>
                  {user?.permissions && user.permissions.length > 0 ? (
                    <div className="flex flex-wrap gap-2">
                      {user.permissions.map(perm => (
                        <Badge key={perm} variant="outline" className="bg-green-900/30 text-green-300 border-green-700">
                          {perm}
                        </Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-slate-500">暂无权限信息</p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* 新建账号弹窗 */}
      {showCreateUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">新建账号</h3>
              <button onClick={() => setShowCreateUser(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">用户名 *</label>
                <Input
                  value={newUser.username}
                  onChange={e => setNewUser({ ...newUser, username: e.target.value })}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入用户名"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">密码 *</label>
                <Input
                  type="password"
                  value={newUser.password}
                  onChange={e => setNewUser({ ...newUser, password: e.target.value })}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入密码"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">邮箱</label>
                <Input
                  value={newUser.email}
                  onChange={e => setNewUser({ ...newUser, email: e.target.value })}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入邮箱（可选）"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-2">角色</label>
                <div className="flex flex-wrap gap-2">
                  {roles.map(role => (
                    <button
                      key={role.name}
                      onClick={() => {
                        const roles = newUser.roles.includes(role.name)
                          ? newUser.roles.filter(r => r !== role.name)
                          : [...newUser.roles, role.name];
                        setNewUser({ ...newUser, roles });
                      }}
                      className={`px-3 py-1 rounded text-sm border ${
                        newUser.roles.includes(role.name)
                          ? 'bg-blue-600 border-blue-500 text-white'
                          : 'bg-slate-700 border-slate-600 text-slate-300 hover:bg-slate-600'
                      }`}
                    >
                      {role.name.replace('ROLE_', '')}
                    </button>
                  ))}
                </div>
              </div>
              <Button onClick={handleCreateUser} className="w-full bg-blue-600 hover:bg-blue-700">
                <Save className="w-4 h-4 mr-2" />
                创建
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 修改密码弹窗 */}
      {showPasswordModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">修改密码 - {showPasswordModal.username}</h3>
              <button onClick={() => { setShowPasswordModal(null); setNewPassword(''); }} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">新密码</label>
                <Input
                  type="password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入新密码"
                />
              </div>
              <Button onClick={handleUpdatePassword} className="w-full bg-blue-600 hover:bg-blue-700">
                <Save className="w-4 h-4 mr-2" />
                确认修改
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 修改角色弹窗 */}
      {showRoleModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">修改角色 - {showRoleModal.username}</h3>
              <button onClick={() => setShowRoleModal(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div className="flex flex-wrap gap-2">
                {roles.map(role => (
                  <button
                    key={role.name}
                    onClick={() => {
                      setSelectedRoles(prev =>
                        prev.includes(role.name)
                          ? prev.filter(r => r !== role.name)
                          : [...prev, role.name]
                      );
                    }}
                    className={`px-3 py-1 rounded text-sm border ${
                      selectedRoles.includes(role.name)
                        ? 'bg-blue-600 border-blue-500 text-white'
                        : 'bg-slate-700 border-slate-600 text-slate-300 hover:bg-slate-600'
                    }`}
                  >
                    {role.name.replace('ROLE_', '')}
                  </button>
                ))}
              </div>
              <Button onClick={handleUpdateRoles} className="w-full bg-blue-600 hover:bg-blue-700">
                <Save className="w-4 h-4 mr-2" />
                保存
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑权限弹窗 */}
      {showPermissionModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-2xl border border-slate-600 max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">编辑权限 - {showPermissionModal.name.replace('ROLE_', '')}</h3>
              <button onClick={() => setShowPermissionModal(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-3">
              <p className="text-sm text-slate-400 mb-2">勾选需要分配给该角色的权限：</p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                {permissions.map(perm => (
                  <label
                    key={perm.name}
                    className={`flex items-center gap-3 p-3 rounded border cursor-pointer transition ${
                      selectedPermissions.includes(perm.name)
                        ? 'bg-blue-900/30 border-blue-700'
                        : 'bg-slate-700/50 border-slate-600 hover:bg-slate-700'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={selectedPermissions.includes(perm.name)}
                      onChange={() => {
                        setSelectedPermissions(prev =>
                          prev.includes(perm.name)
                            ? prev.filter(p => p !== perm.name)
                            : [...prev, perm.name]
                        );
                      }}
                      className="w-4 h-4 accent-blue-600"
                    />
                    <div>
                      <p className="text-sm font-medium">{perm.name}</p>
                      <p className="text-xs text-slate-400">{perm.description}</p>
                    </div>
                  </label>
                ))}
              </div>
              <Button onClick={handleUpdatePermissions} className="w-full bg-blue-600 hover:bg-blue-700 mt-4">
                <Save className="w-4 h-4 mr-2" />
                保存权限设置
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AccountManagement;
