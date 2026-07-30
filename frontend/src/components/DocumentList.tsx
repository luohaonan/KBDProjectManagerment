import React, { useEffect, useRef, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { CheckCircle, Clock, Download, Eye, FileText, FolderInput, Loader2, Lock, Upload, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import api from '../lib/api';

interface Document {
  id: number; fileName: string; fileType: string; deliverableSlotCode: string; milestonePhase: string;
  complianceStatus: 'PENDING' | 'APPROVED' | 'REJECTED'; isLocked: boolean; uploadedAt: string; uploader: number; storagePath: string;
}

interface DeliverableSlot {
  slotCode: string; slotName: string; isRequired: boolean; description?: string; allowedFileTypes?: string; documents: Document[];
}

interface MilestoneDeliverableGroup {
  milestoneCode: string; milestoneName: string; phaseLabel: string; uploadedCount: number; totalCount: number; slots: DeliverableSlot[];
}

interface DocumentListProps {
  projectId: string; currentStage: number; userRoles?: string[]; userPermissions?: string[];
}

const DocumentList: React.FC<DocumentListProps> = ({ projectId, userRoles = [], userPermissions = [] }) => {
  const [groups, setGroups] = useState<MilestoneDeliverableGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploadingSlot, setUploadingSlot] = useState<string | null>(null);
  const fileInputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const canImport = userPermissions.includes('PERMISSION_DELIVERABLE_IMPORT') || userRoles.includes('ROLE_ADMIN') || userRoles.includes('ROLE_PROJECT_ADMIN');

  const loadSlotGroups = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/api/milestone-deliverables/project/${projectId}/slots`);
      const result = res.data as { code: number; data: MilestoneDeliverableGroup[]; message?: string };
      if (result.code === 200 || result.code === 0) setGroups(result.data || []);
      else toast.error(result.message || '加载交付物槽位失败');
    } catch (error: any) {
      console.error('加载交付物槽位失败:', error);
      toast.error(error.response?.data?.message || '加载交付物槽位失败');
    } finally { setLoading(false); }
  };

  useEffect(() => { if (projectId) loadSlotGroups(); }, [projectId]);

  const getStatusBadge = (status: string, isLocked: boolean) => {
    if (isLocked) return <Badge className="bg-green-600 text-white"><Lock className="w-3 h-3 mr-1" />已归档</Badge>;
    switch (status) {
      case 'APPROVED': return <Badge className="bg-green-600 text-white"><CheckCircle className="w-3 h-3 mr-1" />已审核</Badge>;
      case 'REJECTED': return <Badge className="bg-red-600 text-white"><XCircle className="w-3 h-3 mr-1" />未通过</Badge>;
      default: return <Badge className="bg-yellow-600 text-white"><Clock className="w-3 h-3 mr-1" />待审核</Badge>;
    }
  };

  const openFile = async (doc: Document, mode: 'preview' | 'download') => {
    try {
      const response = await api.get(`/api/milestone-deliverables/${doc.id}/${mode}`, { responseType: 'blob' });
      const blobUrl = URL.createObjectURL(response.data);
      if (mode === 'preview') {
        window.open(blobUrl, '_blank', 'noopener,noreferrer');
        window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
      } else {
        const link = document.createElement('a');
        link.href = blobUrl; link.download = doc.fileName; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(blobUrl);
      }
    } catch (error: any) { toast.error(error.response?.status === 403 ? '无权访问该文件' : '文件打开失败'); }
  };

  const importHistoricalFile = async (group: MilestoneDeliverableGroup, slot: DeliverableSlot, file?: File | null) => {
    if (!file) return;
    const uploadKey = `${group.milestoneCode}:${slot.slotCode}`;
    const form = new FormData();
    form.append('file', file); form.append('projectId', projectId); form.append('milestoneCode', group.milestoneCode); form.append('slotCode', slot.slotCode); form.append('fileType', slot.slotName);
    setUploadingSlot(uploadKey);
    try {
      await api.post('/api/milestone-deliverables/import', form, { headers: { 'Content-Type': 'multipart/form-data' } });
      toast.success(`${group.milestoneCode}「${slot.slotName}」导入成功，项目阶段已同步`);
      await loadSlotGroups();
    } catch (error: any) { toast.error(error.response?.data?.message || '交付物导入失败'); }
    finally { setUploadingSlot(null); const input = fileInputRefs.current[uploadKey]; if (input) input.value = ''; }
  };

  const renderDocument = (doc: Document) => (
    <div key={doc.id} className="flex flex-col gap-3 rounded-md border border-slate-600 bg-slate-900/60 p-3 md:flex-row md:items-center md:justify-between">
      <div className="flex items-start gap-3 min-w-0"><FileText className="w-5 h-5 text-blue-400 mt-0.5 shrink-0" /><div className="min-w-0"><div className="text-slate-100 font-medium truncate">{doc.fileName}</div><div className="text-xs text-slate-400 mt-1 flex flex-wrap gap-x-3 gap-y-1"><span>上传时间: {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleDateString('zh-CN') : '-'}</span><span>槽位编码: {doc.deliverableSlotCode}</span></div></div></div>
      <div className="flex items-center gap-2 shrink-0">{getStatusBadge(doc.complianceStatus, doc.isLocked)}<Button size="sm" variant="outline" onClick={() => openFile(doc, 'preview')} title="预览文件" className="bg-slate-700 text-slate-100 border-slate-600"><Eye className="w-4 h-4" /></Button><Button size="sm" variant="outline" onClick={() => openFile(doc, 'download')} title="下载文件" className="bg-slate-700 text-slate-100 border-slate-600"><Download className="w-4 h-4" /></Button></div>
    </div>
  );

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4"><div><h3 className="text-lg font-semibold text-slate-100 flex items-center gap-2"><FileText className="w-5 h-5 text-blue-400" />项目交付物管理</h3><p className="text-sm text-slate-400 mt-1">G0-G9 固定交付物槽位全部展开；空槽位以文件名称占位，管理员可直接导入历史交付物。</p></div>{canImport && <Badge className="bg-blue-600 text-white"><FolderInput className="w-3 h-3 mr-1" />管理员直传已启用</Badge>}</div>
      {loading ? <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center"><Loader2 className="w-5 h-5 animate-spin" />加载中...</div> : <div className="space-y-5">{groups.map((group) => (
        <Card key={group.milestoneCode} className="bg-slate-800 border-slate-600"><CardHeader className="pb-3"><div className="flex items-center justify-between gap-3"><CardTitle className="text-slate-100 text-base">{group.phaseLabel}</CardTitle><Badge className={group.uploadedCount === group.totalCount && group.totalCount > 0 ? 'bg-green-600 text-white' : 'bg-slate-700 text-slate-200'}>{group.uploadedCount}/{group.totalCount} 已上传</Badge></div></CardHeader><CardContent className="space-y-3">
          {group.slots.length === 0 ? <div className="text-sm text-slate-500 py-3">该阶段暂无交付物定义</div> : group.slots.map((slot) => {
            const uploadKey = `${group.milestoneCode}:${slot.slotCode}`; const hasDocuments = slot.documents && slot.documents.length > 0;
            return <div key={slot.slotCode} className="rounded-lg border border-slate-700 bg-slate-900/30 p-4"><div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between"><div><div className="flex items-center gap-2"><span className="font-medium text-slate-100">{slot.slotName}</span>{slot.isRequired && <Badge variant="outline" className="border-orange-400 text-orange-300">必传</Badge>}{!hasDocuments && <Badge variant="outline" className="border-slate-500 text-slate-400">空槽位</Badge>}</div><div className="text-xs text-slate-500 mt-1">固定槽位：{slot.slotCode}</div>{slot.description && <div className="text-xs text-slate-400 mt-1">{slot.description}</div>}</div>{canImport && !hasDocuments && <div className="shrink-0"><input ref={(el) => { fileInputRefs.current[uploadKey] = el; }} type="file" className="hidden" onChange={(e) => importHistoricalFile(group, slot, e.target.files?.[0])} /><Button size="sm" disabled={uploadingSlot === uploadKey} onClick={() => fileInputRefs.current[uploadKey]?.click()} className="bg-blue-600 hover:bg-blue-700 text-white">{uploadingSlot === uploadKey ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Upload className="w-4 h-4 mr-2" />}上传到此槽位</Button></div>}</div><div className="mt-3 space-y-2">{hasDocuments ? slot.documents.map(renderDocument) : <div className="rounded-md border border-dashed border-slate-600 p-3 text-sm text-slate-500">等待上传：{slot.slotName}</div>}</div></div>;
          })}
        </CardContent></Card>
      ))}</div>}
    </div>
  );
};

export { DocumentList };