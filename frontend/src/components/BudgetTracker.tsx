import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { PieChart, Pie, Cell, Legend, Tooltip, ResponsiveContainer } from 'recharts';
import { ExpenseEntryForm } from './ExpenseEntryForm';
import { Plus, AlertTriangle } from 'lucide-react';

interface BudgetData {
  internalCost: number;
  externalCost: number;
  totalBudget: number;
}

interface BudgetTrackerProps {
  data: BudgetData;
  projectName?: string;
  projectId?: number;
}

const COLORS = {
  spent: '#f97316',
  remaining: '#22c55e',
};

export const BudgetTracker: React.FC<BudgetTrackerProps> = ({
  data,
  projectName = '项目',
  projectId = 1,
}) => {
  const [showExpenseForm, setShowExpenseForm] = useState(false);

  const spent = data.internalCost + data.externalCost;
  const remaining = Math.max(0, data.totalBudget - spent);
  const spentPercent = ((spent / data.totalBudget) * 100).toFixed(1);

  const chartData = [
    { name: '已支出', value: spent, color: COLORS.spent },
    { name: '剩余预算', value: remaining, color: COLORS.remaining },
  ];

  // 内部费用和外部费用的对比数据
  const costBreakdownData = [
    {
      name: '内部研发费用',
      value: data.internalCost,
    },
    {
      name: '外部费用',
      value: data.externalCost,
    },
  ];
  
  const BREAKDOWN_COLORS = ['#3b82f6', '#a855f7'];

  const handleExpenseSubmit = (expenseData: any) => {
    console.log('录入支出：', expenseData);
    alert('支出已录入！');
    setShowExpenseForm(false);
  };

  const getBudgetAlert = () => {
    const percent = (spent / data.totalBudget) * 100;
    if (percent > 95) {
      return {
        level: 'critical',
        message: '预算使用率超过95%，请立即采取措施控制支出！',
        color: 'text-red-500',
        bgColor: 'bg-red-900/20',
      };
    } else if (percent > 80) {
      return {
        level: 'warning',
        message: '预算使用率超过80%，请注意控制支出。',
        color: 'text-yellow-500',
        bgColor: 'bg-yellow-900/20',
      };
    }
    return null;
  };

  const budgetAlert = getBudgetAlert();

  return (
    <div className="space-y-6">
      {/* 预算总体追踪 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">预算追踪 - {projectName}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* 环形图 */}
            <div className="flex justify-center">
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1e293b',
                      border: '1px solid #475569',
                    }}
                    formatter={(value: number) => `¥${(value / 10000).toFixed(2)}万`}
                  />
                  <Legend
                    wrapperStyle={{
                      paddingTop: '20px',
                      color: '#cbd5e1',
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* 预算详情 */}
            <div className="space-y-4">
              {/* 总预算 */}
              <div className="p-4 bg-slate-700 rounded">
                <p className="text-slate-400 text-sm mb-1">总预算</p>
                <p className="text-2xl font-bold text-slate-100">
                  ¥{(data.totalBudget / 10000).toFixed(2)}万
                </p>
              </div>

              {/* 已支出 */}
              <div className={`p-4 rounded ${budgetAlert?.bgColor || 'bg-slate-700'}`}>
                <div className="flex items-center justify-between mb-2">
                  <p className={`text-sm font-medium ${budgetAlert?.color || 'text-slate-100'}`}>已支出</p>
                  <span className={`text-xs font-bold px-2 py-1 rounded ${budgetAlert?.color || 'text-slate-100'} bg-slate-700`}>
                    {spentPercent}%
                  </span>
                </div>
                <p className="text-xl font-bold text-slate-100">
                  ¥{(spent / 10000).toFixed(2)}万
                </p>
              </div>

              {/* 剩余预算 */}
              <div className="p-4 bg-slate-700 rounded">
                <p className="text-slate-400 text-sm mb-1">剩余预算</p>
                <p className="text-xl font-bold text-green-400">
                  ¥{(remaining / 10000).toFixed(2)}万
                </p>
              </div>

              {/* 预警信息 */}
              {budgetAlert && (
                <div className={`p-3 rounded text-sm ${budgetAlert.bgColor}`}>
                  <p className={`font-semibold ${budgetAlert.color}`}>
                    {budgetAlert.level === 'critical' ? '⚠️ 严重预警' : '⚠️ 预算预警'}
                  </p>
                  <p className="text-slate-300 text-xs mt-1">
                    {budgetAlert.level === 'critical'
                      ? '预算使用率已超过95%，需立即联系 PMC 申请追加预算'
                      : '预算使用率已超过80%，请留意预算动向'}
                  </p>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 费用分类详情 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">费用分类</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* 费用分类环形图 */}
            <div className="flex justify-center">
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={costBreakdownData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {costBreakdownData.map((_entry, index) => (
                      <Cell key={`cell-${index}`} fill={BREAKDOWN_COLORS[index]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1e293b',
                      border: '1px solid #475569',
                    }}
                    formatter={(value: number) => `¥${(value / 10000).toFixed(2)}万`}
                  />
                  <Legend
                    wrapperStyle={{
                      paddingTop: '20px',
                      color: '#cbd5e1',
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* 费用明细 */}
            <div className="space-y-4">
              {/* 内部研发费用 */}
              <div className="p-4 bg-slate-700 rounded border border-blue-600/30">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded bg-blue-500" />
                    <p className="text-slate-300 font-medium">内部研发费用</p>
                  </div>
                  <span className="text-xs text-slate-400">
                    {((data.internalCost / spent) * 100).toFixed(1)}%
                  </span>
                </div>
                <p className="text-xl font-bold text-slate-100">
                  ¥{(data.internalCost / 10000).toFixed(2)}万
                </p>
                <p className="text-xs text-slate-400 mt-2">
                  包括：人力、实验耗材、设备折旧等
                </p>
              </div>

              {/* 外部费用 */}
              <div className="p-4 bg-slate-700 rounded border border-purple-600/30">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded bg-purple-500" />
                    <p className="text-slate-300 font-medium">外部费用</p>
                  </div>
                  <span className="text-xs text-slate-400">
                    {((data.externalCost / spent) * 100).toFixed(1)}%
                  </span>
                </div>
                <p className="text-xl font-bold text-slate-100">
                  ¥{(data.externalCost / 10000).toFixed(2)}万
                </p>
                <p className="text-xs text-slate-400 mt-2">
                  包括：CRO/CDMO、临床中心、第三方服务、注册费用等
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 预算预警 */}
      {budgetAlert && (
        <Card className={`border-slate-600 ${budgetAlert.bgColor}`}>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <AlertTriangle className={`w-5 h-5 ${budgetAlert.color}`} />
              <div>
                <p className={`font-medium ${budgetAlert.color}`}>预算预警</p>
                <p className="text-sm text-slate-300">{budgetAlert.message}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* 支出录入 */}
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-slate-100">支出管理</h3>
        <Button
          onClick={() => setShowExpenseForm(!showExpenseForm)}
          className="bg-blue-600 hover:bg-blue-700"
        >
          <Plus className="w-4 h-4 mr-2" />
          {showExpenseForm ? '取消录入' : '录入支出'}
        </Button>
      </div>

      {showExpenseForm && (
        <ExpenseEntryForm
          projectId={projectId}
          onSubmit={handleExpenseSubmit}
          onCancel={() => setShowExpenseForm(false)}
        />
      )}
    </div>
  );
};
