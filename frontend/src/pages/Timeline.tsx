import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { ChevronLeft, Calendar, CheckCircle, Clock, AlertCircle, XCircle, Loader2 } from 'lucide-react';
import api from '../lib/api';
import { toast } from 'sonner';

interface MilestoneItem {
  code: string;
  name: string;
  status: string;
  plannedDate: string | null;
  actualDate: string | null;
  leadDeptText: string | null;
  sortNo: number | null;
  decisionResult: string | null;
  decisionAt: string | null;
  decisionNotes: string | null;
}

interface ProjectInfo {
  id: number;
  projectCode: string;
  projectName: string;
  levelCode: string;
  levelName: string;
  lifecyclePhaseLabel: string | null;
}

const statusConfig: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
  NOT_STARTED: { label: '未开始', color: 'bg-slate-600', icon: <Clock className="w-4 h-4" /> },
  IN_PROGRESS: { label: '进行中', color: 'bg-blue-500', icon: <Loader2 className="w-4 h-4 animate-spin" /> },
  SUBMITTED: { label: '评审中', color: 'bg-yellow-500', icon: <AlertCircle className="w-4 h-4" /> },
  APPROVED: { label: '已通过', color: 'bg-green-500', icon: <CheckCircle className="w-4 h-4" /> },
  CONDITIONAL_APPROVED: { label: '有条件通过', color: 'bg-orange-500', icon: <AlertCircle className="w-4 h-4" /> },
  REJECTED: { label: '已拒绝', color: 'bg-red-500', icon: <XCircle className="w-4 h-4" /> },
};

const decisionResultConfig: Record<string, { label: string; color: string }> = {
  GO: { label: '通过 (Go)', color: 'text-green-400' },
  CONDITIONAL_GO: { label: '有条件通过', color: 'text-orange-400' },
  NO_GO: { label: '不通过 (No Go)', color: 'text-red-400' },
};

const stageColors: Record<string, string> = {
  G0: 'bg-gray-500',
  G1: 'bg-blue-400',
  G2: 'bg-blue-500',
  G3: 'bg-green-400',
  G4: 'bg-green-500',
  G5: 'bg-yellow-400',
  G6: 'bg-orange-400',
  G7: 'bg-red-400',
  G8: 'bg-purple-400',
  G9: 'bg-indigo-400',
};

const calculateDeviation = (planned: string | null, actual: string | null): string | null => {
  if (!planned || !actual) return null;
  const p = new Date(planned);
  const a = new Date(actual);
  const diffMonths = (a.getFullYear() - p.getFullYear()) * 12 + (a.getMonth() - p.getMonth());
  if (diffMonths > 0) return `+${diffMonths}月`;
  if (diffMonths < 0) return `${diffMonths}月`;
  return '按时';
};

