import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { ClipboardList, Clock, CheckCircle, AlertCircle, ArrowRight, Loader2, FileText, History, ChevronLeft } from 'lucide-react';
import api from '../lib/api';

interface PendingReviewTask {
  id: number;
  reviewApprovalId: number;
  projectId: number;
  projectName: string;
  projectCode: string;
  milestoneName: string;
  milestoneCode: string;
  submitterName: string;
  submittedAt: string;
  taskId: number;
  approverRole: string;
  reviewType: string;
}

interface ReviewHistoryItem {
  id: number;
  projectId: number;
  projectName: string;
  projectCode: string;
  milestoneName: string;
  milestoneCode: string;
  action: string;
  actorName: string;
  actorRole: string;
  result: string;
  opinion: string;
  actionAt: string;
}

const reviewTypeMap: Record<string, string> = {
  INITIATION: '立项评审',
  MILESTONE: '里程碑评审',
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
  const [activeTab, setActiveTab] = useState('pending');
  const [pendingTasks, setPendingTasks] = useState<PendingReviewTask[]>([]);
  const [historyRecords, setHistoryRecords] = useState<ReviewHistoryItem[]>([]);
  const [loadingPending, setLoadingPending] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // 加载待办评审
  const loadPendingTasks = async () => {
    setLoadingPending(true);
    try {
      const res = await api.get('/api/reviews/my-tasks');
      const result = res.data as { code: number; data: PendingReviewTask[]; message?: string };
      if (result.code === 200 || result.code === 0) {
        setPendingTasks(result.data || []);
      }
    } catch (error: any) {
      console.error('加载待办评审失败:', error);
    } finally {
      setLoadingPending(false);
    }
  };

  // 加载评审历史
  const loadHistory = async () => {
    setLoadingHistory(true);
    try {
      const res = await api.get('/api/reviews/my-records');
      const result = res.data as { code: number; data: ReviewHistoryItem[]; message?: string };
      if (result.code === 200 || result.code === 0) {
        setHistoryRecords(result.data || []);
      }
    } catch (error: any) {
      console.error('加载评审历史失败:', error);
    } finally {
      setLoadingHistory(false);
    }
  };

  useEffect(() => {
    loadPendingTasks();
    loadHistory();
  }, []);

  // 点击待办评审，跳转到审批页面
  const handleTaskClick = (task: PendingReviewTask) => {
    if (task.reviewType === 'INITIATION') {
      navigate(`/project/${task.projectId}/approval`);
    } else {
      // 里程碑评审 - 跳转到项目详情的里程碑控制台
      navigate(`/project/${task.projectId}?tab=milestone`);
    }
  };

  // 点击评审历史，跳转到里程碑控制台
  const handleHistoryClick = (item: ReviewHistoryItem) => {
    navigate(`/project/${item.projectId}?tab=milestone`);
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
        {/* 头部导航 */}
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
              评审中心
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              查看和管理您的评审任务
            </p>
          </div>
        </div>

        {/* 选项卡 */}
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="mb-6 bg-slate-800 border border-slate-600">
            <TabsTrigger
              value="pending"
              className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
            >
              <Clock className="w-4 h-4 mr-2" />
              待办评审
              {pendingTasks.length > 0 && (
                <Badge className="ml-2 bg-red-600 text-white text-xs px-2 py-0.5">
                  {pendingTasks.length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger
              value="history"
              className="data-[state=active]:bg-slate-700 data-[state=active]:text-white text-slate-300"
            >
              <History className="w-4 h-4 mr-2" />
              评审历史
            </TabsTrigger>
          </TabsList>

          {/* 待办评审列表 */}
          <TabsContent value="pending">
            <Card className="bg-slate-800 border-slate-600">
              <CardHeader>
                <CardTitle className="text-slate-100 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-blue-400" />
                  待办评审列表
                </CardTitle>
              </CardHeader>
              <CardContent>
                {loadingPending ? (
                  <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    加载中...
                  </div>
                ) : pendingTasks.length === 0 ? (
                  <div className="text-center py-8">
                    <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-3" />
                    <p className="text-slate-400">暂无待办评审任务</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {pendingTasks.map((task) => (
                      <div
                        key={task.taskId}
                        className="p-4 bg-slate-700 rounded border border-slate-600 cursor-pointer hover:bg-slate-600 transition"
                        onClick={() => handleTaskClick(task)}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <AlertCircle className="w-4 h-4 text-yellow-400" />
                            <span className="text-slate-100 font-medium">
                              {task.projectName}
                            </span>
                            <Badge className="bg-blue-600 text-white text-xs">
                              {task.milestoneCode}
                            </Badge>
                          </div>
                          <Badge className="bg-yellow-600 text-white text-xs">
                            {reviewTypeMap[task.reviewType] || task.reviewType}
                          </Badge>
                        </div>
                        <div className="flex items-center justify-between">
                          <div className="text-sm text-slate-400">
                            <span>提交人: {task.submitterName}</span>
                            <span className="mx-2">|</span>
                            <span>
                              提交时间: {task.submittedAt ? new Date(task.submittedAt).toLocaleString('zh-CN') : '-'}
                            </span>
                          </div>
                          <div className="flex items-center text-blue-400 text-sm">
                            <span>去评审</span>
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

          {/* 评审历史列表 */}
          <TabsContent value="history">
            <Card className="bg-slate-800 border-slate-600">
              <CardHeader>
                <CardTitle className="text-slate-100 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-400" />
                  评审历史列表
                </CardTitle>
              </CardHeader>
              <CardContent>
                {loadingHistory ? (
                  <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    加载中...
                  </div>
                ) : historyRecords.length === 0 ? (
                  <div className="text-center py-8">
                    <FileText className="w-12 h-12 text-slate-500 mx-auto mb-3" />
                    <p className="text-slate-400">暂无评审历史记录</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {historyRecords.map((item) => (
                      <div
                        key={item.id}
                        className="p-4 bg-slate-700 rounded border border-slate-600 cursor-pointer hover:bg-slate-600 transition"
                        onClick={() => handleHistoryClick(item)}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <span className="text-slate-100 font-medium">
                              {item.projectName}
                            </span>
                            <Badge className="bg-blue-600 text-white text-xs">
                              {item.milestoneCode}
                            </Badge>
                          </div>
                          <Badge className={`${resultBadgeMap[item.result]?.color || 'bg-gray-600'} text-white text-xs`}>
                            {resultBadgeMap[item.result]?.label || item.result}
                          </Badge>
                        </div>
                        <div className="text-sm text-slate-400">
                          <span>评审人: {item.actorName}</span>
                          <span className="mx-2">|</span>
                          <span>角色: {item.actorRole}</span>
                          <span className="mx-2">|</span>
                          <span>
                            时间: {new Date(item.actionAt).toLocaleString('zh-CN')}
                          </span>
                        </div>
                        {item.opinion && (
                          <p className="text-sm text-slate-300 mt-2">
                            意见: {item.opinion}
                          </p>
                        )}
                        <div className="mt-2 flex items-center text-blue-400 text-xs">
                          <span>查看项目里程碑</span>
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
