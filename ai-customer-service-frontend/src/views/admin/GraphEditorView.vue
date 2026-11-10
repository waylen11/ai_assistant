<template>
  <div class="graph-editor-page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">流程编排</h2>
        <p class="page-sub">基于 SpringAIAlibabaGraph 可视化定义 AI 客服工作流</p>
        <div class="graph-name-input">
          <el-input
            v-model="currentGraphName"
            placeholder="输入流程名称"
            size="small"
            class="name-input"
            @input="markDirty"
          />
        </div>
      </div>
      <div class="header-actions">
        <el-button class="save-btn" :loading="saving" @click="saveGraph">
          <el-icon><Check /></el-icon> 保存流程
        </el-button>
      </div>
    </div>

    <div class="editor-container glass-card">
      <!-- 左侧节点面板 -->
      <div class="node-panel">
        <p class="panel-title">节点类型</p>
        <div
          v-for="nodeType in nodeTypes"
          :key="nodeType.type"
          class="node-template"
          :class="nodeType.type"
          draggable="true"
          @dragstart="handleDragStart(nodeType)"
        >
          <div class="node-template-icon">
            <el-icon><component :is="nodeType.icon" /></el-icon>
          </div>
          <div class="node-template-info">
            <span class="node-template-name">{{ nodeType.label }}</span>
            <span class="node-template-desc">{{ nodeType.desc }}</span>
          </div>
        </div>

        <div class="panel-divider" />

        <p class="panel-title">已保存流程</p>
        <div class="saved-graphs">
          <div
            v-for="graph in savedGraphs"
            :key="graph.id"
            class="saved-graph-item"
            :class="{ active: currentGraphId === graph.id }"
          >
            <div class="graph-info" @click="loadGraph(graph)">
              <el-icon><Share /></el-icon>
              <span>{{ graph.graphName }}</span>
            </div>
            <div class="graph-actions">
              <el-tag size="small" type="info" class="version-tag">v{{ graph.version }}</el-tag>
              <el-button
                size="small"
                circle
                class="delete-graph-btn"
                @click.stop="deleteGraph(graph.id, graph.graphName)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间画布区域 -->
      <div
        class="canvas-area"
        ref="canvasRef"
        @dragover.prevent
        @drop="handleDrop"
      >
        <!-- SVG 连线层 -->
        <svg class="edges-layer" :width="canvasSize.w" :height="canvasSize.h">
          <defs>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#6366F1" opacity="0.8" />
            </marker>
          </defs>
          <g v-for="edge in edges" :key="edge.id">
            <path
              :d="getEdgePath(edge)"
              stroke="#6366F1"
              stroke-width="2"
              stroke-opacity="0.6"
              fill="none"
              stroke-dasharray="6,3"
              marker-end="url(#arrowhead)"
            />
            <text
              v-if="edge.label"
              :x="getEdgeMidPoint(edge).x"
              :y="getEdgeMidPoint(edge).y"
              text-anchor="middle"
              fill="#6366F1"
              font-size="11"
              opacity="0.8"
            >{{ edge.label }}</text>
          </g>
        </svg>

        <!-- 节点层 -->
        <div
          v-for="node in nodes"
          :key="node.id"
          class="flow-node"
          :class="[node.type, { selected: selectedNodeId === node.id }]"
          :style="{ left: node.x + 'px', top: node.y + 'px' }"
          @mousedown="startDragNode($event, node)"
          @click.stop="selectNode(node)"
        >
          <div class="node-icon">
            <el-icon><component :is="getNodeIcon(node.type)" /></el-icon>
          </div>
          <div class="node-body">
            <span class="node-label">{{ node.label }}</span>
            <span class="node-type-label">{{ node.type }}</span>
          </div>
          <button class="node-delete" @click.stop="deleteNode(node.id)">×</button>
        </div>

        <!-- 画布提示（空状态） -->
        <div v-if="nodes.length === 0" class="canvas-empty">
          <el-icon size="40"><Share /></el-icon>
          <p>从左侧拖拽节点到画布，开始构建流程</p>
          <el-button class="load-default-btn" @click="loadDefaultGraph">
            加载默认客服流程
          </el-button>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="props-panel">
        <template v-if="selectedNode">
          <p class="panel-title">节点属性</p>
          <el-form label-width="70px" size="small">
            <el-form-item label="节点ID">
              <el-input :value="selectedNode.id" disabled />
            </el-form-item>
            <el-form-item label="节点名称">
              <el-input v-model="selectedNode.label" @input="markDirty" />
            </el-form-item>
            <el-form-item label="节点类型">
              <el-input :value="selectedNode.type" disabled />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="selectedNode.description" type="textarea" :rows="3" @input="markDirty" />
            </el-form-item>
          </el-form>

          <!-- 连线操作 -->
          <div class="connect-section">
            <p class="panel-title">添加连线</p>
            <el-form label-width="70px" size="small">
              <el-form-item label="目标节点">
                <el-select v-model="connectTarget" placeholder="选择目标节点">
                  <el-option
                    v-for="n in nodes.filter(n => n.id !== selectedNode.id)"
                    :key="n.id"
                    :label="n.label"
                    :value="n.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="条件标签">
                <el-input v-model="connectLabel" title = "如：knowledge / business" placeholder="如：knowledge / business" />
              </el-form-item>
              <el-button size="small" class="connect-btn" @click="addEdge">添加连线</el-button>
            </el-form>
          </div>
        </template>
        <div v-else class="props-empty">
          <el-icon size="32"><Edit /></el-icon>
          <p>点击节点查看属性</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

