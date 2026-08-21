import React, { useEffect, useRef, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { CheckCircle, Clock, Download, Eye, FileText, FolderInput, Loader2, Lock, Trash2, Upload, XCircle } from 'lucide-react';
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
  const canDelete = userRoles.includes('ROLE_ADMIN') || userRoles.includes('ROLE_PROJECT_ADMIN');

  const isPreviewable = (fileName: string) => {
    const name = fileName.toLowerCase();
    return ['.pdf', '.png', '.jpg', '.jpeg', '.gif', '.txt', '.csv', '.log', '.docx', '.xls', '.xlsx'].some((extension) => name.endsWith(extension));
  };

  const getResponseFileName = (response: { headers?: { [key: string]: unknown } }, fallback: string) => {
    const headerValue = response.headers?.['content-disposition'];
    if (typeof headerValue !== 'string') return fallback;
    const header = headerValue;
    const encoded = header.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    if (encoded) {
      try { return decodeURIComponent(encoded); } catch { return fallback; }
    }
    const plain = header.match(/filename="?([^";]+)"?/i)?.[1];
    return plain || fallback;
  };

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
    const previewWindow = mode === 'preview' ? window.open('', '_blank') : null;
    try {
      const response = await api.get(`/api/milestone-deliverables/${doc.id}/${mode}`, { responseType: 'blob' });
      const lowerName = doc.fileName.toLowerCase();
      if (mode === 'preview' && previewWindow && lowerName.endsWith('.docx')) {
        previewWindow.document.title = doc.fileName;
        const { renderAsync } = await import('docx-preview');
        await renderAsync(response.data, previewWindow.document.body);
        return;
      }
      if (mode === 'preview' && previewWindow && (lowerName.endsWith('.xls') || lowerName.endsWith('.xlsx'))) {
        const XLSX = await import('xlsx');
        const workbook = XLSX.read(await response.data.arrayBuffer(), { type: 'array' });
        const tabs = workbook.SheetNames.map((name) => `<h2>${name.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char] || char))}</h2>${XLSX.utils.sheet_to_html(workbook.Sheets[name])}`).join('');
        previewWindow.document.title = doc.fileName;
        previewWindow.document.body.innerHTML = `<style>body{font-family:Arial,sans-serif;padding:24px;color:#111}table{border-collapse:collapse;margin-bottom:32px}td,th{border:1px solid #bbb;padding:6px 10px;white-space:nowrap}h2{margin-top:24px}</style>${tabs}`;
        return;
      }
      const blobUrl = URL.createObjectURL(response.data);
      if (mode === 'preview') {
        if (previewWindow) previewWindow.location.href = blobUrl;
        window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
      } else {
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = getResponseFileName(response, doc.fileName);
        document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(blobUrl);
      }
    } catch (error: any) {
      previewWindow?.close();
      toast.error(error.response?.status === 403 ? '无权访问该文件' : '文件打开失败');
    }
  };

  const uploadFiles = async (group: MilestoneDeliverableGroup, slot: DeliverableSlot, selected?: FileList | null) => {
    const files = Array.from(selected || []);
    if (files.length === 0) return;
    const uploadKey = `${group.milestoneCode}:${slot.slotCode}`;
    setUploadingSlot(uploadKey);
    let uploaded = 0;
    try {
      for (const file of files) {
        const form = new FormData();
        form.append('file', file); form.append('projectId', projectId); form.append('milestoneCode', group.milestoneCode); form.append('slotCode', slot.slotCode); form.append('fileType', slot.slotName);
        await api.post('/api/milestone-deliverables/import', form, { headers: { 'Content-Type': 'multipart/form-data' } });
        uploaded++;
      }
      toast.success(`${uploaded} 个文件上传成功`);
      await loadSlotGroups();
    } catch (error: any) {
      if (uploaded > 0) await loadSlotGroups();
      toast.error(error.response?.data?.message || `文件上传失败，已成功上传 ${uploaded} 个`);
    } finally {
      setUploadingSlot(null);
      const input = fileInputRefs.current[uploadKey];
      if (input) input.value = '';
    }
  };

  const deleteDocument = async (doc: Document) => {
    if (!window.confirm(`确定要删除文件“${doc.fileName}”吗？`)) return;
    try {
      await api.delete(`/api/milestone-deliverables/${doc.id}`);
      toast.success('文件已删除');
      await loadSlotGroups();
    } catch (error: any) {
      toast.error(error.response?.data?.message || '删除文件失败');
    }
  };

  const renderDocument = (doc: Document) => (
    <div key={doc.id} className="flex flex-col gap-3 rounded-md border border-slate-600 bg-slate-900/60 p-3 md:flex-row md:items-center md:justify-between">
      <div className="flex items-start gap-3 min-w-0"><FileText className="w-5 h-5 text-blue-400 mt-0.5 shrink-0" /><div className="min-w-0"><div className="text-slate-100 font-medium truncate">{doc.fileName}</div><div className="text-xs text-slate-400 mt-1 flex flex-wrap gap-x-3 gap-y-1"><span>上传时间: {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleDateString('zh-CN') : '-'}</span><span>槽位编码: {doc.deliverableSlotCode}</span></div></div></div>
      <div className="flex items-center gap-2 shrink-0">{getStatusBadge(doc.complianceStatus, doc.isLocked)}{isPreviewable(doc.fileName) && <Button size="sm" variant="outline" onClick={() => openFile(doc, 'preview')} title="预览文件" className="bg-slate-700 text-slate-100 border-slate-600"><Eye className="w-4 h-4" /></Button>}<Button size="sm" variant="outline" onClick={() => openFile(doc, 'download')} title="下载文件" className="bg-slate-700 text-slate-100 border-slate-600"><Download className="w-4 h-4" /></Button>{canDelete && <Button size="sm" variant="outline" onClick={() => deleteDocument(doc)} title="删除文件" className="border-red-800 text-red-400 hover:bg-red-900/30"><Trash2 className="w-4 h-4" /></Button>}</div>
    </div>
  );

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4"><div><h3 className="text-lg font-semibold text-slate-100 flex items-center gap-2"><FileText className="w-5 h-5 text-blue-400" />项目交付物管理</h3><p className="text-sm text-slate-400 mt-1">G0-G9 固定交付物槽位全部展开；每个槽位支持一次选择多个文件，新文件将追加到已有列表下方。</p></div>{canImport && <Badge className="bg-blue-600 text-white"><FolderInput className="w-3 h-3 mr-1" />管理员直传已启用</Badge>}</div>
      {loading ? <div className="flex items-center gap-2 text-slate-400 text-sm py-8 justify-center"><Loader2 className="w-5 h-5 animate-spin" />加载中...</div> : <div className="space-y-5">{groups.map((group) => (
        <Card key={group.milestoneCode} className="bg-slate-800 border-slate-600"><CardHeader className="pb-3"><div className="flex items-center justify-between gap-3"><CardTitle className="text-slate-100 text-base">{group.phaseLabel}</CardTitle><Badge className={group.uploadedCount === group.totalCount && group.totalCount > 0 ? 'bg-green-600 text-white' : 'bg-slate-700 text-slate-200'}>{group.uploadedCount}/{group.totalCount} 已上传</Badge></div></CardHeader><CardContent className="space-y-3">
          {group.slots.length === 0 ? <div className="text-sm text-slate-500 py-3">该阶段暂无交付物定义</div> : group.slots.map((slot) => {
            const uploadKey = `${group.milestoneCode}:${slot.slotCode}`; const hasDocuments = slot.documents && slot.documents.length > 0;
            return <div key={slot.slotCode} className="rounded-lg border border-slate-700 bg-slate-900/30 p-4"><div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between"><div><div className="flex items-center gap-2"><span className="font-medium text-slate-100">{slot.slotName}</span>{slot.isRequired && <Badge variant="outline" className="border-orange-400 text-orange-300">必传</Badge>}{!hasDocuments && <Badge variant="outline" className="border-slate-500 text-slate-400">空槽位</Badge>}</div><div className="text-xs text-slate-500 mt-1">固定槽位：{slot.slotCode}</div>{slot.description && <div className="text-xs text-slate-400 mt-1">{slot.description}</div>}</div>{canImport && <div className="shrink-0"><input ref={(el) => { fileInputRefs.current[uploadKey] = el; }} type="file" multiple accept={slot.allowedFileTypes || '.pdf,.doc,.docx'} className="hidden" onChange={(e) => uploadFiles(group, slot, e.target.files)} /><Button size="sm" disabled={uploadingSlot === uploadKey} onClick={() => fileInputRefs.current[uploadKey]?.click()} className="bg-blue-600 hover:bg-blue-700 text-white">{uploadingSlot === uploadKey ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Upload className="w-4 h-4 mr-2" />}上传文件</Button></div>}</div><div className="mt-3 space-y-2">{hasDocuments ? slot.documents.map(renderDocument) : <div className="rounded-md border border-dashed border-slate-600 p-3 text-sm text-slate-500">等待上传：{slot.slotName}</div>}</div>
            </div>;
          })}
        </CardContent></Card>
      ))}</div>}
    </div>
  );
};

export { DocumentList };