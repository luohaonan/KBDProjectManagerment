import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
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
import { BudgetManagementPanel } from '../components/BudgetManagementPanel';
import { ChangeRequestForm } from '../components/ChangeRequestForm';
import { ChevronLeft, FileText, CheckCircle, AlertCircle, Loader2, X, Calendar, Download } from 'lucide-react';
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
  pmUserName: string | null;
  projectStatus: string;
  lifecyclePhaseLabel: string | null;
  reviewStatus?: string;
  initiationStatus?: string | null;
  processOversightDept: {
    deptId: number; deptCode: string; deptName: string;
  } | null;
  currentMilestone: {
    milestoneCode: string; milestoneName: string; phaseLabel: string;
    executorDeptNames?: string[];
  } | null;
  budgetExecution: {
    plannedTotalAmount: number | null; totalSpent: number | null;
    utilizationRatio: number | null; warningLevel: string | null; snapshotMonth: string | null;
  } | null;
}

function fmtYuan(val: number | null | undefined): string {
  if (val == null || val <= 0) return '¥0';
  return '¥' + val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const ProjectDetail: React.FC = () => {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user, hasRole } = useAuth();
  const [project, setProject] = useState<ProjectData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState(searchParams.get('tab') || 'overview');
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const canEditProjectInfo = hasRole('ROLE_PM') || hasRole('ROLE_ADMIN') || hasRole('ROLE_PROJECT_ADMIN');
  // 判断当前用户是否可以上传交付物
  // ADMIN / 项目PM 可以上传所有里程碑交付物
  // 部门负责人/执行人需要属于当前里程碑的执行部门
  const canUploadDeliverables = (() => {
    if (!user) return false;
    const roles = user.roles || [];
    // 只有 ADMIN、部门负责人 (ROLE_DEPT_HEAD) 和部门执行人 (ROLE_DEPT_EXECUTOR) 可以上传交付物
    // PM 负责评审和项目管理，不负责上传核心交付物
    if (roles.includes('ROLE_ADMIN')) return true;
    if (!roles.includes('ROLE_DEPT_HEAD') && !roles.includes('ROLE_DEPT_EXECUTOR')) return false;
    // 检查用户部门是否在里程碑执行部门列表中
    const userDepts = user.departments || [];
    const executorDepts = project?.currentMilestone?.executorDeptNames || [];
    return userDepts.some(dept => executorDepts.includes(dept));
  })();

  const [editForm, setEditForm] = useState({
    projectName: '', levelCode: '', indication: '', targetPathway: '', tppSummary: '',
    description: '', mechanism: '', unmetNeeds: '', scientificBasis: '',
    expectedIndication: '', administrationRoute: '', dosageForm: '', dosageFrequency: '',
    efficacyTarget: '', safetyAdvantage: '', differentiation: '',
    budgetTotal: 0, plannedPccDate: '', plannedIndDate: '', plannedNdaDate: '', plannedEndDate: '',
    budgetToPcc: 0, riskScientific: '', riskCompetitive: '', riskRegulatory: '', suggestionAndSupport: '',
  });

  useEffect(() => {
    if (!projectId) return; setLoading(true); setError(null);
    api.get(`/api/projects/${projectId}`)
      .then((res) => { const data = res.data as any; if (data.code === 200 || data.code === 0) setProject(data.data); else setError(data.message || '获取项目详情失败'); })
      .catch((err) => { setError(err.response?.data?.message || err.message || '网络错误'); })
      .finally(() => setLoading(false));
  }, [projectId]);

  useEffect(() => { if (searchParams.get('tab') === 'milestone' && project) setActiveTab('milestone'); }, [searchParams, project]);

  const openEditDialog = () => {
    if (!project) return;
    setEditForm({
      projectName: project.projectName, levelCode: project.levelCode,
      indication: project.indication, targetPathway: project.targetPathway,
      tppSummary: project.tppSummary, description: project.description || '',
      mechanism: project.mechanism || '', unmetNeeds: project.unmetNeeds || '',
      scientificBasis: project.scientificBasis || '', expectedIndication: project.expectedIndication || '',
      administrationRoute: project.administrationRoute || '', dosageForm: project.dosageForm || '',
      dosageFrequency: project.dosageFrequency || '', efficacyTarget: project.efficacyTarget || '',
      safetyAdvantage: project.safetyAdvantage || '', differentiation: project.differentiation || '',
      budgetTotal: project.budgetTotal ?? 0, plannedPccDate: project.plannedPccDate || '',
      plannedIndDate: project.plannedIndDate || '', plannedNdaDate: project.plannedNdaDate || '',
      plannedEndDate: project.plannedEndDate || '', budgetToPcc: project.budgetToPcc ?? 0,
      riskScientific: project.riskScientific || '', riskCompetitive: project.riskCompetitive || '',
      riskRegulatory: project.riskRegulatory || '', suggestionAndSupport: project.suggestionAndSupport || '',
    });
    setEditing(true);
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setEditForm(prev => ({ ...prev, [name]: type === 'number' ? Number(value) : value }));
  };
  const handleEditSelectChange = (value: string) => setEditForm(prev => ({ ...prev, levelCode: value }));
  const handleEditSelectChangeGeneric = (name: string, value: string) => setEditForm(prev => ({ ...prev, [name]: value }));

  const handleSaveEdit = async () => {
    if (!project || !projectId) return; setSaving(true);
    try {
      await api.put(`/api/projects/${projectId}`, {
        projectName: editForm.projectName, levelCode: editForm.levelCode,
        indication: editForm.indication, targetPathway: editForm.targetPathway,
        tppSummary: editForm.tppSummary, description: editForm.description || null,
        mechanism: editForm.mechanism || null, unmetNeeds: editForm.unmetNeeds || null,
        scientificBasis: editForm.scientificBasis || null, expectedIndication: editForm.expectedIndication || null,
        administrationRoute: editForm.administrationRoute || null, dosageForm: editForm.dosageForm || null,
        dosageFrequency: editForm.dosageFrequency || null, efficacyTarget: editForm.efficacyTarget || null,
        safetyAdvantage: editForm.safetyAdvantage || null, differentiation: editForm.differentiation || null,
        budgetTotal: editForm.budgetTotal || null, plannedPccDate: editForm.plannedPccDate || null,
        plannedIndDate: editForm.plannedIndDate || null, plannedNdaDate: editForm.plannedNdaDate || null,
        plannedEndDate: editForm.plannedEndDate || null, budgetToPcc: editForm.budgetToPcc || null,
        riskScientific: editForm.riskScientific || null, riskCompetitive: editForm.riskCompetitive || null,
        riskRegulatory: editForm.riskRegulatory || null, suggestionAndSupport: editForm.suggestionAndSupport || null,
      });
      toast.success('项目信息更新成功！'); setEditing(false);
      const res = await api.get(`/api/projects/${projectId}`);
      const data = res.data as any; if (data.code === 200 || data.code === 0) setProject(data.data);
    } catch (error) { toast.error('更新失败，请重试'); console.error(error); } finally { setSaving(false); }
  };

  const isG0ReviewSubmitted = project?.reviewStatus === 'IN_REVIEW';

  const handleDownloadReport = async () => {
    if (!projectId) return;
    try {
      toast.info('正在生成立项报告 PDF，请稍候...');
      const res = await api.get(`/api/projects/${projectId}/initiation-report/pdf`, { responseType: 'blob' });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob); const a = document.createElement('a');
      a.href = url; a.download = `立项报告_${project?.projectCode || projectId}.pdf`;
      document.body.appendChild(a); a.click(); window.URL.revokeObjectURL(url); document.body.removeChild(a);
      toast.success('立项报告 PDF 已成功下载！');
    } catch (err: any) { toast.error('生成 PDF 失败：' + (err.response?.data?.message || err.message || '请稍后重试')); }
  };

  const statusConfig: Record<string, { label: string; color: string }> = {
    ACTIVE: { label: '进行中', color: 'bg-green-600' }, DRAFT: { label: '草稿', color: 'bg-blue-600' },
    SUSPENDED: { label: '已暂停', color: 'bg-yellow-600' }, COMPLETED: { label: '已完成', color: 'bg-slate-600' },
    TERMINATED: { label: '已终止', color: 'bg-red-600' },
  };

  if (loading) return (<div className="min-h-screen bg-slate-900 text-white flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-blue-400" /><span className="text-slate-300 ml-3">加载中...</span></div>);
  if (error || !project) return (<div className="min-h-screen bg-slate-900 text-white flex items-center justify-center"><div className="text-center"><p className="text-red-400 text-lg mb-4">{error || '项目不存在'}</p><Button onClick={() => navigate('/dashboard')} variant="outline" className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"><ChevronLeft className="w-4 h-4 mr-2" />返回首页</Button></div></div>);

  const statusLabel = statusConfig[project.projectStatus] || { label: project.projectStatus, color: 'bg-slate-600' };

  return (
    <div className="min-h-screen bg-slate-900 text-white"><div className="mx-auto w-full max-w-7xl px-6 py-6">
      <div className="mb-6 flex items-center gap-4">
        <Button onClick={() => navigate('/dashboard')} variant="outline" className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"><ChevronLeft className="w-4 h-4 mr-2" />返回首页</Button>
      </div>
      <Card className="bg-slate-800 border-slate-600 mb-6"><CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-3xl font-bold text-slate-100">{project.projectName}</h1>
              <Badge className={`${statusLabel.color} text-white px-3 py-1`}>{statusLabel.label}</Badge>
              <Badge className="bg-slate-700 text-slate-200 px-3 py-1">{project.levelName}</Badge>
            </div>
            <p className="text-slate-400">{project.tppSummary || project.projectName}</p>
          </div>
          <div className="text-right"><p className="text-sm text-slate-400">项目编号</p><p className="text-lg font-bold text-slate-100">{project.projectCode}</p></div>
        </div>
        <div className="mt-6 grid grid-cols-1 md:grid-cols-5 gap-4">
          <div className="p-3 bg-slate-700 rounded"><p className="text-sm text-slate-400">靶点</p><p className="text-slate-100 font-semibold">{project.targetPathway || '-'}</p></div>
          <div className="p-3 bg-slate-700 rounded"><p className="text-sm text-slate-400">适应症</p><p className="text-slate-100 font-semibold">{project.indication || '-'}</p></div>
          <div className="p-3 bg-slate-700 rounded"><p className="text-sm text-slate-400">当前阶段</p><p className="text-slate-100 font-semibold">{project.lifecyclePhaseLabel || '-'}</p></div>
          <div className="p-3 bg-slate-700 rounded"><p className="text-sm text-slate-400">总预算</p><p className="text-slate-100 font-semibold">{fmtYuan(project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal)}</p></div>
          <div className="p-3 bg-slate-700 rounded"><p className="text-sm text-slate-400">未使用预算</p><p className="text-slate-100 font-semibold">{fmtYuan((project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal ?? 0) - (project.budgetExecution?.totalSpent ?? 0))}</p></div>
        </div>
      </CardHeader></Card>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="mb-6 flex h-auto flex-wrap justify-start gap-2 rounded-lg border-0 bg-white p-2 shadow-sm">
          <TabsTrigger value="overview" className="rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none data-[state=active]:bg-blue-100 data-[state=active]:text-slate-900 data-[state=active]:border-blue-300"><FileText className="w-4 h-4 mr-2" />概览</TabsTrigger>
          <TabsTrigger value="milestone" className="rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none data-[state=active]:bg-blue-100 data-[state=active]:text-slate-900 data-[state=active]:border-blue-300"><CheckCircle className="w-4 h-4 mr-2" />里程碑控制台</TabsTrigger>
          <TabsTrigger value="budget" className="rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none data-[state=active]:bg-blue-100 data-[state=active]:text-slate-900 data-[state=active]:border-blue-300">预算管理</TabsTrigger>
          <TabsTrigger value="change-request" className="rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none data-[state=active]:bg-blue-100 data-[state=active]:text-slate-900 data-[state=active]:border-blue-300"><AlertCircle className="w-4 h-4 mr-2" />变更申请</TabsTrigger>
          <TabsTrigger value="documents" className="rounded-md border border-slate-300 bg-white px-4 py-2 text-slate-700 shadow-none data-[state=active]:bg-blue-100 data-[state=active]:text-slate-900 data-[state=active]:border-blue-300">交付物管理</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <Card className="bg-slate-800 border-slate-600"><CardHeader><CardTitle className="text-slate-100">项目概况</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div><h3 className="text-slate-100 font-semibold mb-3">项目基本信息</h3>
                  <div className="space-y-2 text-sm">
                    <p><span className="text-slate-400">项目编号：</span><span className="text-slate-100">{project.projectCode}</span></p>
                    <p><span className="text-slate-400">项目分级：</span><span className="text-slate-100">{project.levelName} ({project.levelCode})</span></p>
                    <p><span className="text-slate-400">项目状态：</span><span className="text-slate-100">{statusLabel.label}</span></p>
                    <p><span className="text-slate-400">项目经理：</span><span className="text-slate-100">{project.pmUserName || '-'}</span></p>
                    <p><span className="text-slate-400">当前阶段：</span><span className="text-slate-100">{project.lifecyclePhaseLabel || '-'}</span></p>
                  </div>
                </div>
                <div><h3 className="text-slate-100 font-semibold mb-3">预算概览</h3>
                  <div className="space-y-2 text-sm">
                    <p><span className="text-slate-400">总预算：</span><span className="text-slate-100">{fmtYuan(project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal)}</span></p>
                    <p><span className="text-slate-400">已支出：</span><span className="text-slate-100">{fmtYuan(project.budgetExecution?.totalSpent)}</span></p>
                    <p><span className="text-slate-400">未使用预算：</span><span className="text-slate-100">{fmtYuan((project.budgetExecution?.plannedTotalAmount ?? project.budgetTotal ?? 0) - (project.budgetExecution?.totalSpent ?? 0))}</span></p>
                    <p><span className="text-slate-400">使用率：</span><span className="text-slate-100">{project.budgetExecution?.utilizationRatio != null && project.budgetExecution.utilizationRatio > 0 ? `${project.budgetExecution.utilizationRatio}%` : '0%'}</span></p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="bg-slate-800 border-slate-600"><CardHeader><CardTitle className="text-slate-100">快速操作</CardTitle></CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-3">
                {canEditProjectInfo && !isG0ReviewSubmitted && <Button className="bg-blue-600 hover:bg-blue-700 text-white" onClick={openEditDialog}>编辑项目信息</Button>}
                <Button variant="outline" className="bg-slate-700 text-slate-100 border-slate-600" onClick={() => navigate(`/projects/${projectId}/timeline`)}><Calendar className="w-4 h-4 mr-2" />查看时间表</Button>
                <Button variant="outline" className="bg-slate-700 text-slate-100 border-slate-600 hover:bg-slate-600" onClick={handleDownloadReport}><Download className="w-4 h-4 mr-2" />下载立项报告</Button>
                {hasRole('ROLE_PM') && <Button variant="outline" className="bg-slate-700 text-slate-100 border-slate-600" onClick={() => setActiveTab('change-request')}>项目变更</Button>}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="milestone">
          <MilestoneConsole currentStage={project.currentMilestone?.milestoneCode ? parseInt(project.currentMilestone.milestoneCode.replace('G', '')) || 0 : 0} projectName={project.projectCode} projectId={project.id} currentUserId={user?.id} currentUserRoles={user?.roles} reviewStatus={project.reviewStatus} canUploadDeliverables={canUploadDeliverables} executorDeptName={(project.currentMilestone?.executorDeptNames && project.currentMilestone.executorDeptNames.length > 0) ? project.currentMilestone.executorDeptNames.join('、') : '对应部门'} onReview={() => { api.get(`/api/projects/${projectId}`).then((res) => { const data = res.data as any; if (data.code === 200 || data.code === 0) setProject(data.data); }); }} />
        </TabsContent>

        <TabsContent value="budget">
          <div className="space-y-6">
            <BudgetManagementPanel
              projectId={parseInt(projectId || '0')}
              projectName={project.projectCode}
              onDataChanged={() => setRefreshKey(prev => prev + 1)}
            />
            <BudgetTracker
              projectName={project.projectCode}
              projectId={parseInt(projectId || '0')}
              refreshKey={refreshKey}
            />
          </div>
        </TabsContent>

        <TabsContent value="change-request">
          <ChangeRequestForm projectId={parseInt(projectId || '0')} onCancel={() => setActiveTab('overview')} />
        </TabsContent>

        <TabsContent value="documents" className="space-y-6">
          <DocumentList projectId={String(project.id)} currentStage={0} userRoles={user?.roles} userPermissions={user?.permissions} />
        </TabsContent>
      </Tabs>
    </div>

      {editing && (<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
        <Card className="w-full max-w-5xl mx-4 bg-slate-800 border-slate-600 max-h-[90vh] overflow-y-auto">
          <CardHeader className="flex flex-row items-center justify-between"><CardTitle className="text-slate-100">编辑项目信息</CardTitle><Button variant="ghost" size="sm" onClick={() => setEditing(false)} className="text-slate-400 hover:text-white"><X className="w-5 h-5" /></Button></CardHeader>
          <CardContent className="space-y-6">
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">基本信息</h3>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-sm font-medium text-slate-300 mb-2">项目名称 <span className="text-red-500">*</span></label><Input name="projectName" value={editForm.projectName} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">项目分级 <span className="text-red-500">*</span></label><Select value={editForm.levelCode} onValueChange={handleEditSelectChange}><SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100"><SelectValue /></SelectTrigger><SelectContent className="bg-slate-700 border-slate-600"><SelectItem value="H-L">H-L - 火力全开 临床重大</SelectItem><SelectItem value="G-L">G-L - 临床重大</SelectItem><SelectItem value="H-Q">H-Q - 火力全开 重大临床前</SelectItem><SelectItem value="G-Q">G-Q - 重大临床前</SelectItem><SelectItem value="G-T">G-T - 重大探索</SelectItem><SelectItem value="C-L">C-L - 产能项目</SelectItem><SelectItem value="C-Q">C-Q - 产能项目</SelectItem></SelectContent></Select></div>
              </div>
              <div className="mt-4"><label className="block text-sm font-medium text-slate-300 mb-2">项目描述</label><Textarea name="description" value={editForm.description} onChange={handleEditChange} rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
            </div>
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">科学依据</h3>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-sm font-medium text-slate-300 mb-2">靶点/通路 <span className="text-red-500">*</span></label><Input name="targetPathway" value={editForm.targetPathway} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">拟定适应症</label><Input name="indication" value={editForm.indication} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              </div>
              <div className="mt-4"><label className="block text-sm font-medium text-slate-300 mb-2">生物学机制 <span className="text-red-500">*</span></label><Textarea name="mechanism" value={editForm.mechanism} onChange={handleEditChange} rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              <div className="mt-4"><label className="block text-sm font-medium text-slate-300 mb-2">未满足的临床需求 <span className="text-red-500">*</span></label><Input name="unmetNeeds" value={editForm.unmetNeeds} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              <div className="mt-4"><label className="block text-sm font-medium text-slate-300 mb-2">科学依据 <span className="text-red-500">*</span></label><Textarea name="scientificBasis" value={editForm.scientificBasis} onChange={handleEditChange} rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
            </div>
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">目标产品概览 (TPP)</h3>
              <div><label className="block text-sm font-medium text-slate-300 mb-2">预期适应症</label><Input name="expectedIndication" value={editForm.expectedIndication} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              <div className="grid grid-cols-3 gap-4 mt-4">
                <div><label className="block text-sm font-medium text-slate-300 mb-2">给药途径</label><Select value={editForm.administrationRoute} onValueChange={(v) => handleEditSelectChangeGeneric('administrationRoute', v)}><SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100"><SelectValue placeholder="请选择" /></SelectTrigger><SelectContent className="bg-slate-700 border-slate-600"><SelectItem value="口服">口服</SelectItem><SelectItem value="注射">注射</SelectItem><SelectItem value="吸入">吸入</SelectItem><SelectItem value="经皮">经皮</SelectItem><SelectItem value="舌下">舌下</SelectItem><SelectItem value="直肠">直肠</SelectItem><SelectItem value="眼用">眼用</SelectItem><SelectItem value="鼻用">鼻用</SelectItem><SelectItem value="外用">外用</SelectItem></SelectContent></Select></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">剂型</label><Select value={editForm.dosageForm} onValueChange={(v) => handleEditSelectChangeGeneric('dosageForm', v)}><SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100"><SelectValue placeholder="请选择" /></SelectTrigger><SelectContent className="bg-slate-700 border-slate-600"><SelectItem value="片剂">片剂</SelectItem><SelectItem value="胶囊剂">胶囊剂</SelectItem><SelectItem value="注射剂">注射剂</SelectItem><SelectItem value="颗粒剂">颗粒剂</SelectItem><SelectItem value="口服液">口服液</SelectItem><SelectItem value="混悬剂">混悬剂</SelectItem><SelectItem value="乳膏剂">乳膏剂</SelectItem><SelectItem value="贴剂">贴剂</SelectItem><SelectItem value="气雾剂">气雾剂</SelectItem><SelectItem value="滴眼剂">滴眼剂</SelectItem><SelectItem value="栓剂">栓剂</SelectItem><SelectItem value="丸剂">丸剂</SelectItem><SelectItem value="散剂">散剂</SelectItem></SelectContent></Select></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">剂量频率</label><Input name="dosageFrequency" value={editForm.dosageFrequency} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              </div>
              <div className="mt-4"><label className="block text-sm font-medium text-slate-300 mb-2">预期疗效指标</label><Textarea name="efficacyTarget" value={editForm.efficacyTarget} onChange={handleEditChange} rows={2} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              <div className="mt-4"><label className="block text-sm font-medium text-slate-300 mb-2">安全性优势</label><Textarea name="safetyAdvantage" value={editForm.safetyAdvantage} onChange={handleEditChange} rows={2} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
            </div>
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">差异化优势</h3>
              <div><label className="block text-sm font-medium text-slate-300 mb-2">与现有/在研竞品相比的核心优势 <span className="text-red-500">*</span></label><Textarea name="differentiation" value={editForm.differentiation} onChange={handleEditChange} rows={4} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
            </div>
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">其他信息</h3>
              <div><label className="block text-sm font-medium text-slate-300 mb-2">项目简介</label><Textarea name="tppSummary" value={editForm.tppSummary} onChange={handleEditChange} rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              <div className="grid grid-cols-2 gap-4 mt-4">
                <div><label className="block text-sm font-medium text-slate-300 mb-2">总预算 (元)</label><Input type="number" name="budgetTotal" value={editForm.budgetTotal} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">阶段预算至PCC (元)</label><Input type="number" name="budgetToPcc" value={editForm.budgetToPcc} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              </div>
              <div className="grid grid-cols-5 gap-4 mt-4">
                <div><label className="block text-sm font-medium text-slate-300 mb-2">预估PCC提名日期</label><Input type="date" name="plannedPccDate" value={editForm.plannedPccDate} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">预估IND获批日期</label><Input type="date" name="plannedIndDate" value={editForm.plannedIndDate} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">预估NDA获批日期</label><Input type="date" name="plannedNdaDate" value={editForm.plannedNdaDate} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">预估项目结束日期</label><Input type="date" name="plannedEndDate" value={editForm.plannedEndDate} onChange={handleEditChange} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              </div>
            </div>
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">项目风险评估 <span className="text-red-500">*</span></h3>
              <div className="space-y-4">
                <div><label className="block text-sm font-medium text-slate-300 mb-2">科学风险</label><Textarea name="riskScientific" value={editForm.riskScientific} onChange={handleEditChange} placeholder="靶点有效性风险、成药性风险、安全性风险" rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">竞争风险</label><Textarea name="riskCompetitive" value={editForm.riskCompetitive} onChange={handleEditChange} placeholder="主要竞品进展" rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
                <div><label className="block text-sm font-medium text-slate-300 mb-2">注册风险</label><Textarea name="riskRegulatory" value={editForm.riskRegulatory} onChange={handleEditChange} placeholder="法规路径不确定性" rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
              </div>
            </div>
            <div><h3 className="text-slate-100 font-semibold mb-4 pb-2 border-b border-slate-600">建议与所需支持</h3>
              <div><label className="block text-sm font-medium text-slate-300 mb-2">简述需要PMC提供的资源或决策支持</label><Textarea name="suggestionAndSupport" value={editForm.suggestionAndSupport} onChange={handleEditChange} placeholder="简述需要PMC提供的资源或决策支持" rows={3} className="bg-slate-700 border-slate-600 text-slate-100" /></div>
            </div>
            <div className="flex justify-end gap-3 pt-4 border-t border-slate-600"><Button variant="outline" onClick={() => setEditing(false)} className="bg-slate-700 text-slate-100 border-slate-600">取消</Button><Button onClick={handleSaveEdit} disabled={saving} className="bg-blue-600 hover:bg-blue-700 text-white">{saving ? '保存中...' : '保存'}</Button></div>
          </CardContent>
        </Card>
      </div>)}
    </div>
  );
};

export default ProjectDetail;