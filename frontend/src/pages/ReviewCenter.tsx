import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { ClipboardList, Clock, CheckCircle, AlertCircle, ArrowRight, Loader2, FileText, History, ChevronLeft, FileEdit } from 'lucide-react';
import api from '../lib/api';
import { useAuth } from '../contexts/AuthContext';

interface PendingTodoItem {
  /** 任务类型: PROJECT_COMPLETION / MILESTONE / INITIATION */
  todoType: 'PROJECT_COMPLETION' | 'MILESTONE' | 'INITIATION';
  taskId?: number;
  reviewApprovalId?: number;
  projectId: number;
  projectName: string;
  projectCode: string;
  milestoneName?: string;
  milestoneCode?: string;
  submitterName?: string;
  submittedAt?: string;
  approverRole?: string;
  reviewType?: string;
}

interface HistoryItem {
  id: number;
  projectId: number;
  projectName: string;
  projectCode: string;
  milestoneName?: string;
  milestoneCode?: string;
  action: string;
  actorName: string;
  actorRole: string;
  result: string;
  opinion: string;
  actionAt: string;
  /** 类型: milestone / initiation */
  historyType: string;
}

const reviewTypeMap: Record<string, string> = {
  INITIATION: '立项评审',
  MILESTONE: '里程碑评审',
};

const stepNameMap: Record<string, string> = {
  UPLOAD: '交付物上传',
  DEPT_HEAD_APPROVE: '部门负责人审批',
  PM_TECH_REVIEW: 'PM技术初评',
  COMPLIANCE_OPINION: '合规意见',
  PMC_DECISION: 'PMC决策',
  PM_INTERNAL_REVIEW: 'PM内部评审',
};

const resultBadgeMap: Record<string, { label: string; color: string }> = {
  GO: { label: '通过 (Go)', color: 'bg-green-600' },
  CONDITIONAL_GO: { label: '有条件通过', color: 'bg-yellow-600' },
  NO_GO: { label: '不通过', color: 'bg-red-600' },
  APPROVED: { label: '已通过', color: 'bg-green-600' },
  REJECTED: { label: '未通过', color: 'bg-red-600' },
};

