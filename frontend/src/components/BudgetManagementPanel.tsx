import React, { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import api from '../lib/api';
import { toast } from 'sonner';
import { EXPENSE_CATEGORY_LABEL_MAP, EXPENSE_CATEGORY_OPTIONS } from '../constants/expenseCategories';
import { useAuth } from '../contexts/AuthContext';

interface BudgetWorkflowStep {
  stepCode: string;
  stepName: string;
  status: string;
}

interface BudgetWorkflowProgress {
  requestId: number | null;
  status: string;
  currentNodeName: string | null;
  steps: BudgetWorkflowStep[];
}

interface BudgetLedgerItem {
  id: number;
  occurredOn: string;
  expenseCategory: string;
  amount: number;
  vendorName: string | null;
  referenceNo: string | null;
  description: string | null;
}

interface BudgetAdjustmentItem {
  id: number;
  previousBudgetAmount: number;
  requestedBudgetAmount: number;
  adjustmentAmount: number;
  reasonText: string;
  status: string;
  requestedByName: string | null;
  requestedAt: string | null;
  progress: BudgetWorkflowProgress;
}

interface BudgetManagementResponse {
  projectId: number;
  projectCode: string;
  projectName: string;
  totalBudget: number;
  totalSpent: number;
  remainingBudget: number;
  utilizationRatio: number;
  warningLevel: string;
  warningThresholdPercent: number;
  canManage: boolean;
  canApprove: boolean;
  ledgerItems: BudgetLedgerItem[];
  adjustments: BudgetAdjustmentItem[];
  currentApprovalProgress: BudgetWorkflowProgress;
}

function fmtMoney(value: number | null | undefined) {
  return `¥${(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function getTodayDateString() {
  return new Date().toISOString().split('T')[0];
}

export const BudgetManagementPanel: React.FC<{ projectId: number; projectName?: string; onDataChanged?: () => void }> = ({ projectId, onDataChanged }) => {
  const { user } = useAuth();
  const [data, setData] = useState<BudgetManagementResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submittingExpense, setSubmittingExpense] = useState(false);
  const [decisionSubmitting, setDecisionSubmitting] = useState(false);
  const [decisionOpinion, setDecisionOpinion] = useState('');
  const [activeView, setActiveView] = useState<'adjustment' | 'expenditure'>('adjustment');
  const [expandedAdjustmentIds, setExpandedAdjustmentIds] = useState<number[]>([]);
  const [form, setForm] = useState({ requestedBudget: '', reasonText: '' });
  const [expenseForm, setExpenseForm] = useState({ occurredOn: getTodayDateString(), expenseCategory: EXPENSE_CATEGORY_OPTIONS[0]?.value || 'INTERNAL', amount: '', vendorName: '', referenceNo: '', description: '' });

  const formatBeijingDateTime = (value: string | null | undefined) => {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat('zh-CN', {
      timeZone: 'Asia/Shanghai',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(date).replace(/\//g, '-');
  };

  const toggleAdjustmentExpanded = (id: number) => {
    setExpandedAdjustmentIds(prev => prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]);
  };

  const loadData = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/api/budgets/projects/${projectId}/management`);
      setData(res.data.data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || '加载预算管理数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, [projectId]);

  const latestAdjustment = useMemo(() => data?.adjustments?.[0] || null, [data]);
  const requestedBudgetValue = Number(form.requestedBudget || 0);
  const adjustmentAmount = useMemo(() => {
    const base = data?.totalBudget || 0;
    if (!form.requestedBudget) return 0;
    return requestedBudgetValue - base;
  }, [data?.totalBudget, requestedBudgetValue, form.requestedBudget]);

  const adjustmentLabel = adjustmentAmount >= 0 ? '增加' : '减少';
  const pendingApprovalItem = useMemo(() => data?.adjustments?.find(item => item.progress?.status?.startsWith?.('PENDING_')) || null, [data]);
  const pendingApprovalStepCode = pendingApprovalItem?.progress?.status?.startsWith('PENDING_')
    ? pendingApprovalItem.progress.status.replace('PENDING_', '')
    : undefined;

  const handleDecision = async (decision: 'APPROVE' | 'REJECT' | 'CONDITIONAL_APPROVE') => {
    if (!pendingApprovalItem || !user?.id) {
      toast.error('当前没有可审批的预算调整申请');
      return;
    }
    try {
      setDecisionSubmitting(true);
      await api.post(`/api/projects/${projectId}/change-requests/${pendingApprovalItem.id}/decision`, {
        actorUserId: user.id,
        decision,
        stepCode: pendingApprovalStepCode,
        opinion: decisionOpinion.trim() || null,
      });
      toast.success(
        decision === 'REJECT'
          ? '预算调整已驳回'
          : decision === 'CONDITIONAL_APPROVE'
            ? '预算调整已附条件通过'
            : '预算调整已审批通过'
      );
      setDecisionOpinion('');
      await loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '提交预算审批失败');
    } finally {
      setDecisionSubmitting(false);
    }
  };

  const renderProgress = (progress?: BudgetWorkflowProgress | null) => {
    if (!progress || !progress.steps || progress.steps.length === 0) {
      return <div className="text-xs text-slate-500">暂无审批进度</div>;
    }
    return (
      <div className="space-y-2">
        <div className="text-xs text-slate-300">当前节点：{progress.currentNodeName || '已完成/未开始'}</div>
        <div className="flex flex-wrap gap-2">
          {progress.steps.map((step) => {
            const color = step.status === 'COMPLETED'
              ? 'bg-green-900/40 text-green-300 border-green-700'
              : step.status === 'IN_PROGRESS'
                ? 'bg-blue-900/40 text-blue-300 border-blue-700'
                : step.status === 'REJECTED'
                  ? 'bg-red-900/40 text-red-300 border-red-700'
                  : 'bg-slate-800 text-slate-400 border-slate-600';
            return (
              <div key={step.stepCode} className={`rounded border px-3 py-2 text-xs ${color}`}>
                <div className="font-medium">{step.stepName}</div>
                <div className="mt-1">{step.status}</div>
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  const handleSubmit = async () => {
    if (!form.requestedBudget || !form.reasonText.trim()) {
      toast.error('请填写调整后预算和调整原因');
      return;
    }
    try {
      setSubmitting(true);
      await api.post(`/api/budgets/projects/${projectId}/adjustments`, {
        requestedBudget: Number(form.requestedBudget),
        reasonText: form.reasonText.trim(),
      });
      toast.success('预算调整申请已提交，等待审批');
      setForm({ requestedBudget: '', reasonText: '' });
      await loadData();
      onDataChanged?.();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '提交预算调整失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleExpenseSubmit = async () => {
    if (!expenseForm.amount || Number(expenseForm.amount) <= 0) {
      toast.error('请填写有效的支出金额');
      return;
    }
    try {
      setSubmittingExpense(true);
      await api.post(`/api/budgets/projects/${projectId}/expenditures`, {
        occurredOn: expenseForm.occurredOn || null,
        expenseCategory: expenseForm.expenseCategory,
        amount: Number(expenseForm.amount),
        vendorName: expenseForm.vendorName.trim() || null,
        referenceNo: expenseForm.referenceNo.trim() || null,
        description: expenseForm.description.trim() || null,
      });
      toast.success('支出记录已新增');
      setExpenseForm({ occurredOn: getTodayDateString(), expenseCategory: EXPENSE_CATEGORY_OPTIONS[0]?.value || 'INTERNAL', amount: '', vendorName: '', referenceNo: '', description: '' });
      await loadData();
      onDataChanged?.();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '新增支出记录失败');
    } finally {
      setSubmittingExpense(false);
    }
  };

  if (loading) {
    return <Card className="bg-slate-800 border-slate-600"><CardContent className="py-6 text-slate-300">预算管理加载中...</CardContent></Card>;
  }

  if (!data) return null;

  return (
    <div className="space-y-6">
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader><CardTitle className="text-slate-100">预算总览</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="rounded bg-slate-700 p-4"><div className="text-sm text-slate-400">项目总预算</div><div className="mt-2 text-xl font-bold text-slate-100">{fmtMoney(data.totalBudget)}</div></div>
            <div className="rounded bg-slate-700 p-4"><div className="text-sm text-slate-400">累计执行</div><div className="mt-2 text-xl font-bold text-slate-100">{fmtMoney(data.totalSpent)}</div></div>
            <div className="rounded bg-slate-700 p-4"><div className="text-sm text-slate-400">剩余预算</div><div className="mt-2 text-xl font-bold text-green-400">{fmtMoney(data.remainingBudget)}</div></div>
            <div className="rounded bg-slate-700 p-4"><div className="text-sm text-slate-400">执行率 / 预警</div><div className="mt-2 text-xl font-bold text-slate-100">{(data.utilizationRatio || 0).toFixed(1)}%</div><div className={`text-xs mt-1 ${data.warningLevel === 'RED' ? 'text-red-400' : data.warningLevel === 'YELLOW' ? 'text-yellow-400' : 'text-green-400'}`}>{data.warningLevel} / 阈值 {(data.warningThresholdPercent || 0).toFixed(1)}%</div></div>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button onClick={() => setActiveView('adjustment')} className={activeView === 'adjustment' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-slate-700 hover:bg-slate-600'}>预算调整</Button>
            <Button onClick={() => setActiveView('expenditure')} className={activeView === 'expenditure' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-slate-700 hover:bg-slate-600'}>支出管理</Button>
          </div>
          <div className="rounded border border-slate-600 bg-slate-700/60 p-4">
            <div className="mb-2 text-sm font-medium text-slate-100">当前审批进度</div>
            {renderProgress(data.currentApprovalProgress)}
          </div>
        </CardContent>
      </Card>

      {activeView === 'adjustment' ? (
        <Card className="bg-slate-800 border-slate-600">
          <CardHeader><CardTitle className="text-slate-100">预算调整明细</CardTitle></CardHeader>
          <CardContent className="space-y-6">
            {!data.canManage && !data.canApprove && <div className="rounded bg-amber-900/30 p-3 text-sm text-amber-300">当前账号暂无预算管理权限，可查看预算数据但不可提交预算调整或维护支出记录。</div>}
            {latestAdjustment && (
              <div className="rounded border border-slate-600 bg-slate-700 p-4 text-sm text-slate-200">
                最近一次申请：由 {latestAdjustment.requestedByName || '-'} 于 {formatBeijingDateTime(latestAdjustment.requestedAt)} 提交，
                调整至 <span className="font-semibold">{fmtMoney(latestAdjustment.requestedBudgetAmount)}</span>，状态：
                <span className="ml-1 font-semibold">{latestAdjustment.status}</span>
              </div>
            )}
            {data.canApprove && pendingApprovalItem && (
              <div className="rounded border border-blue-700 p-4 space-y-4">
                <div>
                  <div className="text-sm font-semibold text-slate-100">待我审批的预算调整</div>
                  <div className="mt-2 text-sm text-slate-200">
                    申请人：{pendingApprovalItem.requestedByName || '-'}；状态：{pendingApprovalItem.status}
                  </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                  <div className="rounded p-3 border border-slate-700">
                    <div className="text-slate-400">调整前预算</div>
                    <div className="mt-1 font-semibold text-slate-100">{fmtMoney(pendingApprovalItem.previousBudgetAmount)}</div>
                  </div>
                  <div className="rounded p-3 border border-slate-700">
                    <div className="text-slate-400">调整后预算</div>
                    <div className="mt-1 font-semibold text-slate-100">{fmtMoney(pendingApprovalItem.requestedBudgetAmount)}</div>
                  </div>
                  <div className="rounded p-3 border border-slate-700">
                    <div className="text-slate-400">调整金额</div>
                    <div className="mt-1 font-semibold text-slate-100">{pendingApprovalItem.adjustmentAmount >= 0 ? '+' : ''}{fmtMoney(pendingApprovalItem.adjustmentAmount)}</div>
                  </div>
                </div>
                <div className="text-sm text-slate-300">调整原因：{pendingApprovalItem.reasonText || '-'}</div>
                <div>
                  <label className="block text-sm text-slate-300 mb-2">审批意见</label>
                  <Textarea rows={3} value={decisionOpinion} onChange={e => setDecisionOpinion(e.target.value)} className="bg-slate-700 border-slate-600 text-slate-100" />
                </div>
                <div className="flex flex-wrap gap-3 justify-end">
                  <Button disabled={decisionSubmitting} onClick={() => handleDecision('REJECT')} className="bg-red-600 hover:bg-red-700 text-white">{decisionSubmitting ? '提交中...' : '驳回'}</Button>
                  <Button disabled={decisionSubmitting} onClick={() => handleDecision('CONDITIONAL_APPROVE')} className="bg-amber-600 hover:bg-amber-700 text-white">{decisionSubmitting ? '提交中...' : '附条件通过'}</Button>
                  <Button disabled={decisionSubmitting} onClick={() => handleDecision('APPROVE')} className="bg-green-600 hover:bg-green-700 text-white">{decisionSubmitting ? '提交中...' : '审批通过'}</Button>
                </div>
              </div>
            )}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm text-slate-300 mb-2">调整后项目总预算</label>
                <Input type="number" value={form.requestedBudget} disabled={!data.canManage} onChange={e => setForm(prev => ({ ...prev, requestedBudget: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
              </div>
              <div>
                <label className="block text-sm text-slate-300 mb-2">调整金额</label>
                <div className="rounded border border-slate-600 bg-slate-700 px-3 py-2 text-sm text-slate-100">
                  {form.requestedBudget ? `${adjustmentLabel} ${fmtMoney(Math.abs(adjustmentAmount))}` : '输入调整后预算后自动计算'}
                </div>
              </div>
            </div>
            <div>
              <label className="block text-sm text-slate-300 mb-2">调整原因</label>
              <Textarea rows={3} value={form.reasonText} disabled={!data.canManage} onChange={e => setForm(prev => ({ ...prev, reasonText: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
            </div>
            <div className="flex justify-end">
              <Button onClick={handleSubmit} disabled={!data.canManage || submitting} className="bg-blue-600 hover:bg-blue-700 text-white">{submitting ? '提交中...' : '提交预算调整审批'}</Button>
            </div>

            <div className="space-y-3 border-t border-slate-700 pt-4">
              <div className="text-sm font-medium text-slate-100">预算调整记录</div>
              {data.adjustments.length === 0 ? <div className="text-sm text-slate-400">暂无预算调整记录</div> : data.adjustments.map(item => {
                const expanded = expandedAdjustmentIds.includes(item.id);
                return (
                  <div key={item.id} className="rounded border border-slate-700 bg-slate-700 text-sm text-slate-200">
                    <button
                      type="button"
                      onClick={() => toggleAdjustmentExpanded(item.id)}
                      className="flex w-full flex-wrap items-center justify-between gap-3 p-4 text-left hover:bg-slate-600/40"
                    >
                      <div>#{item.id} · {item.requestedByName || '-'} · {formatBeijingDateTime(item.requestedAt)}</div>
                      <div className="flex items-center gap-3">
                        <div className="font-semibold">{item.status}</div>
                        <div className="text-xs text-slate-400">{expanded ? '收起' : '展开'}</div>
                      </div>
                    </button>
                    {expanded && (
                      <div className="border-t border-slate-600 px-4 pb-4 pt-3">
                        <div className="text-slate-300">{fmtMoney(item.previousBudgetAmount)} → {fmtMoney(item.requestedBudgetAmount)}</div>
                        <div className="mt-1 text-slate-300">调整金额：{item.adjustmentAmount >= 0 ? '+' : ''}{fmtMoney(item.adjustmentAmount)}</div>
                        <div className="mt-1 text-slate-400">{item.reasonText}</div>
                        <div className="mt-3">{renderProgress(item.progress)}</div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card className="bg-slate-800 border-slate-600">
          <CardHeader><CardTitle className="text-slate-100">支出管理</CardTitle></CardHeader>
          <CardContent className="space-y-6">
            {!data.canManage && <div className="rounded bg-amber-900/30 p-3 text-sm text-amber-300">当前账号没有支出管理权限。</div>}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm text-slate-300 mb-2">支出日期</label>
                <Input type="date" value={expenseForm.occurredOn} disabled={!data.canManage} onChange={e => setExpenseForm(prev => ({ ...prev, occurredOn: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
              </div>
              <div>
                <label className="block text-sm text-slate-300 mb-2">支出分类</label>
                <select value={expenseForm.expenseCategory} disabled={!data.canManage} onChange={e => setExpenseForm(prev => ({ ...prev, expenseCategory: e.target.value }))} className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2">
                  {EXPENSE_CATEGORY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm text-slate-300 mb-2">支出金额</label>
                <Input type="number" value={expenseForm.amount} disabled={!data.canManage} onChange={e => setExpenseForm(prev => ({ ...prev, amount: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
              </div>
              <div>
                <label className="block text-sm text-slate-300 mb-2">供应商</label>
                <Input value={expenseForm.vendorName} disabled={!data.canManage} onChange={e => setExpenseForm(prev => ({ ...prev, vendorName: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
              </div>
              <div>
                <label className="block text-sm text-slate-300 mb-2">单据号</label>
                <Input value={expenseForm.referenceNo} disabled={!data.canManage} onChange={e => setExpenseForm(prev => ({ ...prev, referenceNo: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
              </div>
              <div className="md:col-span-3">
                <label className="block text-sm text-slate-300 mb-2">支出说明</label>
                <Textarea rows={3} value={expenseForm.description} disabled={!data.canManage} onChange={e => setExpenseForm(prev => ({ ...prev, description: e.target.value }))} className="bg-slate-700 border-slate-600 text-slate-100" />
              </div>
            </div>
            <div className="flex justify-end">
              <Button onClick={handleExpenseSubmit} disabled={!data.canManage || submittingExpense} className="bg-blue-600 hover:bg-blue-700 text-white">{submittingExpense ? '提交中...' : '新增支出记录'}</Button>
            </div>
            <div className="overflow-x-auto border-t border-slate-700 pt-4">
              <table className="min-w-full text-sm text-slate-200">
                <thead>
                  <tr className="border-b border-slate-700 text-slate-400">
                    <th className="px-3 py-2 text-left">日期</th>
                    <th className="px-3 py-2 text-left">分类</th>
                    <th className="px-3 py-2 text-left">金额</th>
                    <th className="px-3 py-2 text-left">供应商/单据号</th>
                    <th className="px-3 py-2 text-left">说明</th>
                  </tr>
                </thead>
                <tbody>
                  {data.ledgerItems.length === 0 ? (
                    <tr><td colSpan={5} className="px-3 py-4 text-center text-slate-400">暂无支出记录</td></tr>
                  ) : data.ledgerItems.map(item => (
                    <tr key={item.id} className="border-b border-slate-800">
                      <td className="px-3 py-2">{item.occurredOn || '-'}</td>
                      <td className="px-3 py-2">{item.expenseCategory ? (EXPENSE_CATEGORY_LABEL_MAP[item.expenseCategory] || item.expenseCategory) : '-'}</td>
                      <td className="px-3 py-2">{fmtMoney(item.amount)}</td>
                      <td className="px-3 py-2">{[item.vendorName, item.referenceNo].filter(Boolean).join(' / ') || '-'}</td>
                      <td className="px-3 py-2">{item.description || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};