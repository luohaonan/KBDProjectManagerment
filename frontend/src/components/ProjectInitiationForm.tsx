import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { FileUp, Download } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';

interface FormData {
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
}

interface ProjectInitiationFormProps {
  projectId?: string;
  initialData?: Partial<FormData>;
  onSubmit?: (data: FormData) => void;
}

export const ProjectInitiationForm: React.FC<ProjectInitiationFormProps> = ({
  projectId,
  initialData,
  onSubmit,
}) => {
  const [formData, setFormData] = useState<FormData>({
    targetPath: initialData?.targetPath || '',
    mechanism: initialData?.mechanism || '',
    indication: initialData?.indication || '',
    unmetNeeds: initialData?.unmetNeeds || '',
    scientificBasis: initialData?.scientificBasis || '',
    expectedIndication: initialData?.expectedIndication || '',
    administrationRoute: initialData?.administrationRoute || '',
    dosageForm: initialData?.dosageForm || '',
    dosageFrequency: initialData?.dosageFrequency || '',
    efficacyTarget: initialData?.efficacyTarget || '',
    safetyAdvantage: initialData?.safetyAdvantage || '',
    differentiation: initialData?.differentiation || '',
  });
  const [downloading, setDownloading] = useState(false);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onSubmit) {
      onSubmit(formData);
    }
  };

  const handleDownloadPdf = async () => {
    if (!projectId) {
      toast.error('项目 ID 不存在');
      return;
    }
    setDownloading(true);
    try {
      // 从 projectId (projectCode) 获取实际项目 ID
      // 这里 projectId 传的是 projectCode，需要先获取项目详情
      const res = await api.get(`/api/projects/${projectId}/initiation-report`);
      const result = res.data as { code: number; data: any; message?: string };
      if (result.code === 200 || result.code === 0) {
        const reportData = result.data;
        // 使用动态导入加载 jspdf
        const { default: jsPDF } = await import('jspdf');
        const doc = new jsPDF('p', 'mm', 'a4');

        // 标题
        doc.setFontSize(18);
        doc.text('项目立项报告', 105, 20, { align: 'center' });
        doc.setFontSize(14);
        doc.text('Project Initiation Report', 105, 28, { align: 'center' });

        // 分隔线
        doc.setDrawColor(200, 200, 200);
        doc.line(15, 33, 195, 33);

        let y = 42;
        const leftMargin = 15;
        const labelWidth = 60;
        const lineHeight = 8;

        const addField = (label: string, value: string | null | undefined) => {
          if (y > 275) {
            doc.addPage();
            y = 20;
          }
          doc.setFontSize(10);
          doc.setTextColor(100, 100, 100);
          doc.text(label, leftMargin, y);
          doc.setTextColor(0, 0, 0);
          const displayValue = value || '-';
          // 处理长文本换行
          const maxWidth = 170;
          const lines = doc.splitTextToSize(displayValue, maxWidth);
          doc.text(lines, leftMargin + labelWidth, y);
          y += lineHeight * Math.max(1, lines.length);
        };

        // 基本信息
        doc.setFontSize(12);
        doc.setTextColor(50, 50, 50);
        doc.text('基本信息', leftMargin, y);
        y += 6;
        addField('项目编号:', reportData.projectCode);
        addField('项目名称:', reportData.projectName);
        addField('项目分级:', `${reportData.levelName} (${reportData.levelCode})`);
        addField('靶点/通路:', reportData.targetPathway);
        addField('适应症:', reportData.indication);
        addField('项目描述:', reportData.tppSummary);

        y += 4;
        doc.setDrawColor(200, 200, 200);
        doc.line(leftMargin, y, 195, y);
        y += 6;

        // 科学依据
        doc.setFontSize(12);
        doc.setTextColor(50, 50, 50);
        doc.text('科学依据', leftMargin, y);
        y += 6;
        addField('生物学机制:', reportData.mechanism);
        addField('未满足需求:', reportData.unmetNeeds);
        addField('科学依据:', reportData.scientificBasis);

        y += 4;
        doc.setDrawColor(200, 200, 200);
        doc.line(leftMargin, y, 195, y);
        y += 6;

        // TPP
        doc.setFontSize(12);
        doc.setTextColor(50, 50, 50);
        doc.text('目标产品概览 (TPP)', leftMargin, y);
        y += 6;
        addField('预期适应症:', reportData.expectedIndication);
        addField('给药途径:', reportData.administrationRoute);
        addField('剂型:', reportData.dosageForm);
        addField('剂量频率:', reportData.dosageFrequency);
        addField('预期疗效指标:', reportData.efficacyTarget);
        addField('安全性优势:', reportData.safetyAdvantage);
        addField('差异化优势:', reportData.differentiation);

        y += 4;
        doc.setDrawColor(200, 200, 200);
        doc.line(leftMargin, y, 195, y);
        y += 6;

        // 发起人信息
        doc.setFontSize(12);
        doc.setTextColor(50, 50, 50);
        doc.text('发起信息', leftMargin, y);
        y += 6;
        addField('发起人:', reportData.initiatorName);
        addField('发起时间:', reportData.initiationTime);

        // 保存 PDF
        doc.save(`立项报告_${reportData.projectCode}_${new Date().toISOString().slice(0, 10)}.pdf`);
        toast.success('立项报告已下载');
      } else {
        toast.error(result.message || '获取报告数据失败');
      }
    } catch (error: any) {
      toast.error('下载失败: ' + (error.response?.data?.message || error.message));
      console.error('Download PDF error:', error);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* 项目背景与依据 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">项目背景与依据</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                靶点/通路 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="targetPath"
                value={formData.targetPath}
                onChange={handleChange}
                placeholder="描述靶点名称、生物学机制"
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                生物学机制 <span className="text-red-500">*</span>
              </label>
              <textarea
                name="mechanism"
                value={formData.mechanism}
                onChange={handleChange}
                placeholder="详细说明靶点的生物学机制"
                rows={3}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  拟定适应症 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="indication"
                  value={formData.indication}
                  onChange={handleChange}
                  placeholder="例：肺癌、糖尿病等"
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  未满足的临床需求 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="unmetNeeds"
                  value={formData.unmetNeeds}
                  onChange={handleChange}
                  placeholder="现有治疗方案的局限性"
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                科学依据 <span className="text-red-500">*</span>
              </label>
              <textarea
                name="scientificBasis"
                value={formData.scientificBasis}
                onChange={handleChange}
                placeholder="支持靶点与疾病关联性的关键文献/数据"
                rows={3}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>
          </form>
        </CardContent>
      </Card>

      {/* 目标产品概览 (TPP) */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">目标产品概览 (TPP) 初拟</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                预期适应症 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="expectedIndication"
                value={formData.expectedIndication}
                onChange={handleChange}
                placeholder="具体描述"
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  给药途径 <span className="text-red-500">*</span>
                </label>
                <select
                  name="administrationRoute"
                  value={formData.administrationRoute}
                  onChange={handleChange}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100"
                  required
                >
                  <option value="">请选择</option>
                  <option value="口服">口服</option>
                  <option value="注射">注射</option>
                  <option value="吸入">吸入</option>
                  <option value="经皮">经皮</option>
                  <option value="舌下">舌下</option>
                  <option value="直肠">直肠</option>
                  <option value="眼用">眼用</option>
                  <option value="鼻用">鼻用</option>
                  <option value="外用">外用</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  剂型 <span className="text-red-500">*</span>
                </label>
                <select
                  name="dosageForm"
                  value={formData.dosageForm}
                  onChange={handleChange}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100"
                  required
                >
                  <option value="">请选择</option>
                  <option value="片剂">片剂</option>
                  <option value="胶囊剂">胶囊剂</option>
                  <option value="注射剂">注射剂</option>
                  <option value="颗粒剂">颗粒剂</option>
                  <option value="口服液">口服液</option>
                  <option value="混悬剂">混悬剂</option>
                  <option value="乳膏剂">乳膏剂</option>
                  <option value="贴剂">贴剂</option>
                  <option value="气雾剂">气雾剂</option>
                  <option value="滴眼剂">滴眼剂</option>
                  <option value="栓剂">栓剂</option>
                  <option value="丸剂">丸剂</option>
                  <option value="散剂">散剂</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  剂量频率 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="dosageFrequency"
                  value={formData.dosageFrequency}
                  onChange={handleChange}
                  placeholder="QD/BID 等"
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                预期疗效指标 <span className="text-red-500">*</span>
              </label>
              <textarea
                name="efficacyTarget"
                value={formData.efficacyTarget}
                onChange={handleChange}
                placeholder="详细说明预期的临床疗效指标"
                rows={2}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                安全性优势 <span className="text-red-500">*</span>
              </label>
              <textarea
                name="safetyAdvantage"
                value={formData.safetyAdvantage}
                onChange={handleChange}
                placeholder="与现有治疗方案相比的安全性优势"
                rows={2}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>
          </form>
        </CardContent>
      </Card>

      {/* 差异化优势 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">差异化优势</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                与现有/在研竞品相比的核心优势 <span className="text-red-500">*</span>
              </label>
              <textarea
                name="differentiation"
                value={formData.differentiation}
                onChange={handleChange}
                placeholder="详细说明本产品的预期核心优势"
                rows={4}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-slate-100 placeholder-slate-400"
                required
              />
            </div>

            {/* 文件上传示例 */}
            <div className="border-2 border-dashed border-slate-600 rounded p-6 text-center">
              <FileUp className="w-8 h-8 text-slate-400 mx-auto mb-2" />
              <p className="text-slate-300 text-sm">
                拖拽或点击上传支持文件
              </p>
              <p className="text-slate-500 text-xs mt-1">
                支持 PDF, DOC, DOCX 格式，单个文件不超过 50MB
              </p>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="flex justify-end gap-4">
        <Button variant="outline" className="bg-slate-700 text-slate-100 border-slate-600">
          取消
        </Button>
        <Button
          onClick={handleDownloadPdf}
          disabled={downloading}
          className="bg-green-600 hover:bg-green-700 text-white"
        >
          <Download className="w-4 h-4 mr-2" />
          {downloading ? '下载中...' : '下载立项表单'}
        </Button>
      </div>
    </div>
  );
};