const Timeline: React.FC = () => {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectInfo | null>(null);
  const [milestones, setMilestones] = useState<MilestoneItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId) return;
    loadTimelineData();
  }, [projectId]);

  const loadTimelineData = async () => {
    try {
      setLoading(true);
      const [projectRes, milestonesRes] = await Promise.all([
        api.get(`/api/projects/${projectId}`),
        api.get(`/api/milestones/project/${projectId}`),
      ]);

      const projectData = projectRes.data as { code: number; data: ProjectInfo; message?: string };
      if (projectData.code === 200 || projectData.code === 0) {
        setProject(projectData.data);
      }

      const milestonesData = milestonesRes.data as { code: number; data: MilestoneItem[]; message?: string };
      if (milestonesData.code === 200 || milestonesData.code === 0) {
        setMilestones(milestonesData.data || []);
      }
    } catch (error) {
      toast.error('加载时间表数据失败');
      console.error('Timeline load error:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
        <div className="flex items-center gap-3">
          <Loader2 className="w-6 h-6 animate-spin text-blue-400" />
          <span className="text-slate-300">加载中...</span>
        </div>
      </div>
    );
  }

  const sortedMilestones = [...milestones].sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0));

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
        {/* 项目信息简介 */}
        <div className="mb-4">
          <h1 className="text-2xl font-bold text-slate-100">
            {project?.projectName || '项目'} - 项目时间表
          </h1>
          <p className="text-sm text-slate-400">
            {project?.projectCode} | {project?.levelName} | 当前阶段: {project?.lifecyclePhaseLabel || '-'}
          </p>
        </div>

        {/* 返回按钮 - 放在项目信息简介下方，里程碑时间表卡片左上方 */}
        <div className="mb-4">
          <Button
            onClick={() => navigate(-1)}
            variant="outline"
            className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            返回
          </Button>
        </div>

        {/* 甘特图时间表 */}
        <Card className="bg-slate-800 border-slate-600 mb-6">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-slate-100 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-blue-400" />
                里程碑时间表 (Gantt Chart)
              </CardTitle>
              <div className="flex items-center gap-3 text-xs">
                <span className="flex items-center gap-1">
                  <span className="w-3 h-3 rounded bg-blue-500"></span> 进行中
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-3 h-3 rounded bg-green-500"></span> 已完成
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-3 h-3 rounded bg-slate-600"></span> 未开始
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-3 h-3 rounded bg-yellow-500"></span> 评审中
                </span>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {/* 表头 */}
            <div className="grid grid-cols-12 gap-2 mb-2 px-4 py-2 bg-slate-700 rounded text-xs text-slate-400 font-semibold uppercase">
              <div className="col-span-1">阶段</div>
              <div className="col-span-2">里程碑名称</div>
              <div className="col-span-1">状态</div>
              <div className="col-span-2">计划日期</div>
              <div className="col-span-2">实际日期</div>
              <div className="col-span-1">偏差</div>
              <div className="col-span-2">主导部门</div>
              <div className="col-span-1">决策</div>
            </div>

            {/* 里程碑行 */}
            {sortedMilestones.map((ms, index) => {
              const statusInfo = statusConfig[ms.status] || { label: ms.status, color: 'bg-slate-600', icon: null };
              const deviation = calculateDeviation(ms.plannedDate, ms.actualDate);
              const deviationColor = deviation === '按时' ? 'text-green-400' : deviation?.startsWith('+') ? 'text-red-400' : 'text-slate-400';
              const decisionInfo = ms.decisionResult ? decisionResultConfig[ms.decisionResult] : null;

              return (
                <div
                  key={ms.code}
                  className={`grid grid-cols-12 gap-2 items-center px-4 py-3 rounded transition ${
                    index % 2 === 0 ? 'bg-slate-750' : 'bg-slate-800'
                  } hover:bg-slate-700`}
                >
                  {/* 阶段 */}
                  <div className="col-span-1">
                    <Badge className={`${stageColors[ms.code] || 'bg-slate-600'} text-white px-2 py-0.5 text-xs`}>
                      {ms.code}
                    </Badge>
                  </div>

                  {/* 里程碑名称 */}
                  <div className="col-span-2 text-sm text-slate-200 font-medium truncate">
                    {ms.name}
                  </div>

                  {/* 状态 */}
                  <div className="col-span-1">
                    <div className={`flex items-center gap-1 text-xs ${statusInfo.color.replace('bg-', 'text-')}`}>
                      {statusInfo.icon}
                      <span>{statusInfo.label}</span>
                    </div>
                  </div>

                  {/* 计划日期 */}
                  <div className="col-span-2 text-sm text-slate-300">
                    {ms.plannedDate || '-'}
                  </div>

                  {/* 实际日期 */}
                  <div className="col-span-2 text-sm text-slate-300">
                    {ms.actualDate || '-'}
                  </div>

                  {/* 偏差 */}
                  <div className={`col-span-1 text-sm ${deviationColor}`}>
                    {deviation || '-'}
                  </div>

                  {/* 主导部门 */}
                  <div className="col-span-2 text-sm text-slate-300 truncate">
                    {ms.leadDeptText || '-'}
                  </div>

                  {/* 决策 */}
                  <div className="col-span-1">
                    {decisionInfo ? (
                      <span className={`text-xs ${decisionInfo.color}`}>
                        {decisionInfo.label}
                      </span>
                    ) : (
                      <span className="text-xs text-slate-500">-</span>
                    )}
                  </div>
                </div>
              );
            })}

            {sortedMilestones.length === 0 && (
              <div className="text-center py-8 text-slate-400">
                暂无里程碑数据
              </div>
            )}
          </CardContent>
        </Card>

        {/* 甘特图可视化 */}
        <Card className="bg-slate-800 border-slate-600 mb-6">
          <CardHeader>
            <CardTitle className="text-slate-100">进度可视化</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {sortedMilestones.map((ms) => {
                const statusInfo = statusConfig[ms.status] || { label: ms.status, color: 'bg-slate-600', icon: null };
                const barColor = stageColors[ms.code] || 'bg-slate-600';

                // Calculate progress width based on status
                let progressWidth = '0%';
                if (ms.status === 'APPROVED' || ms.status === 'CONDITIONAL_APPROVED') {
                  progressWidth = '100%';
                } else if (ms.status === 'IN_PROGRESS') {
                  progressWidth = '50%';
                } else if (ms.status === 'SUBMITTED') {
                  progressWidth = '75%';
                }

                return (
                  <div key={ms.code} className="flex items-center gap-4">
                    {/* 标签 */}
                    <div className="w-20 flex-shrink-0">
                      <Badge className={`${barColor} text-white text-xs`}>
                        {ms.code}
                      </Badge>
                    </div>

                    {/* 名称 */}
                    <div className="w-36 flex-shrink-0 text-sm text-slate-300 truncate">
                      {ms.name}
                    </div>

                    {/* 进度条 */}
                    <div className="flex-1">
                      <div className="relative h-6 bg-slate-700 rounded-full overflow-hidden">
                        <div
                          className={`h-full ${barColor} rounded-full transition-all duration-500 ease-in-out`}
                          style={{ width: progressWidth }}
                        />
                        {/* 计划日期标记 */}
                        {ms.plannedDate && (
                          <div className="absolute top-0 h-full border-l-2 border-dashed border-white/40"
                            style={{ left: '60%' }}
                            title={`计划: ${ms.plannedDate}`}
                          />
                        )}
                      </div>
                    </div>

                    {/* 日期信息 */}
                    <div className="w-48 flex-shrink-0 text-xs text-slate-400 text-right">
                      {ms.plannedDate && (
                        <span>计划: {ms.plannedDate}</span>
                      )}
                      {ms.actualDate && (
                        <span className="ml-2 text-green-400">实际: {ms.actualDate}</span>
                      )}
                    </div>

                    {/* 状态 */}
                    <div className="w-20 flex-shrink-0 text-xs text-right">
                      <span className={`${statusInfo.color.replace('bg-', 'text-')}`}>
                        {statusInfo.label}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* 里程碑对比表 (Table 9 from policy) */}
        <Card className="bg-slate-800 border-slate-600">
          <CardHeader>
            <CardTitle className="text-slate-100">里程碑对比表</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-600">
                    <th className="text-left py-3 px-4 text-slate-400 font-semibold">里程碑</th>
                    <th className="text-left py-3 px-4 text-slate-400 font-semibold">计划完成日期</th>
                    <th className="text-left py-3 px-4 text-slate-400 font-semibold">实际完成日期</th>
                    <th className="text-left py-3 px-4 text-slate-400 font-semibold">偏差</th>
                    <th className="text-left py-3 px-4 text-slate-400 font-semibold">说明</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedMilestones.map((ms) => {
                    const deviation = calculateDeviation(ms.plannedDate, ms.actualDate);
                    const deviationColor = deviation === '按时' ? 'text-green-400' : deviation?.startsWith('+') ? 'text-red-400' : 'text-slate-400';

                    return (
                      <tr key={ms.code} className="border-b border-slate-700 hover:bg-slate-700/50">
                        <td className="py-3 px-4">
                          <span className="text-slate-200">{ms.code} {ms.name}</span>
                        </td>
                        <td className="py-3 px-4 text-slate-300">{ms.plannedDate || '-'}</td>
                        <td className="py-3 px-4 text-slate-300">{ms.actualDate || '-'}</td>
                        <td className={`py-3 px-4 ${deviationColor}`}>{deviation || '-'}</td>
                        <td className="py-3 px-4 text-slate-400">
                          {ms.decisionNotes || '-'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Timeline;
