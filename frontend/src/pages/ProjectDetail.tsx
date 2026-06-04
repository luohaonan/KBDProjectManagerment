import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { MilestoneConsole } from '../components/MilestoneConsole';
import { DocumentList } from '../components/DocumentList';
import { BudgetTracker } from '../components/BudgetTracker';
import { ChangeRequestForm } from '../components/ChangeRequestForm';
import { ChevronLeft, FileText, CheckCircle, AlertCircle, Loader2, X, Calendar, ClipboardList } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';

interface ProjectData {
  id: number;
  projectCode: string;
  projectName: string;
  levelCode: string;
  levelName: string;
  indication: string;
  targetPathway: string;
  tppSummary: string;
  description: string | null;
  mechanism: string | null;
  unmetNeeds: string | null;
  scientificBasis: string | null;
  expectedIndication: string | null;
  administrationRoute: string | null;
  dosageForm: string | null;
  dosageFrequency: string | null;
  efficacyTarget: string | null;
  safetyAdvantage: string | null;
  differentiation: string | null;
  budgetTotal: number | null;
  plannedPccDate: string | null;
  plannedIndDate: string | null;
  plannedNdaDate: string | null;
  plannedEndDate: string | null;
  budgetToPcc: number | null;
  riskScientific: string | null;
  riskCompetitive: string | null;
  riskRegulatory: string | null;
  suggestionAndSupport: string | null;
  pmUserId: number;
  projectStatus: string;
  lifecyclePhaseLabel: string | null;
  reviewStatus?: string;
  initiationStatus?: string | null;
  processOversightDept: {
    deptId: number;
    deptCode: string;
    deptName: string;
  } | null;
  currentMilestone: {
    milestoneCode: string;
    milestoneName: string;
    phaseLabel: string;
  } | null;
  budgetExecution: {
    plannedTotalAmount: number | null;
    totalSpent: number | null;
    utilizationRatio: number | null;
    warningLevel: string | null;
    snapshotMonth: string | null;
  } | null;
}

