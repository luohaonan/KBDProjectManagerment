import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Textarea } from '../components/ui/textarea';
import { ChevronLeft } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';

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
  plannedStartDate: string;
  plannedEndDate: string;
  plannedPccDate: string;
  plannedIndDate: string;
  plannedNdaDate: string;
  budgetToPcc: number;
  riskScientific: string;
  riskCompetitive: string;
  riskRegulatory: string;
  suggestionAndSupport: string;
}

const CreateProject: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
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
    plannedStartDate: '',
    plannedEndDate: '',
    plannedPccDate: '',
    plannedIndDate: '',
    plannedNdaDate: '',
    budgetToPcc: 0,
    riskScientific: '',
    riskCompetitive: '',
    riskRegulatory: '',
    suggestionAndSupport: '',
  });

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      // 前端表单单位为万元，后端数据库存储单位为元，需转换
      const budgetTotalInYuan = formData.budgetTotal ? formData.budgetTotal * 10000 : 0;
      const budgetToPccInYuan = formData.budgetToPcc ? formData.budgetToPcc * 10000 : 0;

      const requestData = {
        projectName: formData.projectName,
        levelCode: formData.levelCode,
        indication: formData.indication,
        targetPathway: formData.targetPath,
        tppSummary: formData.description,
        description: formData.description,
        mechanism: formData.mechanism,
        unmetNeeds: formData.unmetNeeds,
        scientificBasis: formData.scientificBasis,
        expectedIndication: formData.expectedIndication,
        administrationRoute: formData.administrationRoute,
        dosageForm: formData.dosageForm,
        dosageFrequency: formData.dosageFrequency,
        efficacyTarget: formData.efficacyTarget,
        safetyAdvantage: formData.safetyAdvantage,
        differentiation: formData.differentiation,
        budgetTotal: budgetTotalInYuan,
        plannedPccDate: formData.plannedPccDate || null,
        plannedIndDate: formData.plannedIndDate || null,
        plannedNdaDate: formData.plannedNdaDate || null,
        budgetToPcc: budgetToPccInYuan,
        riskScientific: formData.riskScientific,
        riskCompetitive: formData.riskCompetitive,
        riskRegulatory: formData.riskRegulatory,
        suggestionAndSupport: formData.suggestionAndSupport,
      };

      const response = await api.post('/api/projects', requestData);
      toast.success('项目创建成功！');
      navigate(`/project/${response.data.data.id}`);
    } catch (error) {
      toast.error('项目创建失败，请重试');
      console.error('Create project error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
        <h1 className="text-3xl font-bold mb-6">创建新项目</h1>

        {/* 返回按钮 - 基本信息卡片左上方 */}
        <div className="mb-4">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => navigate('/')}
            className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            返回
          </Button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 基本信息 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">基本信息</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    项目名称 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    name="projectName"
                    value={formData.projectName}
                    onChange={handleChange}
                    placeholder="输入项目名称"
                    required
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    项目分级 <span className="text-red-500">*</span>
                  </label>
                  <Select value={formData.levelCode} onValueChange={(value) => handleSelectChange('levelCode', value)}>
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

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  项目描述
                </label>
                <Textarea
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  placeholder="项目详细描述"
                  rows={3}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div className="grid grid-cols-5 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    计划开始日期
                  </label>
                  <Input
                    type="date"
                    name="plannedStartDate"
                    value={formData.plannedStartDate}
                    onChange={handleChange}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    预估PCC提名日期
                  </label>
                  <Input
                    type="date"
                    name="plannedPccDate"
                    value={formData.plannedPccDate}
                    onChange={handleChange}
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
                    value={formData.plannedIndDate}
                    onChange={handleChange}
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
                    value={formData.plannedNdaDate}
                    onChange={handleChange}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    计划结束日期
                  </label>
                  <Input
                    type="date"
                    name="plannedEndDate"
                    value={formData.plannedEndDate}
                    onChange={handleChange}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    总预算 (万元)
                  </label>
                  <Input
                    type="number"
                    name="budgetTotal"
                    value={formData.budgetTotal}
                    onChange={handleChange}
                    placeholder="输入总预算"
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
                    value={formData.budgetToPcc}
                    onChange={handleChange}
                    placeholder="输入阶段预算至PCC"
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 科学依据 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">科学依据</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  靶点/通路 <span className="text-red-500">*</span>
                </label>
                <Input
                  name="targetPath"
                  value={formData.targetPath}
                  onChange={handleChange}
                  placeholder="描述靶点名称、生物学机制"
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  生物学机制 <span className="text-red-500">*</span>
                </label>
                <Textarea
                  name="mechanism"
                  value={formData.mechanism}
                  onChange={handleChange}
                  placeholder="详细说明靶点的生物学机制"
                  rows={3}
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    拟定适应症 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    name="indication"
                    value={formData.indication}
                    onChange={handleChange}
                    placeholder="例：肺癌、糖尿病等"
                    required
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    未满足的临床需求 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    name="unmetNeeds"
                    value={formData.unmetNeeds}
                    onChange={handleChange}
                    placeholder="现有治疗方案的局限性"
                    required
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  科学依据 <span className="text-red-500">*</span>
                </label>
                <Textarea
                  name="scientificBasis"
                  value={formData.scientificBasis}
                  onChange={handleChange}
                  placeholder="支持靶点与疾病关联性的关键文献/数据"
                  rows={3}
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
            </CardContent>
          </Card>

          {/* 目标产品概览 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">目标产品概览 (TPP)</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  预期适应症
                </label>
                <Input
                  name="expectedIndication"
                  value={formData.expectedIndication}
                  onChange={handleChange}
                  placeholder="预期商业化适应症"
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    给药途径
                  </label>
                  <Select
                    value={formData.administrationRoute}
                    onValueChange={(value) => handleSelectChange('administrationRoute', value)}
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
                    value={formData.dosageForm}
                    onValueChange={(value) => handleSelectChange('dosageForm', value)}
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
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  剂量频率
                </label>
                <Input
                  name="dosageFrequency"
                  value={formData.dosageFrequency}
                  onChange={handleChange}
                  placeholder="QD/BID 等"
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  预期疗效指标
                </label>
                <Textarea
                  name="efficacyTarget"
                  value={formData.efficacyTarget}
                  onChange={handleChange}
                  placeholder="详细说明预期的临床疗效指标"
                  rows={2}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  安全性优势
                </label>
                <Textarea
                  name="safetyAdvantage"
                  value={formData.safetyAdvantage}
                  onChange={handleChange}
                  placeholder="与现有治疗方案相比的安全性优势"
                  rows={2}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  差异化优势 <span className="text-red-500">*</span>
                </label>
                <Textarea
                  name="differentiation"
                  value={formData.differentiation}
                  onChange={handleChange}
                  placeholder="与现有/在研竞品相比的核心优势"
                  rows={2}
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
            </CardContent>
          </Card>

          {/* 项目风险评估 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">
                项目风险评估 <span className="text-red-500">*</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  科学风险
                </label>
                <Textarea
                  name="riskScientific"
                  value={formData.riskScientific}
                  onChange={handleChange}
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
                  value={formData.riskCompetitive}
                  onChange={handleChange}
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
                  value={formData.riskRegulatory}
                  onChange={handleChange}
                  placeholder="法规路径不确定性"
                  rows={3}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
            </CardContent>
          </Card>

          {/* 建议与所需支持 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">建议与所需支持</CardTitle>
            </CardHeader>
            <CardContent>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  简述需要PMC提供的资源或决策支持
                </label>
                <Textarea
                  name="suggestionAndSupport"
                  value={formData.suggestionAndSupport}
                  onChange={handleChange}
                  placeholder="简述需要PMC提供的资源或决策支持"
                  rows={3}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
            </CardContent>
          </Card>

          {/* 提交按钮 */}
          <div className="flex justify-end gap-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/')}
              className="bg-slate-700 text-slate-100 border-slate-600"
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={loading}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              {loading ? '创建中...' : '创建项目'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateProject;
