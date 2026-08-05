import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { useAuth } from '../contexts/AuthContext';
import api from '../lib/api';
import { toast } from 'sonner';
import { UserPlus, Key, Shield, ShieldCheck, ArrowLeft, Save, X, Building2, UserCog, Trash2, Edit3, Plus } from 'lucide-react';

const lightTagClass = 'bg-slate-100 text-slate-700 border-slate-300';
const lightTagClassXs = 'bg-slate-100 text-slate-700 border-slate-300 text-xs';
const selectableTagClass = 'bg-white border-slate-300 text-slate-700 hover:bg-slate-100';
const selectableTagActiveClass = 'bg-blue-100 border-blue-300 text-slate-900';
const managementTabClass = 'rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none hover:bg-slate-100';
const managementTabActiveClass = 'bg-blue-100 border-blue-300 text-slate-900';

type PermissionRisk = 'high' | 'medium' | 'workflow' | 'weak' | 'normal';

interface PermissionMeta {
  module: string;
  risk: PermissionRisk;
  riskLabel: string;
  note: string;
}

const DEFAULT_PERMISSION_META: PermissionMeta = {
  module: '其他 / 未分类',
  risk: 'normal',
  riskLabel: '普通',
  note: '当前未配置专项说明，请结合业务测试确认实际影响。',
};

const PERMISSION_META_MAP: Record<string, PermissionMeta> = {
  PERMISSION_DELETE_PROJECT: {
    module: '项目管理',
    risk: 'high',
    riskLabel: '高风险',
    note: '控制项目删除能力，删除项目会级联影响评审、立项、预算、交付物、通知等业务数据。',
  },
  PERMISSION_CREATE_PROJECT: {
    module: '项目管理',
    risk: 'weak',
    riskLabel: '弱生效',
    note: '当前创建入口主要由 ROLE_ADMIN / ROLE_PROJECT_ADMIN 控制，本权限疑似未完全接入后端校验。',
  },
  PERMISSION_VIEW_ALL_PROJECTS: {
    module: '项目管理',
    risk: 'weak',
    riskLabel: '弱生效',
    note: '当前项目可见性主要由角色和业务逻辑控制，本权限疑似未完全接入后端校验。',
  },
  PERMISSION_BUDGET_VIEW: {
    module: '预算管理',
    risk: 'weak',
    riskLabel: '弱生效',
    note: '当前预算查看还受到项目 PM、审批人、申请人等业务身份限制。',
  },
  PERMISSION_BUDGET_MANAGE: {
    module: '预算管理',
    risk: 'high',
    riskLabel: '高风险',
    note: '控制预算调整和支出管理，但仍需满足项目 PM / 项目管理员等业务身份条件。',
  },
  PERMISSION_DELIVERABLE_VIEW: {
    module: '交付物管理',
    risk: 'weak',
    riskLabel: '弱生效',
    note: '当前疑似基础查看/预留权限，实际查看范围还受到角色、上传者、部门、项目关系限制。',
  },
  PERMISSION_DELIVERABLE_VIEW_ALL: {
    module: '交付物管理',
    risk: 'medium',
    riskLabel: '中高风险',
    note: '控制项目交付物全量查看能力，同时兼容管理员、项目管理员、PMC 等角色。',
  },
  PERMISSION_DELIVERABLE_IMPORT: {
    module: '交付物管理',
    risk: 'high',
    riskLabel: '高风险',
    note: '控制历史交付物导入能力，会影响项目资料完整性和历史文件归档。',
  },
  PERMISSION_APPROVE_INITIATION: {
    module: '审批流程',
    risk: 'workflow',
    riskLabel: '流程关键',
    note: '用于立项审批人候选分配，修改后可能影响立项审批任务能否正常生成。',
  },
};

const PERMISSION_MODULE_ORDER = ['项目管理', '预算管理', '交付物管理', '审批流程', '账号与组织', '系统配置', '其他 / 未分类'];

const getPermissionMeta = (permissionName: string): PermissionMeta => {
  return PERMISSION_META_MAP[permissionName] || DEFAULT_PERMISSION_META;
};