const ReviewCenter: React.FC = () => {
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const [activeTab, setActiveTab] = useState('pending');
  const [pendingTodos, setPendingTodos] = useState<PendingTodoItem[]>([]);
  const [historyItems, setHistoryItems] = useState<HistoryItem[]>([]);
  const [loadingPending, setLoadingPending] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);

  const loadPendingTodos = async () => {
    setLoadingPending(true);
    try {
      // 加载里程碑评审待办
      const reviewRes = await api.get('/api/reviews/pending-tasks');
      const reviewResult = reviewRes.data as { code: number; data: any[]; message?: string };
      const todos: PendingTodoItem[] = [];

      if (reviewResult.code === 200 || reviewResult.code === 0) {
        for (const task of (reviewResult.data || [])) {
          todos.push({
            todoType: task.reviewType === 'INITIATION' ? 'INITIATION' : 'MILESTONE',
            taskId: task.taskId,
            reviewApprovalId: task.reviewApprovalId,
            projectId: task.projectId,
            projectName: task.projectName,
            projectCode: task.projectCode,
            milestoneName: task.milestoneName,
            milestoneCode: task.milestoneCode,
            submitterName: task.submitterName,
            submittedAt: task.submittedAt,
            approverRole: task.approverRole,
            reviewType: task.reviewType,
          });
        }
      }

      // 加载项目完善待办（DRAFT 状态项目 - 仅 PM 角色可见）
      if (hasRole('ROLE_PM')) {
        try {
          const projRes = await api.get('/api/projects');
          const projResult = projRes.data as { code: number; data: any[]; message?: string };
          if (projResult.code === 200 || projResult.code === 0) {
            for (const p of (projResult.data || [])) {
              if (p.projectStatus === 'DRAFT' && p.pmUserId === user?.id) {
                todos.push({
                  todoType: 'PROJECT_COMPLETION',
                  projectId: p.id,
                  projectName: p.projectName,
                  projectCode: p.projectCode,
                  milestoneName: p.lifecyclePhaseLabel || 'G0-项目立项',
                  milestoneCode: 'G0',
                });
              }
            }
          }
        } catch {}
      }

      setPendingTodos(todos);
    } catch (error: any) {
      console.error('加载待办事项失败:', error);
    } finally {
      setLoadingPending(false);
    }
  };

  const loadHistory = async () => {
    setLoadingHistory(true);
    try {
      const res = await api.get('/api/reviews/my-records');
      const result = res.data as { code: number; data: any[]; message?: string };
      const items: HistoryItem[] = [];

      if (result.code === 200 || result.code === 0) {
        for (const item of (result.data || [])) {
          items.push({
            id: item.id,
            projectId: item.projectId,
            projectName: item.projectName,
            projectCode: item.projectCode,
            milestoneName: item.milestoneName,
            milestoneCode: item.milestoneCode,
            action: item.action,
            actorName: item.actorName,
            actorRole: item.actorRole,
            result: item.result,
            opinion: item.opinion,
            actionAt: item.actionAt,
            historyType: 'milestone',
          });
        }
      }

      setHistoryItems(items);
    } catch (error: any) {
      console.error('加载已办事项失败:', error);
    } finally {
      setLoadingHistory(false);
    }
  };

  useEffect(() => {
    loadPendingTodos();
    loadHistory();
  }, []);

  const handleTodoClick = (todo: PendingTodoItem) => {
    if (todo.todoType === 'PROJECT_COMPLETION') {
      // 项目完善 - 跳转到创建项目页面填写剩余字段
      navigate(`/create-project?projectId=${todo.projectId}&mode=complete`);
    } else if (todo.todoType === 'INITIATION') {
      navigate(`/project/${todo.projectId}?tab=overview&openInitiation=true`);
    } else {
      // 里程碑评审
      navigate(`/project/${todo.projectId}?tab=milestone&reviewTaskId=${todo.taskId}`);
    }
  };

  const handleHistoryClick = (item: HistoryItem) => {
    navigate(`/project/${item.projectId}?tab=milestone`);
  };

  const getStepDisplayName = (todo: PendingTodoItem) => {
    if (!todo.approverRole) return '待处理';
    if (stepNameMap[todo.approverRole]) return stepNameMap[todo.approverRole];
    if (todo.approverRole === 'ROLE_PMC') return 'PMC决策';
    if (todo.approverRole === 'ROLE_PM') return 'PM技术初评';
    if (todo.approverRole === 'ROLE_DEPT_HEAD') return '部门负责人审批';
    if (todo.approverRole === 'ROLE_COMPLIANCE') return '合规意见';
    return todo.approverRole;
  };

  const getActionLabel = (todo: PendingTodoItem) => {
    if (todo.todoType === 'PROJECT_COMPLETION') return '完善项目信息';
    return '去评审';
  };

  const getTypeBadge = (todo: PendingTodoItem) => {
    if (todo.todoType === 'PROJECT_COMPLETION') {
      return <Badge className="bg-purple-600 text-white text-xs">项目新建</Badge>;
    }
    if (todo.todoType === 'INITIATION') {
      return <Badge className="bg-indigo-600 text-white text-xs">项目立项</Badge>;
    }
    return <Badge className="bg-yellow-600 text-white text-xs">里程碑评审</Badge>;
  };

  // 分三类统计
  const projectCompletionCount = pendingTodos.filter(t => t.todoType === 'PROJECT_COMPLETION').length;
  const initiationCount = pendingTodos.filter(t => t.todoType === 'INITIATION').length;
  const milestoneCount = pendingTodos.filter(t => t.todoType === 'MILESTONE').length;

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
        <div className="mb-6 flex items-center gap-4">
          <Button
            variant="outline"
            onClick={() => navigate('/dashboard')}
            className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            返回首页
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
              <ClipboardList className="w-6 h-6 text-blue-400" />
              待办事项
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              查看和管理您的待办任务
              {pendingTodos.length > 0 && (
                <span className="ml-2">
                  （项目新建 {projectCompletionCount}个 / 项目立项 {initiationCount}个 / 里程碑评审 {milestoneCount}个）
                </span>
              )}
            </p>
          </div>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="mb-6 bg-slate-800 border border-slate-600">
            <TabsTrigger
              value="pending"
              className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
            >
              <Clock className="w-4 h-4 mr-2" />
              待办事项
              {pendingTodos.length > 0 && (
                <Badge className="ml-2 bg-red-600 text-white text-xs px-2 py-0.5">
                  {pendingTodos.length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger
              value="history"
              className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
            >
              <History className="w-4 h-4 mr-2" />
              已办事项
            </TabsTrigger>
          </TabsList>

          <TabsContent value="pending">
            <Card className="bg-slate-800 border-slate-600">
              <CardHeader>
                <CardTitle className="text-slate-100 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-blue-400" />
                  待办事项列表
                </CardTitle>
              </CardHeader>
              <CardContent>
                {loadingPending ? (
                  <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    加载中...
                  </div>
                ) : pendingTodos.length === 0 ? (
                  <div className="text-center py-8">
                    <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-3" />
                    <p className="text-slate-400">暂无待办事项</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {pendingTodos.map((todo, idx) => (
                      <div
                        key={`${todo.todoType}-${todo.taskId || todo.projectId}-${idx}`}
                        className="p-4 bg-slate-700 rounded border border-slate-600 cursor-pointer hover:bg-slate-600 transition"
                        onClick={() => handleTodoClick(todo)}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <AlertCircle className="w-4 h-4 text-yellow-400" />
                            <span className="text-slate-100 font-medium">
                              {todo.projectName}
                            </span>
                            <Badge className="bg-blue-600 text-white text-xs">
                              {todo.milestoneCode || '-'}
                            </Badge>
                            {todo.milestoneName && (
                              <Badge className="bg-indigo-600 text-white text-xs">
                                {todo.milestoneName}
                              </Badge>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            {todo.approverRole && (
                              <Badge className="bg-slate-500 text-white text-xs">
                                {getStepDisplayName(todo)}
                              </Badge>
                            )}
                            {getTypeBadge(todo)}
                          </div>
                        </div>
                        <div className="flex items-center justify-between">
                          <div className="text-sm text-slate-400">
                            {todo.submitterName && (
                              <>
                                <span>提交人: {todo.submitterName}</span>
                                <span className="mx-2">|</span>
                              </>
                            )}
                            {todo.submittedAt && (
                              <span>
                                提交时间: {new Date(todo.submittedAt).toLocaleString('zh-CN')}
                              </span>
                            )}
                            {todo.todoType === 'PROJECT_COMPLETION' && (
                              <span>项目管理员已创建此项目，请尽快完善项目信息</span>
                            )}
                          </div>
                          <div className="flex items-center text-blue-400 text-sm">
                            {todo.todoType === 'PROJECT_COMPLETION' ? (
                              <>
                                <FileEdit className="w-4 h-4 mr-1" />
                                <span>完善项目信息</span>
                              </>
                            ) : (
                              <>
                                <span>去评审</span>
                              </>
                            )}
                            <ArrowRight className="w-4 h-4 ml-1" />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="history">
            <Card className="bg-slate-800 border-slate-600">
              <CardHeader>
                <CardTitle className="text-slate-100 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-400" />
                  已办事项列表
                </CardTitle>
              </CardHeader>
              <CardContent>
                {loadingHistory ? (
                  <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    加载中...
                  </div>
                ) : historyItems.length === 0 ? (
                  <div className="text-center py-8">
                    <FileText className="w-12 h-12 text-slate-500 mx-auto mb-3" />
                    <p className="text-slate-400">暂无已办事项记录</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {historyItems.map((item, idx) => (
                      <div
                        key={`${item.id}-${item.historyType}-${idx}`}
                        className="p-4 bg-slate-700 rounded border border-slate-600 cursor-pointer hover:bg-slate-600 transition"
                        onClick={() => handleHistoryClick(item)}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <span className="text-slate-100 font-medium">
                              {item.projectName}
                            </span>
                            <Badge className="bg-blue-600 text-white text-xs">
                              {item.milestoneCode || '-'}
                            </Badge>
                          </div>
                          <Badge className={`${resultBadgeMap[item.result]?.color || 'bg-gray-600'} text-white text-xs`}>
                            {resultBadgeMap[item.result]?.label || item.result || item.action}
                          </Badge>
                        </div>
                        <div className="text-sm text-slate-400">
                          <span>评审人: {item.actorName}</span>
                          <span className="mx-2">|</span>
                          <span>角色: {item.actorRole}</span>
                          <span className="mx-2">|</span>
                          <span>
                            时间: {item.actionAt ? new Date(item.actionAt).toLocaleString('zh-CN') : '-'}
                          </span>
                        </div>
                        {item.opinion && (
                          <p className="text-sm text-slate-300 mt-2">
                            意见: {item.opinion}
                          </p>
                        )}
                        <div className="mt-2 flex items-center text-blue-400 text-xs">
                          <span>查看项目</span>
                          <ArrowRight className="w-3 h-3 ml-1" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default ReviewCenter;