import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

interface ChangeRequestFormData {
  changeType: 'MILESTONE_RESCHEDULE' | 'BUDGET_CHANGE' | 'SCOPE_CHANGE' | 'RESOURCE_CHANGE';
  title: string;
  description: string;
  reason: string;
  impact: string;
  // Milestone reschedule specific
  milestoneCode?: string;
  newPlannedDate?: string;
  // Budget change specific
  budgetChangeAmount?: number;
  budgetChangeReason?: string;
}

interface ChangeRequestFormProps {
  projectId: number;
  onSubmit?: (data: ChangeRequestFormData) => void;
  onCancel?: () => void;
}

export const ChangeRequestForm: React.FC<ChangeRequestFormProps> = ({
  projectId: _projectId,
  onSubmit,
  onCancel,
}) => {
  const [formData, setFormData] = useState<ChangeRequestFormData>({
    changeType: 'SCOPE_CHANGE',
    title: '',
    description: '',
    reason: '',
    impact: '',
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onSubmit) {
      onSubmit(formData);
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
                onValueChange={(value) => handleSelectChange('changeType', value)}
              >
                <SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="bg-slate-700 border-slate-600">
                  <SelectItem value="SCOPE_CHANGE">范围变更</SelectItem>
                  <SelectItem value="MILESTONE_RESCHEDULE">里程碑调整</SelectItem>
                  <SelectItem value="BUDGET_CHANGE">预算变更</SelectItem>
                  <SelectItem value="RESOURCE_CHANGE">资源变更</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                变更标题 <span className="text-red-500">*</span>
              </label>
              <Input
                name="title"
                value={formData.title}
                onChange={handleChange}
                placeholder="简要描述变更内容"
                required
                className="bg-slate-700 border-slate-600 text-slate-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                详细描述 <span className="text-red-500">*</span>
              </label>
              <Textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="详细说明变更的具体内容"
                rows={4}
                required
                className="bg-slate-700 border-slate-600 text-slate-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                变更原因 <span className="text-red-500">*</span>
              </label>
              <Textarea
                name="reason"
                value={formData.reason}
                onChange={handleChange}
                placeholder="说明提出此变更的原因"
                rows={3}
                required
                className="bg-slate-700 border-slate-600 text-slate-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                预期影响 <span className="text-red-500">*</span>
              </label>
              <Textarea
                name="impact"
                value={formData.impact}
                onChange={handleChange}
                placeholder="描述此变更对项目进度、成本、质量等方面的影响"
                rows={3}
                required
                className="bg-slate-700 border-slate-600 text-slate-100"
              />
            </div>

            {/* 里程碑调整特定字段 */}
            {formData.changeType === 'MILESTONE_RESCHEDULE' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    里程碑代码
                  </label>
                  <Input
                    name="milestoneCode"
                    value={formData.milestoneCode || ''}
                    onChange={handleChange}
                    placeholder="要调整的里程碑代码"
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    新计划日期
                  </label>
                  <Input
                    type="date"
                    name="newPlannedDate"
                    value={formData.newPlannedDate || ''}
                    onChange={handleChange}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>
            )}

            {/* 预算变更特定字段 */}
            {formData.changeType === 'BUDGET_CHANGE' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    预算变更金额 (万元)
                  </label>
                  <Input
                    type="number"
                    name="budgetChangeAmount"
                    value={formData.budgetChangeAmount || ''}
                    onChange={handleChange}
                    placeholder="正数表示增加，负数表示减少"
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    预算变更原因
                  </label>
                  <Textarea
                    name="budgetChangeReason"
                    value={formData.budgetChangeReason || ''}
                    onChange={handleChange}
                    placeholder="详细说明预算变更的具体原因"
                    rows={2}
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
              </div>
            )}

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
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                提交变更申请
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};