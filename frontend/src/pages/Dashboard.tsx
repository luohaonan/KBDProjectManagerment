import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { AlertTriangle, CheckCircle, Clock, ArrowRight, BarChart3, Download, Calendar, Plus, Trash2, X } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';

interface Project {
  id: number;
  projectCode: string;
  projectName: string;
  levelCode: string;
  levelName: string;
  status: string;
  lifecyclePhaseLabel: string;
  budgetExecutionSummary: {
    utilizationRatio: number | null;
  };
}

interface DashboardStats {
  inProgressProjects: number;
  pendingMilestoneReviews: number;
  budgetAlerts: number;
}

const stages = [
  'G0', 'G1', 'G2', 'G3', 'G4', 'G5', 'G6', 'G7', 'G8', 'G9'
];

const stageColors = [
  'bg-gray-400', 'bg-blue-400', 'bg-blue-500', 'bg-green-400', 'bg-green-500',
  'bg-yellow-400', 'bg-orange-400', 'bg-red-400', 'bg-purple-400', 'bg-indigo-400'
];

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [filterLevel, setFilterLevel] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [deleteConfirm, setDeleteConfirm] = useState<Project | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [projectsResponse, statsResponse] = await Promise.all([
        api.get('/api/projects'),
        api.get('/api/stats/dashboard')
      ]);

      setProjects(projectsResponse.data.data || []);
      setStats(statsResponse.data.data);
    } catch (error) {
      toast.error('加载仪表盘数据失败');
      console.error('Dashboard load error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewStats = () => {
    // Navigate to a stats page or show modal
    toast.info('统计功能开发中');
  };

  const handleCreateProject = () => {
    navigate('/create-project');
  };

  const handleExportReport = async () => {
    try {
      const response = await api.get('/api/projects/export', {
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'projects.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('报告导出成功');
    } catch (error) {
      toast.error('导出报告失败');
    }
  };

  const handleViewTimeline = (projectId: number) => {
    navigate(`/projects/${projectId}/timeline`);
  };

  const filteredProjects = filterLevel === 'all' ? projects : projects.filter(p => p.levelCode === filterLevel);

  const handleProjectClick = (projectId: number) => {
    navigate(`/project/${projectId}`);
  };

  const handleDeleteProject = async () => {
    if (!deleteConfirm) return;
    try {
      setDeleting(true);
      await api.delete(`/api/projects/${deleteConfirm.id}`);
      toast.success(`项目 "${deleteConfirm.projectName}" 已删除`);
      setDeleteConfirm(null);
      // 重新加载项目列表
      loadDashboardData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '删除项目失败');
    } finally {
      setDeleting(false);
    }
  };

  const canDeleteProject = hasPermission('PERMISSION_DELETE_PROJECT');

  const renderProjectCard = (project: Project) => {
    const currentStage = project.lifecyclePhaseLabel ? parseInt(project.lifecyclePhaseLabel.split('-')[0].substring(1), 10) || 0 : 0;
    const utilizationRatio = project.budgetExecutionSummary?.utilizationRatio ?? 0;

    return (
      <div
        key={project.id}
        className="mb-6 p-4 bg-slate-700 rounded cursor-pointer hover:bg-slate-600 transition"
        onClick={() => handleProjectClick(project.id)}
      >
        <div className="flex items-center justify-between mb-3">
          <div>
            <h3 className="text-lg font-semibold">{project.projectName}</h3>
            <p className="text-sm text-slate-400">ID: {project.projectCode}</p>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="bg-slate-600 text-slate-200">
              {project.levelCode}
            </Badge>
            <Button
              size="sm"
              variant="outline"
              onClick={(e) => {
                e.stopPropagation();
                handleViewTimeline(project.id);
              }}
              className="border-slate-500 text-slate-300 hover:bg-slate-600"
            >
              <Calendar className="w-3 h-3 mr-1" />
              时间表
            </Button>
            {canDeleteProject && (
              <Button
                size="sm"
                variant="outline"
                onClick={(e) => {
                  e.stopPropagation();
                  setDeleteConfirm(project);
                }}
                className="border-red-700 text-red-400 hover:bg-red-900/50 hover:text-red-300"
                title="删除项目"
              >
                <Trash2 className="w-3 h-3" />
              </Button>
            )}
            <ArrowRight className="w-5 h-5 text-slate-400" />
          </div>
        </div>

        <div className="flex space-x-2 mb-2">
          {stages.map((stage, index) => (
            <div
              key={stage}
              className={`flex-1 h-8 rounded ${index < currentStage ? stageColors[index] : 'bg-slate-600'} ${index === currentStage ? 'animate-pulse' : ''} flex items-center justify-center text-xs font-bold`}
              title={stage}
            >
              {stage}
            </div>
          ))}
        </div>

        <div className="text-sm text-slate-400">
          预算执行率: {(utilizationRatio ?? 0).toFixed(1)}%
        </div>
      </div>
    );
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
      {/* 顶部标题栏 - 标题居中 */}
      <div className="border-b border-slate-700 px-6 py-4">
        <div className="max-w-7xl mx-auto">
          <h1 className="text-3xl font-bold text-center">首页</h1>
        </div>
      </div>

      {/* 内容区域 - 使用最大宽度 */}
      <div className="mx-auto w-full max-w-7xl px-6 py-6">

        {/* 第一排：统计卡片 */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <Card className="bg-slate-800 border-slate-600">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-slate-400">进行中项目</p>
                    <p className="text-2xl font-bold text-blue-400">{stats.inProgressProjects}</p>
                  </div>
                  <Clock className="w-8 h-8 text-blue-400" />
                </div>
              </CardContent>
            </Card>

            <Card className="bg-slate-800 border-slate-600">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-slate-400">待评审里程碑</p>
                    <p className="text-2xl font-bold text-yellow-400">{stats.pendingMilestoneReviews}</p>
                  </div>
                  <CheckCircle className="w-8 h-8 text-yellow-400" />
                </div>
              </CardContent>
            </Card>

            <Card className="bg-slate-800 border-slate-600">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-slate-400">预算预警</p>
                    <p className="text-2xl font-bold text-red-400">{stats.budgetAlerts}</p>
                  </div>
                  <AlertTriangle className="w-8 h-8 text-red-400" />
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 第二排：预警中心 & 待办评审（与第一排等宽横向排列） */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          {/* 预警中心 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">预警中心 (Alert Center)</CardTitle>
            </CardHeader>
            <CardContent>
              {filteredProjects
                .filter(project => project.budgetExecutionSummary && (project.budgetExecutionSummary.utilizationRatio ?? 0) > 80)
                .map(project => {
                  const utilizationRatio = project.budgetExecutionSummary?.utilizationRatio ?? 0;
                  let alertIcon = null;
                  let alertColor = '';
                  if (utilizationRatio > 95) {
                    alertIcon = <AlertTriangle className="w-5 h-5 text-red-500 animate-pulse" />;
                    alertColor = 'text-red-500';
                  } else if (utilizationRatio > 80) {
                    alertIcon = <AlertTriangle className="w-5 h-5 text-yellow-500" />;
                    alertColor = 'text-yellow-500';
                  }
                  return (
                    <div key={project.id} className="flex items-center justify-between mb-3 p-2 bg-slate-700 rounded">
                      <span className="text-sm truncate">{project.projectName}</span>
                      <div className="flex items-center">
                        <span className={`text-sm mr-2 ${alertColor}`}>
                          {utilizationRatio.toFixed(1)}%
                        </span>
                        {alertIcon}
                      </div>
                    </div>
                  );
                })}
              {filteredProjects.filter(project => project.budgetExecutionSummary && (project.budgetExecutionSummary.utilizationRatio ?? 0) > 80).length === 0 && (
                <p className="text-slate-400">暂无预算预警</p>
              )}
            </CardContent>
          </Card>

          {/* 待办评审 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">待办评审</CardTitle>
            </CardHeader>
            <CardContent>
              {stats && stats.pendingMilestoneReviews > 0 ? (
                <div className="mb-3 p-3 bg-slate-700 rounded">
                  <div className="flex items-center mb-1">
                    <Clock className="w-4 h-4 mr-2 text-blue-400" />
                    <span className="text-sm font-semibold">待评审里程碑</span>
                  </div>
                  <p className="text-xs text-slate-400">共 {stats.pendingMilestoneReviews} 个待评审</p>
                </div>
              ) : (
                <p className="text-slate-400">暂无待办评审</p>
              )}
            </CardContent>
          </Card>
        </div>

        {/* 第三排：筛选/操作按钮 & 项目管道 */}
        <div className="grid grid-cols-1 gap-4">
          {/* 操作按钮右对齐 */}
          <div className="flex items-center justify-end gap-3 flex-wrap">
            <Select value={filterLevel} onValueChange={setFilterLevel}>
              <SelectTrigger className="w-40 bg-slate-800 border-slate-600 text-slate-200">
                <SelectValue placeholder="选择项目分级" />
              </SelectTrigger>
              <SelectContent className="bg-slate-800 border-slate-600">
                <SelectItem value="all">全部</SelectItem>
                <SelectItem value="H-L">H-L</SelectItem>
                <SelectItem value="G-L">G-L</SelectItem>
                <SelectItem value="H-Q">H-Q</SelectItem>
                <SelectItem value="G-Q">G-Q</SelectItem>
                <SelectItem value="G-T">G-T</SelectItem>
                <SelectItem value="C-L">C-L</SelectItem>
                <SelectItem value="C-Q">C-Q</SelectItem>
              </SelectContent>
            </Select>
            <Button onClick={handleCreateProject} className="bg-green-600 hover:bg-green-700">
              <Plus className="w-4 h-4 mr-2" />
              创建新项目
            </Button>
            <Button onClick={handleViewStats} className="bg-blue-600 hover:bg-blue-700">
              <BarChart3 className="w-4 h-4 mr-2" />
              查看统计
            </Button>
            <Button onClick={handleExportReport} variant="outline" className="border-slate-600 text-slate-300 hover:bg-slate-700">
              <Download className="w-4 h-4 mr-2" />
              导出报告
            </Button>
          </div>

          {/* 项目管道 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">项目管道 (Project Pipeline)</CardTitle>
            </CardHeader>
            <CardContent>
              {filteredProjects.map(project => renderProjectCard(project))}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* 删除确认弹窗 */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-red-400">确认删除项目</h3>
              <button onClick={() => setDeleteConfirm(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <p className="text-slate-300">
                确定要删除项目 <span className="font-semibold text-white">{deleteConfirm.projectName}</span> 吗？
              </p>
              <p className="text-sm text-slate-400">
                项目编号: {deleteConfirm.projectCode}
              </p>
              <p className="text-sm text-yellow-400">
                此操作不可撤销，删除后所有相关数据将被永久移除。
              </p>
              <div className="flex gap-3 justify-end">
                <Button
                  variant="outline"
                  onClick={() => setDeleteConfirm(null)}
                  className="border-slate-600 text-slate-300 hover:bg-slate-700"
                  disabled={deleting}
                >
                  取消
                </Button>
                <Button
                  onClick={handleDeleteProject}
                  className="bg-red-600 hover:bg-red-700"
                  disabled={deleting}
                >
                  {deleting ? '删除中...' : '确认删除'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
