import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { useAuth } from '../contexts/AuthContext';
import api from '../lib/api';
import { toast } from 'sonner';
import { UserPlus, Key, Shield, ShieldCheck, ArrowLeft, Save, X, Building2, UserCog, Trash2, Edit3 } from 'lucide-react';

const lightTagClass = 'bg-slate-100 text-slate-700 border-slate-300';
const lightTagClassXs = 'bg-slate-100 text-slate-700 border-slate-300 text-xs';
const selectableTagClass = 'bg-white border-slate-300 text-slate-700 hover:bg-slate-100';
const selectableTagActiveClass = 'bg-blue-100 border-blue-300 text-slate-900';
const managementTabClass = 'rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none hover:bg-slate-100';
const managementTabActiveClass = 'bg-blue-100 border-blue-300 text-slate-900';

interface UserInfo {
  id: number;
  username: string;
  email: string;
  isActive: boolean;
  roles: string[];
  departmentIds?: number[];
  departmentNames?: string[];
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

interface DepartmentInfo {
  id: number;
  deptCode: string;
  deptName: string;
  deptType: string;
  parentId?: number;
  isActive: boolean;
  headUserId?: number;
  headUserName?: string;
  memberCount: number;
}

const AccountManagement: React.FC = () => {
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [permissions, setPermissions] = useState<PermissionInfo[]>([]);
  const [departments, setDepartments] = useState<DepartmentInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'users' | 'roles' | 'departments'>('users');
  const [showCreateUser, setShowCreateUser] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState<{ userId: number; username: string } | null>(null);
  const [showRoleModal, setShowRoleModal] = useState<{ userId: number; username: string; currentRoles: string[] } | null>(null);
  const [showPermissionModal, setShowPermissionModal] = useState<RoleInfo | null>(null);
  const [showDeptDetail, setShowDeptDetail] = useState<DepartmentInfo | null>(null);
  const [deptMembers, setDeptMembers] = useState<UserInfo[]>([]);
  const [loadingDeptMembers, setLoadingDeptMembers] = useState(false);
  const [showAddMember, setShowAddMember] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<UserInfo[]>([]);
  const [selectedAddUserId, setSelectedAddUserId] = useState<number | undefined>(undefined);
  const [showEditDept, setShowEditDept] = useState<DepartmentInfo | null>(null);
  const [editDeptName, setEditDeptName] = useState('');

  // 表单状态
  const [newUser, setNewUser] = useState({ username: '', password: '', email: '', roles: [] as string[], departmentIds: [] as number[] });
  const [newPassword, setNewPassword] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);

  const isAdmin = hasRole('ROLE_ADMIN');

  useEffect(() => {
    loadData();
  }, [isAdmin]);

  const loadData = async () => {
    try {
      setLoading(true);
      if (isAdmin) {
        const [usersRes, rolesRes, permsRes, deptRes] = await Promise.all([
          api.get('/api/users'),
          api.get('/api/users/roles'),
          api.get('/api/users/permissions'),
          api.get('/api/departments'),
        ]);
        setUsers(usersRes.data.data || []);
        setRoles(rolesRes.data.data || []);
        setPermissions(permsRes.data.data || []);
        setDepartments(deptRes.data.data || []);
      } else {
        // 非管理员只加载权限列表（用于展示）
        const permsRes = await api.get('/api/users/permissions');
        setPermissions(permsRes.data.data || []);
      }
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
      setNewUser({ username: '', password: '', email: '', roles: [], departmentIds: [] });
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

  const openDeptDetail = async (dept: DepartmentInfo) => {
    setShowDeptDetail(dept);
    setLoadingDeptMembers(true);
    try {
      const res = await api.get(`/api/departments/${dept.id}/members`);
      setDeptMembers(res.data.data || []);
    } catch (error) {
      toast.error('加载部门成员失败');
      setDeptMembers([]);
    } finally {
      setLoadingDeptMembers(false);
    }
  };

  const handleAssignHead = async (deptId: number, userId: number | null) => {
    try {
      await api.put(`/api/departments/${deptId}/head`, { userId });
      toast.success(userId ? '部门负责人设置成功' : '已取消部门负责人');
      loadData();
      // 刷新当前部门详情
      if (showDeptDetail && showDeptDetail.id === deptId) {
        const res = await api.get(`/api/departments/${deptId}/members`);
        setDeptMembers(res.data.data || []);
        // 更新部门信息
        const deptRes = await api.get(`/api/departments/${deptId}`);
        setShowDeptDetail(deptRes.data.data);
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || '设置部门负责人失败');
    }
  };

  const handleAddMember = async () => {
    if (!showDeptDetail || !selectedAddUserId) return;
    try {
      await api.put(`/api/departments/${showDeptDetail.id}/members`, { userId: selectedAddUserId });
      toast.success('成员添加成功');
      setShowAddMember(false);
      setSelectedAddUserId(undefined);
      // 刷新成员列表
      const res = await api.get(`/api/departments/${showDeptDetail.id}/members`);
      setDeptMembers(res.data.data || []);
      // 刷新部门信息
      const deptRes = await api.get(`/api/departments/${showDeptDetail.id}`);
      setShowDeptDetail(deptRes.data.data);
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '添加成员失败');
    }
  };

  const handleRemoveMember = async (userId: number) => {
    if (!showDeptDetail) return;
    if (!window.confirm('确定要将该成员移出部门吗？')) return;
    try {
      await api.delete(`/api/departments/${showDeptDetail.id}/members/${userId}`);
      toast.success('成员已移出部门');
      // 刷新成员列表
      const res = await api.get(`/api/departments/${showDeptDetail.id}/members`);
      setDeptMembers(res.data.data || []);
      // 刷新部门信息
      const deptRes = await api.get(`/api/departments/${showDeptDetail.id}`);
      setShowDeptDetail(deptRes.data.data);
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '移出成员失败');
    }
  };