const getRiskBadgeClass = (risk: PermissionRisk) => {
  switch (risk) {
    case 'high':
      return 'bg-red-100 text-red-700 border-red-300';
    case 'medium':
      return 'bg-orange-100 text-orange-700 border-orange-300';
    case 'workflow':
      return 'bg-purple-100 text-purple-700 border-purple-300';
    case 'weak':
      return 'bg-amber-100 text-amber-700 border-amber-300';
    default:
      return 'bg-slate-100 text-slate-700 border-slate-300';
  }
};

const groupPermissionsByModule = (items: PermissionInfo[]) => {
  const groups = items.reduce<Record<string, PermissionInfo[]>>((acc, permission) => {
    const moduleName = getPermissionMeta(permission.name).module;
    if (!acc[moduleName]) acc[moduleName] = [];
    acc[moduleName].push(permission);
    return acc;
  }, {});

  return Object.entries(groups).sort(([a], [b]) => {
    const indexA = PERMISSION_MODULE_ORDER.indexOf(a);
    const indexB = PERMISSION_MODULE_ORDER.indexOf(b);
    return (indexA === -1 ? 999 : indexA) - (indexB === -1 ? 999 : indexB);
  });
};

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

interface EditUserForm {
  userId: number;
  originalUsername: string;
  username: string;
  email: string;
  roles: string[];
  departmentIds: number[];
}

interface PermissionChangePreview {
  added: PermissionInfo[];
  removed: PermissionInfo[];
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
  const [showEditUser, setShowEditUser] = useState<EditUserForm | null>(null);
  const [showPasswordModal, setShowPasswordModal] = useState<{ userId: number; username: string } | null>(null);
  const [showRoleModal, setShowRoleModal] = useState<{ userId: number; username: string; currentRoles: string[] } | null>(null);
  const [showPermissionModal, setShowPermissionModal] = useState<RoleInfo | null>(null);
  const [permissionChangePreview, setPermissionChangePreview] = useState<PermissionChangePreview | null>(null);
  const [showDeptDetail, setShowDeptDetail] = useState<DepartmentInfo | null>(null);
  const [deptMembers, setDeptMembers] = useState<UserInfo[]>([]);
  const [loadingDeptMembers, setLoadingDeptMembers] = useState(false);
  const [showAddMember, setShowAddMember] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<UserInfo[]>([]);
  const [selectedAddUserId, setSelectedAddUserId] = useState<number | undefined>(undefined);
  const [showEditDept, setShowEditDept] = useState<DepartmentInfo | null>(null);
  const [editDeptName, setEditDeptName] = useState('');
  const [showCreateDept, setShowCreateDept] = useState(false);
  const [newDeptName, setNewDeptName] = useState('');
  const [newDeptType, setNewDeptType] = useState('PDT');
  const [creatingDept, setCreatingDept] = useState(false);

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

