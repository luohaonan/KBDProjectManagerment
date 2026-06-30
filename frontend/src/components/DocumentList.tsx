import React, { useState, useEffect } from 'react';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Upload, FileText, Lock, CheckCircle, XCircle, Clock, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../lib/api';

interface Document {
  id: number;
  fileName: string;
  fileType: string;
  deliverableSlotCode: string;
  milestonePhase: string;
  complianceStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  isLocked: boolean;
  uploadedAt: string;
  uploader: number;
  storagePath: string;
}

interface DocumentListProps {
  projectId: string;
  currentStage: number;
}

const DocumentList: React.FC<DocumentListProps> = ({ projectId, currentStage }) => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  const loadDocuments = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/api/projects/${projectId}/documents`);
      const result = res.data as { code: number; data: Document[]; message?: string };
      if (result.code === 200 || result.code === 0) {
        setDocuments(result.data || []);
      }
    } catch (error: any) {
      console.error('加载文档失败:', error);
      toast.error('加载项目文档失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (projectId) {
      loadDocuments();
    }
  }, [projectId]);

  const getStatusBadge = (status: string, isLocked: boolean) => {
    if (isLocked) {
      return <Badge className="bg-green-600 text-white"><Lock className="w-3 h-3 mr-1" />已归档</Badge>;
    }
    switch (status) {
      case 'APPROVED':
        return <Badge className="bg-green-600 text-white"><CheckCircle className="w-3 h-3 mr-1" />已审核</Badge>;
      case 'REJECTED':
        return <Badge className="bg-red-600 text-white"><XCircle className="w-3 h-3 mr-1" />未通过</Badge>;
      default:
        return <Badge className="bg-yellow-600 text-white"><Clock className="w-3 h-3 mr-1" />待审核</Badge>;
    }
  };

  const handleUpload = async () => {
    toast.info('请使用里程碑控制台中的交付物上传功能');
  };

  const milestonePhaseNames: Record<string, string> = {
    G0: '项目立项', G1: '先导化合物', G2: '优选化合物',
    G3: 'PCC提名', G4: 'GLP临床前', G5: 'IND申报',
    G6: '临床Ⅰ期', G7: '临床Ⅱ期', G8: '临床Ⅲ期', G9: 'NDA获批',
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-slate-100 flex items-center gap-2">
          <FileText className="w-5 h-5 text-blue-400" />
          项目文档管理
        </h3>
        <Button
          onClick={handleUpload}
          variant="outline"
          className="bg-slate-700 text-slate-100 border-slate-600 hover:bg-slate-600"
        >
          <Upload className="w-4 h-4 mr-2" />
          上传文档
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center">
          <Loader2 className="w-5 h-5 animate-spin" />
          加载中...
        </div>
      ) : documents.length === 0 ? (
        <div className="text-center py-8 text-slate-400">
          <FileText className="w-12 h-12 text-slate-500 mx-auto mb-3" />
          <p>暂无项目文档</p>
          <p className="text-sm text-slate-500 mt-1">
            系统管理员、项目经理、PMC成员和部门负责人可查看所有项目文档
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {documents.map((doc) => (
            <Card key={doc.id} className="bg-slate-800 border-slate-600">
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <FileText className="w-5 h-5 text-blue-400" />
                  <div>
                    <div className="text-slate-100 font-medium">{doc.fileName}</div>
                    <div className="text-xs text-slate-400 mt-1 flex gap-3">
                      <span>阶段: {milestonePhaseNames[doc.milestonePhase] || doc.milestonePhase}</span>
                      {doc.deliverableSlotCode && <span>槽位: {doc.deliverableSlotCode}</span>}
                      <span>上传时间: {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleDateString('zh-CN') : '-'}</span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  {getStatusBadge(doc.complianceStatus, doc.isLocked)}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

export { DocumentList };