// 节点类型定义
const nodeTypes = [
  { type: 'intent_recognition', label: '意图识别', icon: 'MagicStick', desc: '分析用户意图' },
  { type: 'knowledge_retrieval', label: '知识检索', icon: 'Search', desc: 'RAG 语义检索' },
  { type: 'business_execution', label: '业务执行', icon: 'Operation', desc: '处理业务操作' },
  { type: 'reply_generation', label: '回复生成', icon: 'ChatDotRound', desc: '生成最终回复' },
  { type: 'condition', label: '条件判断', icon: 'Switch', desc: '条件路由分支' },
]

interface FlowNode {
  id: string; type: string; label: string; x: number; y: number;
  description?: string; [key: string]: any
}
interface FlowEdge {
  id: string; source: string; target: string; label?: string; condition?: string
}

const nodes = ref<FlowNode[]>([])
const edges = ref<FlowEdge[]>([])
const selectedNodeId = ref<string | null>(null)
const selectedNode = ref<FlowNode | null>(null)
const canvasRef = ref<HTMLElement>()
const canvasSize = reactive({ w: 800, h: 600 })
const saving = ref(false)
const isDirty = ref(false)
const savedGraphs = ref<any[]>([])
const currentGraphId = ref<number | null>(null)
const connectTarget = ref('')
const connectLabel = ref('')
const currentGraphName = ref('')
let draggingNodeType: any = null
let dragOffset = { x: 0, y: 0 }
let draggingNode: FlowNode | null = null

onMounted(() => {
  fetchSavedGraphs()
})

async function fetchSavedGraphs() {
  try {
    const res = await request.get('/admin/graph') as any
    savedGraphs.value = res.data || []
  } catch {}
}

async function loadDefaultGraph() {
  try {
    const res = await request.get('/admin/graph/default') as any
    const def = res.data
    nodes.value = (def.nodes || []).map((n: any) => ({ ...n, description: n.description || '' }))
    edges.value = def.edges || []
    selectedNodeId.value = null
    selectedNode.value = null
    ElMessage.success('已加载默认客服流程')
  } catch {
    ElMessage.error('加载失败')
  }
}

function loadGraph(graph: any) {
  try {
    const def = JSON.parse(graph.graphJson)
    nodes.value = def.nodes || []
    edges.value = def.edges || []
    currentGraphId.value = graph.id
    currentGraphName.value = graph.graphName
    ElMessage.success(`已加载：${graph.graphName}`)
  } catch {
    ElMessage.error('流程数据解析失败')
  }
}

async function saveGraph() {
  const graphJson = JSON.stringify({ nodes: nodes.value, edges: edges.value })
  const graphName = currentGraphName.value.trim() || '新建流程'
  saving.value = true
  try {
    if (currentGraphId.value) {
      await request.put(`/admin/graph/${currentGraphId.value}`, {
        graphJson,
        graphName,
      })
    } else {
      const res = await request.post('/admin/graph', {
        graphName, graphJson, graphType: 'customer_service',
      }) as any
      currentGraphId.value = res.data?.id
    }
    isDirty.value = false
    ElMessage.success('流程已保存')
    fetchSavedGraphs()
  } finally {
    saving.value = false
  }
}

