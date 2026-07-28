import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import api from '../lib/api';
import { toast } from 'sonner';
import { Save, GitBranch, Plus, X, GripVertical, AlertCircle, ChevronLeft, Trash2 } from 'lucide-react';

/* ========== types ========== */
interface ProcessNode {
  id: number; processDefinitionId: number; nodeCode: string; nodeName: string;
  nodeType: string; approverRule: string; approverValue: string; decisionType: string;
  isUploader: boolean; deliverableSlotCode: string;
  positionX: number; positionY: number; sortOrder: number;
}
interface ProcessEdge {
  id: number; processDefinitionId: number;
  fromNodeId: number; fromNodeCode: string; toNodeId: number; toNodeCode: string;
}
interface ProcessDefinition {
  id: number; processType: string; milestoneCode: string; description: string;
  isActive: boolean; nodes: ProcessNode[]; edges: ProcessEdge[];
}

/* ========== constants ========== */
const UNIFIED_NODE_COLOR = 'bg-blue-600';
const NODE_COLORS: Record<string, string> = { UPLOAD: UNIFIED_NODE_COLOR, DEPT_HEAD_APPROVE: UNIFIED_NODE_COLOR, ROLE_APPROVE: UNIFIED_NODE_COLOR, DECISION: UNIFIED_NODE_COLOR };
const LAYER_X_GAP = 160; const NODE_Y_GAP = 70; const BASE_X = 50; const BASE_Y = 30;

/* ========== auto-layout (optional, called manually) ========== */
function autoLayout(nodes: ProcessNode[], edges: ProcessEdge[]): ProcessNode[] {
  if (nodes.length === 0) return nodes;
  const children = new Map<number, number[]>(); const pCount = new Map<number, number>();
  nodes.forEach(n => { children.set(n.id, []); pCount.set(n.id, 0); });
  edges.forEach(e => { children.get(e.fromNodeId)?.push(e.toNodeId); pCount.set(e.toNodeId, (pCount.get(e.toNodeId) ?? 0) + 1); });
  const layers: number[][] = []; const assigned = new Set<number>();
  let frontier = nodes.filter(n => (pCount.get(n.id) ?? 0) === 0).map(n => n.id);
  while (frontier.length > 0) {
    const layer: number[] = []; const next: number[] = [];
    for (const nid of frontier) {
      if (assigned.has(nid)) continue; assigned.add(nid); layer.push(nid);
      (children.get(nid) || []).forEach(kid => { pCount.set(kid, (pCount.get(kid) ?? 1) - 1); if ((pCount.get(kid) ?? 0) === 0 && !assigned.has(kid)) next.push(kid); });
    }
    if (layer.length) layers.push(layer); frontier = [...new Set(next)];
  }
  const remaining = nodes.filter(n => !assigned.has(n.id)).map(n => n.id); if (remaining.length) layers.push(remaining);
  return nodes.map(n => {
    const li = layers.findIndex(l => l.includes(n.id)); const layer = layers[li] || []; const row = layer.indexOf(n.id);
    return { ...n, positionX: BASE_X + (li >= 0 ? li : nodes.length) * LAYER_X_GAP, positionY: BASE_Y + (row >= 0 ? row : 0) * NODE_Y_GAP + (layer.length === 1 ? 50 : 0), sortOrder: (li >= 0 ? li : nodes.length) * 100 + (row >= 0 ? row : 0) };
  });
}

