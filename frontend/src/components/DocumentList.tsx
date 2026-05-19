import React, { useState, useEffect } from 'react';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Upload, FileText, Lock, CheckCircle, XCircle, Clock } from 'lucide-react';

interface Document {
  id: string;
  fileName: string;
  fileType: string;
  complianceStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  isLocked: boolean;
  uploadedAt: string;
}

interface DocumentListProps {
  projectId: string;
  currentStage: number;
}

const DocumentList: React.FC<DocumentListProps> = ({ projectId, currentStage }) => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [uploading, setUploading] = useState(false);

  // 模拟数据
  useEffect(() => {
    const mockDocuments: Document[] = [
      {
        id: '1',
        fileName: 'PROJECT_INITIATION_REPORT.pdf',
        fileType: 'PROJECT_INITIATION_REPORT',
        complianceStatus: 'APPROVED',
        isLocked: true,
        uploadedAt: '2024-01-15',
      },
      {
        id: '2',
        fileName: 'PCC_NOMINATION_REPORT.docx',
        fileType: 'PCC_NOMINATION_REPORT',
        complianceStatus: 'PENDING',
        isLocked: false,
        uploadedAt: '2024-01-20',
      },
    ];
    setDocuments(mockDocuments);
  }, [projectId, currentStage]);

  const getStatusBadge = (status: string, isLocked: boolean) => {
    if (isLocked) {
      return <Badge className="bg-green-600 text-white"><Lock className="w-3 h-3 mr-1" />已归档</Badge>;
    }

    switch (status) {
      case 'PENDING':
        return <Badge className="bg-blue-600 text-white"><Clock className="w-3 h-3 mr-1" />待审核</Badge>;
      case 'APPROVED':
        return <Badge className="bg-green-600 text-white"><CheckCircle className="w-3 h-3 mr-1" />已通过</Badge>;
      case 'REJECTED':
        return <Badge className="bg-red-600 text-white"><XCircle className="w-3 h-3 mr-1" />已驳回</Badge>;
      default:
        return <Badge className="bg-gray-600 text-white"><Upload className="w-3 h-3 mr-1" />待上传</Badge>;
    }
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setUploading(true);
      // 模拟上传
      setTimeout(() => {
        setUploading(false);
        alert('文件上传成功！');
      }, 2000);
    }
  };

  return (
    <div className="space-y-4">
      {/* 上传区域 */}
      <Card className="bg-slate-700 border-slate-600">
        <CardContent className="p-4">
          <div className="flex items-center gap-4">
            <input
              type="file"
              id="file-upload"
              className="hidden"
              onChange={handleFileUpload}
              accept=".pdf,.doc,.docx,.xls,.xlsx"
            />
            <label htmlFor="file-upload">
              <Button
                asChild
                variant="outline"
                className="bg-slate-600 text-slate-100 border-slate-500 hover:bg-slate-500"
                disabled={uploading}
              >
                <span>
                  <Upload className="w-4 h-4 mr-2" />
                  {uploading ? '上传中...' : '上传交付物'}
                </span>
              </Button>
            </label>
            <p className="text-sm text-slate-400">
              支持 PDF、Word、Excel 格式文件，最大 50MB
            </p>
          </div>
        </CardContent>
      </Card>

      {/* 文档列表 */}
      <div className="space-y-2">
        {documents.map((doc) => (
          <Card key={doc.id} className="bg-slate-700 border-slate-600">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <FileText className="w-5 h-5 text-slate-400" />
                  <div>
                    <p className="text-slate-100 font-medium">{doc.fileName}</p>
                    <p className="text-sm text-slate-400">上传时间：{doc.uploadedAt}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {getStatusBadge(doc.complianceStatus, doc.isLocked)}
                  <Button variant="outline" size="sm" className="bg-slate-600 text-slate-100 border-slate-500">
                    下载
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {documents.length === 0 && (
        <Card className="bg-slate-700 border-slate-600">
          <CardContent className="p-8 text-center">
            <FileText className="w-12 h-12 text-slate-500 mx-auto mb-4" />
            <p className="text-slate-400">暂无交付物</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default DocumentList;
export { DocumentList };