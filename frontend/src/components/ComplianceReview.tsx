import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Textarea } from '../components/ui/textarea';
import { FileText, Eye, CheckCircle, XCircle } from 'lucide-react';

interface PendingDocument {
  id: string;
  fileName: string;
  projectId: string;
  milestonePhase: string;
  uploadedAt: string;
}

const ComplianceReview: React.FC = () => {
  const [pendingDocuments, setPendingDocuments] = useState<PendingDocument[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<PendingDocument | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');

  // 模拟数据
  useEffect(() => {
    const mockDocs: PendingDocument[] = [
      {
        id: '1',
        fileName: 'IND_DOSSIER.pdf',
        projectId: 'KBD-001',
        milestonePhase: 'G5',
        uploadedAt: '2024-01-20',
      },
      {
        id: '2',
        fileName: 'GLP_TOX_REPORT.docx',
        projectId: 'KBD-002',
        milestonePhase: 'G4',
        uploadedAt: '2024-01-21',
      },
    ];
    setPendingDocuments(mockDocs);
  }, []);

  const handleReview = (approved: boolean) => {
    if (!selectedDoc) return;

    alert(`文档 ${selectedDoc.fileName} 审核${approved ? '通过' : '驳回'}`);

    // 移除已审核的文档
    setPendingDocuments(prev => prev.filter(doc => doc.id !== selectedDoc.id));
    setSelectedDoc(null);
    setReviewNotes('');
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        <h1 className="text-3xl font-bold text-slate-100">药政合规审核终端</h1>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* 待审核文档列表 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100 flex items-center gap-2">
                <FileText className="w-5 h-5" />
                待审核申报资料 ({pendingDocuments.length})
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {pendingDocuments.map((doc) => (
                <Card
                  key={doc.id}
                  className={`bg-slate-700 border-slate-600 cursor-pointer transition-colors ${
                    selectedDoc?.id === doc.id ? 'ring-2 ring-blue-500' : ''
                  }`}
                  onClick={() => setSelectedDoc(doc)}
                >
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-slate-100 font-medium">{doc.fileName}</p>
                        <p className="text-sm text-slate-400">
                          项目: {doc.projectId} | 阶段: {doc.milestonePhase} | 上传: {doc.uploadedAt}
                        </p>
                      </div>
                      <Badge className="bg-yellow-600 text-white">待审核</Badge>
                    </div>
                  </CardContent>
                </Card>
              ))}

              {pendingDocuments.length === 0 && (
                <div className="text-center py-8">
                  <FileText className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                  <p className="text-slate-400">暂无待审核文档</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 审核面板 */}
          <Card className="bg-slate-800 border-slate-600">
            <CardHeader>
              <CardTitle className="text-slate-100">文档审核</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {selectedDoc ? (
                <>
                  <div className="space-y-2">
                    <h3 className="text-slate-100 font-medium">{selectedDoc.fileName}</h3>
                    <p className="text-sm text-slate-400">
                      项目: {selectedDoc.projectId} | 阶段: {selectedDoc.milestonePhase}
                    </p>
                  </div>

                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      className="bg-slate-700 text-slate-100 border-slate-600"
                      onClick={() => alert('预览功能')}
                    >
                      <Eye className="w-4 h-4 mr-2" />
                      预览
                    </Button>
                    <Button
                      variant="outline"
                      className="bg-slate-700 text-slate-100 border-slate-600"
                      onClick={() => alert('下载功能')}
                    >
                      下载
                    </Button>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm text-slate-400">审核意见</label>
                    <Textarea
                      value={reviewNotes}
                      onChange={(e) => setReviewNotes(e.target.value)}
                      placeholder="请输入审核意见..."
                      className="bg-slate-700 border-slate-600 text-slate-100"
                    />
                  </div>

                  <div className="flex gap-2">
                    <Button
                      onClick={() => handleReview(true)}
                      className="bg-green-600 hover:bg-green-700 text-white"
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      合规通过
                    </Button>
                    <Button
                      onClick={() => handleReview(false)}
                      className="bg-red-600 hover:bg-red-700 text-white"
                    >
                      <XCircle className="w-4 h-4 mr-2" />
                      驳回
                    </Button>
                  </div>
                </>
              ) : (
                <div className="text-center py-8">
                  <FileText className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                  <p className="text-slate-400">请选择要审核的文档</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default ComplianceReview;