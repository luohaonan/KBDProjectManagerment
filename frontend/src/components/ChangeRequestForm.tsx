import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { toast } from 'sonner';
import api from '../lib/api';

interface ChangeRequestFormData {
  changeType: string;
  reasonText: string;
  beforeText: string;
  afterText: string;
  impactMilestoneText: string;
  impactBudgetText: string;
  impactResourceText: string;
}

interface ChangeRequestFormProps {
  projectId: number;
  onSuccess?: () => void;
  onCancel?: () => void;
}

// 变更类型映射（前端展示 → 后端Enum）
const changeTypeMap: Record<string, string> = {
  OBJECTIVE_SCOPE: '项目目标/范围变更',
  MILESTONE_SCHEDULE: '关键里程碑时间调整',
  BUDGET: '预算变更',
  OWNER_PM: '项目负责人变更',
  PAUSE_TERMINATE: '项目暂停/终止',
  OTHER: '其他',
};

export const ChangeRequestForm: React.FC<ChangeRequestFormProps> = ({
  projectId,
  onSuccess,
  onCancel,
}) => {
  const [formData, setFormData] = useState<ChangeRequestFormData>({
    changeType: 'MILESTONE_SCHEDULE',
    reasonText: '',
    beforeText: '',
    afterText: '',
    impactMilestoneText: '',
    impactBudgetText: '',
    impactResourceText: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await api.post(`/api/projects/${projectId}/change-requests`, {
        ...formData,
        requestedBy: Number(localStorage.getItem('userId') || '0'),
      });
      const result = res.data as { code: number; data: any; message?: string };
      if (result.code === 200 || result.code === 0) {
        toast.success('变更申请已提交，等待效率管理部审批');
        onSuccess?.();
      } else {
        toast.error(result.message || '提交失败');
      }
    } catch (error: any) {
      toast.error(error?.response?.data?.message || '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">项目变更申请</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                变更类型 <span className="text-red-500">*</span>
              </label>
              <Select
                value={formData.changeType}
                onValueChange={(value) => setFormData(prev => ({ ...prev, changeType: value }))}
              >
                <SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="bg-slate-700 border-slate-600">
                  {Object.entries(changeTypeMap).map(([key, label]) => (
                    <SelectItem key={key} value={key}>{label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                变更原因 <span className="text-red-500">*</span>
              </label>
              <Textarea
                name="reasonText"
                value={formData.reasonText}
                onChange={handleChange}
                placeholder="详细说明导致变更的背景、原因及必要性"
                rows={3}
                required
                className="bg-slate-700 border-slate-600 text-slate-100"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  变更前
                </label>
                <Textarea
                  name="beforeText"
                  value={formData.beforeText}
                  onChange={handleChange}
                  placeholder="描述原计划/状态"
                  rows={3}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  变更后
                </label>
                <Textarea
                  name="afterText"
                  value={formData.afterText}
                  onChange={handleChange}
                  placeholder="描述变更后的计划/状态"
                  rows={3}
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
            </div>

            <div className="border-t border-slate-600 pt-4">
              <h4 className="text-sm font-medium text-slate-300 mb-3">影响分析</h4>
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    对里程碑的影响
                  </label>
                  <Textarea
                    name="impactMilestoneText"
                    value={formData.impactMilestoneText}
                    onChange={handleChange}
                    placeholder="说明对后续里程碑的影响"
                    rows={2}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    对预算的影响
                  </label>
                  <Textarea
                    name="impactBudgetText"
                    value={formData.impactBudgetText}
                    onChange={handleChange}
                    placeholder="说明费用增减情况"
                    rows={2}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    对资源的影响
                  </label>
                  <Textarea
                    name="impactResourceText"
                    value={formData.impactResourceText}
                    onChange={handleChange}
                    placeholder="说明人力/外部资源需求变化"
                    rows={2}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-4 pt-4">
              {onCancel && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={onCancel}
                  className="bg-slate-700 text-slate-100 border-slate-600"
                >
                  取消
                </Button>
              )}
              <Button
                type="submit"
                disabled={submitting}
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                {submitting ? '提交中...' : '提交变更申请'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};