const ProjectDetail: React.FC = () => {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { user, hasRole, hasPermission } = useAuth();
  const [project, setProject] = useState<ProjectData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  // 立项申请相关状态
  const [showInitiation, setShowInitiation] = useState(false);
  const [initiationData, setInitiationData] = useState<any>(null);
  const [initiationLoading, setInitiationLoading] = useState(false);
  const [applicationContent, setApplicationContent] = useState('');
  const [submittingInitiation, setSubmittingInitiation] = useState(false);
  const [approvalOpinion, setApprovalOpinion] = useState('');
  const [approvingTaskId, setApprovingTaskId] = useState<number | null>(null);
  const [approvingDecision, setApprovingDecision] = useState<string>('');
  const sameUserId = (a?: number | null, b?: number | null) =>
    a != null && b != null && Number(a) === Number(b);
  const canApproveInitiation = hasPermission('PERMISSION_APPROVE_INITIATION');
  const myPendingInitiationTask = initiationData?.tasks?.find(
    (t: { approverUserId: number; status: string }) =>
      sameUserId(t.approverUserId, user?.id) && t.status === 'PENDING'
  );
  const isInitiationApproved = project?.initiationStatus === 'APPROVED';
  const canUploadDeliverables =
    user?.department === '新药资讯部' && user?.username === 'efficiency_user';
  const [editForm, setEditForm] = useState({
    projectName: '',
    levelCode: '',
    indication: '',
    targetPathway: '',
    tppSummary: '',
    description: '',
    mechanism: '',
    unmetNeeds: '',
    scientificBasis: '',
    expectedIndication: '',
    administrationRoute: '',
    dosageForm: '',
    dosageFrequency: '',
    efficacyTarget: '',
    safetyAdvantage: '',
    differentiation: '',
    budgetTotal: 0,
    plannedPccDate: '',
    plannedIndDate: '',
    plannedNdaDate: '',
    plannedEndDate: '',
    budgetToPcc: 0,
    riskScientific: '',
    riskCompetitive: '',
    riskRegulatory: '',
    suggestionAndSupport: '',
  });

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    api.get(`/api/projects/${projectId}`)
      .then((res) => {
        // 后端返回 Result<ProjectDetailResponse> 结构
        const data = res.data as { code: number; data: ProjectData; message?: string };
        if (data.code === 200 || data.code === 0) {
          setProject(data.data);
        } else {
          setError(data.message || '获取项目详情失败');
        }
      })
      .catch((err) => {
        setError(err.response?.data?.message || err.message || '网络错误');
      })
      .finally(() => setLoading(false));
  }, [projectId]);

  const openEditDialog = () => {
    if (!project) return;
    setEditForm({
      projectName: project.projectName,
      levelCode: project.levelCode,
      indication: project.indication,
      targetPathway: project.targetPathway,
      tppSummary: project.tppSummary,
      description: project.description || '',
      mechanism: project.mechanism || '',
      unmetNeeds: project.unmetNeeds || '',
      scientificBasis: project.scientificBasis || '',
      expectedIndication: project.expectedIndication || '',
      administrationRoute: project.administrationRoute || '',
      dosageForm: project.dosageForm || '',
      dosageFrequency: project.dosageFrequency || '',
      efficacyTarget: project.efficacyTarget || '',
      safetyAdvantage: project.safetyAdvantage || '',
      differentiation: project.differentiation || '',
      // 数据库存储单位为元，编辑表单单位为万元，需转换
      budgetTotal: project.budgetTotal ? project.budgetTotal / 10000 : 0,
      plannedPccDate: project.plannedPccDate || '',
      plannedIndDate: project.plannedIndDate || '',
      plannedNdaDate: project.plannedNdaDate || '',
      plannedEndDate: project.plannedEndDate || '',
      // 数据库存储单位为元，编辑表单单位为万元，需转换
      budgetToPcc: project.budgetToPcc ? project.budgetToPcc / 10000 : 0,
      riskScientific: project.riskScientific || '',
      riskCompetitive: project.riskCompetitive || '',
      riskRegulatory: project.riskRegulatory || '',
      suggestionAndSupport: project.suggestionAndSupport || '',
    });
    setEditing(true);
  };

  const handleEditChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    setEditForm(prev => ({ ...prev, [name]: type === 'number' ? Number(value) : value }));
  };

  const handleEditSelectChange = (value: string) => {
    setEditForm(prev => ({ ...prev, levelCode: value }));
  };

  const handleEditSelectChangeGeneric = (name: string, value: string) => {
    setEditForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSaveEdit = async () => {
    if (!project || !projectId) return;
    setSaving(true);
    try {
      // 编辑表单单位为万元，后端数据库存储单位为元，需转换
      const budgetTotalInYuan = editForm.budgetTotal ? editForm.budgetTotal * 10000 : null;
      const budgetToPccInYuan = editForm.budgetToPcc ? editForm.budgetToPcc * 10000 : null;

      await api.put(`/api/projects/${projectId}`, {
        projectName: editForm.projectName,
        levelCode: editForm.levelCode,
        indication: editForm.indication,
        targetPathway: editForm.targetPathway,
        tppSummary: editForm.tppSummary,
        description: editForm.description || null,
        mechanism: editForm.mechanism || null,
        unmetNeeds: editForm.unmetNeeds || null,
        scientificBasis: editForm.scientificBasis || null,
        expectedIndication: editForm.expectedIndication || null,
        administrationRoute: editForm.administrationRoute || null,
        dosageForm: editForm.dosageForm || null,
        dosageFrequency: editForm.dosageFrequency || null,
        efficacyTarget: editForm.efficacyTarget || null,
        safetyAdvantage: editForm.safetyAdvantage || null,
        differentiation: editForm.differentiation || null,
        budgetTotal: budgetTotalInYuan,
        plannedPccDate: editForm.plannedPccDate || null,
        plannedIndDate: editForm.plannedIndDate || null,
        plannedNdaDate: editForm.plannedNdaDate || null,
        plannedEndDate: editForm.plannedEndDate || null,
        budgetToPcc: budgetToPccInYuan,
        riskScientific: editForm.riskScientific || null,
        riskCompetitive: editForm.riskCompetitive || null,
        riskRegulatory: editForm.riskRegulatory || null,
        suggestionAndSupport: editForm.suggestionAndSupport || null,
      });
      toast.success('项目信息更新成功！');
      setEditing(false);
      // 重新加载项目详情
      const res = await api.get(`/api/projects/${projectId}`);
      const data = res.data as { code: number; data: ProjectData; message?: string };
      if (data.code === 200 || data.code === 0) {
        setProject(data.data);
      }
    } catch (error) {
      toast.error('更新失败，请重试');
      console.error('Update project error:', error);
    } finally {
      setSaving(false);
    }
  };

  // ==================== 立项申请相关函数 ====================

  const openInitiationDialog = async () => {
    if (!projectId) return;
    setShowInitiation(true);
    setInitiationLoading(true);
    try {
      const res = await api.get(`/api/initiations/${projectId}`);
      const data = res.data as { code: number; data: any; message?: string };
      if (data.code === 200 || data.code === 0) {
        setInitiationData(data.data);
        if (data.data?.applicationContent) {
          setApplicationContent(data.data.applicationContent);
        }
      } else {
        // 没有立项申请记录，清空
        setInitiationData(null);
        setApplicationContent('');
      }
    } catch {
      setInitiationData(null);
      setApplicationContent('');
    } finally {
      setInitiationLoading(false);
    }
  };

  const handleSubmitInitiation = async () => {
    if (!projectId || !user?.id) return;
    if (!applicationContent.trim()) {
      toast.error('请填写立项申请信息');
      return;
    }
    setSubmittingInitiation(true);
    try {
      const res = await api.post(`/api/initiations/${projectId}/submit`, {
        actorUserId: user.id,
        applicationContent: applicationContent.trim(),
      });
      const data = res.data as { code: number; data: any; message?: string };
      if (data.code === 200 || data.code === 0) {
        toast.success('立项申请已提交！');
        setInitiationData(data.data);
        // 刷新项目数据
        const projRes = await api.get(`/api/projects/${projectId}`);
        const projData = projRes.data as { code: number; data: ProjectData; message?: string };
        if (projData.code === 200 || projData.code === 0) {
          setProject(projData.data);
        }
      } else {
        toast.error(data.message || '提交失败');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || err.message || '提交失败');
    } finally {
      setSubmittingInitiation(false);
    }
  };

  const handleInitiationDecision = async (taskId: number, decision: string) => {
    if (!projectId || !user?.id) return;
    if (!approvalOpinion.trim()) {
      toast.error('请填写审批意见');
      return;
    }
    setApprovingTaskId(taskId);
    setApprovingDecision(decision);
    try {
      const res = await api.post(`/api/initiations/${projectId}/tasks/${taskId}/decision`, {
        actorUserId: user.id,
        decision,
        opinion: approvalOpinion.trim(),
      });
      const data = res.data as { code: number; data: any; message?: string };
      if (data.code === 200 || data.code === 0) {
        toast.success(decision === 'APPROVED' ? '已同意立项申请！' : '已拒绝立项申请！');
        setInitiationData(data.data);
        setApprovalOpinion('');
        const projRes = await api.get(`/api/projects/${projectId}`);
        const projData = projRes.data as { code: number; data: ProjectData; message?: string };
        if (projData.code === 200 || projData.code === 0) {
          setProject(projData.data);
        } else if (data.data?.status) {
          setProject((prev) => (prev ? { ...prev, initiationStatus: data.data.status } : prev));
        }
      } else {
        toast.error(data.message || '操作失败');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || err.message || '操作失败');
    } finally {
      setApprovingTaskId(null);
      setApprovingDecision('');
    }
  };

  const statusConfig: Record<string, { label: string; color: string }> = {
    ACTIVE: { label: '进行中', color: 'bg-green-600' },
    DRAFT: { label: '草稿', color: 'bg-blue-600' },
    SUSPENDED: { label: '已暂停', color: 'bg-yellow-600' },
    COMPLETED: { label: '已完成', color: 'bg-slate-600' },
    TERMINATED: { label: '已终止', color: 'bg-red-600' },
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
        <div className="flex items-center gap-3">
          <Loader2 className="w-6 h-6 animate-spin text-blue-400" />
          <span className="text-slate-300">加载中...</span>
        </div>
      </div>
    );
  }

  if (error || !project) {
    return (
      <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-400 text-lg mb-4">{error || '项目不存在'}</p>
          <Button
            onClick={() => navigate(-1)}
            variant="outline"
            className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            返回
          </Button>
        </div>
      </div>
    );
  }

  const statusLabel = statusConfig[project.projectStatus] || { label: project.projectStatus, color: 'bg-slate-600' };

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      {/* 内容区域 */}
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
      {/* 头部导航 */}
      <div className="mb-6 flex items-center gap-4">
        <Button
          onClick={() => navigate(-1)}
          variant="outline"
          className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
        >
          <ChevronLeft className="w-4 h-4 mr-2" />
          返回
        </Button>
      </div>

      {/* 项目头部信息 */}
      <Card className="bg-slate-800 border-slate-600 mb-6">
        <CardHeader>
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <h1 className="text-3xl font-bold text-slate-100">{project.projectName}</h1>
                <Badge className={`${statusLabel.color} text-white px-3 py-1`}>
                  {statusLabel.label}
                </Badge>
                <Badge className="bg-slate-700 text-slate-200 px-3 py-1">
                  {project.levelName}
                </Badge>
              </div>
              <p className="text-slate-400">{project.tppSummary || project.projectName}</p>
            </div>
            <div className="text-right">
              <p className="text-sm text-slate-400">项目编号</p>
              <p className="text-lg font-bold text-slate-100">{project.projectCode}</p>
            </div>
          </div>

          {/* 项目基本信息 */}
          <div className="mt-6 grid grid-cols-1 md:grid-cols-5 gap-4">
            <div className="p-3 bg-slate-700 rounded">
              <p className="text-sm text-slate-400">靶点</p>
              <p className="text-slate-100 font-semibold">{project.targetPathway || '-'}</p>
            </div>
            <div className="p-3 bg-slate-700 rounded">
              <p className="text-sm text-slate-400">适应症</p>
              <p className="text-slate-100 font-semibold">{project.indication || '-'}</p>
            </div>
            <div className="p-3 bg-slate-700 rounded">
              <p className="text-sm text-slate-400">当前阶段</p>
              <p className="text-slate-100 font-semibold">{project.lifecyclePhaseLabel || '-'}</p>
            </div>
            <div className="p-3 bg-slate-700 rounded">
              <p className="text-sm text-slate-400">总预算</p>
              <p className="text-slate-100 font-semibold">
                {(() => {
                  const displayBudget = project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal;
                  return displayBudget != null && displayBudget > 0
                    ? `¥${(displayBudget / 10000).toFixed(2)}万`
                    : '¥0.00万';
                })()}
              </p>
            </div>
            <div className="p-3 bg-slate-700 rounded">
              <p className="text-sm text-slate-400">未使用预算</p>
              <p className="text-slate-100 font-semibold">
                {(() => {
                  const displayBudget = project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal;
                  const spent = project.budgetExecution?.totalSpent ?? 0;
                  return displayBudget != null && displayBudget > 0
                    ? `¥${((displayBudget - spent) / 10000).toFixed(2)}万`
                    : '¥0.00万';
                })()}
              </p>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* 选项卡 */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="mb-6 bg-slate-800 border border-slate-600">
          <TabsTrigger
            value="overview"
            className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
          >
            <FileText className="w-4 h-4 mr-2" />
            概览
          </TabsTrigger>
          <TabsTrigger
            value="milestone"
            className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
          >
            <CheckCircle className="w-4 h-4 mr-2" />
            里程碑控制台
          </TabsTrigger>
          <TabsTrigger
            value="budget"
            className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
          >
            预算追踪
          </TabsTrigger>
          <TabsTrigger
            value="change-request"
            className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
          >
            <AlertCircle className="w-4 h-4 mr-2" />
            变更申请
          </TabsTrigger>
          <TabsTrigger
            value="documents"
            className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
          >
            交付物管理
          </TabsTrigger>
        </TabsList>

        {/* 概览标签 */}
        <TabsContent value="overview" className="space-y-6">
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">项目概况</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <h3 className="text-slate-100 font-semibold mb-3">项目基本信息</h3>
                  <div className="space-y-2 text-sm">
                    <p>
                      <span className="text-slate-400">项目编号：</span>
                      <span className="text-slate-100">{project.projectCode}</span>
                    </p>
                    <p>
                      <span className="text-slate-400">项目分级：</span>
                      <span className="text-slate-100">{project.levelName} ({project.levelCode})</span>
                    </p>
                    <p>
                      <span className="text-slate-400">项目状态：</span>
                      <span className="text-slate-100">{statusLabel.label}</span>
                    </p>
                    <p>
                      <span className="text-slate-400">当前阶段：</span>
                      <span className="text-slate-100">{project.lifecyclePhaseLabel || '-'}</span>
                    </p>
                  </div>
                </div>

                <div>
                  <h3 className="text-slate-100 font-semibold mb-3">预算概览</h3>
                  <div className="space-y-2 text-sm">
                    <p>
                      <span className="text-slate-400">总预算：</span>
                      <span className="text-slate-100">
                        {(() => {
                          const displayBudget = project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal;
                          return displayBudget != null && displayBudget > 0
                            ? `¥${(displayBudget / 10000).toFixed(2)}万`
                            : '¥0.00万';
                        })()}
                      </span>
                    </p>
                    <p>
                      <span className="text-slate-400">已支出：</span>
                      <span className="text-slate-100">
                        {project.budgetExecution?.totalSpent != null && project.budgetExecution.totalSpent > 0
                          ? `¥${(project.budgetExecution.totalSpent / 10000).toFixed(2)}万`
                          : '¥0.00万'}
                      </span>
                    </p>
                    <p>
                      <span className="text-slate-400">未使用预算：</span>
                      <span className="text-slate-100">
                        {(() => {
                          const displayBudget = project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal;
                          const spent = project.budgetExecution?.totalSpent ?? 0;
                          return displayBudget != null && displayBudget > 0
                            ? `¥${((displayBudget - spent) / 10000).toFixed(2)}万`
                            : '¥0.00万';
                        })()}
                      </span>
                    </p>
                    <p>
                      <span className="text-slate-400">使用率：</span>
                      <span className="text-slate-100">
                        {project.budgetExecution?.utilizationRatio != null && project.budgetExecution.utilizationRatio > 0
                          ? `${project.budgetExecution.utilizationRatio}%`
                          : '0%'}
                      </span>
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 快速操作 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">快速操作</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-3">
                <Button
                  className="bg-blue-600 hover:bg-blue-700 text-white"
                  onClick={openEditDialog}
                >
                  编辑项目信息
                </Button>
                <Button
                  variant="outline"
                  className="bg-slate-700 text-slate-100 border-slate-600"
                  onClick={() => navigate(`/projects/${projectId}/timeline`)}
                >
                  <Calendar className="w-4 h-4 mr-2" />
                  查看时间表
                </Button>
                <Button variant="outline" className="bg-slate-700 text-slate-100 border-slate-600">
                  下载报告
                </Button>
                <Button variant="outline" className="bg-slate-700 text-slate-100 border-slate-600" onClick={() => setActiveTab('change-request')}>
                  项目变更
                </Button>
                {(hasRole('ROLE_PM') || canApproveInitiation) && (
                  <Button
                    className="bg-purple-600 hover:bg-purple-700 text-white"
                    onClick={openInitiationDialog}
                  >
                    <ClipboardList className="w-4 h-4 mr-2" />
                    立项申请
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* 里程碑控制台标签 */}
        <TabsContent value="milestone">
          <MilestoneConsole
            currentStage={project.currentMilestone?.milestoneCode ? parseInt(project.currentMilestone.milestoneCode.replace('G', '')) || 0 : 0}
            projectName={project.projectCode}
            projectId={project.id}
            currentUserId={user?.id}
            currentUserRoles={user?.roles}
            reviewStatus={project.reviewStatus}
            initiationApproved={isInitiationApproved}
            canUploadDeliverables={canUploadDeliverables}
            onReview={() => {
              // 评审提交后刷新项目数据
              api.get(`/api/projects/${projectId}`).then((res) => {
                const data = res.data as { code: number; data: ProjectData; message?: string };
                if (data.code === 200 || data.code === 0) {
                  setProject(data.data);
                }
              });
            }}
          />
        </TabsContent>

        {/* 预算追踪标签 */}
        <TabsContent value="budget">
          <BudgetTracker
            data={{
              internalCost: 0,
              externalCost: 0,
              totalBudget: project.budgetExecution?.plannedTotalAmount ?? 0,
            }}
            projectName={project.projectCode}
            projectId={parseInt(projectId || '0')}
          />
        </TabsContent>

        {/* 变更申请标签 */}
        <TabsContent value="change-request">
          <ChangeRequestForm
            projectId={parseInt(projectId || '0')}
            onSubmit={(data) => {
              console.log('提交变更申请：', data);
              alert('变更申请已提交！');
            }}
            onCancel={() => setActiveTab('overview')}
          />
        </TabsContent>

        {/* 交付物管理标签 */}
        <TabsContent value="documents" className="space-y-6">
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">{project.lifecyclePhaseLabel || project.projectCode} 阶段交付物</CardTitle>
            </CardHeader>
            <CardContent>
              <DocumentList projectId={project.projectCode} currentStage={0} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
      </div>

      {/* 编辑项目信息弹窗 */}
      {editing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <Card className="w-full max-w-5xl mx-4 bg-slate-800 border-slate-600 max-h-[90vh] overflow-y-auto">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-slate-100">编辑项目信息</CardTitle>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setEditing(false)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* 基本信息 */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">基本信息</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      项目名称 <span className="text-red-500">*</span>
                    </label>
                    <Input
                      name="projectName"
                      value={editForm.projectName}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      项目分级 <span className="text-red-500">*</span>
                    </label>
                    <Select value={editForm.levelCode} onValueChange={handleEditSelectChange}>
                      <SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="bg-slate-700 border-slate-600">
                        <SelectItem value="H-L">H-L - 火力全开 临床重大</SelectItem>
                        <SelectItem value="G-L">G-L - 临床重大</SelectItem>
                        <SelectItem value="H-Q">H-Q - 火力全开 重大临床前</SelectItem>
                        <SelectItem value="G-Q">G-Q - 重大临床前</SelectItem>
                        <SelectItem value="G-T">G-T - 重大探索</SelectItem>
                        <SelectItem value="C-L">C-L - 产能项目</SelectItem>
                        <SelectItem value="C-Q">C-Q - 产能项目</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    项目描述
                  </label>
                  <Textarea
                    name="description"
                    value={editForm.description}
                    onChange={handleEditChange}
                    rows={3}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              {/* 科学依据 */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">科学依据</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      靶点/通路 <span className="text-red-500">*</span>
                    </label>
                    <Input
                      name="targetPathway"
                      value={editForm.targetPathway}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      拟定适应症 <span className="text-red-500">*</span>
                    </label>
                    <Input
                      name="indication"
                      value={editForm.indication}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    生物学机制 <span className="text-red-500">*</span>
                  </label>
                  <Textarea
                    name="mechanism"
                    value={editForm.mechanism}
                    onChange={handleEditChange}
                    rows={3}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    未满足的临床需求 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    name="unmetNeeds"
                    value={editForm.unmetNeeds}
                    onChange={handleEditChange}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    科学依据 <span className="text-red-500">*</span>
                  </label>
                  <Textarea
                    name="scientificBasis"
                    value={editForm.scientificBasis}
                    onChange={handleEditChange}
                    rows={3}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              {/* 目标产品概览 (TPP) */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">目标产品概览 (TPP)</h3>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    预期适应症
                  </label>
                  <Input
                    name="expectedIndication"
                    value={editForm.expectedIndication}
                    onChange={handleEditChange}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>

                <div className="grid grid-cols-3 gap-4 mt-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      给药途径
                    </label>
                    <Select
                      value={editForm.administrationRoute}
                      onValueChange={(value) => handleEditSelectChangeGeneric('administrationRoute', value)}
                    >
                      <SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100">
                        <SelectValue placeholder="请选择" />
                      </SelectTrigger>
                      <SelectContent className="bg-slate-700 border-slate-600">
                        <SelectItem value="口服">口服</SelectItem>
                        <SelectItem value="注射">注射</SelectItem>
                        <SelectItem value="吸入">吸入</SelectItem>
                        <SelectItem value="经皮">经皮</SelectItem>
                        <SelectItem value="舌下">舌下</SelectItem>
                        <SelectItem value="直肠">直肠</SelectItem>
                        <SelectItem value="眼用">眼用</SelectItem>
                        <SelectItem value="鼻用">鼻用</SelectItem>
                        <SelectItem value="外用">外用</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      剂型
                    </label>
                    <Select
                      value={editForm.dosageForm}
                      onValueChange={(value) => handleEditSelectChangeGeneric('dosageForm', value)}
                    >
                      <SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100">
                        <SelectValue placeholder="请选择" />
                      </SelectTrigger>
                      <SelectContent className="bg-slate-700 border-slate-600">
                        <SelectItem value="片剂">片剂</SelectItem>
                        <SelectItem value="胶囊剂">胶囊剂</SelectItem>
                        <SelectItem value="注射剂">注射剂</SelectItem>
                        <SelectItem value="颗粒剂">颗粒剂</SelectItem>
                        <SelectItem value="口服液">口服液</SelectItem>
                        <SelectItem value="混悬剂">混悬剂</SelectItem>
                        <SelectItem value="乳膏剂">乳膏剂</SelectItem>
                        <SelectItem value="贴剂">贴剂</SelectItem>
                        <SelectItem value="气雾剂">气雾剂</SelectItem>
                        <SelectItem value="滴眼剂">滴眼剂</SelectItem>
                        <SelectItem value="栓剂">栓剂</SelectItem>
                        <SelectItem value="丸剂">丸剂</SelectItem>
                        <SelectItem value="散剂">散剂</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      剂量频率
                    </label>
                    <Input
                      name="dosageFrequency"
                      value={editForm.dosageFrequency}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    预期疗效指标
                  </label>
                  <Textarea
                    name="efficacyTarget"
                    value={editForm.efficacyTarget}
                    onChange={handleEditChange}
                    rows={2}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    安全性优势
                  </label>
                  <Textarea
                    name="safetyAdvantage"
                    value={editForm.safetyAdvantage}
                    onChange={handleEditChange}
                    rows={2}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              {/* 差异化优势 */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">差异化优势</h3>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    与现有/在研竞品相比的核心优势 <span className="text-red-500">*</span>
                  </label>
                  <Textarea
                    name="differentiation"
                    value={editForm.differentiation}
                    onChange={handleEditChange}
                    rows={4}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              {/* 项目简介与预算 */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">其他信息</h3>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    项目简介
                  </label>
                  <Textarea
                    name="tppSummary"
                    value={editForm.tppSummary}
                    onChange={handleEditChange}
                    rows={3}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4 mt-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      总预算 (万元)
                    </label>
                    <Input
                      type="number"
                      name="budgetTotal"
                      value={editForm.budgetTotal}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      阶段预算至PCC (万元)
                    </label>
                    <Input
                      type="number"
                      name="budgetToPcc"
                      value={editForm.budgetToPcc}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-5 gap-4 mt-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      预估PCC提名日期
                    </label>
                    <Input
                      type="date"
                      name="plannedPccDate"
                      value={editForm.plannedPccDate}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      预估IND获批日期
                    </label>
                    <Input
                      type="date"
                      name="plannedIndDate"
                      value={editForm.plannedIndDate}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      预估NDA获批日期
                    </label>
                    <Input
                      type="date"
                      name="plannedNdaDate"
                      value={editForm.plannedNdaDate}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      预估项目结束日期
                    </label>
                    <Input
                      type="date"
                      name="plannedEndDate"
                      value={editForm.plannedEndDate}
                      onChange={handleEditChange}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                </div>
              </div>

              {/* 项目风险评估 */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">
                  项目风险评估 <span className="text-red-500">*</span>
                </h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      科学风险
                    </label>
                    <Textarea
                      name="riskScientific"
                      value={editForm.riskScientific}
                      onChange={handleEditChange}
                      placeholder="靶点有效性风险、成药性风险、安全性风险"
                      rows={3}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      竞争风险
                    </label>
                    <Textarea
                      name="riskCompetitive"
                      value={editForm.riskCompetitive}
                      onChange={handleEditChange}
                      placeholder="主要竞品进展"
                      rows={3}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      注册风险
                    </label>
                    <Textarea
                      name="riskRegulatory"
                      value={editForm.riskRegulatory}
                      onChange={handleEditChange}
                      placeholder="法规路径不确定性"
                      rows={3}
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>
                </div>
              </div>

              {/* 建议与所需支持 */}
              <div>
                <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">建议与所需支持</h3>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    简述需要PMC提供的资源或决策支持
                  </label>
                  <Textarea
                    name="suggestionAndSupport"
                    value={editForm.suggestionAndSupport}
                    onChange={handleEditChange}
                    placeholder="简述需要PMC提供的资源或决策支持"
                    rows={3}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-600">
                <Button
                  variant="outline"
                  onClick={() => setEditing(false)}
                  className="bg-slate-700 text-slate-100 border-slate-600"
                >
                  取消
                </Button>
                <Button
                  onClick={handleSaveEdit}
                  disabled={saving}
                  className="bg-blue-600 hover:bg-blue-700 text-white"
                >
                  {saving ? '保存中...' : '保存'}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* 立项申请弹窗 */}
      {showInitiation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <Card className="w-full max-w-5xl mx-4 bg-slate-800 border-slate-600 max-h-[90vh] overflow-y-auto">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-slate-100 flex items-center gap-2">
                <ClipboardList className="w-5 h-5 text-purple-400" />
                立项申请
              </CardTitle>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowInitiation(false)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-6">
              {initiationLoading ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="w-6 h-6 animate-spin text-blue-400" />
                  <span className="ml-3 text-slate-300">加载中...</span>
                </div>
              ) : (
                <>
                  {/* 项目信息（只读） */}
                  <div>
                    <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">项目信息</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-slate-400">项目名称：</span>
                        <span className="text-slate-100">{project?.projectName}</span>
                      </div>
                      <div>
                        <span className="text-slate-400">项目编号：</span>
                        <span className="text-slate-100">{project?.projectCode}</span>
                      </div>
                      <div>
                        <span className="text-slate-400">项目分级：</span>
                        <span className="text-slate-100">{project?.levelName} ({project?.levelCode})</span>
                      </div>
                      <div>
                        <span className="text-slate-400">靶点/通路：</span>
                        <span className="text-slate-100">{project?.targetPathway || '-'}</span>
                      </div>
                      <div>
                        <span className="text-slate-400">适应症：</span>
                        <span className="text-slate-100">{project?.indication || '-'}</span>
                      </div>
                      <div>
                        <span className="text-slate-400">当前阶段：</span>
                        <span className="text-slate-100">{project?.lifecyclePhaseLabel || '-'}</span>
                      </div>
                      <div className="md:col-span-2">
                        <span className="text-slate-400">项目简介：</span>
                        <span className="text-slate-100">{project?.tppSummary || '-'}</span>
                      </div>
                    </div>
                  </div>

                  {/* 申请信息栏 - 项目经理可见 */}
                  {hasRole('ROLE_PM') && (
                    <div>
                      <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">申请信息</h3>
                      {!initiationData || initiationData.status === 'REJECTED' ? (
                        <div className="space-y-4">
                          <Textarea
                            placeholder="请填写立项申请内容..."
                            value={applicationContent}
                            onChange={(e) => setApplicationContent(e.target.value)}
                            rows={6}
                            className="bg-slate-700 border-slate-600 text-slate-100"
                          />
                          <div className="flex justify-end">
                            <Button
                              onClick={handleSubmitInitiation}
                              disabled={submittingInitiation}
                              className="bg-purple-600 hover:bg-purple-700 text-white"
                            >
                              {submittingInitiation ? '提交中...' : '提交立项申请'}
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="space-y-3">
                          <div className="p-4 bg-slate-700 rounded">
                            <p className="text-slate-300 whitespace-pre-wrap">{initiationData.applicationContent || '无申请内容'}</p>
                          </div>
                          <div className="flex items-center gap-2 text-sm">
                            <span className="text-slate-400">提交人：</span>
                            <span className="text-slate-100">{initiationData.submitterName || '-'}</span>
                            <span className="text-slate-400 ml-4">提交时间：</span>
                            <span className="text-slate-100">
                              {initiationData.submittedAt ? new Date(initiationData.submittedAt).toLocaleString('zh-CN') : '-'}
                            </span>
                            <span className="text-slate-400 ml-4">状态：</span>
                            <Badge className={
                              initiationData.status === 'APPROVED' ? 'bg-green-600' :
                              initiationData.status === 'REJECTED' ? 'bg-red-600' :
                              'bg-yellow-600'
                            }>
                              {initiationData.status === 'APPROVED' ? '已通过' :
                               initiationData.status === 'REJECTED' ? '已驳回' : '审批中'}
                            </Badge>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* 审批信息栏 - 拥有「审批立项申请」权限的用户可见 */}
                  {canApproveInitiation && initiationData && (
                    <div>
                      <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">审批信息</h3>
                      <div className="space-y-4">
                        <div className="p-4 bg-slate-700 rounded">
                          <p className="text-sm text-slate-400 mb-2">申请内容：</p>
                          <p className="text-slate-100 whitespace-pre-wrap">{initiationData.applicationContent || '无申请内容'}</p>
                        </div>
                        <div className="flex items-center gap-2 text-sm flex-wrap">
                          <span className="text-slate-400">提交人：</span>
                          <span className="text-slate-100">{initiationData.submitterName || '-'}</span>
                          <span className="text-slate-400 ml-4">提交时间：</span>
                          <span className="text-slate-100">
                            {initiationData.submittedAt ? new Date(initiationData.submittedAt).toLocaleString('zh-CN') : '-'}
                          </span>
                        </div>

                        {/* 全部审批人及状态 */}
                        {initiationData.tasks && initiationData.tasks.length > 0 && (
                          <div>
                            <p className="text-sm font-medium text-slate-300 mb-2">审批人进度</p>
                            <div className="space-y-2">
                              {initiationData.tasks.map((task: {
                                id: number;
                                approverName: string;
                                approverRole: string;
                                status: string;
                                opinion?: string;
                                decidedAt?: string;
                              }) => {
                                const statusStyle =
                                  task.status === 'APPROVED'
                                    ? 'border-green-600 bg-green-950/40'
                                    : task.status === 'REJECTED'
                                    ? 'border-red-600 bg-red-950/40'
                                    : 'border-yellow-600 bg-yellow-950/30';
                                const statusBadge =
                                  task.status === 'APPROVED'
                                    ? <Badge className="bg-green-600">已同意</Badge>
                                    : task.status === 'REJECTED'
                                    ? <Badge className="bg-red-600">已拒绝</Badge>
                                    : <Badge className="bg-yellow-600">待审批</Badge>;
                                return (
                                  <div
                                    key={task.id}
                                    className={`p-3 rounded border flex items-center justify-between ${statusStyle}`}
                                  >
                                    <div>
                                      <p className="text-slate-100 font-medium">{task.approverName}</p>
                                      <p className="text-xs text-slate-400">
                                        {task.approverRole === 'ROLE_PMC' ? 'PMC成员' : task.approverRole}
                                      </p>
                                      {task.opinion && (
                                        <p className="text-xs text-slate-400 mt-1">意见：{task.opinion}</p>
                                      )}
                                    </div>
                                    <div className="text-right">
                                      {statusBadge}
                                      {task.decidedAt && (
                                        <p className="text-xs text-slate-400 mt-1">
                                          {new Date(task.decidedAt).toLocaleString('zh-CN')}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        )}

                        {initiationData.status === 'SUBMITTED' && (
                          <>
                            <div>
                              <label className="block text-sm font-medium text-slate-300 mb-2">审批意见</label>
                              <Textarea
                                placeholder="请填写审批意见..."
                                value={approvalOpinion}
                                onChange={(e) => setApprovalOpinion(e.target.value)}
                                rows={3}
                                className="bg-slate-700 border-slate-600 text-slate-100"
                                disabled={!myPendingInitiationTask}
                              />
                            </div>
                            {!myPendingInitiationTask && (
                              <p className="text-sm text-slate-400">
                                {initiationData.tasks?.some((t: { approverUserId: number }) =>
                                  sameUserId(t.approverUserId, user?.id)
                                )
                                  ? '您已完成本项目的立项审批'
                                  : '当前账号不在本项目的审批人列表中，或审批任务尚未生成（请刷新页面）'}
                              </p>
                            )}
                            <div className="flex justify-end gap-3">
                              <Button
                                onClick={() => handleInitiationDecision(myPendingInitiationTask!.id, 'REJECTED')}
                                disabled={approvingTaskId !== null || !myPendingInitiationTask}
                                className="bg-red-600 hover:bg-red-700 text-white disabled:opacity-50"
                              >
                                {approvingTaskId !== null && approvingDecision === 'REJECTED' ? '处理中...' : '拒绝'}
                              </Button>
                              <Button
                                onClick={() => handleInitiationDecision(myPendingInitiationTask!.id, 'APPROVED')}
                                disabled={approvingTaskId !== null || !myPendingInitiationTask}
                                className="bg-green-600 hover:bg-green-700 text-white disabled:opacity-50"
                              >
                                {approvingTaskId !== null && approvingDecision === 'APPROVED' ? '处理中...' : '同意'}
                              </Button>
                            </div>
                          </>
                        )}

                        {initiationData.status === 'APPROVED' && (
                          <div className="p-4 bg-green-900/30 border border-green-700 rounded">
                            <p className="text-green-400 font-semibold">立项申请已全部通过</p>
                          </div>
                        )}
                        {initiationData.status === 'REJECTED' && (
                          <div className="p-4 bg-red-900/30 border border-red-700 rounded">
                            <p className="text-red-400 font-semibold">立项申请已被驳回</p>
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {/* 项目经理查看审批进度 */}
                  {hasRole('ROLE_PM') && initiationData?.tasks && initiationData.tasks.length > 0 && !canApproveInitiation && (
                    <div>
                      <h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">立项审批进度</h3>
                      <div className="space-y-2">
                        {initiationData.tasks.map((task: { id: number; approverName: string; status: string; opinion?: string }) => (
                          <div
                            key={task.id}
                            className={`p-3 rounded border flex justify-between ${
                              task.status === 'APPROVED'
                                ? 'border-green-600 bg-green-950/40'
                                : task.status === 'REJECTED'
                                ? 'border-red-600 bg-red-950/40'
                                : 'border-yellow-600 bg-yellow-950/30'
                            }`}
                          >
                            <span className="text-slate-100">{task.approverName}</span>
                            <Badge
                              className={
                                task.status === 'APPROVED'
                                  ? 'bg-green-600'
                                  : task.status === 'REJECTED'
                                  ? 'bg-red-600'
                                  : 'bg-yellow-600'
                              }
                            >
                              {task.status === 'APPROVED' ? '已同意' : task.status === 'REJECTED' ? '已拒绝' : '待审批'}
                            </Badge>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* 无立项申请记录时显示提示 */}
                  {!initiationData && canApproveInitiation && (
                    <div className="text-center py-8 text-slate-400">
                      暂无立项申请记录
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default ProjectDetail;
