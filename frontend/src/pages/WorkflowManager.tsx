import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import api from '../lib/api';
import { toast } from 'sonner';
import { Save, GitBranch, Plus, X, GripVertical, AlertCircle } from 'lucide-react';

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
const NODE_COLORS: Record<string, string> = { UPLOAD: 'bg-blue-600', DEPT_HEAD_APPROVE: 'bg-orange-600', ROLE_APPROVE: 'bg-purple-600', DECISION: 'bg-red-600' };
const LAYER_X_GAP = 160; const NODE_Y_GAP = 70; const BASE_X = 50; const BASE_Y = 30;

/* ========== auto-layout ========== */
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

  // socket mode: when user clicks a right-socket, sourceNodeId is set; clicking a left-socket connects it
  const [sourceNodeId, setSourceNodeId] = useState<number | null>(null);

  // drag states
  const isDragging = useRef(false);
  const dragStart = useRef<{ x: number; y: number; nodeId: number; origX: number; origY: number } | null>(null);

  // ref data
  const [depts, setDepts] = useState<{ id: number; deptName: string; headUserId?: number }[]>([]);
  const [allUsers, setAllUsers] = useState<{ id: number; username: string }[]>([]);
  const [pmcUsers, setPmcUsers] = useState<{ id: number; username: string }[]>([]);
  const [executors, setExecutors] = useState<Record<string, { username: string }[]>>({});

  useEffect(() => { loadProcesses(); loadRefData(); }, [processType]);

  const loadRefData = async () => {
    try {
      const [dr, ur, pr] = await Promise.all([api.get('/api/departments'), api.get('/api/users'), api.get('/api/users/by-role?role=ROLE_PMC')]);
      setDepts((dr.data as any).data || []); setAllUsers((ur.data as any).data || []); setPmcUsers((pr.data as any).data || []);
    } catch {}
  };
  const loadExecutorsForDept = async (deptId: string) => {
    if (executors[deptId]) return;
    try { const r = await api.get('/api/users/by-role?role=DEPT_EXECUTOR'); setExecutors(prev => ({ ...prev, [deptId]: (r.data as any).data || [] })); } catch { setExecutors(prev => ({ ...prev, [deptId]: [] })); }
  };

  const loadProcesses = async () => {
    try { setLoading(true); const res = await api.get(`/api/workflow/by-type/${processType}`); setProcesses((res.data as any).data || []); } catch { toast.error('加载失败'); } finally { setLoading(false); }
  };

  const selectProcess = (p: ProcessDefinition) => {
    // normalize legacy data
    const normalized = (p.nodes || []).map(n => (!n.isUploader && n.approverRule === 'ROLE_COMPLIANCE') ? { ...n, approverRule: 'DEPT_HEAD', approverValue: '7' } : n);
    // auto-layout
    const rawEdges = p.edges || [];
    const laidOut = autoLayout(normalized, rawEdges);
    setCanvasKey(`cv-${p.id}-${Date.now()}`);
    setSelectedProcess(p); setNodes(laidOut); setEdges(rawEdges); setEditingNode(null); setSourceNodeId(null);
    laidOut.filter(n => n.isUploader && n.approverValue).forEach(n => loadExecutorsForDept(n.approverValue));
  };

  /* ---- add / delete ---- */
  const addNode = () => {
    const maxX = nodes.reduce((x, n) => Math.max(x, n.positionX), 0);
    const id = -Date.now() - Math.random();
    setNodes(prev => [...prev, { id, processDefinitionId: selectedProcess?.id || 0, nodeCode: `N_${Math.abs(id)}`, nodeName: '新节点', nodeType: 'DECISION', approverRule: 'ROLE_PM', approverValue: '', decisionType: processType === 'MILESTONE' ? 'GO_NO_GO' : 'APPROVE_REJECT', isUploader: false, deliverableSlotCode: '', positionX: maxX + LAYER_X_GAP, positionY: BASE_Y + 80, sortOrder: prev.length + 1 }]);
  };
  const deleteNode = (id: number) => { setNodes(p => p.filter(n => n.id !== id)); setEdges(p => p.filter(e => e.fromNodeId !== id && e.toNodeId !== id)); if (editingNode?.id === id) setEditingNode(null); setSourceNodeId(prev => prev === id ? null : prev); };
  const deleteEdge = (id: number) => setEdges(p => p.filter(e => e.id !== id));

  /* ---- socket-based connection ---- */
  const handleRightSocket = (e: React.MouseEvent, nodeId: number) => { e.stopPropagation(); setSourceNodeId(nodeId); };
  const handleLeftSocket = (e: React.MouseEvent, targetId: number) => {
    e.stopPropagation();
    if (sourceNodeId && sourceNodeId !== targetId) {
      const from = nodes.find(n => n.id === sourceNodeId); const to = nodes.find(n => n.id === targetId);
      if (from && to) {
        const dup = edges.some(ed => ed.fromNodeId === sourceNodeId && ed.toNodeId === targetId);
        if (!dup) setEdges(prev => [...prev, { id: -Date.now() - Math.random(), processDefinitionId: selectedProcess?.id || 0, fromNodeId: sourceNodeId, fromNodeCode: from.nodeCode, toNodeId: targetId, toNodeCode: to.nodeCode }]);
      }
    }
    setSourceNodeId(null);
  };

  /* ---- drag to reposition ---- */
  const handleMouseDown = (e: React.MouseEvent, nodeId: number) => { e.stopPropagation(); e.preventDefault(); const n = nodes.find(x => x.id === nodeId); if (!n) return; dragStart.current = { x: e.clientX, y: e.clientY, nodeId, origX: n.positionX, origY: n.positionY }; isDragging.current = false; };
  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!dragStart.current) return;
    if (Math.abs(e.clientX - dragStart.current.x) > 3 || Math.abs(e.clientY - dragStart.current.y) > 3) isDragging.current = true;
    if (!isDragging.current) return;
    setNodes(prev => prev.map(n => n.id === dragStart.current!.nodeId ? { ...n, positionX: dragStart.current!.origX + e.clientX - dragStart.current!.x, positionY: dragStart.current!.origY + e.clientY - dragStart.current!.y } : n));
  }, []);
  const handleMouseUp = useCallback((e: React.MouseEvent) => {
    const moved = isDragging.current;
    if (!moved && dragStart.current && !e.ctrlKey) { const n = nodes.find(x => x.id === dragStart.current!.nodeId); if (n) setEditingNode({ ...n }); }
    dragStart.current = null; isDragging.current = false;
  }, [nodes]);

  /* ---- save ---- */
  const handleSave = async () => {
    if (!selectedProcess) return;
    try { setSaving(true);
      await api.put(`/api/workflow/${selectedProcess.id}`, {
        description: selectedProcess.description, isActive: selectedProcess.isActive,
        nodes: nodes.map(n => ({ nodeCode: n.nodeCode, nodeName: n.nodeName, nodeType: n.nodeType, approverRule: n.approverRule, approverValue: n.approverValue, decisionType: n.decisionType, isUploader: n.isUploader, deliverableSlotCode: n.deliverableSlotCode, positionX: n.positionX, positionY: n.positionY, sortOrder: n.sortOrder })),
        edges: edges.map(e => ({ fromNodeCode: e.fromNodeCode, toNodeCode: e.toNodeCode })),
      });
      toast.success('流程保存成功'); loadProcesses();
    } catch { toast.error('保存失败'); } finally { setSaving(false); }
  };

  /* ---- helpers ---- */
  const getConnectedInCount = (id: number) => edges.filter(e => e.toNodeId === id).length;
  const isStart = (id: number) => edges.every(e => e.toNodeId !== id);
  const findDeptName = (deptId: string) => depts.find(d => String(d.id) === deptId)?.deptName || deptId;
  const getExecutors = (deptId: string) => { const u = executors[deptId]; return !u ? '加载中...' : u.length === 0 ? '无' : u.map(x => x.username).join(', '); };
  const getDefaultDecisionType = () => processType === 'MILESTONE' ? 'GO_NO_GO' : 'APPROVE_REJECT';
  // unconnected nodes check
  const unconnectedNodes = nodes.filter(n => edges.every(e => e.fromNodeId !== n.id && e.toNodeId !== n.id));

  const cW = Math.max(800, ...nodes.map(n => n.positionX)) + 200;
  const cH = Math.max(380, ...nodes.map(n => n.positionY)) + 150;

  return (
    <div className="min-h-screen bg-slate-900 text-white p-6">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">流程管理</h1>
          <select value={processType} onChange={e => { setProcessType(e.target.value); setSelectedProcess(null); setNodes([]); setEdges([]); }}
            className="bg-slate-800 border border-slate-600 text-slate-200 rounded px-3 py-1.5 text-sm">
            <option value="MILESTONE">里程碑审批</option><option value="CHANGE">项目变更</option><option value="TERMINATION">项目终止</option>
          </select>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* process list */}
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

        {/* canvas */}
        <Card className="bg-slate-800 border-slate-600 lg:col-span-3">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-slate-100">{selectedProcess ? (selectedProcess.description || selectedProcess.milestoneCode || selectedProcess.processType) : '请选择流程'}</CardTitle>
            {selectedProcess && (
              <div className="flex gap-2">
                <Button size="sm" onClick={addNode} className="bg-green-600 hover:bg-green-700"><Plus className="w-3 h-3 mr-1" />添加节点</Button>
                <Button size="sm" onClick={handleSave} disabled={saving} className="bg-blue-600 hover:bg-blue-700"><Save className="w-3 h-3 mr-1" />{saving ? '保存中...' : '保存'}</Button>
              </div>
            )}
          </CardHeader>
          <CardContent>
            {!selectedProcess ? (<p className="text-slate-400 text-center py-20">请从左侧列表选择一个流程</p>) : (
              <div className="overflow-auto" style={{ maxHeight: '62vh' }}>
                {/* unconnected warning */}
                {unconnectedNodes.length > 0 && (
                  <div className="mb-2 p-2 bg-red-900/50 border border-red-700 rounded text-xs flex items-center gap-2">
                    <AlertCircle className="w-3 h-3 text-red-400" />
                    <span className="text-red-300">{unconnectedNodes.length} 个节点未连接：{unconnectedNodes.map(n => n.nodeName).join('、')}</span>
                    <span className="text-red-400">（断开节点不会触发审批）</span>
                  </div>
                )}
                <div key={canvasKey || 'cv-empty'} className="relative rounded" style={{ width: cW, height: cH, minHeight: 380 }}
                  onMouseMove={handleMouseMove} onMouseUp={handleMouseUp} onMouseLeave={() => { dragStart.current = null; isDragging.current = false; }}>
                  {/* SVG edges */}
                  <svg className="absolute top-0 left-0 w-full h-full pointer-events-none" style={{ zIndex: 5, minHeight: cH }}>
                    <defs><marker id="arrow" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto"><polygon points="0 0, 10 3.5, 0 7" fill="#6b7280" /></marker></defs>
                    {edges.map((e, i) => {
                      const f = nodes.find(n => n.id === e.fromNodeId); const t = nodes.find(n => n.id === e.toNodeId);
                      if (!f || !t) return null;
                      return <line key={`e-${i}`} x1={f.positionX + 140} y1={f.positionY + 28} x2={t.positionX} y2={t.positionY + 28} stroke="#6b7280" strokeWidth="2" markerEnd="url(#arrow)" />;
                    })}
                  </svg>
                  {/* nodes — rendered AFTER svg so they paint on top */}
                  {nodes.map((n, i) => (
                    <div key={`n-${i}`} className={`absolute w-[140px] select-none ${sourceNodeId === n.id ? 'ring-2 ring-blue-400 z-20' : 'z-10'}`}
                      style={{ left: n.positionX, top: n.positionY }}>
                      {/* LEFT socket (target) */}
                      <div className="absolute -left-2 top-6 w-4 h-4 rounded-full bg-green-500 border-2 border-green-300 cursor-crosshair hover:scale-125 transition"
                        style={{ zIndex: 30 }}
                        title="点击接收连线"
                        onMouseDown={e => { e.preventDefault(); e.stopPropagation(); handleLeftSocket(e, n.id); }} />
                      {/* node body */}
                      <div onMouseDown={e => handleMouseDown(e, n.id)}
                        className={`rounded-lg p-2 border-2 ml-1 ${NODE_COLORS[n.nodeType] || 'bg-gray-600'} ${editingNode?.id === n.id ? 'border-yellow-400' : 'border-transparent'} cursor-move`}>
                        <div className="flex items-center justify-between mb-1">
                          <div className="flex gap-1 min-w-0"><GripVertical className="w-3 h-3 text-gray-400 flex-shrink-0" /><span className="text-[11px] font-bold text-white truncate">{n.nodeName}</span></div>
                          <button onClick={e => { e.stopPropagation(); deleteNode(n.id); }} className="text-red-300 hover:text-red-100 flex-shrink-0 ml-1"><X className="w-3 h-3" /></button>
                        </div>
                        <Badge className="text-[10px] bg-black/30">{n.isUploader ? '上传' : '审批'}</Badge>
                        {isStart(n.id) && <Badge className="text-[10px] bg-green-500/50 ml-1">起始</Badge>}
                        {getConnectedInCount(n.id) > 1 && <span className="text-[9px] text-yellow-300 ml-1">←{getConnectedInCount(n.id)}汇</span>}
                        {edges.filter(e => e.fromNodeId === n.id).length > 1 && <span className="text-[9px] text-blue-300 ml-1">{edges.filter(e => e.fromNodeId === n.id).length}→</span>}
                        <div className="mt-1 space-y-0.5">
                          {edges.filter(e => e.fromNodeId === n.id).map(e => (<button key={e.id} onClick={ev => { ev.stopPropagation(); deleteEdge(e.id); }} className="text-[9px] text-gray-400 hover:text-red-400 block w-full text-left truncate">✕ → {nodes.find(x => x.id === e.toNodeId)?.nodeName || '?'}</button>))}
                        </div>
                      </div>
                      {/* RIGHT socket (source) */}
                      <div className={`absolute -right-1 top-6 w-4 h-4 rounded-full border-2 cursor-crosshair hover:scale-125 transition ${sourceNodeId === n.id ? 'bg-blue-500 border-blue-300 scale-125' : 'bg-blue-400 border-blue-200'}`}
                        style={{ zIndex: 30 }}
                        title="点击作为连线起点"
                        onMouseDown={e => { e.preventDefault(); e.stopPropagation(); handleRightSocket(e, n.id); }} />
                    </div>
                  ))}
                  {nodes.length === 0 && <div className="absolute inset-0 flex items-center justify-center text-slate-500 text-sm">点击"添加节点"添加 | 右键点→源 左绿点→目标创建连线 | 拖拽节点移动</div>}
                </div>

                {/* socket mode indicator */}
                {sourceNodeId && (
                  <div className="mt-2 p-1.5 bg-blue-900/60 border border-blue-600 rounded text-xs text-blue-300 flex items-center gap-2">
                    <span>🔗 已选择起点: {nodes.find(n => n.id === sourceNodeId)?.nodeName}，点击另一个节点的<Badge className="bg-green-600 text-[10px]">●</Badge>绿点完成连线</span>
                    <Button size="sm" variant="outline" className="border-blue-600 text-blue-300 h-5 text-[10px]" onClick={() => setSourceNodeId(null)}>取消</Button>
                  </div>
                )}
              </div>
            )}

            {/* ===== editor modal ===== */}
            {editingNode && (
              <div className="fixed inset-0 bg-black/50 z-[200] flex items-center justify-center" onClick={() => setEditingNode(null)}>
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
                      <select value={editingNode.approverRule} onChange={e => setEditingNode({ ...editingNode, approverRule: e.target.value, approverValue: e.target.value === 'ROLE_PM' ? 'PM' : '' })}
                        className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2 text-sm mb-3">
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
                      {editingNode.approverRule === 'ROLE_PMC' && (<div className="mb-3 p-2 bg-yellow-900/30 rounded text-[10px] text-yellow-400">⚠ PMC成员在流程运行时会自动拉取，无需单独设置。请创建并行分支节点</div>)}
                      {editingNode.approverRule === 'SPECIFIC_USER' && (
                        <select value={editingNode.approverValue} onChange={e => setEditingNode({ ...editingNode, approverValue: e.target.value })} className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2 text-sm mb-3">
                          <option value="">请选择用户</option>
                          {allUsers.map(u => <option key={u.id} value={String(u.id)}>{u.username}</option>)}
                        </select>
                      )}
                      <label className="text-sm text-slate-400 mt-2 block">决策类型</label>
                      <select value={editingNode.decisionType || getDefaultDecisionType()} onChange={e => setEditingNode({ ...editingNode, decisionType: e.target.value })}
                        className="w-full bg-slate-700 border border-slate-600 text-slate-100 rounded px-3 py-2 text-sm mb-3">
                        <option value="GO_NO_GO">Go / Conditional Go / No Go</option>
                        <option value="APPROVE_REJECT">同意 / 拒绝</option>
                      </select>
                    </>
                  )}
                  <div className="flex justify-end gap-2 mt-4"><Button size="sm" variant="outline" onClick={() => setEditingNode(null)} className="border-slate-600 text-slate-300">关闭</Button></div>
                </div>
              </div>
            )}

            <div className="mt-4 p-3 bg-slate-700 rounded text-xs text-slate-400 flex flex-col gap-1">
              <div><GitBranch className="w-3 h-3 inline mr-1" />点击节点 → 编辑属性 | 拖拽节点 → 移动 | <span className="text-blue-400">●右蓝点</span> 选择起点 → <span className="text-green-400">●左绿点</span> 连线 | 多出边=并行</div>
              <div className="text-slate-500">⚠ 流程目前用于可视化配置。实际审批流程仍由里程碑控制台触发（`ReviewService`），未来将通过流程引擎驱动审批。</div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default WorkflowManager;