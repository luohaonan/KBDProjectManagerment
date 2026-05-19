import React, { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FileText, Microscope, Stethoscope, ShieldCheck } from 'lucide-react';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { DataTable, DataTableColumn } from '../components/ui/data-table';

interface ApprovalItem {
  id: string;
  phase: string;
  milestone: string;
  reportType: string;
  submittedBy: string;
  submittedDate: string;
  status: string;
}

const mockApprovalItems: ApprovalItem[] = [
  {
    id: 'APR-001',
    phase: 'G3',
    milestone: '候选化合物提名 (PCC)',
    reportType: 'PCC 提名报告',
    submittedBy: 'PM Zhang',
    submittedDate: '2026-04-22',
    status: '待审',
  },
  {
    id: 'APR-002',
    phase: 'G4',
    milestone: '临床前开发完成 (GLP)',
    reportType: 'GLP毒理报告',
    submittedBy: 'PM Li',
    submittedDate: '2026-04-18',
    status: '已审阅',
  },
];

const reportPreview = {
  title: 'PCC 提名报告摘要',
  summary: '本报告包含候选化合物筛选结果、体内外药效评估、初步ADME数据、专利策略和推荐PCC方案。',
  keyPoints: [
    '候选化合物具有显著体外活性',
    '初步药代动力学显示口服暴露良好',
    '安全性评价符合早期开发要求',
  ],
};