  const handleUpdateUser = async () => {
    if (!showEditUser) return;
    if (!showEditUser.username.trim()) {
      toast.error('用户名不能为空');
      return;
    }
    try {
      await api.put(`/api/users/${showEditUser.userId}`, {
        username: showEditUser.username.trim(),
        email: showEditUser.email.trim(),
        roles: showEditUser.roles,
        departmentIds: showEditUser.departmentIds,
      });
      toast.success('账号信息更新成功');
      setShowEditUser(null);
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '更新账号信息失败');
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
      setPermissionChangePreview(null);
      setShowPermissionModal(null);
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '更新权限失败');
    }
  };

  const getPermissionInfoByName = (name: string): PermissionInfo => {
    return permissions.find(p => p.name === name) || { id: 0, name, description: '' };
  };

  const buildPermissionChangePreview = (): PermissionChangePreview => {
    if (!showPermissionModal) return { added: [], removed: [] };

    const originalNames = new Set(showPermissionModal.permissions.map(p => p.name));
    const selectedNames = new Set(selectedPermissions);

    const added = selectedPermissions
      .filter(name => !originalNames.has(name))
      .map(getPermissionInfoByName);

    const removed = showPermissionModal.permissions
      .map(p => p.name)
      .filter(name => !selectedNames.has(name))
      .map(getPermissionInfoByName);

    return { added, removed };
  };

  const handlePreviewPermissionChanges = () => {
    const preview = buildPermissionChangePreview();
    if (preview.added.length === 0 && preview.removed.length === 0) {
      toast.info('权限没有变化，无需保存');
      return;
    }
    setPermissionChangePreview(preview);
  };

  const closePermissionModal = () => {
    setPermissionChangePreview(null);
    setShowPermissionModal(null);
  };

  const openRoleModal = (u: UserInfo) => {
    setSelectedRoles(u.roles);
    setShowRoleModal({ userId: u.id, username: u.username, currentRoles: u.roles });
  };

  const openEditUserModal = (u: UserInfo) => {
    setShowEditUser({
      userId: u.id,
      originalUsername: u.username,
      username: u.username,
      email: u.email || '',
      roles: u.roles || [],
      departmentIds: u.departmentIds || [],
    });
  };

  const openPermissionModal = (r: RoleInfo) => {
    setSelectedPermissions(r.permissions.map(p => p.name));
    setPermissionChangePreview(null);
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

  const handleCreateDepartment = async () => {
    if (!newDeptName.trim()) {
      toast.error('部门名称不能为空');
      return;
    }
    setCreatingDept(true);
    try {
      await api.post('/api/departments', {
        deptName: newDeptName.trim(),
        deptType: newDeptType,
      });
      toast.success('部门创建成功');
      setShowCreateDept(false);
      setNewDeptName('');
      setNewDeptType('PDT');
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '创建部门失败');
    } finally {
      setCreatingDept(false);
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
                              onClick={() => openEditUserModal(u)}
                              className="border-slate-600 text-slate-300 hover:bg-slate-700"
                              title="修改账号信息"
                            >
                              <Edit3 className="w-3 h-3" />
                            </Button>
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
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">部门管理</h2>
              <Button onClick={() => setShowCreateDept(true)} className="bg-green-600 hover:bg-green-700">
                <Plus className="w-4 h-4 mr-2" />
                新建部门
              </Button>
            </div>
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
                    <div className="space-y-3">
                      {role.permissions.length === 0 ? (
                        <p className="text-sm text-slate-500">该权限组暂未分配权限</p>
                      ) : (
                        groupPermissionsByModule(role.permissions).map(([moduleName, modulePermissions]) => (
                          <div key={moduleName} className="rounded-md border border-slate-700 bg-slate-900/40 p-3">
                            <div className="mb-2 flex items-center justify-between gap-2">
                              <p className="text-sm font-medium text-slate-200">{moduleName}</p>
                              <Badge variant="outline" className="bg-slate-700 text-slate-300 border-slate-600 text-xs">
                                {modulePermissions.length} 项
                              </Badge>
                            </div>
                            <div className="flex flex-wrap gap-2">
                              {modulePermissions.map(p => {
                                const meta = getPermissionMeta(p.name);
                                return (
                                  <div key={p.name} className="rounded border border-slate-700 bg-slate-800 px-2 py-1">
                                    <div className="flex flex-wrap items-center gap-1">
                                      <span className="text-xs font-medium text-slate-100">{p.name}</span>
                                      <Badge variant="outline" className={`${getRiskBadgeClass(meta.risk)} text-[10px] px-1 py-0`}>
                                        {meta.riskLabel}
                                      </Badge>
                                    </div>
                                    <p className="mt-1 max-w-xl text-[11px] text-slate-400">{meta.note}</p>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        ))
                      )}
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

      {/* 修改账号信息弹窗 */}
      {showEditUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">修改账号信息 - {showEditUser.originalUsername}</h3>
              <button onClick={() => setShowEditUser(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">用户名 *</label>
                <Input
                  value={showEditUser.username}
                  onChange={e => setShowEditUser({ ...showEditUser, username: e.target.value })}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入用户名"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">邮箱</label>
                <Input
                  value={showEditUser.email}
                  onChange={e => setShowEditUser({ ...showEditUser, email: e.target.value })}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入邮箱（可选）"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-2">角色（可多选）</label>
                <div className="flex flex-wrap gap-2">
                  {roles.map(role => (
                    <button
                      key={role.name}
                      type="button"
                      onClick={() => {
                        const nextRoles = showEditUser.roles.includes(role.name)
                          ? showEditUser.roles.filter(r => r !== role.name)
                          : [...showEditUser.roles, role.name];
                        setShowEditUser({ ...showEditUser, roles: nextRoles });
                      }}
                      className={`px-3 py-1 rounded text-sm border ${
                        showEditUser.roles.includes(role.name)
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
                        const departmentIds = showEditUser.departmentIds.includes(dept.id)
                          ? showEditUser.departmentIds.filter(id => id !== dept.id)
                          : [...showEditUser.departmentIds, dept.id];
                        setShowEditUser({ ...showEditUser, departmentIds });
                      }}
                      className={`px-3 py-1 rounded text-sm border ${
                        showEditUser.departmentIds.includes(dept.id)
                          ? 'bg-blue-100 border-blue-300 text-slate-900'
                          : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      {dept.deptName}
                    </button>
                  ))}
                </div>
              </div>
              <Button onClick={handleUpdateUser} className="w-full bg-blue-600 hover:bg-blue-700">
                <Save className="w-4 h-4 mr-2" />
                保存修改
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
              <button onClick={closePermissionModal} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-3">
              <div className="rounded-md border border-amber-700/60 bg-amber-950/30 p-3 text-sm text-amber-100">
                <p className="font-medium">安全提示</p>
                <p className="mt-1 text-xs text-amber-200/90">
                  当前保存会覆盖该权限组的全部权限。高风险、流程关键、弱生效权限请结合业务测试确认后再调整。
                </p>
              </div>
              <p className="text-sm text-slate-400 mb-2">按模块勾选需要分配给该角色的权限：</p>
              <div className="space-y-4">
                {groupPermissionsByModule(permissions).map(([moduleName, modulePermissions]) => (
                  <div key={moduleName} className="rounded-lg border border-slate-600 bg-slate-900/50 p-3">
                    <div className="mb-3 flex items-center justify-between gap-2">
                      <h4 className="text-sm font-semibold text-slate-100">{moduleName}</h4>
                      <Badge variant="outline" className="bg-slate-700 text-slate-300 border-slate-600 text-xs">
                        {modulePermissions.filter(perm => selectedPermissions.includes(perm.name)).length}/{modulePermissions.length}
                      </Badge>
                    </div>
                    <div className="grid grid-cols-1 gap-2">
                      {modulePermissions.map(perm => {
                        const meta = getPermissionMeta(perm.name);
                        const checked = selectedPermissions.includes(perm.name);
                        return (
                          <label
                            key={perm.name}
                            className={`flex items-start gap-3 p-3 rounded border cursor-pointer transition ${
                              checked
                                ? 'bg-blue-100 border-blue-300 text-slate-900'
                                : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-100'
                            }`}
                          >
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() => {
                                setSelectedPermissions(prev =>
                                  prev.includes(perm.name)
                                    ? prev.filter(p => p !== perm.name)
                                    : [...prev, perm.name]
                                );
                              }}
                              className="mt-1 w-4 h-4 accent-blue-600"
                            />
                            <div className="min-w-0 flex-1">
                              <div className="flex flex-wrap items-center gap-2">
                                <p className="text-sm font-medium break-all">{perm.name}</p>
                                <Badge variant="outline" className={`${getRiskBadgeClass(meta.risk)} text-[10px] px-1.5 py-0`}>
                                  {meta.riskLabel}
                                </Badge>
                              </div>
                              <p className="mt-1 text-xs text-slate-500">{perm.description}</p>
                              <p className="mt-1 text-xs text-slate-500">{meta.note}</p>
                            </div>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
              <Button onClick={handlePreviewPermissionChanges} className="w-full bg-blue-600 hover:bg-blue-700 mt-4">
                <Save className="w-4 h-4 mr-2" />
                预览变更并保存
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 权限变更 Diff 确认弹窗 */}
      {showPermissionModal && permissionChangePreview && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-[60]">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-3xl border border-slate-600 max-h-[82vh] overflow-y-auto shadow-2xl">
            <div className="flex justify-between items-start gap-4 mb-4">
              <div>
                <h3 className="text-lg font-semibold text-slate-100">
                  确认权限变更 - {showPermissionModal.name.replace('ROLE_', '')}
                </h3>
                <p className="mt-1 text-sm text-slate-400">
                  请确认以下新增和移除权限。确认后会覆盖该权限组的全部权限配置。
                </p>
              </div>
              <button onClick={() => setPermissionChangePreview(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="rounded-lg border border-green-700/60 bg-green-950/20 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <h4 className="font-semibold text-green-200">新增权限</h4>
                  <Badge variant="outline" className="bg-green-100 text-green-700 border-green-300 text-xs">
                    +{permissionChangePreview.added.length}
                  </Badge>
                </div>
                {permissionChangePreview.added.length === 0 ? (
                  <p className="text-sm text-slate-500">无新增权限</p>
                ) : (
                  <div className="space-y-3">
                    {groupPermissionsByModule(permissionChangePreview.added).map(([moduleName, modulePermissions]) => (
                      <div key={moduleName}>
                        <p className="mb-2 text-xs font-medium text-slate-300">{moduleName}</p>
                        <div className="space-y-2">
                          {modulePermissions.map(perm => {
                            const meta = getPermissionMeta(perm.name);
                            return (
                              <div key={perm.name} className="rounded border border-slate-700 bg-slate-900/70 p-2">
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className="text-sm font-medium text-slate-100 break-all">{perm.name}</span>
                                  <Badge variant="outline" className={`${getRiskBadgeClass(meta.risk)} text-[10px] px-1.5 py-0`}>
                                    {meta.riskLabel}
                                  </Badge>
                                </div>
                                <p className="mt-1 text-xs text-slate-400">{meta.note}</p>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="rounded-lg border border-red-700/60 bg-red-950/20 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <h4 className="font-semibold text-red-200">移除权限</h4>
                  <Badge variant="outline" className="bg-red-100 text-red-700 border-red-300 text-xs">
                    -{permissionChangePreview.removed.length}
                  </Badge>
                </div>
                {permissionChangePreview.removed.length === 0 ? (
                  <p className="text-sm text-slate-500">无移除权限</p>
                ) : (
                  <div className="space-y-3">
                    {groupPermissionsByModule(permissionChangePreview.removed).map(([moduleName, modulePermissions]) => (
                      <div key={moduleName}>
                        <p className="mb-2 text-xs font-medium text-slate-300">{moduleName}</p>
                        <div className="space-y-2">
                          {modulePermissions.map(perm => {
                            const meta = getPermissionMeta(perm.name);
                            return (
                              <div key={perm.name} className="rounded border border-slate-700 bg-slate-900/70 p-2">
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className="text-sm font-medium text-slate-100 break-all">{perm.name}</span>
                                  <Badge variant="outline" className={`${getRiskBadgeClass(meta.risk)} text-[10px] px-1.5 py-0`}>
                                    {meta.riskLabel}
                                  </Badge>
                                </div>
                                <p className="mt-1 text-xs text-slate-400">{meta.note}</p>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="mt-4 rounded-md border border-amber-700/60 bg-amber-950/30 p-3 text-xs text-amber-100">
              如果移除了“高风险”或“流程关键”权限，可能导致按钮仍可见但业务流程失败、审批任务无法分配或关键操作被拒绝。
            </div>

            <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button
                variant="outline"
                onClick={() => setPermissionChangePreview(null)}
                className="border-slate-600 text-slate-300 hover:bg-slate-700"
              >
                返回继续修改
              </Button>
              <Button onClick={handleUpdatePermissions} className="bg-blue-600 hover:bg-blue-700">
                <Save className="w-4 h-4 mr-2" />
                确认保存变更
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

      {/* 新建部门弹窗 */}
      {showCreateDept && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold"><Building2 className="w-5 h-5 text-blue-400 inline mr-2" />新建部门</h3>
              <button onClick={() => { setShowCreateDept(false); setNewDeptName(''); }} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">部门名称 *</label>
                <Input
                  value={newDeptName}
                  onChange={e => setNewDeptName(e.target.value)}
                  className="bg-slate-700 border-slate-600 text-white"
                  placeholder="请输入部门名称"
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">部门类型</label>
                <select
                  value={newDeptType}
                  onChange={e => setNewDeptType(e.target.value)}
                  className="w-full bg-slate-700 border border-slate-600 text-white rounded px-3 py-2 text-sm"
                >
                  <option value="PDT">PDT（产品开发团队）</option>
                  <option value="ROSS">ROSS（研发支持服务）</option>
                  <option value="OTHER">OTHER（其他）</option>
                </select>
              </div>
              <Button onClick={handleCreateDepartment} disabled={creatingDept} className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50">
                <Save className="w-4 h-4 mr-2" />
                {creatingDept ? '创建中...' : '创建部门'}
              </Button>
            </div>
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