  const handleDeleteUser = async (targetUser: UserInfo) => {
    if (!window.confirm(`确定要删除用户"${targetUser.username}"吗？此操作不可撤销。`)) return;
    try {
      await api.delete(`/api/users/${targetUser.id}`);
      toast.success('用户已删除');
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '删除用户失败');
    }
  };

  const handleUpdateDepartment = async () => {
    if (!showEditDept || !editDeptName.trim()) {
      toast.error('部门名称不能为空');
      return;
    }
    try {
      await api.put(`/api/departments/${showEditDept.id}`, { deptName: editDeptName.trim() });
      toast.success('部门名称已更新');
      setShowEditDept(null);
      setEditDeptName('');
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '更新部门失败');
    }
  };

  const handleDeleteDepartment = async (dept: DepartmentInfo) => {
    if (!window.confirm(`确定要删除部门"${dept.deptName}"吗？此操作不可撤销。`)) return;
    try {
      await api.delete(`/api/departments/${dept.id}`);
      toast.success('部门已删除');
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '删除部门失败');
    }
  };

  const handleDeleteRole = async (role: RoleInfo) => {
    if (!window.confirm(`确定要删除权限组"${role.name.replace('ROLE_', '')}"吗？此操作不可撤销。`)) return;
    try {
      await api.delete(`/api/users/roles/${role.id}`);
      toast.success('权限组已删除');
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '删除权限组失败');
    }
  };

  const openAddMember = () => {
    if (!showDeptDetail) return;
    // 过滤出不在当前部门的用户
    const memberIds = new Set(deptMembers.map(m => m.id));
    const available = users.filter(u => !memberIds.has(u.id));
    setAvailableUsers(available);
    setSelectedAddUserId(undefined);
    setShowAddMember(true);
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
        {/* 当前用户信息 - 仅管理员可见（非管理员用下方"我的权限"卡片替代） */}
        {isAdmin && (
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
                      <Badge key={role} variant="outline" className={lightTagClass}>
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
                      <Badge key={perm} variant="outline" className={lightTagClassXs}>
                        {perm}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* 标签切换 */}
        {isAdmin && (
          <div className="mb-6 flex flex-wrap gap-2 rounded-lg bg-white p-2 shadow-sm">
            <Button
              variant={activeTab === 'users' ? 'default' : 'outline'}
              onClick={() => setActiveTab('users')}
              className={`${managementTabClass} ${activeTab === 'users' ? managementTabActiveClass : ''}`}
            >
              <Shield className="w-4 h-4 mr-2" />
              用户管理
            </Button>
            <Button
              variant={activeTab === 'departments' ? 'default' : 'outline'}
              onClick={() => setActiveTab('departments')}
              className={`${managementTabClass} ${activeTab === 'departments' ? managementTabActiveClass : ''}`}
            >
              <Building2 className="w-4 h-4 mr-2" />
              部门管理
            </Button>
            <Button
              variant={activeTab === 'roles' ? 'default' : 'outline'}
              onClick={() => setActiveTab('roles')}
              className={`${managementTabClass} ${activeTab === 'roles' ? managementTabActiveClass : ''}`}
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
                      <th className="text-left p-4">所属部门</th>
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
                              <Badge key={role} variant="outline" className={lightTagClassXs}>
                                {role.replace('ROLE_', '')}
                              </Badge>
                            ))}
                          </div>
                        </td>
                        <td className="p-4 text-sm text-slate-400">
                          {(u.departmentNames && u.departmentNames.length > 0)
                            ? u.departmentNames.join(', ')
                            : '-'}
                        </td>
                        <td className="p-4">
                          <Badge variant="outline" className={u.isActive ? lightTagClass : lightTagClass}>
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
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleDeleteUser(u)}
                              className="border-red-800 text-red-400 hover:bg-red-900/20"
                              title="删除用户"
                            >
                              <Trash2 className="w-3 h-3" />
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

        {/* 部门管理面板 */}
        {activeTab === 'departments' && isAdmin && (
          <div>
            <h2 className="text-xl font-semibold mb-4">部门管理</h2>
            <div className="grid gap-4">
              {departments.map(dept => (
                <Card key={dept.id} className="bg-slate-800 border-slate-600">
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <CardTitle className="text-slate-100 text-lg flex items-center gap-2">
                          <Building2 className="w-5 h-5 text-blue-400" />
                          {dept.deptName}
                        </CardTitle>
                        <p className="text-sm text-slate-400 mt-1">
                          编码: {dept.deptCode} | 类型: {dept.deptType}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setEditDeptName(dept.deptName);
                            setShowEditDept(dept);
                          }}
                          className="border-slate-600 text-slate-300 hover:bg-slate-700"
                          title="修改名称"
                        >
                          <Edit3 className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleDeleteDepartment(dept)}
                          className="border-red-800 text-red-400 hover:bg-red-900/20"
                          title="删除部门"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => openDeptDetail(dept)}
                          className="border-slate-600 text-slate-300 hover:bg-slate-700"
                        >
                          <UserCog className="w-4 h-4 mr-1" />
                          查看成员
                        </Button>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="flex gap-6 text-sm">
                      <div>
                        <span className="text-slate-400">成员数: </span>
                        <span className="text-slate-200 font-medium">{dept.memberCount}</span>
                      </div>
                      <div>
                        <span className="text-slate-400">负责人: </span>
                        <span className="text-slate-200 font-medium">
                          {dept.headUserName || <span className="text-slate-500">未设置</span>}
                        </span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
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
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => openPermissionModal(role)}
                          className="border-slate-600 text-slate-300 hover:bg-slate-700"
                        >
                          <ShieldCheck className="w-4 h-4 mr-1" />
                          编辑权限
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleDeleteRole(role)}
                          className="border-red-800 text-red-400 hover:bg-red-900/20"
                          title="删除权限组"
                        >
                          <Trash2 className="w-3 h-3" />
                          删除
                        </Button>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="flex flex-wrap gap-1">
                      {role.permissions.map(p => (
                        <Badge key={p.name} variant="outline" className={lightTagClassXs}>
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
          <div className="space-y-4">
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
                        <Badge key={role} variant="outline" className={lightTagClass}>
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
                          <Badge key={perm} variant="outline" className={lightTagClass}>
                            {perm}
                          </Badge>
                        ))}
                      </div>
                    ) : (
                      <p className="text-slate-500">暂无权限信息</p>
                    )}
                  </div>
                  <div className="pt-2">
                    <Button
                      variant="outline"
                      onClick={() => user && setShowPasswordModal({ userId: user.id!, username: user.username })}
                      className="border-slate-600 text-slate-300 hover:bg-slate-700"
                    >
                      <Key className="w-4 h-4 mr-2" />
                      修改密码
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
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
                          ? selectableTagActiveClass
                          : selectableTagClass
                      }`}
                    >
                      {role.name.replace('ROLE_', '')}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-2">所属部门（可多选）</label>
                <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto">
                  {departments.map(dept => (
                    <button
                      key={dept.id}
                      onClick={() => {
                        const ids = newUser.departmentIds.includes(dept.id)
                          ? newUser.departmentIds.filter(id => id !== dept.id)
                          : [...newUser.departmentIds, dept.id];
                        setNewUser({ ...newUser, departmentIds: ids });
                      }}
                      className={`px-3 py-1 rounded text-sm border ${
                        newUser.departmentIds.includes(dept.id)
                          ? 'bg-blue-100 border-blue-300 text-slate-900'
                          : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      {dept.deptName}
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
                        ? selectableTagActiveClass
                        : selectableTagClass
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
                          ? 'bg-blue-100 border-blue-300 text-slate-900'
                          : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-100'
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
                        <p className="text-xs text-slate-500">{perm.description}</p>
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

      {/* 部门详情弹窗 */}
      {showDeptDetail && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-2xl border border-slate-600 max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <Building2 className="w-5 h-5 text-blue-400" />
                <h3 className="text-lg font-semibold">{showDeptDetail.deptName}</h3>
                <Badge variant="outline" className="bg-slate-700 text-slate-300 border-slate-600 ml-2">
                  {showDeptDetail.deptCode}
                </Badge>
              </div>
              <button onClick={() => setShowDeptDetail(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* 部门信息 */}
            <div className="bg-slate-700/50 rounded p-3 mb-4 text-sm">
              <div className="flex gap-6">
                <div>
                  <span className="text-slate-400">类型: </span>
                  <span className="text-slate-200">{showDeptDetail.deptType}</span>
                </div>
                <div>
                  <span className="text-slate-400">成员数: </span>
                  <span className="text-slate-200 font-medium">{showDeptDetail.memberCount}</span>
                </div>
                <div>
                  <span className="text-slate-400">负责人: </span>
                  <span className="text-slate-200 font-medium">
                    {showDeptDetail.headUserName || <span className="text-slate-500">未设置</span>}
                  </span>
                </div>
              </div>
            </div>

            {/* 成员列表 */}
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-md font-semibold">部门成员</h4>
              <Button
                size="sm"
                variant="outline"
                onClick={openAddMember}
                className="border-green-700 text-green-300 hover:bg-green-900/30 text-xs"
              >
                <UserPlus className="w-3 h-3 mr-1" />
                添加成员
              </Button>
            </div>
            {loadingDeptMembers ? (
              <div className="text-center text-slate-400 py-8">加载中...</div>
            ) : deptMembers.length === 0 ? (
              <div className="text-center text-slate-500 py-8">该部门暂无成员</div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-700 text-slate-400">
                    <th className="text-left p-2">用户名</th>
                    <th className="text-left p-2">邮箱</th>
                    <th className="text-left p-2">角色</th>
                    <th className="text-right p-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {deptMembers.map(member => (
                    <tr key={member.id} className="border-b border-slate-700 hover:bg-slate-700/50">
                      <td className="p-2 font-medium">
                        {member.username}
                  {showDeptDetail.headUserId === member.id && (
                          <Badge variant="outline" className="ml-2 bg-slate-100 text-slate-700 border-slate-300 text-xs">
                            负责人
                          </Badge>
                        )}
                      </td>
                      <td className="p-2 text-slate-400">{member.email || '-'}</td>
                      <td className="p-2">
                        <div className="flex flex-wrap gap-1">
                          {member.roles.map(role => (
                            <Badge key={role} variant="outline" className={lightTagClassXs}>
                              {role.replace('ROLE_', '')}
                            </Badge>
                          ))}
                        </div>
                      </td>
                      <td className="p-2 text-right">
                        <div className="flex gap-1 justify-end">
                          {showDeptDetail.headUserId === member.id ? (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleAssignHead(showDeptDetail.id, null)}
                              className="border-red-700 text-red-300 hover:bg-red-900/30 text-xs"
                            >
                              取消负责人
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleAssignHead(showDeptDetail.id, member.id)}
                              className="border-slate-600 text-slate-300 hover:bg-slate-700 text-xs"
                            >
                              设为负责人
                            </Button>
                          )}
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleRemoveMember(member.id)}
                            className="border-red-800 text-red-400 hover:bg-red-900/20 text-xs"
                          >
                            <X className="w-3 h-3" />
                            移出
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* 修改部门名称弹窗 */}
      {showEditDept && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">修改部门名称 - {showEditDept.deptName}</h3>
              <button onClick={() => { setShowEditDept(null); setEditDeptName(''); }} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">部门名称</label>
                <Input
                  value={editDeptName}
                  onChange={e => setEditDeptName(e.target.value)}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入部门名称"
                />
              </div>
              <Button onClick={handleUpdateDepartment} className="w-full bg-blue-600 hover:bg-blue-700">
                <Save className="w-4 h-4 mr-2" />
                保存修改
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 添加成员弹窗 */}
      {showAddMember && showDeptDetail && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">添加成员 - {showDeptDetail.deptName}</h3>
              <button onClick={() => { setShowAddMember(false); setSelectedAddUserId(undefined); }} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-2">选择用户</label>
                {availableUsers.length === 0 ? (
                  <p className="text-slate-500 text-sm py-4 text-center">所有用户都已加入该部门</p>
                ) : (
                  <select
                    value={selectedAddUserId || ''}
                    onChange={e => setSelectedAddUserId(e.target.value ? Number(e.target.value) : undefined)}
                    className="w-full bg-slate-700 border border-slate-600 text-white rounded px-3 py-2 text-sm"
                  >
                    <option value="">请选择用户</option>
                    {availableUsers.map(u => (
                      <option key={u.id} value={u.id}>
                        {u.username} {u.email ? `(${u.email})` : ''} - {u.roles.map(r => r.replace('ROLE_', '')).join(', ')}
                      </option>
                    ))}
                  </select>
                )}
              </div>
              <Button
                onClick={handleAddMember}
                disabled={!selectedAddUserId}
                className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
              >
                <UserPlus className="w-4 h-4 mr-2" />
                确认添加
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AccountManagement;