const PmcApproval: React.FC = () => {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [selectedDecision, setSelectedDecision] = useState<'go' | 'conditional' | 'no-go' | null>(null);
  const [conditionalText, setConditionalText] = useState('');
  const [archiveConfirmed, setArchiveConfirmed] = useState(false);
  const [disposeConfirmed, setDisposeConfirmed] = useState(false);

  const columns: DataTableColumn<ApprovalItem>[] = useMemo(() => [
    { header: '审批编号', accessor: 'id' },
    { header: '阶段', accessor: row => (
        <div className="flex items-center gap-2">
          {row.phase.includes('Q') ? <Microscope className="w-4 h-4 text-cyan-400" /> : <Stethoscope className="w-4 h-4 text-rose-400" />}
          <span>{row.phase}</span>
        </div>
      ) as any,
      searchable: true,
    },
    { header: '里程碑', accessor: 'milestone', searchable: true },
    { header: '报告类型', accessor: 'reportType', searchable: true },
    { header: '提交人', accessor: 'submittedBy' },
    { header: '提交日期', accessor: 'submittedDate' },
    { header: '状态', accessor: row => <Badge variant="outline" className="bg-slate-700 text-slate-200">{row.status}</Badge> as any },
  ], []);

  const decisionDescription = useMemo(() => {
    switch (selectedDecision) {
      case 'go':
        return '您的选择：Go。项目进入下一阶段审批流程。';
      case 'conditional':
        return '您的选择：Conditional Go。请录入三个月观察期的具体要求。';
      case 'no-go':
        return '您的选择：No Go。请确认进入项目终止流程。';
      default:
        return '请选择审批结果。';
    }
  }, [selectedDecision]);

  const canSubmit = selectedDecision === 'go' || (selectedDecision === 'conditional' && conditionalText.trim().length > 0) || (selectedDecision === 'no-go' && disposeConfirmed && archiveConfirmed);

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <div className="mx-auto w-full max-w-7xl px-6 py-6">
      <div className="mb-6 flex items-center gap-4">
        <Button
          onClick={() => navigate(-1)}
          variant="outline"
          className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700"
        >
          返回
        </Button>
        <div>
          <h1 className="text-3xl font-bold">PMC 审批界面</h1>
          <p className="text-slate-400">项目 {projectId ?? '未知'} 的里程碑审批</p>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[1.4fr_0.6fr] gap-6">
        <div className="space-y-6">
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">报告预览</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center gap-3 text-slate-100">
                  <FileText className="w-5 h-5 text-cyan-400" />
                  <h2 className="text-xl font-semibold">{reportPreview.title}</h2>
                </div>
                <p className="text-slate-300">{reportPreview.summary}</p>
                <div className="space-y-2">
                  {reportPreview.keyPoints.map((point, index) => (
                    <div key={index} className="flex items-start gap-2">
                      <span className="mt-1 h-2 w-2 rounded-full bg-blue-400" />
                      <p className="text-slate-300 text-sm">{point}</p>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">审批决策</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-3">
                <Button
                  className={selectedDecision === 'go' ? 'bg-green-600 hover:bg-green-700' : 'bg-slate-700 text-slate-100'}
                  onClick={() => setSelectedDecision('go')}
                >
                  Go
                </Button>
                <Button
                  className={selectedDecision === 'conditional' ? 'bg-yellow-600 hover:bg-yellow-700' : 'bg-slate-700 text-slate-100'}
                  onClick={() => setSelectedDecision('conditional')}
                >
                  Conditional Go
                </Button>
                <Button
                  className={selectedDecision === 'no-go' ? 'bg-red-600 hover:bg-red-700' : 'bg-slate-700 text-slate-100'}
                  onClick={() => setSelectedDecision('no-go')}
                >
                  No Go
                </Button>
              </div>

              <div className="rounded-lg border border-slate-700 bg-slate-900 p-4">
                <p className="text-slate-300">{decisionDescription}</p>
              </div>

              {selectedDecision === 'conditional' && (
                <div className="space-y-3">
                  <label className="text-sm font-medium text-slate-300">三个月观察期要求</label>
                  <textarea
                    value={conditionalText}
                    onChange={e => setConditionalText(e.target.value)}
                    rows={5}
                    placeholder="请输入条件性通过的具体要求，例如监控指标、数据提交周期、风险控制点等。"
                    className="w-full rounded-md border border-slate-700 bg-slate-800 px-3 py-2 text-slate-100 placeholder:text-slate-500"
                  />
                </div>
              )}

              {selectedDecision === 'no-go' && (
                <div className="space-y-4">
                  <Card className="bg-slate-900 border border-slate-700">
                    <CardHeader>
                      <CardTitle className="text-slate-100 text-base">项目终止流程</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="space-y-3">
                        <div className="flex items-start gap-3">
                          <ShieldCheck className="w-5 h-5 text-red-400 mt-1" />
                          <div>
                            <p className="text-slate-100 font-medium">资产处置</p>
                            <p className="text-slate-400 text-sm">确认实验设备、委托服务、药品样本等资产处置方案。</p>
                          </div>
                        </div>

                        <div className="flex items-start gap-3">
                          <ShieldCheck className="w-5 h-5 text-red-400 mt-1" />
                          <div>
                            <p className="text-slate-100 font-medium">归档确认</p>
                            <p className="text-slate-400 text-sm">确认所有项目资料已按照研发文档规范归档并留存备查。</p>
                          </div>
                        </div>
                      </div>

                      <div className="grid gap-3 sm:grid-cols-2">
                        <label className="inline-flex items-center gap-2 rounded-md border border-slate-700 bg-slate-800 px-3 py-2">
                          <input
                            type="checkbox"
                            checked={disposeConfirmed}
                            onChange={e => setDisposeConfirmed(e.target.checked)}
                            className="h-4 w-4 rounded border-slate-600 bg-slate-700 text-blue-500"
                          />
                          <span className="text-slate-200 text-sm">资产处置已确认</span>
                        </label>
                        <label className="inline-flex items-center gap-2 rounded-md border border-slate-700 bg-slate-800 px-3 py-2">
                          <input
                            type="checkbox"
                            checked={archiveConfirmed}
                            onChange={e => setArchiveConfirmed(e.target.checked)}
                            className="h-4 w-4 rounded border-slate-600 bg-slate-700 text-blue-500"
                          />
                          <span className="text-slate-200 text-sm">归档流程已确认</span>
                        </label>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              )}

              <Button
                disabled={!canSubmit}
                className={`w-full ${canSubmit ? 'bg-blue-600 hover:bg-blue-700' : 'bg-slate-700 text-slate-400 cursor-not-allowed'}`}
                onClick={() => {
                  alert(`审批结果已提交: ${selectedDecision}`);
                }}
              >
                提交审批
              </Button>
            </CardContent>
          </Card>

          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">报告列表</CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                data={mockApprovalItems}
                columns={columns}
                initialRowsPerPage={5}
              />
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">阶段视觉标识</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center gap-3 rounded-md border border-slate-700 bg-slate-900 p-4">
                  <Microscope className="w-6 h-6 text-cyan-400" />
                  <div>
                    <p className="text-slate-100 font-semibold">临床前阶段 (Q)</p>
                    <p className="text-slate-400 text-sm">使用 Microscope 图标展示预临床研发阶段。</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 rounded-md border border-slate-700 bg-slate-900 p-4">
                  <Stethoscope className="w-6 h-6 text-rose-400" />
                  <div>
                    <p className="text-slate-100 font-semibold">临床阶段 (L)</p>
                    <p className="text-slate-400 text-sm">使用 Stethoscope 图标展示临床研发阶段。</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">PMC 说明</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-slate-400 text-sm">
                该审批界面用于 PMC 对 PM 提交的里程碑报告进行线上化决策。请选择合适的审批结果，并在必要时补充条件性通过或终止流程信息。
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
      </div>
    </div>
  );
};

export default PmcApproval;
