import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Textarea } from './ui/textarea';
import { FileUp, CheckCircle, AlertCircle, Save, Send, ThumbsUp, ThumbsDown, History, Loader2, FileText } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';

interface DeliverableItem {
  id: string;
  name: string;
  required: boolean;
  uploaded: boolean;
  file?: File;
}

interface ReviewApprovalTask {
  id: number;
  reviewApprovalId: number;
  approverUserId: number;
  approverName: string;
  approverRole: string;
  sortOrder: number;
  status: string;
  decision: string | null;
  opinion: string | null;
  decidedAt: string | null;
}

interface ReviewApproval {
  id: number;
  projectId: number;
  projectMilestoneId: number;
  submitterUserId: number;
  submitterName: string;
  submitComment: string;
  status: string;
  submittedAt: string | null;
  finishedAt: string | null;
  tasks: ReviewApprovalTask[];
}

interface ReviewRecord {
  id: number;
  projectId: number;
  projectMilestoneId: number;
  action: string;
  actorUserId: number;
  actorName: string;
  actorRole: string;
  result: string;
  opinion: string;
  actionAt: string;
}

interface MilestoneConsoleProps {
  currentStage: number;
  projectName: string;
  projectId: number;
  currentUserId?: number;
  currentUserRoles?: string[];
  reviewStatus?: string;
  onReview?: () => void;
}

const getDeliverablesForStage = (stage: number): DeliverableItem[] => {
  const stageDeliverablesMap: { [key: number]: DeliverableItem[] } = {
    0: [
      { id: '1', name: '立项报告', required: true, uploaded: false },
      { id: '2', name: '靶点评估文档', required: true, uploaded: false },
    ],
    1: [
      { id: '1', name: '先导化合物', required: true, uploaded: false },
      { id: '2', name: '专利分析', required: true, uploaded: false },
    ],
    2: [
      { id: '1', name: '优选化合物', required: true, uploaded: false },
      { id: '2', name: '专利分析', required: true, uploaded: false },
    ],
    3: [
      { id: '1', name: 'PCC 提名报告', required: true, uploaded: false },
      { id: '2', name: '体内外药效数据', required: true, uploaded: false },
      { id: '3', name: '初步ADME数据', required: true, uploaded: false },
      { id: '4', name: '初步安全性评估', required: true, uploaded: false },
      { id: '5', name: '专利策略文档', required: true, uploaded: false },
    ],
    4: [
      { id: '1', name: 'GLP毒理报告', required: true, uploaded: false },
      { id: '2', name: '药效总结报告', required: true, uploaded: false },
      { id: '3', name: 'CMC初步总结报告', required: true, uploaded: false },
      { id: '4', name: '专利FTO报告', required: true, uploaded: false },
    ],
    5: [
      { id: '1', name: 'IND申报资料', required: true, uploaded: false },
      { id: '2', name: '受理通知书', required: true, uploaded: false },
      { id: '3', name: '临床试验批件', required: true, uploaded: false },
    ],
    6: [
      { id: '1', name: '临床I期总结报告', required: true, uploaded: false },
      { id: '2', name: '临床II期试验方案', required: true, uploaded: false },
    ],
    7: [
      { id: '1', name: '临床II期总结报告', required: true, uploaded: false },
      { id: '2', name: '临床III期试验方案', required: true, uploaded: false },
      { id: '3', name: '注册策略确认文档', required: true, uploaded: false },
    ],
    8: [
      { id: '1', name: '临床III期研究报告', required: true, uploaded: false },
      { id: '2', name: '上市后承诺文档', required: true, uploaded: false },
    ],
    9: [
      { id: '1', name: 'NDA申报资料', required: true, uploaded: false },
      { id: '2', name: '受理通知书', required: true, uploaded: false },
      { id: '3', name: '药品注册证书', required: true, uploaded: false },
    ],
  };

  return stageDeliverablesMap[stage] || [];
};

const stageName = ['G0', 'G1', 'G2', 'G3', 'G4', 'G5', 'G6', 'G7', 'G8', 'G9'];
const stageDescription = [
  '项目立项',
  '先导化合物确认',
  '优选化合物',
  '候选化合物提名 (PCC)',
  '临床前开发完成 (GLP)',
  '临床试验申请获批 (IND)',
  '临床 I 期',
  '临床 II 期',
  '临床 III 期',
  '新药上市申请获批 (NDA)',
];