function handleDragStart(nodeType: any) {
  draggingNodeType = nodeType
}

function handleDrop(e: DragEvent) {
  if (!draggingNodeType || !canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left - 60
  const y = e.clientY - rect.top - 30
  nodes.value.push({
    id: `${draggingNodeType.type}_${Date.now()}`,
    type: draggingNodeType.type,
    label: draggingNodeType.label,
    x: Math.max(0, x), y: Math.max(0, y),
    description: draggingNodeType.desc,
  })
  draggingNodeType = null
  markDirty()
}

function selectNode(node: FlowNode) {
  selectedNodeId.value = node.id
  selectedNode.value = node
}

function deleteNode(nodeId: string) {
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
  if (selectedNodeId.value === nodeId) {
    selectedNodeId.value = null
    selectedNode.value = null
  }
  markDirty()
}

function addEdge() {
  if (!selectedNode.value || !connectTarget.value) {
    ElMessage.warning('请选择目标节点')
    return
  }
  edges.value.push({
    id: `e_${Date.now()}`,
    source: selectedNode.value.id,
    target: connectTarget.value,
    label: connectLabel.value || '',
  })
  connectTarget.value = ''
  connectLabel.value = ''
  markDirty()
}

function startDragNode(e: MouseEvent, node: FlowNode) {
  draggingNode = node
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  dragOffset = { x: e.clientX - rect.left, y: e.clientY - rect.top }

  const onMove = (ev: MouseEvent) => {
    if (!draggingNode || !canvasRef.value) return
    const cr = canvasRef.value.getBoundingClientRect()
    draggingNode.x = Math.max(0, ev.clientX - cr.left - dragOffset.x)
    draggingNode.y = Math.max(0, ev.clientY - cr.top - dragOffset.y)
  }
  const onUp = () => {
    draggingNode = null
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    markDirty()
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function getEdgePath(edge: FlowEdge) {
  const src = nodes.value.find(n => n.id === edge.source)
  const tgt = nodes.value.find(n => n.id === edge.target)
  if (!src || !tgt) return ''
  const x1 = src.x + 90, y1 = src.y + 30
  const x2 = tgt.x, y2 = tgt.y + 30
  const mx = (x1 + x2) / 2
  return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`
}

function getEdgeMidPoint(edge: FlowEdge) {
  const src = nodes.value.find(n => n.id === edge.source)
  const tgt = nodes.value.find(n => n.id === edge.target)
  if (!src || !tgt) return { x: 0, y: 0 }
  return { x: (src.x + tgt.x) / 2 + 45, y: (src.y + tgt.y) / 2 + 24 }
}

function getNodeIcon(type: string) {
  return nodeTypes.find(n => n.type === type)?.icon || 'Operation'
}

function markDirty() { isDirty.value = true }

async function deleteGraph(graphId: number, graphName: string) {
  try {
    await ElMessageBox.confirm(
      `确定要删除流程 "${graphName}" 吗？`,
      '删除流程',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await request.delete(`/admin/graph/${graphId}`)
    ElMessage.success('流程已删除')
    await fetchSavedGraphs()
    if (currentGraphId.value === graphId) {
      currentGraphId.value = null
      nodes.value = []
      edges.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.graph-editor-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-shrink: 0;
}

.page-title { font-size: 20px; font-weight: 600; color: #F1F5F9; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: #64748B; margin-bottom: 8px; }

.graph-name-input {
  margin-top: 8px;
  max-width: 300px;
}

.name-input {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.07) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    box-shadow: none !important;
  }
  
  :deep(.el-input__inner) {
    color: #F1F5F9 !important;
  }
  
  :deep(.el-input__placeholder) {
    color: #475569 !important;
  }
}

.header-actions { display: flex; gap: 10px; }

.save-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6) !important;
  border: none !important;
  color: white !important;
  border-radius: 10px !important;
}

// 编辑器主体
.editor-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 0;
  gap: 0;
}

// 左侧节点面板
.node-panel {
  width: 190px;
  min-width: 190px;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  padding: 16px 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.panel-title {
  font-size: 11px;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
  margin: 4px 0 6px;
}

.node-template {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  cursor: grab;
  transition: all 0.2s;
  
  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(99, 102, 241, 0.3);
  }
  
  &:active { cursor: grabbing; }
}

.node-template-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: white;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  flex-shrink: 0;
}

.node-template-name {
  font-size: 12px;
  color: #CBD5E1;
  display: block;
  font-weight: 500;
}

.node-template-desc {
  font-size: 10px;
  color: #475569;
  display: block;
}

.panel-divider {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  margin: 4px 0;
}

.saved-graphs { display: flex; flex-direction: column; gap: 4px; }

.saved-graph-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 7px 8px;
  border-radius: 7px;
  font-size: 12px;
  color: #64748B;
  transition: all 0.2s;
  
  &:hover { background: rgba(255, 255, 255, 0.04); }
  &:hover .graph-info { color: #94A3B8; }
  &.active { background: rgba(99, 102, 241, 0.1); }
  &.active .graph-info { color: #818CF8; }
}

.graph-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  cursor: pointer;
  
  span { 
    flex: 1; 
    overflow: hidden; 
    text-overflow: ellipsis; 
    white-space: nowrap; 
  }
}

.graph-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.version-tag {
  font-size: 9px !important;
  padding: 0 4px !important;
  height: 16px !important;
  line-height: 16px !important;
  min-width: 28px !important;
  text-align: right !important;
}

.delete-graph-btn {
  color: #EF4444 !important;
  opacity: 0;
  transition: opacity 0.2s;
  
  .saved-graph-item:hover & {
    opacity: 1;
  }
  
  &:hover {
    background: rgba(239, 68, 68, 0.1) !important;
  }
}

// 画布区
.canvas-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: 
    radial-gradient(circle, rgba(99, 102, 241, 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  cursor: crosshair;
}

.edges-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: visible;
}

// 流程节点
.flow-node {
  position: absolute;
  width: 140px;
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: move;
  user-select: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  
  &:hover {
    border-color: rgba(99, 102, 241, 0.4);
    box-shadow: 0 4px 20px rgba(99, 102, 241, 0.15);
  }
  
  &.selected {
    border-color: #6366F1;
    box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.25);
  }
  
  &.intent_recognition .node-icon { background: rgba(99, 102, 241, 0.2); color: #818CF8; }
  &.knowledge_retrieval .node-icon { background: rgba(16, 185, 129, 0.15); color: #10B981; }
  &.business_execution .node-icon { background: rgba(245, 158, 11, 0.15); color: #F59E0B; }
  &.reply_generation .node-icon { background: rgba(59, 130, 246, 0.15); color: #3B82F6; }
  &.condition .node-icon { background: rgba(239, 68, 68, 0.12); color: #EF4444; }
}

.node-icon {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
}

.node-body { flex: 1; overflow: hidden; }
.node-label { font-size: 12px; color: #E2E8F0; font-weight: 500; display: block; }
.node-type-label { font-size: 10px; color: #475569; display: block; overflow: hidden; text-overflow: ellipsis; }

.node-delete {
  position: absolute;
  top: -7px;
  right: -7px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.8);
  color: white;
  border: none;
  font-size: 12px;
  cursor: pointer;
  display: none;
  align-items: center;
  justify-content: center;
  line-height: 1;
  
  .flow-node:hover & { display: flex; }
}

.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #334155;
  pointer-events: none;
  
  p { font-size: 13px; }
  
  .load-default-btn {
    pointer-events: all;
    background: rgba(99, 102, 241, 0.1) !important;
    border: 1px solid rgba(99, 102, 241, 0.2) !important;
    color: #818CF8 !important;
    border-radius: 20px !important;
  }
}

// 右侧属性面板
.props-panel {
  width: 220px;
  min-width: 220px;
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  padding: 16px 14px;
  overflow-y: auto;
  
  :deep(.el-form-item) { margin-bottom: 12px; }
  :deep(.el-form-item__label) { color: #64748B; font-size: 12px; }
}

.connect-section { margin-top: 16px; }

.connect-btn {
  width: 100%;
  background: rgba(99, 102, 241, 0.1) !important;
  border: 1px solid rgba(99, 102, 241, 0.2) !important;
  color: #818CF8 !important;
  border-radius: 8px !important;
}

.props-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #334155;
  gap: 8px;
  
  p { font-size: 12px; }
}
</style>
