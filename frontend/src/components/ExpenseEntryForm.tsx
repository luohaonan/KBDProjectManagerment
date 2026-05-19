import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

interface ExpenseEntryData {
  projectId: number;
  milestoneCode: string;
  expenseType: 'INTERNAL' | 'EXTERNAL' | 'EQUIPMENT' | 'TRAVEL' | 'CONSULTING' | 'OTHER';
  amount: number;
  description: string;
  expenseDate: string;
  vendor?: string;
  invoiceNumber?: string;
}

interface ExpenseEntryFormProps {
  projectId: number;
  onSubmit?: (data: ExpenseEntryData) => void;
  onCancel?: () => void;
}

export const ExpenseEntryForm: React.FC<ExpenseEntryFormProps> = ({
  projectId,
  onSubmit,
  onCancel,
}) => {
  const [formData, setFormData] = useState<ExpenseEntryData>({
    projectId,
    milestoneCode: '',
    expenseType: 'INTERNAL',
    amount: 0,
    description: '',
    expenseDate: new Date().toISOString().split('T')[0],
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
          <CardTitle className="text-slate-100">支出录入</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  里程碑代码 <span className="text-red-500">*</span>
                </label>
                <Input
                  name="milestoneCode"
                  value={formData.milestoneCode}
                  onChange={handleChange}
                  placeholder="例：G1-01, G2-02"
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  支出类型 <span className="text-red-500">*</span>
                </label>
                <Select
                  value={formData.expenseType}
                  onValueChange={(value) => handleSelectChange('expenseType', value)}
                >
                  <SelectTrigger className="bg-slate-700 border-slate-600 text-slate-100">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="bg-slate-700 border-slate-600">
                    <SelectItem value="INTERNAL">内部研发费用</SelectItem>
                    <SelectItem value="EXTERNAL">外部合作费用</SelectItem>
                    <SelectItem value="EQUIPMENT">设备采购</SelectItem>
                    <SelectItem value="TRAVEL">差旅费</SelectItem>
                    <SelectItem value="CONSULTING">咨询费</SelectItem>
                    <SelectItem value="OTHER">其他</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  支出金额 (元) <span className="text-red-500">*</span>
                </label>
                <Input
                  type="number"
                  name="amount"
                  value={formData.amount}
                  onChange={handleChange}
                  placeholder="输入支出金额"
                  min="0"
                  step="0.01"
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  支出日期 <span className="text-red-500">*</span>
                </label>
                <Input
                  type="date"
                  name="expenseDate"
                  value={formData.expenseDate}
                  onChange={handleChange}
                  required
                  className="bg-slate-700 border-slate-600 text-slate-100"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                支出描述 <span className="text-red-500">*</span>
              </label>
              <Textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="详细说明此次支出的用途和内容"
                rows={3}
                required
                className="bg-slate-700 border-slate-600 text-slate-100"
              />
            </div>

            {(formData.expenseType === 'EXTERNAL' || formData.expenseType === 'EQUIPMENT' || formData.expenseType === 'CONSULTING') && (
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    供应商/服务商
                  </label>
                  <Input
                    name="vendor"
                    value={formData.vendor || ''}
                    onChange={handleChange}
                    placeholder="供应商名称"
                    className="bg-slate-700 border-slate-600 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    发票号
                  </label>
                  <Input
                    name="invoiceNumber"
                    value={formData.invoiceNumber || ''}
                    onChange={handleChange}
                    placeholder="发票号码"
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
                录入支出
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};