const statusBadgeMap: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'bg-gray-600' },
  SUBMITTED: { label: '评审中', color: 'bg-blue-600' },
  APPROVED: { label: '已通过', color: 'bg-green-600' },
  REJECTED: { label: '未通过', color: 'bg-red-600' },
  GO: { label: '通过 (Go)', color: 'bg-green-600' },
  CONDITIONAL_GO: { label: '有条件通过', color: 'bg-yellow-600' },
  NO_GO: { label: '不通过', color: 'bg-red-600' },
};

const reviewStatusMap: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待评审', color: 'bg-yellow-600' },
  APPROVED: { label: '已通过', color: 'bg-green-600' },
  REJECTED: { label: '未通过', color: 'bg-red-600' },
  GO: { label: '通过 (Go)', color: 'bg-green-600' },
  CONDITIONAL_GO: { label: '有条件通过', color: 'bg-yellow-600' },
  NO_GO: { label: '不通过', color: 'bg-red-600' },
};

export const MilestoneConsole: React.FC<MilestoneConsoleProps> = ({
  currentStage,
  projectName,
  projectId,
  currentUserId,
  currentUserRoles = [],
  reviewStatus,
  onReview,
}) => {
  const [deliverables, setDeliverables] = useState<DeliverableItem[]>(
    getDeliverablesForStage(currentStage)
  );
  const [reviews, setReviews] = useState<ReviewApproval[]>([]);
  const [records, setRecords] = useState<ReviewRecord[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(false);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const [submitComment, setSubmitComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [decisionLoading, setDecisionLoading] = useState<number | null>(null);
  const [decisionOpinion, setDecisionOpinion] = useState('');
  const isPm = currentUserRoles.includes('ROLE_PM');

  // 加载评审数据
  const loadReviews = async () => {
    if (!projectId) return;
    setLoadingReviews(true);
    try {
      const res = await api.get(`/api/reviews/${projectId}`);
      const result = res.data as { code: number; data: ReviewApproval[]; message?: string };
      if (result.code === 200 || result.code === 0) {
        setReviews(result.data || []);
      }
    } catch (error: any) {
      console.error('加载评审数据失败:', error);
    } finally {
      setLoadingReviews(false);
    }
  };

  // 加载评审记录
  const loadRecords = async () => {
    if (!projectId) return;
    setLoadingRecords(true);
    try {
      const res = await api.get(`/api/reviews/${projectId}/records`);
      const result = res.data as { code: number; data: ReviewRecord[]; message?: string };
      if (result.code === 200 || result.code === 0) {
        setRecords(result.data || []);
      }
    } catch (error: any) {
      console.error('加载评审记录失败:', error);
    } finally {
      setLoadingRecords(false);
    }
  };

  useEffect(() => {
    if (projectId) {
      loadReviews();
      loadRecords();
    }
  }, [projectId]);

  const handleFileUpload = (itemId: string, file: File) => {
    setDeliverables(prev =>
      prev.map(item =>
        item.id === itemId
          ? { ...item, uploaded: true, file }
          : item
      )
    );
  };

  const allRequiredUploaded = deliverables
    .filter(d => d.required)
    .every(d => d.uploaded);

  // 保存草稿
  const handleSaveDraft = async () => {
    if (!projectId || !currentUserId) {
      toast.error('请先登录');
      return;
    }
    setSavingDraft(true);
    try {
      const res = await api.post(`/api/reviews/${projectId}/draft`, {
        actorUserId: currentUserId,
        submitComment: submitComment,
      });
      const result = res.data as { code: number; message?: string };
      if (result.code === 200 || result.code === 0) {
        toast.success('草稿已保存');
        loadReviews();
      } else {
        toast.error(result.message || '保存草稿失败');
      }
    } catch (error: any) {
      toast.error('保存草稿失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setSavingDraft(false);
    }
  };

  // 提交评审
  const handleSubmitReview = async () => {
    if (!projectId || !currentUserId) {
      toast.error('请先登录');
      return;
    }
    if (!allRequiredUploaded) {
      toast.error('请先上传所有必填文件');
      return;
    }
    setSubmitting(true);
    try {
      const res = await api.post(`/api/reviews/${projectId}/submit`, {
        actorUserId: currentUserId,
        submitComment: submitComment,
      });
      const result = res.data as { code: number; message?: string };
      if (result.code === 200 || result.code === 0) {
        toast.success('评审申请已提交');
        loadReviews();
        loadRecords();
        if (onReview) onReview();
      } else {
        toast.error(result.message || '提交评审失败');
      }
    } catch (error: any) {
      toast.error('提交评审失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setSubmitting(false);
    }
  };

  // 执行审批决策
  const handleDecision = async (taskId: number, decision: string) => {
    if (!currentUserId) {
      toast.error('请先登录');
      return;
    }
    setDecisionLoading(taskId);
    try {
      const res = await api.post(`/api/reviews/${projectId}/tasks/${taskId}/decision`, {
        actorUserId: currentUserId,
        decision: decision,
        opinion: decisionOpinion,
      });
      const result = res.data as { code: number; message?: string };
      if (result.code === 200 || result.code === 0) {
        toast.success(decision === 'APPROVED' ? '已通过' : '已驳回');
        setDecisionOpinion('');
        loadReviews();
        loadRecords();
      } else {
        toast.error(result.message || '操作失败');
      }
    } catch (error: any) {
      toast.error('操作失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setDecisionLoading(null);
    }
  };

  // 获取当前活跃的评审（最近提交的）
  const activeReview = reviews.length > 0 ? reviews[0] : null;
  const isUnderReview = activeReview?.status === 'SUBMITTED';
  const isApproved = activeReview?.status === 'APPROVED';
  const isRejected = activeReview?.status === 'REJECTED';

  // 当前用户是否有待审批的任务
  const myPendingTask = activeReview?.tasks?.find(
    t => t.approverUserId === currentUserId && t.status === 'PENDING'
  );

  return (
    <div className="space-y-6">
      {/* 阶段信息头 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-slate-100">
                {projectName} - {stageName[currentStage]}
              </CardTitle>
              <p className="text-sm text-slate-400 mt-2">
                {stageDescription[currentStage]}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {reviewStatus && reviewStatusMap[reviewStatus] && (
                <Badge className={`${reviewStatusMap[reviewStatus].color} text-white px-3 py-1`}>
                  {reviewStatusMap[reviewStatus].label}
                </Badge>
              )}
              <Badge className="bg-blue-600 text-white px-3 py-1">
                {stageName[currentStage]}
              </Badge>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* 核心交付物清单 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">核心交付物清单</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {deliverables.map(item => (
              <div
                key={item.id}
                className="flex items-center justify-between p-4 bg-slate-700 rounded border border-slate-600"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-slate-100 font-medium">
                      {item.name}
                    </span>
                    {item.required && (
                      <span className="text-red-500 text-sm">必填</span>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  {item.uploaded ? (
                    <div className="flex items-center gap-2 text-green-400">
                      <CheckCircle className="w-5 h-5" />
                      <span className="text-sm">{item.file?.name}</span>
                    </div>
                  ) : (
                    <label className="flex items-center gap-2 px-3 py-2 bg-slate-600 rounded cursor-pointer hover:bg-slate-500 transition">
                      <FileUp className="w-4 h-4 text-slate-300" />
                      <span className="text-sm text-slate-300">上传</span>
                      <input
                        type="file"
                        hidden
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            handleFileUpload(item.id, file);
                          }
                        }}
                        accept=".pdf,.doc,.docx"
                      />
                    </label>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* 上传状态摘要 */}
          <div className="mt-6 p-4 bg-slate-700 rounded flex items-start gap-3">
            {allRequiredUploaded ? (
              <>
                <CheckCircle className="w-5 h-5 text-green-400 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-green-400 font-semibold">所有必填文件已上传</p>
                  <p className="text-slate-300 text-sm">
                    现在可以提交评审申请
                  </p>
                </div>
              </>
            ) : (
              <>
                <AlertCircle className="w-5 h-5 text-yellow-500 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-yellow-500 font-semibold">还有必填文件未上传</p>
                  <p className="text-slate-300 text-sm">
                    请完成所有必填项才能提交评审
                  </p>
                </div>
              </>
            )}
          </div>
        </CardContent>
      </Card>

      {/* 评审操作 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">评审操作</CardTitle>
        </CardHeader>
        <CardContent>
          {/* 评审备注 */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-300 mb-2">
              评审备注
            </label>
            <Textarea
              value={submitComment}
              onChange={(e) => setSubmitComment(e.target.value)}
              placeholder="请输入评审备注说明..."
              className="bg-slate-700 border-slate-600 text-slate-100 min-h-[80px]"
              disabled={isUnderReview || isApproved}
            />
          </div>

          {/* 操作按钮 - PM 角色 */}
          <div className="flex gap-4">
            {isPm && !isUnderReview && !isApproved && (
              <>
                <Button
                  disabled={!allRequiredUploaded || submitting}
                  onClick={handleSubmitReview}
                  className={`flex-1 ${
                    allRequiredUploaded
                      ? 'bg-blue-600 hover:bg-blue-700 text-white'
                      : 'bg-slate-600 text-slate-400 cursor-not-allowed'
                  }`}
                >
                  {submitting ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />提交中...</>
                  ) : (
                    <><Send className="w-4 h-4 mr-2" />提交评审</>
                  )}
                </Button>
                <Button
                  variant="outline"
                  disabled={savingDraft}
                  onClick={handleSaveDraft}
                  className="flex-1 bg-slate-700 text-slate-100 border-slate-600 hover:bg-slate-600"
                >
                  {savingDraft ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />保存中...</>
                  ) : (
                    <><Save className="w-4 h-4 mr-2" />保存草稿</>
                  )}
                </Button>
              </>
            )}

            {/* 评审状态提示 */}
            {isUnderReview && (
              <div className="flex-1 p-3 bg-blue-900/30 border border-blue-700 rounded flex items-center gap-2">
                <AlertCircle className="w-5 h-5 text-blue-400" />
                <span className="text-blue-300">评审已提交，等待审批</span>
              </div>
            )}
            {isApproved && (
              <div className="flex-1 p-3 bg-green-900/30 border border-green-700 rounded flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-green-400" />
                <span className="text-green-300">评审已通过</span>
              </div>
            )}
            {isRejected && (
              <div className="flex-1 p-3 bg-red-900/30 border border-red-700 rounded flex items-center gap-2">
                <AlertCircle className="w-5 h-5 text-red-400" />
                <span className="text-red-300">评审未通过，请修改后重新提交</span>
              </div>
            )}
          </div>

          {/* 评审操作卡片 - 仅在用户有当前里程碑阶段的评审任务时可见 */}
          {myPendingTask && (
            <div className="mt-4 p-4 bg-slate-700 rounded border border-slate-600">
              <h4 className="text-slate-100 font-semibold mb-3">评审操作</h4>
              <div className="mb-3">
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  评审意见
                </label>
                <Textarea
                  value={decisionOpinion}
                  onChange={(e) => setDecisionOpinion(e.target.value)}
                  placeholder="请输入评审意见..."
                  className="bg-slate-600 border-slate-500 text-slate-100 min-h-[80px]"
                />
              </div>
              <div className="flex gap-3">
                <Button
                  onClick={() => handleDecision(myPendingTask.id, 'GO')}
                  disabled={decisionLoading === myPendingTask.id}
                  className="flex-1 bg-green-600 hover:bg-green-700 text-white"
                >
                  {decisionLoading === myPendingTask.id ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />处理中...</>
                  ) : (
                    <><ThumbsUp className="w-4 h-4 mr-2" />通过 (Go)</>
                  )}
                </Button>
                <Button
                  onClick={() => handleDecision(myPendingTask.id, 'CONDITIONAL_GO')}
                  disabled={decisionLoading === myPendingTask.id}
                  className="flex-1 bg-yellow-600 hover:bg-yellow-700 text-white"
                >
                  {decisionLoading === myPendingTask.id ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />处理中...</>
                  ) : (
                    <><AlertCircle className="w-4 h-4 mr-2" />有条件通过 (Conditional Go)</>
                  )}
                </Button>
                <Button
                  onClick={() => handleDecision(myPendingTask.id, 'NO_GO')}
                  disabled={decisionLoading === myPendingTask.id}
                  className="flex-1 bg-red-600 hover:bg-red-700 text-white"
                >
                  {decisionLoading === myPendingTask.id ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />处理中...</>
                  ) : (
                    <><ThumbsDown className="w-4 h-4 mr-2" />不通过 (No Go)</>
                  )}
                </Button>
              </div>
            </div>
          )}

          {/* 评审历史 */}
          <div className="mt-6 pt-6 border-t border-slate-600">
            <h3 className="text-slate-100 font-semibold mb-3 flex items-center gap-2">
              <History className="w-4 h-4" />
              评审历史
            </h3>

            {/* 评审列表 */}
            {loadingReviews ? (
              <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
                <Loader2 className="w-4 h-4 animate-spin" />
                加载中...
              </div>
            ) : reviews.length === 0 ? (
              <p className="text-slate-400 text-sm">
                暂无评审记录。提交评审后，评审历史将显示在此。
              </p>
            ) : (
              <div className="space-y-4">
                {reviews.map((review) => (
                  <div key={review.id} className="p-4 bg-slate-700 rounded border border-slate-600">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-slate-100 font-medium">{review.submitterName}</span>
                        <span className="text-slate-400 text-sm">提交于</span>
                        <span className="text-slate-400 text-sm">
                          {review.submittedAt ? new Date(review.submittedAt).toLocaleString('zh-CN') : '-'}
                        </span>
                      </div>
                      <Badge className={`${statusBadgeMap[review.status]?.color || 'bg-gray-600'} text-white`}>
                        {statusBadgeMap[review.status]?.label || review.status}
                      </Badge>
                    </div>
                    {review.submitComment && (
                      <p className="text-slate-300 text-sm mb-2">{review.submitComment}</p>
                    )}
                    {/* 审批任务列表 */}
                    {review.tasks && review.tasks.length > 0 && (
                      <div className="mt-3 space-y-2">
                        <p className="text-xs text-slate-400 font-medium">审批记录：</p>
                        {review.tasks.map((task) => (
                          <div key={task.id} className="flex items-center justify-between p-2 bg-slate-800 rounded">
                            <div className="flex items-center gap-2">
                              <span className="text-slate-200 text-sm">{task.approverName}</span>
                              <span className="text-slate-500 text-xs">({task.approverRole})</span>
                            </div>
                            <div className="flex items-center gap-2">
                              {task.status === 'PENDING' && (
                                <Badge className="bg-yellow-600 text-white text-xs">待审批</Badge>
                              )}
                              {(task.status === 'APPROVED' || task.decision === 'GO') && (
                                <Badge className="bg-green-600 text-white text-xs">通过 (Go)</Badge>
                              )}
                              {task.decision === 'CONDITIONAL_GO' && (
                                <Badge className="bg-yellow-600 text-white text-xs">有条件通过</Badge>
                              )}
                              {(task.status === 'REJECTED' || task.decision === 'NO_GO') && (
                                <Badge className="bg-red-600 text-white text-xs">不通过</Badge>
                              )}
                              {task.decidedAt && (
                                <span className="text-slate-500 text-xs">
                                  {new Date(task.decidedAt).toLocaleString('zh-CN')}
                                </span>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* 评审记录 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100 flex items-center gap-2">
            <FileText className="w-4 h-4" />
            评审记录
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loadingRecords ? (
            <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
              <Loader2 className="w-4 h-4 animate-spin" />
              加载中...
            </div>
          ) : records.length === 0 ? (
            <p className="text-slate-400 text-sm">
              暂无评审记录。
            </p>
          ) : (
            <div className="space-y-3">
              {records.map((record) => (
                <div key={record.id} className="p-3 bg-slate-700 rounded border border-slate-600">
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <span className="text-slate-100 text-sm font-medium">{record.actorName}</span>
                      <span className="text-slate-500 text-xs">({record.actorRole})</span>
                      <span className="text-slate-400 text-xs">
                        {new Date(record.actionAt).toLocaleString('zh-CN')}
                      </span>
                    </div>
                    <Badge className={
                      record.result === 'GO' || record.result === 'APPROVED' ? 'bg-green-600 text-white text-xs' :
                      record.result === 'CONDITIONAL_GO' ? 'bg-yellow-600 text-white text-xs' :
                      record.result === 'NO_GO' || record.result === 'REJECTED' ? 'bg-red-600 text-white text-xs' :
                      'bg-blue-600 text-white text-xs'
                    }>
                      {record.result === 'GO' || record.result === 'APPROVED' ? '通过 (Go)' :
                       record.result === 'CONDITIONAL_GO' ? '有条件通过' :
                       record.result === 'NO_GO' || record.result === 'REJECTED' ? '不通过' : record.result}
                    </Badge>
                  </div>
                  <p className="text-slate-400 text-xs">
                    操作: {record.action === 'SUBMIT' ? '提交评审' :
                           record.action === 'APPROVE' || record.action === 'GO' ? '评审通过 (Go)' :
                           record.action === 'CONDITIONAL_GO' ? '有条件通过' :
                           record.action === 'REJECT' || record.action === 'NO_GO' ? '评审不通过' :
                           record.action === 'DRAFT' ? '保存草稿' : record.action}
                  </p>
                  {record.opinion && (
                    <p className="text-slate-300 text-sm mt-1">意见: {record.opinion}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
