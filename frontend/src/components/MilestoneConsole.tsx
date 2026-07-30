import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Textarea } from './ui/textarea';
import { FileUp, CheckCircle, AlertCircle, Save, Send, ThumbsUp, ThumbsDown, History, Loader2, FileText, Download, Eye, Trash2 } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';

interface DeliverableSlot {
  slotCode: string;
  slotName: string;
  isRequired: boolean;
  description?: string;
  allowedFileTypes?: string;
  documents: DeliverableDocument[];
}

interface DeliverableDocument {
  id: number;
  fileName: string;
  fileType?: string;
  storagePath?: string;
  uploader?: number;
  uploadedAt?: string;
  complianceStatus?: string;
  isLocked?: boolean;
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

interface StepProgress {
  stepCode: string;
  stepName: string;
  nodeCode: string;
  nodeType: string;
  status: string;
  completedAt: string | null;
  approverRule?: string | null;
  approverRuleLabel?: string | null;
  expectedApproverLabel?: string | null;
  active: boolean;
  future: boolean;
  tasks: TaskDetail[];
}

interface TaskDetail {
  taskId: number;
  approverUserId: number;
  approverName: string;
  approverRole: string;
  deliverableSlotCode: string;
  decision: string | null;
  opinion: string | null;
  decidedAt: string | null;
  status: string;
}

interface ReviewProgressData {
  projectId: number;
  projectMilestoneId: number;
  milestoneCode: string;
  milestoneName: string;
  currentStep: string;
  status: string;
  steps: StepProgress[];
}

interface MilestoneConsoleProps {
  currentStage: number;
  projectName: string;
  projectId: number;
  currentUserId?: number;
  currentUserRoles?: string[];
  reviewStatus?: string;
  /** 是否允许上传核心交付物（ROLE_DEPT_EXECUTOR 或 ADMIN 角色） */
  canUploadDeliverables?: boolean;
  /** 部门执行人所属部门名称，用于动态提示 */
  executorDeptName?: string;
  onReview?: () => void;
}

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
  canUploadDeliverables = false,
  executorDeptName = '对应部门',
  onReview,
}) => {
  const [deliverableSlots, setDeliverableSlots] = useState<DeliverableSlot[]>([]);
  const [loadingDeliverables, setLoadingDeliverables] = useState(false);
  const [uploadingSlot, setUploadingSlot] = useState<string | null>(null);
  const [reviews, setReviews] = useState<ReviewApproval[]>([]);
  const [records, setRecords] = useState<ReviewRecord[]>([]);
  const [reviewProgress, setReviewProgress] = useState<ReviewProgressData | null>(null);
  const [loadingReviews, setLoadingReviews] = useState(false);
  const [loadingProgress, setLoadingProgress] = useState(false);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const [submitComment, setSubmitComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [decisionLoading, setDecisionLoading] = useState<number | null>(null);
  const [decisionOpinion, setDecisionOpinion] = useState('');
  const isPm = currentUserRoles.includes('ROLE_PM');

  const milestoneCode = stageName[currentStage];

  // 加载交付物清单
  const loadDeliverables = async () => {
    if (!projectId) return;
    setLoadingDeliverables(true);
    try {
      const res = await api.get(`/api/milestone-deliverables/project/${projectId}/milestone/${milestoneCode}`);
      const result = res.data as { code: number; data: DeliverableSlot[]; message?: string };
      if (result.code === 200 || result.code === 0) {
        setDeliverableSlots(result.data || []);
      }
    } catch (error: any) {
      console.error('加载交付物清单失败:', error);
      toast.error('加载交付物清单失败');
    } finally {
      setLoadingDeliverables(false);
    }
  };

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

  // 加载评审流程进度（未提交时也能显示流程节点配置）
  const loadProgress = async () => {
    if (!projectId) return;
    setLoadingProgress(true);
    try {
      const res = await api.get(`/api/reviews/${projectId}/progress`);
      const result = res.data as { code: number; data: ReviewProgressData; message?: string };
      if (result.code === 200 || result.code === 0) {
        setReviewProgress(result.data);
      }
    } catch (error: any) {
      // progress API 可能因里程碑配置缺失而报错，不阻塞其他功能
    } finally {
      setLoadingProgress(false);
    }
  };

  useEffect(() => {
    if (projectId) {
      loadDeliverables();
      loadReviews();
      loadRecords();
      loadProgress();
    }
  }, [projectId, milestoneCode]);

  // 上传交付物
  const handleUpload = async (slotCode: string, file: File) => {
    if (!projectId) return;
    setUploadingSlot(slotCode);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('projectId', String(projectId));
      formData.append('milestoneCode', milestoneCode);
      formData.append('slotCode', slotCode);

      const res = await api.post('/api/milestone-deliverables/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const result = res.data as { code: number; data: DeliverableDocument; message?: string };
      if (result.code === 200 || result.code === 0) {
        toast.success('上传成功');
        loadDeliverables();
      } else {
        toast.error(result.message || '上传失败');
      }
    } catch (error: any) {
      toast.error('上传失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setUploadingSlot(null);
    }
  };

  // 删除交付物
  const handleDelete = async (docId: number) => {
    if (!confirm('确定要删除该交付物吗？')) return;
    try {
      const res = await api.delete(`/api/milestone-deliverables/${docId}`);
      const result = res.data as { code: number; message?: string };
      if (result.code === 200 || result.code === 0) {
        toast.success('删除成功');
        loadDeliverables();
      } else {
        toast.error(result.message || '删除失败');
      }
    } catch (error: any) {
      toast.error('删除失败: ' + (error.response?.data?.message || error.message));
    }
  };

  // 预览交付物
  const handlePreview = (docId: number) => {
    const token = localStorage.getItem('token');
    const url = `${api.defaults.baseURL}/api/milestone-deliverables/${docId}/preview`;
    fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
      .then(async res => {
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        const blob = await res.blob();
        const previewUrl = window.URL.createObjectURL(blob);
        window.open(previewUrl, '_blank', 'noopener,noreferrer');
        setTimeout(() => window.URL.revokeObjectURL(previewUrl), 60_000);
      })
      .catch(err => {
        console.error('预览文件失败', err);
        toast.error('预览文件失败');
      });
  };

  // 下载交付物
  const handleDownload = (docId: number, fileName: string) => {
    const token = localStorage.getItem('token');
    const url = `${api.defaults.baseURL}/api/milestone-deliverables/${docId}/download`;
    // 使用 fetch 携带 token 下载
    fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(res => res.blob())
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(() => toast.error('下载失败'));
  };

  const allRequiredUploaded = deliverableSlots
    .filter(s => s.isRequired)
    .every(s => s.documents && s.documents.length > 0);

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
        loadProgress();
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
        toast.success(
          decision === 'GO' ? '已通过' :
          decision === 'CONDITIONAL_GO' ? '有条件通过' :
          '已驳回'
        );
        setDecisionOpinion('');
        loadReviews();
        loadRecords();
        loadProgress();
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
    t => Number(t.approverUserId) === Number(currentUserId) && t.status === 'PENDING'
  );

  // 当前用户是否已完成审批（有已完成的任务记录）
  const myCompletedTask = activeReview?.tasks?.find(
    t => Number(t.approverUserId) === Number(currentUserId) && t.status !== 'PENDING'
  );

  // 提交评审后或已通过时，执行人也不能再上传
  const allowUpload = canUploadDeliverables && !isUnderReview && !isApproved;

  return (
    <div className="space-y-6">
      {/* 阶段信息头 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-slate-100">
                {projectName}
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
                {milestoneCode}-{stageDescription[currentStage]}
              </Badge>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* 核心交付物清单 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">核心交付物清单</CardTitle>
          {canUploadDeliverables && !isUnderReview && !isApproved && (
             <p className="text-sm text-slate-400 mt-1">
               请上传所有必填交付物后提交评审
             </p>
           )}
           {canUploadDeliverables && isUnderReview && (
             <p className="text-sm text-blue-400 mt-1">
               评审已提交，等待审批中，暂不可修改交付物
             </p>
           )}
           {canUploadDeliverables && isApproved && (
             <p className="text-sm text-green-400 mt-1">
               评审已通过，交付物已锁定
             </p>
           )}
        </CardHeader>
        <CardContent>
          {loadingDeliverables ? (
            <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
              <Loader2 className="w-4 h-4 animate-spin" />
              加载中...
            </div>
          ) : deliverableSlots.length === 0 ? (
            <p className="text-slate-400 text-sm py-4">
              暂无交付物定义
            </p>
          ) : (
            <div className="space-y-4">
              {deliverableSlots.map(slot => {
                const hasDoc = slot.documents && slot.documents.length > 0;
                const doc = hasDoc ? slot.documents[0] : null;
                const isUploading = uploadingSlot === slot.slotCode;
                return (
                  <div
                    key={slot.slotCode}
                    className="flex items-center justify-between p-4 bg-slate-700 rounded border border-slate-600"
                  >
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-slate-100 font-medium">
                          {slot.slotName}
                        </span>
                        {slot.isRequired && (
                          <span className="text-red-500 text-sm">必填</span>
                        )}
                      </div>
                      {slot.description && (
                        <p className="text-sm text-slate-400 mt-1">{slot.description}</p>
                      )}
                    </div>

                    <div className="flex items-center gap-3">
                      {hasDoc && doc ? (
                        <div className="flex items-center gap-3">
                          <div className="flex items-center gap-2 text-green-400">
                            <CheckCircle className="w-4 h-4" />
                            <span className="text-sm">{doc.fileName}</span>
                          </div>
                          <button
                            className="p-1.5 rounded hover:bg-slate-600 transition text-slate-300 hover:text-white"
                            title="预览"
                            onClick={() => handlePreview(doc.id)}
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            className="p-1.5 rounded hover:bg-slate-600 transition text-slate-300 hover:text-white"
                            title="下载"
                            onClick={() => handleDownload(doc.id, doc.fileName)}
                          >
                            <Download className="w-4 h-4" />
                          </button>
                          {allowUpload && !doc.isLocked && (
                            <button
                              className="p-1.5 rounded hover:bg-red-900/50 transition text-slate-300 hover:text-red-400"
                              title="删除"
                              onClick={() => handleDelete(doc.id)}
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      ) : allowUpload ? (
                        <label className={`flex items-center gap-2 px-3 py-2 bg-slate-600 rounded cursor-pointer hover:bg-slate-500 transition ${isUploading ? 'opacity-50 cursor-wait' : ''}`}>
                          {isUploading ? (
                            <Loader2 className="w-4 h-4 text-slate-300 animate-spin" />
                          ) : (
                            <FileUp className="w-4 h-4 text-slate-300" />
                          )}
                          <span className="text-sm text-slate-300">
                            {isUploading ? '上传中...' : '上传'}
                          </span>
                          <input
                            type="file"
                            hidden
                            disabled={isUploading}
                            onChange={(e) => {
                              const file = e.target.files?.[0];
                              if (file) {
                                handleUpload(slot.slotCode, file);
                              }
                            }}
                            accept={slot.allowedFileTypes || '.pdf,.doc,.docx'}
                          />
                        </label>
                      ) : (
                        <span className="text-sm text-slate-500">未上传</span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* 上传状态摘要 - 仅在可编辑状态下显示 */}
          {allowUpload && deliverableSlots.length > 0 && (
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
          )}
        </CardContent>
      </Card>

      {/* 评审操作 */}
      <Card className="bg-slate-800 border-slate-600">
        <CardHeader>
          <CardTitle className="text-slate-100">评审操作</CardTitle>
        </CardHeader>
        <CardContent>
          {/* 评审备注 - 仅部门执行人在未提交状态下显示可编辑输入框 */}
           {canUploadDeliverables && !isUnderReview && !isApproved && (
             <div className="mb-4">
               <label className="block text-sm font-medium text-slate-300 mb-2">
                 评审备注
               </label>
               <Textarea
                 value={submitComment}
                 onChange={(e) => setSubmitComment(e.target.value)}
                 placeholder="请输入评审备注说明..."
                 className="bg-slate-700 border-slate-600 text-slate-100 min-h-[80px]"
               />
             </div>
           )}
           {/* 评审状态提示 */}
           {isUnderReview && myPendingTask && (
             <div className="mb-4 p-3 bg-yellow-900/30 border border-yellow-700 rounded flex items-center gap-2">
               <AlertCircle className="w-5 h-5 text-yellow-400" />
               <span className="text-yellow-300">待您审批，请在下方填写评审意见并作出决策</span>
             </div>
           )}
           {isUnderReview && !myPendingTask && myCompletedTask && (
             <p className="mb-4 flex items-center gap-2 text-sm text-blue-400">
               <CheckCircle className="w-5 h-5 text-blue-400" />
               <span>您已完成审批，等待后续评审</span>
             </p>
           )}
           {isUnderReview && !myPendingTask && !myCompletedTask && (
             <p className="mb-4 flex items-center gap-2 text-sm text-blue-400">
               <AlertCircle className="w-5 h-5 text-blue-400" />
               <span>评审已提交，等待审批中，暂不可修改交付物</span>
             </p>
           )}
           {isApproved && (
             <div className="mb-4 p-3 bg-green-900/30 border border-green-700 rounded flex items-center gap-2">
               <CheckCircle className="w-5 h-5 text-green-400" />
               <span className="text-green-300">评审已通过</span>
             </div>
           )}

          {/* 操作按钮 - 部门执行人 */}
          <div className="flex gap-4">
            {canUploadDeliverables && !isUnderReview && !isApproved && (
              <>
                <Button
                  disabled={!allRequiredUploaded || submitting}
                  onClick={handleSubmitReview}
                  className={`flex-1 ${allRequiredUploaded ? 'bg-blue-600 hover:bg-blue-700 text-white' : 'bg-slate-600 text-slate-400 cursor-not-allowed'}`}
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

            {/* 评审未通过状态提示 */}
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

          {/* 评审流程 */}
           <div className="mt-6 pt-6 border-t border-slate-600">
             <h3 className="text-slate-100 font-semibold mb-3 flex items-center gap-2">
               <History className="w-4 h-4" />
               评审流程
             </h3>

            {/* 评审流程进度 - 优先展示从 progress API 获取的流程节点配置 */}
            {loadingProgress ? (
              <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
                <Loader2 className="w-4 h-4 animate-spin" />
                加载中...
              </div>
            ) : reviewProgress && reviewProgress.steps && reviewProgress.steps.length > 0 ? (
              <div className="space-y-3">
                {reviewProgress.steps.map((step) => (
                  <div key={step.stepCode} className="p-4 bg-slate-700 rounded border border-slate-600">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-slate-100 font-medium">{step.stepName}</span>
                        <span className="text-slate-500 text-xs">({step.stepCode})</span>
                        {step.active && <Badge className="bg-blue-700 text-white text-xs">当前节点</Badge>}
                      </div>
                      <Badge className={
                        step.status === 'APPROVED' ? 'bg-green-600 text-white text-xs' :
                        step.status === 'REJECTED' ? 'bg-red-600 text-white text-xs' :
                        step.status === 'IN_PROGRESS' ? 'bg-blue-600 text-white text-xs' :
                        'bg-slate-500 text-white text-xs'
                      }>
                        {step.status === 'APPROVED' ? '已通过' :
                         step.status === 'REJECTED' ? '未通过' :
                         step.status === 'IN_PROGRESS' ? '进行中' : '待处理'}
                      </Badge>
                    </div>
                    {step.tasks && step.tasks.length > 0 && (
                      <div className="mt-2 space-y-1">
                        {step.tasks.map((task) => (
                          <div key={task.taskId} className="text-sm text-slate-300 flex items-center gap-2">
                            <span>{task.approverName}</span>
                            {task.status === 'PENDING' && <Badge className="bg-yellow-600 text-white text-xs">待审批</Badge>}
                            {(task.status === 'APPROVED' || task.decision === 'GO') && <Badge className="bg-green-600 text-white text-xs">已通过</Badge>}
                            {(task.status === 'REJECTED' || task.decision === 'NO_GO') && <Badge className="bg-red-600 text-white text-xs">未通过</Badge>}
                          </div>
                        ))}
                      </div>
                    )}
                    {(!step.tasks || step.tasks.length === 0) && (
                      <p className="text-sm text-slate-400 mt-1">
                        {step.future
                          ? `待激活：${step.expectedApproverLabel || step.approverRuleLabel || '对应审批节点'}`
                          : step.active
                            ? '当前节点等待系统生成具体审批任务'
                            : '审批流程已按流程管理预配置，提交后将直接进入对应审批节点'}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            ) : reviews.length === 0 ? (
               <p className="text-slate-400 text-sm">
                 当前阶段暂未返回可展示的评审节点，请先检查流程管理中的流程图配置。
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

       {/* 评审历史 */}
       <Card className="bg-slate-800 border-slate-600">
         <CardHeader>
           <CardTitle className="text-slate-100 flex items-center gap-2">
             <FileText className="w-4 h-4" />
             评审历史
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
               暂无评审历史。
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