/* ========== component ========== */
const WorkflowManager: React.FC = () => {
  const [processes, setProcesses] = useState<ProcessDefinition[]>([]);
  const [selectedProcess, setSelectedProcess] = useState<ProcessDefinition | null>(null);
  const [nodes, setNodes] = useState<ProcessNode[]>([]);
  const [edges, setEdges] = useState<ProcessEdge[]>([]);
  const [editingNode, setEditingNode] = useState<ProcessNode | null>(null);
  const [processType, setProcessType] = useState('MILESTONE');
  const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false);
  const [canvasKey, setCanvasKey] = useState('');

  const [sourceNodeId, setSourceNodeId] = useState<number | null>(null);

  const isDragging = useRef(false);
  const dragStart = useRef<{ x: number; y: number; nodeId: number; origX: number; origY: number } | null>(null);
  const nodesSnapshot = useRef<ProcessNode[]>([]);
  useEffect(() => { nodesSnapshot.current = nodes; }, [nodes]);

  const [depts, setDepts] = useState<{ id: number; deptName: string; headUserId?: number }[]>([]);
  const [allUsers, setAllUsers] = useState<{ id: number; username: string }[]>([]);
  const [executors, setExecutors] = useState<Record<string, { username: string }[]>>({});

  useEffect(() => { loadProcesses(); loadRefData(); }, [processType]);

  const loadRefData = async () => {
    try {
      const [dr, ur] = await Promise.all([api.get('/api/departments'), api.get('/api/users')]);
      setDepts((dr.data as any).data || []); setAllUsers((ur.data as any).data || []);
    } catch {}
  };
  const loadExecutorsForDept = async (deptId: string) => {
    if (executors[deptId]) return;
    try { const r = await api.get('/api/users/by-role?role=DEPT_EXECUTOR'); setExecutors(prev => ({ ...prev, [deptId]: (r.data as any).data || [] })); } catch { setExecutors(prev => ({ ...prev, [deptId]: [] })); }
  };

  const loadProcesses = async () => {
    try { setLoading(true);
      console.log('========== [loadProcesses] START ==========');
      console.log('[loadProcesses] processType=', processType);
      const res = await api.get(`/api/workflow/by-type/${processType}`);
      const data = (res.data as any).data || [];
      console.log('[loadProcesses] 获取到', data.length, '个流程');
      data.forEach((p: ProcessDefinition) => {
        console.log('[loadProcesses]   流程 id=', p.id, ', milestoneCode=', p.milestoneCode, ', nodes=', (p.nodes || []).length, ', edges=', (p.edges || []).length);
        (p.nodes || []).forEach((n: ProcessNode) => {
          console.log('[loadProcesses]     node: id=', n.id, ', code=', n.nodeCode, ', name=', n.nodeName, ', type=', n.nodeType, ', approverRule=', n.approverRule, ', isUploader=', n.isUploader);
        });
        (p.edges || []).forEach((e: ProcessEdge) => {
          console.log('[loadProcesses]     edge: id=', e.id, ', ', e.fromNodeCode, '(', e.fromNodeId, ') -> ', e.toNodeCode, '(', e.toNodeId, ')');
        });
      });
      console.log('========== [loadProcesses] END ==========');
      setProcesses(data);
    } catch { toast.error('加载失败'); } finally { setLoading(false); }
  };

  const selectProcess = (p: ProcessDefinition) => {
    console.log('========== [selectProcess] START ==========');
    console.log('[selectProcess] 流程 id=', p.id, ', nodes=', (p.nodes || []).length, ', edges=', (p.edges || []).length);
    const normalized = (p.nodes || []).map(n => (!n.isUploader && n.approverRule === 'ROLE_COMPLIANCE') ? { ...n, approverRule: 'DEPT_HEAD', approverValue: '7' } : n);
    console.log('[selectProcess] 归一化后 nodes=', normalized.length);
    setCanvasKey(`cv-${p.id}-${Date.now()}`);
    setSelectedProcess(p); setNodes(normalized); setEdges(p.edges || []); setEditingNode(null); setSourceNodeId(null);
    console.log('========== [selectProcess] END ==========');
    normalized.filter(n => n.isUploader && n.approverValue).forEach(n => loadExecutorsForDept(n.approverValue));
  };

  /* ================== unconnected nodes computed value ================== */
  const unconnectedNodes = useMemo(() => {
    const conn = new Set<number>();
    for (const e of edges) { conn.add(e.fromNodeId); conn.add(e.toNodeId); }
    return nodes.filter(n => !conn.has(n.id));
  }, [nodes, edges]);

  /* ---- add / delete / auto-layout ---- */
  const addNode = () => {
    const maxX = nodes.length > 0 ? nodes.reduce((x, n) => Math.max(x, n.positionX ?? 0), 0) : 0;
    const id = -(Date.now() * 1000 + Math.floor(Math.random() * 1000));
    setNodes(prev => [...prev, {
      id, processDefinitionId: selectedProcess?.id || 0, nodeCode: `N_${Math.abs(id)}`,
      nodeName: '新节点', nodeType: 'DECISION', approverRule: 'ROLE_PM',
      approverValue: '', decisionType: getDecisionType(), isUploader: false,
      deliverableSlotCode: '', positionX: maxX + LAYER_X_GAP, positionY: BASE_Y + 80, sortOrder: prev.length + 1
    }]);
  };

  const deleteNode = (id: number) => {
    setNodes(p => p.filter(n => n.id !== id));
    setEdges(p => p.filter(e => e.fromNodeId !== id && e.toNodeId !== id));
    if (editingNode?.id === id) setEditingNode(null);
    setSourceNodeId(prev => prev === id ? null : prev);
    if (dragStart.current?.nodeId === id) { dragStart.current = null; isDragging.current = false; }
  };

  const deleteAllOrphanNodes = () => {
    const orphanIds = new Set(unconnectedNodes.map(n => n.id));
    setNodes(p => p.filter(n => !orphanIds.has(n.id)));
    setEdges(p => p.filter(e => !orphanIds.has(e.fromNodeId) && !orphanIds.has(e.toNodeId)));
    toast.success(`已删除 ${orphanIds.size} 个未连接节点`);
  };

  const runAutoLayout = () => {
    setNodes(prev => autoLayout(prev, edges));
    toast.success('自动布局完成');
  };

  const deleteEdge = (id: number) => setEdges(p => p.filter(e => e.id !== id));

  /* ---- socket-based connection ---- */
  const handleRightSocket = (e: React.MouseEvent, nodeId: number) => { e.stopPropagation(); setSourceNodeId(nodeId); };
  const handleLeftSocket = (e: React.MouseEvent, targetId: number) => {
    e.stopPropagation();
    if (sourceNodeId && sourceNodeId !== targetId) {
      const from = nodesSnapshot.current.find(n => n.id === sourceNodeId);
      const to = nodesSnapshot.current.find(n => n.id === targetId);
      if (from && to) {
        const dup = edges.some(ed => ed.fromNodeId === sourceNodeId && ed.toNodeId === targetId);
        if (!dup) {
          const eid = -(Date.now() * 1000 + Math.floor(Math.random() * 1000));
          setEdges(prev => [...prev, { id: eid, processDefinitionId: selectedProcess?.id || 0, fromNodeId: sourceNodeId, fromNodeCode: from.nodeCode, toNodeId: targetId, toNodeCode: to.nodeCode }]);
        }
      }
    }
    setSourceNodeId(null);
  };

  /* ---- drag to reposition ---- */
  const handleMouseDown = useCallback((e: React.MouseEvent, nodeId: number) => {
    e.stopPropagation(); e.preventDefault();
    const n = nodesSnapshot.current.find(x => x.id === nodeId);
    if (!n) { dragStart.current = null; isDragging.current = false; return; }
    dragStart.current = { x: e.clientX, y: e.clientY, nodeId, origX: n.positionX, origY: n.positionY };
    isDragging.current = false;
  }, []);
  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!dragStart.current) return;
    if (!isDragging.current && (Math.abs(e.clientX - dragStart.current.x) > 3 || Math.abs(e.clientY - dragStart.current.y) > 3)) isDragging.current = true;
    if (!isDragging.current) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    const { nodeId, origX, origY } = dragStart.current;
    setNodes(prev => prev.map(n => {
      if (n.id !== nodeId) return n;
      const nx = origX + dx; const ny = origY + dy;
      return { ...n, positionX: Math.max(0, Number.isFinite(nx) ? nx : origX), positionY: Math.max(0, Number.isFinite(ny) ? ny : origY) };
    }));
  }, []);
  const handleMouseUp = useCallback((e: React.MouseEvent) => {
    const moved = isDragging.current;
    if (!moved && dragStart.current && !e.ctrlKey) {
      const n = nodesSnapshot.current.find(x => x.id === dragStart.current!.nodeId);
      if (n) setEditingNode({ ...n });
    }
    dragStart.current = null; isDragging.current = false;
  }, []);

  /* ---- save (filter out orphan nodes AND edges before sending) ---- */
  const handleSave = async () => {
    if (!selectedProcess) return;
    try { setSaving(true);
      console.log('========== [handleSave] START ==========');
      console.log('[handleSave] selectedProcess.id=', selectedProcess.id);
      console.log('[handleSave] 当前 nodes 数量=', nodes.length);
      nodes.forEach(n => {
        console.log('[handleSave]   node: id=', n.id, ', code=', n.nodeCode, ', name=', n.nodeName, ', type=', n.nodeType, ', approverRule=', n.approverRule, ', approverValue=', n.approverValue, ', isUploader=', n.isUploader);
      });
      console.log('[handleSave] 当前 edges 数量=', edges.length);
      edges.forEach(e => {
        console.log('[handleSave]   edge: id=', e.id, ', ', e.fromNodeCode, '(', e.fromNodeId, ') -> ', e.toNodeCode, '(', e.toNodeId, ')');
      });

      // 第一步：构建已连接节点的集合
      const conn = new Set<number>();
      for (const e of edges) { conn.add(e.fromNodeId); conn.add(e.toNodeId); }
      console.log('[handleSave] conn 集合大小=', conn.size, ', 内容=', [...conn]);
      // 只保留在边中出现过的节点（已连接的节点）
      const cleanNodes = nodes.filter(n => conn.has(n.id));
      console.log('[handleSave] cleanNodes 数量=', cleanNodes.length);
      // 第二步：构建 cleanNodes 的 nodeCode 集合，用于过滤边
      const cleanNodeCodes = new Set(cleanNodes.map(n => n.nodeCode));
      console.log('[handleSave] cleanNodeCodes=', [...cleanNodeCodes]);
      // 只保留两端节点都在 cleanNodes 中的边
      const cleanEdges = edges.filter(e =>
        cleanNodeCodes.has(e.fromNodeCode) && cleanNodeCodes.has(e.toNodeCode)
      );
      console.log('[handleSave] cleanEdges 数量=', cleanEdges.length);

      const filteredNodeCount = nodes.length - cleanNodes.length;
      const filteredEdgeCount = edges.length - cleanEdges.length;
      if (filteredNodeCount > 0 || filteredEdgeCount > 0) {
        const parts: string[] = [];
        if (filteredNodeCount > 0) parts.push(`${filteredNodeCount} 个未连接节点`);
        if (filteredEdgeCount > 0) parts.push(`${filteredEdgeCount} 条无效边`);
        toast.info(`已自动过滤 ${parts.join('、')}`);
      }

      const payload = {
        description: selectedProcess.description, isActive: selectedProcess.isActive,
        nodes: cleanNodes.map(n => ({ nodeCode: n.nodeCode, nodeName: n.nodeName, nodeType: n.nodeType, approverRule: n.approverRule, approverValue: n.approverValue, decisionType: n.decisionType, isUploader: n.isUploader, deliverableSlotCode: n.deliverableSlotCode, positionX: n.positionX, positionY: n.positionY, sortOrder: n.sortOrder })),
        edges: cleanEdges.map(e => ({ fromNodeCode: e.fromNodeCode, toNodeCode: e.toNodeCode })),
      };
      console.log('[handleSave] 发送 payload.nodes 数量=', payload.nodes.length);
      console.log('[handleSave] 发送 payload.edges 数量=', payload.edges.length);
      console.log('[handleSave] 完整 payload=', JSON.stringify(payload, null, 2));

      await api.put(`/api/workflow/${selectedProcess.id}`, payload);
      console.log('[handleSave] API 调用成功');
      toast.success('流程保存成功');
      console.log('========== [handleSave] END (重新加载) ==========');
      await loadProcesses();
      setNodes([]); setEdges([]); setSelectedProcess(null); setEditingNode(null); setSourceNodeId(null);
    } catch (err) { console.error('[handleSave] 保存失败:', err); toast.error('保存失败'); } finally { setSaving(false); }
  };

  /* ---- helpers ---- */
  const getConnectedInCount = (id: number) => edges.filter(e => e.toNodeId === id).length;
  const isStart = (id: number) => edges.every(e => e.toNodeId !== id);
  const findDeptName = (deptId: string) => depts.find(d => String(d.id) === deptId)?.deptName || deptId;
  const getExecutors = (deptId: string) => { const u = executors[deptId]; return !u ? '加载中...' : u.length === 0 ? '无' : u.map(x => x.username).join(', '); };
  const getDecisionType = () => processType === 'MILESTONE' ? 'GO_NO_GO' : 'APPROVE_REJECT';
  const getDecisionLabel = () => processType === 'MILESTONE' ? 'Go / Conditional Go / No Go' : '同意 / 拒绝';

  const validXs = nodes.map(n => n.positionX).filter((x): x is number => Number.isFinite(x) && x >= 0);
  const validYs = nodes.map(n => n.positionY).filter((y): y is number => Number.isFinite(y) && y >= 0);
  const cW = Math.max(800, validXs.length > 0 ? Math.max(...validXs) : 0) + 200;
  const cH = Math.max(380, validYs.length > 0 ? Math.max(...validYs) : 0) + 150;

  const navigate = useNavigate();

  const confirmNodeEdit = () => {
    if (!editingNode) return;
    const correctDt = getDecisionType();
    const nodeToSave = editingNode.decisionType !== correctDt ? { ...editingNode, decisionType: correctDt } : editingNode;
    setNodes(prev => prev.map(n => n.id === nodeToSave.id ? nodeToSave : n));
    setEditingNode(null);
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white p-6">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={() => navigate('/dashboard')} className="bg-slate-800 text-slate-100 border-slate-600 hover:bg-slate-700">
            <ChevronLeft className="w-3 h-3 mr-1" />返回首页
          </Button>
          <h1 className="text-2xl font-bold">流程管理</h1>
          <select value={processType} onChange={e => { setProcessType(e.target.value); setSelectedProcess(null); setNodes([]); setEdges([]); }} className="bg-slate-800 border border-slate-600 text-slate-200 rounded px-3 py-1.5 text-sm">
            <option value="MILESTONE">里程碑审批</option><option value="CHANGE">项目变更</option><option value="TERMINATION">项目终止</option>
          </select>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        <Card className="bg-slate-800 border-slate-600 lg:col-span-1">
          <CardHeader><CardTitle className="text-slate-100 text-sm">流程列表</CardTitle></CardHeader>
          <CardContent>
            {loading ? <p className="text-slate-400 text-sm">加载中...</p> : (
              <div className="space-y-2 max-h-[60vh] overflow-y-auto">
                {processes.map(p => (
                  <div key={p.id} className={`p-2 rounded cursor-pointer text-sm ${selectedProcess?.id === p.id ? 'bg-blue-700' : 'bg-slate-700 hover:bg-slate-600'}`} onClick={() => selectProcess(p)}>
                    <div className="font-medium">{p.description || p.milestoneCode || p.processType}</div>
                    <div className="flex gap-2 mt-1 flex-wrap">
                      <Badge className="bg-slate-500 text-[10px]">{p.processType}</Badge>
                      {p.milestoneCode && <Badge className="bg-blue-500 text-[10px]">{p.milestoneCode}</Badge>}
                      <Badge className={p.isActive ? 'bg-green-600 text-[10px]' : 'bg-gray-600 text-[10px]'}>{p.isActive ? '启用' : '停用'}</Badge>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-slate-800 border-slate-600 lg:col-span-3">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-slate-100">{selectedProcess ? (selectedProcess.description || selectedProcess.milestoneCode || selectedProcess.processType) : '请选择流程'}</CardTitle>
            {selectedProcess && (
              <div className="flex gap-2 flex-wrap">
                <Button size="sm" onClick={addNode} className="bg-green-600 hover:bg-green-700"><Plus className="w-3 h-3 mr-1" />添加节点</Button>
                {unconnectedNodes.length > 0 && (
                  <Button size="sm" onClick={deleteAllOrphanNodes} className="bg-red-700 hover:bg-red-800"><Trash2 className="w-3 h-3 mr-1" />删除未连接({unconnectedNodes.length})</Button>
                )}
                <Button size="sm" onClick={runAutoLayout} className="bg-blue-600 hover:bg-blue-700"><GitBranch className="w-3 h-3 mr-1" />自动布局</Button>
                <Button size="sm" onClick={handleSave} disabled={saving} className="bg-blue-600 hover:bg-blue-700"><Save className="w-3 h-3 mr-1" />{saving ? '保存中...' : '保存'}</Button>
              </div>
            )}
          </CardHeader>
          <CardContent>
            {!selectedProcess ? (<p className="text-slate-400 text-center py-20">请从左侧列表选择一个流程</p>) : (
              <div className="overflow-auto" style={{ maxHeight: '62vh' }}>
                {unconnectedNodes.length > 0 && (
                  <div className="mb-2 p-2 bg-red-900/50 border border-red-700 rounded text-xs flex items-center gap-2">
                    <AlertCircle className="w-3 h-3 text-red-400" />
                    <span className="text-red-300">{unconnectedNodes.length} 个节点未连接：{unconnectedNodes.map(n => n.nodeName).join('、')}</span>
                    <span className="text-red-400">（断开节点不会触发审批，保存时自动过滤）</span>
                  </div>
                )}
                <div key={canvasKey || 'cv-empty'} className="relative rounded" style={{ width: cW, height: cH, minHeight: 380 }}
                  onMouseMove={handleMouseMove} onMouseUp={handleMouseUp}
                  onMouseLeave={() => { dragStart.current = null; isDragging.current = false; }}>
                  <svg className="absolute top-0 left-0 w-full h-full pointer-events-none" style={{ zIndex: 5, minHeight: cH }}>
                    <defs><marker id="arrow" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto"><polygon points="0 0, 10 3.5, 0 7" fill="#6b7280" /></marker></defs>
                    {edges.map((e, i) => {
                      const f = nodes.find(n => n.id === e.fromNodeId); const t = nodes.find(n => n.id === e.toNodeId);
                      if (!f || !t) return null;
                      return <line key={`e-${i}`} x1={f.positionX + 140} y1={f.positionY + 28} x2={t.positionX} y2={t.positionY + 28} stroke="#6b7280" strokeWidth="2" markerEnd="url(#arrow)" />;
                    })}
                  </svg>
                  {nodes.map((n, i) => (
                    <div key={`n-${i}`} className={`absolute w-[140px] select-none ${sourceNodeId === n.id ? 'ring-2 ring-blue-400 z-20' : 'z-10'}`} style={{ left: n.positionX, top: n.positionY }}>
                      <div className="absolute -left-2 top-6 w-4 h-4 rounded-full bg-green-500 border-2 border-green-300 cursor-crosshair hover:scale-125 transition" style={{ zIndex: 30 }} title="点击接收连线"
                        onMouseDown={e => { e.preventDefault(); e.stopPropagation(); handleLeftSocket(e, n.id); }} />
                      <div onMouseDown={e => handleMouseDown(e, n.id)}
                        className={`rounded-lg p-2 border-2 ml-1 min-h-[56px] ${NODE_COLORS[n.nodeType] || UNIFIED_NODE_COLOR} ${editingNode?.id === n.id ? 'border-yellow-400' : 'border-transparent'} cursor-move`}>
                        <div className="flex items-center justify-between mb-1">
                          <div className="flex gap-1 min-w-0"><GripVertical className="w-3 h-3 text-gray-400 flex-shrink-0" /><span className="text-[11px] font-bold text-white truncate">{n.nodeName}</span></div>
                          <button onMouseDown={e => e.stopPropagation()} onClick={e => { e.stopPropagation(); deleteNode(n.id); }} className="text-red-300 hover:text-red-100 flex-shrink-0 ml-1"><X className="w-3 h-3" /></button>
                        </div>
                        <Badge className="text-[10px] bg-black/30">{n.isUploader ? '上传' : '审批'}</Badge>
                        {isStart(n.id) && <Badge className="text-[10px] bg-green-500/50 ml-1">起始</Badge>}
                        {getConnectedInCount(n.id) > 1 && <span className="text-[9px] text-yellow-300 ml-1">←{getConnectedInCount(n.id)}汇</span>}
                        {edges.filter(e => e.fromNodeId === n.id).length > 1 && <span className="text-[9px] text-blue-300 ml-1">{edges.filter(e => e.fromNodeId === n.id).length}→</span>}
                        <div className="mt-1 space-y-0.5">
                          {edges.filter(e => e.fromNodeId === n.id).map(e => (<button key={e.id} onMouseDown={ev => ev.stopPropagation()} onClick={ev => { ev.stopPropagation(); deleteEdge(e.id); }} className="text-[9px] text-gray-400 hover:text-red-400 block w-full text-left truncate">✕ → {nodesSnapshot.current.find(x => x.id === e.toNodeId)?.nodeName || '?'}</button>))}
                        </div>
                      </div>
                      <div className={`absolute -right-1 top-6 w-4 h-4 rounded-full border-2 cursor-crosshair hover:scale-125 transition ${sourceNodeId === n.id ? 'bg-blue-500 border-blue-300 scale-125' : 'bg-blue-400 border-blue-200'}`} style={{ zIndex: 30 }} title="点击作为连线起点"
                        onMouseDown={e => { e.preventDefault(); e.stopPropagation(); handleRightSocket(e, n.id); }} />
                    </div>
                  ))}
                  {nodes.length === 0 && <div className="absolute inset-0 flex items-center justify-center text-slate-500 text-sm">点击"添加节点"添加 | 右键点 → 源 左绿点 → 目标创建连线 | 拖拽节点移动</div>}
                </div>
                {sourceNodeId && (
                  <div className="mt-2 p-1.5 bg-blue-900/60 border border-blue-600 rounded text-xs text-blue-300 flex items-center gap-2">
                    <span>🔗 已选择起点: {nodes.find(n => n.id === sourceNodeId)?.nodeName}，点击另一个节点的<Badge className="bg-green-600 text-[10px]">●</Badge>绿点完成连线</span>
                    <Button size="sm" variant="outline" className="border-blue-600 text-blue-300 h-5 text-[10px]" onClick={() => setSourceNodeId(null)}>取消</Button>
                  </div>
                )}
              </div>
            )}
            {editingNode && (
              <div className="fixed inset-0 bg-black/50 z-[200] flex items-center justify-center" onClick={confirmNodeEdit}>
                <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md border border-slate-600 max-h-[85vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
                  <h3 className="text-lg font-semibold mb-4">{editingNode.isUploader ? '上传节点信息' : '编辑审批节点'}</h3>
                  <label className="text-sm text-slate-400 block">节点名称</label>
                  <Input value={editingNode.nodeName} onChange={e => setEditingNode({ ...editingNode, nodeName: e.target.value })} className="bg-slate-700 border-slate-600 text-slate-100 text-sm mb-3" />
                  {editingNode.isUploader ? (
                    <div className="space-y-2 p-3 bg-slate-700/50 rounded border border-slate-600">
                      <div><label className="text-xs text-slate-500">上传部门</label><p className="text-sm">{findDeptName(editingNode.approverValue)}</p></div>
                      <div><label className="text-xs text-slate-500">部门执行人</label><p className="text-xs text-slate-300">{getExecutors(editingNode.approverValue)}</p></div>
                    </div>
                  ) : (
                    <>
                      <label className="text-sm text-slate-400 mt-2 block">审批人规则</label>
                      <select value={editingNode.approverRule} onChange={e => setEditingNode({ ...editingNode, approverRule: e.target.value, approverValue: e.target.value === 'ROLE_PM' ? 'PM' : '' })} className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2 text-sm mb-3">
                        <option value="DEPT_HEAD">部门负责人</option><option value="ROLE_PM">项目经理(PM)</option><option value="ROLE_PMC">PMC成员</option><option value="SPECIFIC_USER">指定用户</option>
                      </select>
                      <label className="text-sm text-slate-400 block">审批角色</label>
                      {editingNode.approverRule === 'DEPT_HEAD' && (
                        <select value={editingNode.approverValue} onChange={e => setEditingNode({ ...editingNode, approverValue: e.target.value })} className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2 text-sm mb-3">
                          <option value="">请选择部门</option>
                          {depts.map(d => <option key={d.id} value={String(d.id)}>{d.deptName} {d.headUserId ? '(✓负责人)' : '(未设)'}</option>)}
                        </select>
                      )}
                      {editingNode.approverRule === 'ROLE_PM' && <Input value="项目经理" disabled className="bg-slate-700 border-slate-600 text-slate-400 text-sm mb-3" />}
                      {editingNode.approverRule === 'ROLE_PMC' && (
                        <div className="mb-3 p-2 bg-slate-700 border border-slate-600 rounded text-[11px] text-slate-200">
                          📋 PMC成员在流程运行时会自动拉取所有项目管理委员会成员，进行并行审批，无需单独设置。
                        </div>
                      )}
                      {editingNode.approverRule === 'SPECIFIC_USER' && (
                        <select value={editingNode.approverValue} onChange={e => setEditingNode({ ...editingNode, approverValue: e.target.value })} className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2 text-sm mb-3">
                          <option value="">请选择用户</option>
                          {allUsers.map(u => <option key={u.id} value={String(u.id)}>{u.username}</option>)}
                        </select>
                      )}
                      <label className="text-sm text-slate-400 mt-2 block">决策类型</label>
                      <div className="w-full bg-slate-700 border border-slate-600 text-slate-300 rounded px-3 py-2 text-sm mb-1 select-none">{getDecisionLabel()}</div>
                      <p className="text-[10px] text-slate-500 mb-3">决策类型由流程类型自动确定，不可修改</p>
                    </>
                  )}
                  <div className="flex justify-end gap-2 mt-4">
                    <Button size="sm" variant="outline" onClick={() => setEditingNode(null)} className="border-slate-600 text-slate-300">取消</Button>
                    <Button size="sm" onClick={confirmNodeEdit} className="bg-blue-600 hover:bg-blue-700">确定</Button>
                  </div>
                </div>
              </div>
            )}
            <div className="mt-4 p-3 bg-slate-700 rounded text-xs text-slate-400 flex flex-col gap-1">
              <div><GitBranch className="w-3 h-3 inline mr-1" />点击节点 → 编辑属性 | 拖拽节点 → 移动 | <span className="text-blue-400">●右蓝点</span> 选择起点 → <span className="text-green-400">●左绿点</span> 连线 | 多出边=并行 | 保存时自动过滤未连接节点</div>
              <div className="text-slate-500">流程引擎已接入里程碑审批和变更/终止审批。修改流程后保存即可实时生效，实际审批将根据配置的审批节点自动生成审批任务。</div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default WorkflowManager;