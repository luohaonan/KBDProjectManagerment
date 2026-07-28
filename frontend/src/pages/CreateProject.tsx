import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Textarea } from '../components/ui/textarea';
import { ChevronLeft } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';

interface ProjectFormData {
  projectName: string;
  levelCode: string;
  description: string;
  targetPath: string;
  mechanism: string;
  indication: string;
  unmetNeeds: string;
  scientificBasis: string;
  expectedIndication: string;
  administrationRoute: string;
  dosageForm: string;
  dosageFrequency: string;
  efficacyTarget: string;
  safetyAdvantage: string;
  differentiation: string;
  budgetTotal: number;
  plannedPccDate: string;
  plannedIndDate: string;
  plannedNdaDate: string;
  plannedEndDate: string;
  budgetToPcc: number;
  riskScientific: string;
  riskCompetitive: string;
  riskRegulatory: string;
  suggestionAndSupport: string;
  pmUserId: number | null;
}

interface PmUserOption {
  id: number;
  username: string;
  email: string;
}

interface ExistingProjectData {
  id: number;
  projectName: string;
  levelCode: string;
  pmUserId: number;
}

const CreateProject: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user, hasRole } = useAuth();
  const [loading, setLoading] = useState(false);
  const [pmUsers, setPmUsers] = useState<PmUserOption[]>([]);
  const [loadingPmUsers, setLoadingPmUsers] = useState(false);
  const [loadingProject, setLoadingProject] = useState(false);

  const mode = searchParams.get('mode') || 'create';
  const projectIdFromUrl = searchParams.get('projectId');
  const [existingProject, setExistingProject] = useState<ExistingProjectData | null>(null);

  const [formData, setFormData] = useState<ProjectFormData>({
    projectName: '',
    levelCode: 'H-L',
    description: '',
    targetPath: '',
    mechanism: '',
    indication: '',
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
    pmUserId: null,
  });

  const isAdminOrProjectAdmin = hasRole('ROLE_ADMIN') || hasRole('ROLE_PROJECT_ADMIN');
  const isCreateMode = mode === 'create';
  const isCompleteMode = mode === 'complete' && !!projectIdFromUrl;

  // 如果是完善模式，加载已有项目数据并验证用户是否是该项目指派的 PM
  useEffect(() => {
    if (isCompleteMode && projectIdFromUrl) {
      loadExistingProject();
    }
  }, [isCompleteMode, projectIdFromUrl]);

  const loadExistingProject = async () => {
    setLoadingProject(true);
    try {
      const res = await api.get(`/api/projects/${projectIdFromUrl}`);
      const data = res.data as { code: number; data: any; message?: string };
      if (data.code === 200 || data.code === 0) {
        const proj = data.data as any;
        // 验证当前用户是该项目的 PM
        if (proj.pmUserId && Number(proj.pmUserId) !== Number(user?.id)) {
          toast.error('只有被指派的项目经理才能完善该项目信息');
          navigate('/review-center');
          return;
        }
        setExistingProject({
          id: proj.id,
          projectName: proj.projectName,
          levelCode: proj.levelCode,
          pmUserId: proj.pmUserId,
        });
        setFormData(prev => ({
          ...prev,
          projectName: proj.projectName || '',
          levelCode: proj.levelCode || 'H-L',
          description: proj.description || '',
          pmUserId: proj.pmUserId || null,
        }));
      } else {
        toast.error(data.message || '加载项目信息失败');
        navigate('/review-center');
      }
    } catch {
      toast.error('加载项目信息失败');
      navigate('/review-center');
    } finally {
      setLoadingProject(false);
    }
  };

  // 加载项目经理候选用户列表（仅创建模式需要）
  useEffect(() => {
    if (user && isCreateMode && isAdminOrProjectAdmin) {
      loadPmUsers();
    }
  }, [user, isCreateMode, isAdminOrProjectAdmin]);

  const loadPmUsers = async () => {
    setLoadingPmUsers(true);
    try {
      const res = await api.get('/api/users/by-role', { params: { role: 'ROLE_PM' } });
      const result = res.data as { code: number; data: PmUserOption[] };
      if (result.code === 200 || result.code === 0) {
        setPmUsers(result.data || []);
      }
    } catch (error) {
      console.error('加载项目经理列表失败:', error);
    } finally {
      setLoadingPmUsers(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'number' ? Number(value) : value
    }));
  };

  const handleSelectChange = (name: string, value: string) => {
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePmSelectChange = (value: string) => {
    setFormData(prev => ({ ...prev, pmUserId: value ? parseInt(value, 10) : null }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 完善模式：通过 API 更新项目
    if (isCompleteMode && projectIdFromUrl) {
      if (!formData.indication.trim()) {
        toast.error('请填写拟定适应症');
        return;
      }

      setLoading(true);
      try {
        const budgetTotalInYuan = formData.budgetTotal || 0;
        const budgetToPccInYuan = formData.budgetToPcc || 0;

        await api.put(`/api/projects/${projectIdFromUrl}`, {
          projectName: formData.projectName.trim(),
          levelCode: formData.levelCode,
          indication: formData.indication || '',
          targetPathway: formData.targetPath || '',
          tppSummary: formData.description || '',
          description: formData.description || null,
          mechanism: formData.mechanism || null,
          unmetNeeds: formData.unmetNeeds || null,
          scientificBasis: formData.scientificBasis || null,
          expectedIndication: formData.expectedIndication || null,
          administrationRoute: formData.administrationRoute || null,
          dosageForm: formData.dosageForm || null,
          dosageFrequency: formData.dosageFrequency || null,
          efficacyTarget: formData.efficacyTarget || null,
          safetyAdvantage: formData.safetyAdvantage || null,
          differentiation: formData.differentiation || null,
          budgetTotal: budgetTotalInYuan,
          plannedPccDate: formData.plannedPccDate || null,
          plannedIndDate: formData.plannedIndDate || null,
          plannedNdaDate: formData.plannedNdaDate || null,
          plannedEndDate: formData.plannedEndDate || null,
          budgetToPcc: budgetToPccInYuan,
          riskScientific: formData.riskScientific || null,
          riskCompetitive: formData.riskCompetitive || null,
          riskRegulatory: formData.riskRegulatory || null,
          suggestionAndSupport: formData.suggestionAndSupport || null,
        });

        toast.success('项目信息已完善！');
        navigate(`/project/${projectIdFromUrl}`);
      } catch (error) {
        toast.error('保存失败，请重试');
        console.error('Update project error:', error);
      } finally {
        setLoading(false);
      }
      return;
    }

    // 创建模式（管理员/项目管理员）
    if (!formData.projectName.trim()) {
      toast.error('请输入项目名称');
      return;
    }
    if (!formData.pmUserId) {
      toast.error('请选择项目经理');
      return;
    }

    setLoading(true);
    try {
      const requestData: any = {
        projectName: formData.projectName.trim(),
        levelCode: formData.levelCode,
        pmUserId: formData.pmUserId,
        createdByUserId: user?.id || null,
        // 管理员只填基本信息，其余字段为空
        indication: '',
        targetPathway: '',
        tppSummary: '',
        description: formData.description || null,
        mechanism: null,
        unmetNeeds: null,
        scientificBasis: null,
        expectedIndication: null,
        administrationRoute: null,
        dosageForm: null,
        dosageFrequency: null,
        efficacyTarget: null,
        safetyAdvantage: null,
        differentiation: null,
        budgetTotal: 0,
        plannedPccDate: null,
        plannedIndDate: null,
        plannedNdaDate: null,
        plannedEndDate: null,
        budgetToPcc: 0,
        riskScientific: null,
        riskCompetitive: null,
        riskRegulatory: null,
        suggestionAndSupport: null,
      };

      const response = await api.post('/api/projects', requestData);
      toast.success('项目创建成功！已通知项目经理完善项目信息。');
      navigate(`/project/${response.data.data.id}`);
    } catch (error) {
      toast.error('项目创建失败，请重试');
      console.error('Create project error:', error);
    } finally {
      setLoading(false);
    }
  };

  // 如果不是管理员/项目管理员来创建，也不是 PM 来完善，则重定向
  if (!isCompleteMode && !isAdminOrProjectAdmin) {
    navigate('/dashboard', { replace: true });
    return null;
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
        {isCompleteMode ? (
          <h1 className="mb-6 text-3xl font-bold text-slate-900">完善项目信息</h1>
        ) : (
          <h1 className="mb-6 text-3xl font-bold text-slate-900">创建新项目</h1>
        )}

        <div className="mb-4">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => navigate(isCompleteMode ? '/review-center' : '/dashboard')}
            className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            返回
          </Button>
        </div>

        {loadingProject ? (
          <div className="flex items-center justify-center py-20">
            <span className="text-slate-400">加载中...</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 基本信息卡片 - 始终显示 */}
            <Card>
              <CardHeader>
                <CardTitle className="text-slate-900">基本信息</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">
                      项目名称 <span className="text-red-500">*</span>
                    </label>
                    <Input
                      name="projectName"
                      value={formData.projectName}
                      onChange={handleChange}
                      placeholder="输入项目名称"
                      required
                      disabled={isCompleteMode}
                    />
                  </div>
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">
                      项目分级 <span className="text-red-500">*</span>
                    </label>
                    <Select value={formData.levelCode} onValueChange={(value) => handleSelectChange('levelCode', value)}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
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

                {/* 项目经理下拉 - 仅创建模式 */}
                {isCreateMode && (
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">
                      项目经理 <span className="text-red-500">*</span>
                    </label>
                    <Select
                      value={formData.pmUserId ? String(formData.pmUserId) : ''}
                      onValueChange={handlePmSelectChange}
                      disabled={loadingPmUsers}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder={loadingPmUsers ? '加载中...' : '请选择项目经理'} />
                      </SelectTrigger>
                      <SelectContent className="max-h-60">
                        {pmUsers.map((pm) => (
                          <SelectItem key={pm.id} value={String(pm.id)}>
                            {pm.username} {pm.id === user?.id ? '（当前用户）' : ''}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <p className="mt-1 text-xs text-slate-500">
                      选择此项目的负责人，创建完成后将通知项目经理完善项目信息
                    </p>
                  </div>
                )}

                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-700">
                    项目描述
                  </label>
                  <Textarea
                    name="description"
                    value={formData.description}
                    onChange={handleChange}
                    placeholder="项目详细描述"
                    rows={3}
                  />
                </div>

                {/* 完整模式下的日期和预算 */}
                {isCompleteMode && (
                  <>
                    <div className="grid grid-cols-5 gap-4">
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">预估PCC提名日期</label>
                        <Input type="date" name="plannedPccDate" value={formData.plannedPccDate} onChange={handleChange} />
                      </div>
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">预估IND获批日期</label>
                        <Input type="date" name="plannedIndDate" value={formData.plannedIndDate} onChange={handleChange} />
                      </div>
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">预估NDA获批日期</label>
                        <Input type="date" name="plannedNdaDate" value={formData.plannedNdaDate} onChange={handleChange} />
                      </div>
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">计划结束日期</label>
                        <Input type="date" name="plannedEndDate" value={formData.plannedEndDate} onChange={handleChange} />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">总预算 (元)</label>
                        <Input type="number" name="budgetTotal" value={formData.budgetTotal} onChange={handleChange} placeholder="输入总预算" />
                      </div>
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">阶段预算至PCC (元)</label>
                        <Input type="number" name="budgetToPcc" value={formData.budgetToPcc} onChange={handleChange} placeholder="输入阶段预算至PCC" />
                      </div>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>

            {/* 其余卡片仅在 PM 完善模式下显示 */}
            {isCompleteMode && (
              <>
                <Card>
                  <CardHeader><CardTitle className="text-slate-900">科学依据</CardTitle></CardHeader>
                  <CardContent className="space-y-4">
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">靶点/通路</label><Input name="targetPath" value={formData.targetPath} onChange={handleChange} placeholder="描述靶点名称、生物学机制" /></div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">生物学机制</label><Textarea name="mechanism" value={formData.mechanism} onChange={handleChange} placeholder="详细说明靶点的生物学机制" rows={3} /></div>
                    <div className="grid grid-cols-2 gap-4">
                      <div><label className="mb-2 block text-sm font-medium text-slate-700">拟定适应症 <span className="text-red-500">*</span></label><Input name="indication" value={formData.indication} onChange={handleChange} placeholder="例：肺癌、糖尿病等" required /></div>
                      <div><label className="mb-2 block text-sm font-medium text-slate-700">未满足的临床需求</label><Input name="unmetNeeds" value={formData.unmetNeeds} onChange={handleChange} placeholder="现有治疗方案的局限性" /></div>
                    </div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">科学依据</label><Textarea name="scientificBasis" value={formData.scientificBasis} onChange={handleChange} placeholder="支持靶点与疾病关联性的关键文献/数据" rows={3} /></div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader><CardTitle className="text-slate-900">目标产品概览 (TPP)</CardTitle></CardHeader>
                  <CardContent className="space-y-4">
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">预期适应症</label><Input name="expectedIndication" value={formData.expectedIndication} onChange={handleChange} placeholder="预期商业化适应症" /></div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">给药途径</label>
                        <Select value={formData.administrationRoute} onValueChange={(value) => handleSelectChange('administrationRoute', value)}>
                          <SelectTrigger><SelectValue placeholder="请选择" /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="口服">口服</SelectItem><SelectItem value="注射">注射</SelectItem><SelectItem value="吸入">吸入</SelectItem>
                            <SelectItem value="经皮">经皮</SelectItem><SelectItem value="舌下">舌下</SelectItem><SelectItem value="直肠">直肠</SelectItem>
                            <SelectItem value="眼用">眼用</SelectItem><SelectItem value="鼻用">鼻用</SelectItem><SelectItem value="外用">外用</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">剂型</label>
                        <Select value={formData.dosageForm} onValueChange={(value) => handleSelectChange('dosageForm', value)}>
                          <SelectTrigger><SelectValue placeholder="请选择" /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="片剂">片剂</SelectItem><SelectItem value="胶囊剂">胶囊剂</SelectItem><SelectItem value="注射剂">注射剂</SelectItem>
                            <SelectItem value="颗粒剂">颗粒剂</SelectItem><SelectItem value="口服液">口服液</SelectItem><SelectItem value="混悬剂">混悬剂</SelectItem>
                            <SelectItem value="乳膏剂">乳膏剂</SelectItem><SelectItem value="贴剂">贴剂</SelectItem><SelectItem value="气雾剂">气雾剂</SelectItem>
                            <SelectItem value="滴眼剂">滴眼剂</SelectItem><SelectItem value="栓剂">栓剂</SelectItem><SelectItem value="丸剂">丸剂</SelectItem>
                            <SelectItem value="散剂">散剂</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">剂量频率</label><Input name="dosageFrequency" value={formData.dosageFrequency} onChange={handleChange} placeholder="QD/BID 等" /></div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">预期疗效指标</label><Textarea name="efficacyTarget" value={formData.efficacyTarget} onChange={handleChange} placeholder="详细说明预期的临床疗效指标" rows={2} /></div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">安全性优势</label><Textarea name="safetyAdvantage" value={formData.safetyAdvantage} onChange={handleChange} placeholder="与现有治疗方案相比的安全性优势" rows={2} /></div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">差异化优势</label><Textarea name="differentiation" value={formData.differentiation} onChange={handleChange} placeholder="与现有/在研竞品相比的核心优势" rows={2} /></div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader><CardTitle className="text-slate-900">项目风险评估</CardTitle></CardHeader>
                  <CardContent className="space-y-4">
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">科学风险</label><Textarea name="riskScientific" value={formData.riskScientific} onChange={handleChange} placeholder="靶点有效性风险、成药性风险、安全性风险" rows={3} /></div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">竞争风险</label><Textarea name="riskCompetitive" value={formData.riskCompetitive} onChange={handleChange} placeholder="主要竞品进展" rows={3} /></div>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">注册风险</label><Textarea name="riskRegulatory" value={formData.riskRegulatory} onChange={handleChange} placeholder="法规路径不确定性" rows={3} /></div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader><CardTitle className="text-slate-900">建议与所需支持</CardTitle></CardHeader>
                  <CardContent>
                    <div><label className="mb-2 block text-sm font-medium text-slate-700">简述需要PMC提供的资源或决策支持</label><Textarea name="suggestionAndSupport" value={formData.suggestionAndSupport} onChange={handleChange} placeholder="简述需要PMC提供的资源或决策支持" rows={3} /></div>
                  </CardContent>
                </Card>
              </>
            )}

            {/* 提交按钮 */}
            <div className="flex justify-end gap-4">
              <Button type="button" variant="outline" onClick={() => navigate(isCompleteMode ? '/review-center' : '/dashboard')}>
                取消
              </Button>
              <Button type="submit" disabled={loading} className="bg-blue-600 hover:bg-blue-700 text-white">
                {loading ? '保存中...' : isCompleteMode ? '保存并提交' : '创建项目并通知项目经理'}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default CreateProject;