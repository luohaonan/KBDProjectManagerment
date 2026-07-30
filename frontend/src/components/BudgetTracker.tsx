import React, { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { PieChart, Pie, Cell, Legend, Tooltip, ResponsiveContainer } from 'recharts';
import { AlertTriangle } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';
import { EXPENSE_CATEGORY_OPTIONS } from '../constants/expenseCategories';

interface BudgetLedgerItem {
  id: number;
  occurredOn: string;
  expenseCategory: string;
  amount: number;
  vendorName: string | null;
  referenceNo: string | null;
  description: string | null;
}

interface BudgetTrackerData {
  totalBudget: number;
  totalSpent: number;
  remainingBudget: number;
  utilizationRatio: number;
  warningLevel: string;
  warningThresholdPercent: number;
  ledgerItems: BudgetLedgerItem[];
}

interface BudgetTrackerProps {
  projectName?: string;
  projectId: number;
  refreshKey?: number;
}

const COLORS = {
  spent: '#f97316',
  remaining: '#22c55e',
};

export const BudgetTracker: React.FC<BudgetTrackerProps> = ({
  projectName = '项目',
  projectId,
  refreshKey = 0,
}) => {
  const [data, setData] = useState<BudgetTrackerData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        const res = await api.get(`/api/budgets/projects/${projectId}/management`);
        setData(res.data.data);
      } catch (error: any) {
        toast.error(error.response?.data?.message || '加载预算追踪数据失败');
      } finally {
        setLoading(false);
      }
    };
    void loadData();
  }, [projectId, refreshKey]);

  const spent = data?.totalSpent ?? 0;
  const remaining = data?.remainingBudget ?? 0;
  const totalBudget = data?.totalBudget ?? 0;
  const spentPercent = totalBudget > 0 ? (data?.utilizationRatio ?? 0).toFixed(1) : '0.0';

  const chartData = [
    { name: '已支出', value: spent, color: COLORS.spent },
    { name: '剩余预算', value: remaining, color: COLORS.remaining },
  ];

  const costBreakdownData = useMemo(() => {
    const totals = new Map<string, number>();
    for (const option of EXPENSE_CATEGORY_OPTIONS) {
      totals.set(option.value, 0);
    }
    for (const item of data?.ledgerItems || []) {
      totals.set(item.expenseCategory, (totals.get(item.expenseCategory) || 0) + (item.amount || 0));
    }
    return EXPENSE_CATEGORY_OPTIONS
      .map((option) => ({
        name: option.label,
        value: totals.get(option.value) || 0,
        description: option.description,
      }))
      .filter((item) => item.value > 0 || !(data?.ledgerItems?.length));
  }, [data]);

  const BREAKDOWN_COLORS = ['#3b82f6', '#a855f7', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6'];

  const getBudgetAlert = () => {
    const percent = data?.utilizationRatio ?? 0;
    const warningThreshold = data?.warningThresholdPercent ?? 80;
    if (percent > 95) {
      return {
        level: 'critical',
        message: '预算使用率超过95%，请立即采取措施控制支出！',
        color: 'text-red-500',
        bgColor: 'bg-red-900/20',
      };
    } else if (percent > warningThreshold) {
      return {
        level: 'warning',
        message: `预算使用率超过${warningThreshold}%，请注意控制支出。`,
        color: 'text-yellow-500',
        bgColor: 'bg-yellow-900/20',
      };
    }
    return null;
  };

  const budgetAlert = getBudgetAlert();

  if (loading) {
    return <Card className="bg-slate-800 border-slate-600"><CardContent className="py-6 text-slate-300">预算追踪加载中...</CardContent></Card>;
  }

  if (!data) {
    return null;
  }

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
                  ¥{(totalBudget / 10000).toFixed(2)}万
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
                    当前预警阈值 {data.warningThresholdPercent?.toFixed(1) || '80.0'}% / {budgetAlert.level === 'critical'
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
              {costBreakdownData.length === 0 ? (
                <div className="p-4 bg-slate-700 rounded text-sm text-slate-400">暂无费用分类数据</div>
              ) : costBreakdownData.map((item, index) => (
                <div key={item.name} className="p-4 bg-slate-700 rounded border" style={{ borderColor: `${BREAKDOWN_COLORS[index]}55` }}>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 rounded" style={{ backgroundColor: BREAKDOWN_COLORS[index] }} />
                      <p className="text-slate-300 font-medium">{item.name}</p>
                    </div>
                    <span className="text-xs text-slate-400">
                      {spent > 0 ? `${((item.value / spent) * 100).toFixed(1)}%` : '0.0%'}
                    </span>
                  </div>
                  <p className="text-xl font-bold text-slate-100">
                    ¥{(item.value / 10000).toFixed(2)}万
                  </p>
                  <p className="text-xs text-slate-400 mt-2">
                    {item.description || '—'}
                  </p>
                </div>
              ))}
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

    </div>
  );
};
