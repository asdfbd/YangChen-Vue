<script lang="ts" setup>
import type {TableColumnType} from 'ant-design-vue';
import {
  Button,
  Card,
  DatePicker,
  Drawer,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Tag,
} from 'ant-design-vue';

import type {SysOperLog} from '#/api/monitor/log';
import {cleanOperLogApi, deleteOperLogApi, listOperLogApi,} from '#/api/monitor/log';

import {computed, onMounted, reactive, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {useAccessStore} from '@vben/stores';
import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';
import {downloadBlob} from '#/utils/download';

type TableColumn = TableColumnType & { resizable?: boolean; visible?: boolean };
type TableKey = number | string;

defineOptions({name: 'MonitorOperlog'});

const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const businessTypeOptions = [
  {label: '其它', value: 0},
  {label: '新增', value: 1},
  {label: '修改', value: 2},
  {label: '删除', value: 3},
  {label: '授权', value: 4},
  {label: '导出', value: 5},
  {label: '导入', value: 6},
  {label: '强退', value: 7},
  {label: '生成代码', value: 8},
  {label: '清空数据', value: 9},
];

const statusOptions = [
  {label: '成功', value: '0'},
  {label: '失败', value: '1'},
];

const loading = ref(false);
const showSearch = ref(true);
const selectedRowKeys = ref<TableKey[]>([]);
const total = ref(0);
const tableData = ref<SysOperLog[]>([]);
const detailVisible = ref(false);
const currentLog = ref<SysOperLog>();

const query = reactive({
  beginTime: '',
  businessType: undefined as number | undefined,
  endTime: '',
  operIp: '',
  operName: '',
  pageNum: 1,
  pageSize: 10,
  status: undefined as string | undefined,
  title: '',
});
const columns = ref<TableColumn[]>([
  {dataIndex: 'title', key: 'title', width: 140, ellipsis: true, title: '系统模块', visible: true},
  {dataIndex: 'businessType', key: 'businessType', width: 110, title: '操作类型', visible: true},
  {dataIndex: 'operName', key: 'operName', width: 120, ellipsis: true, title: '操作人员', visible: true},
  {dataIndex: 'operIp', key: 'operIp', width: 130, title: '主机地址', visible: true},
  {dataIndex: 'operLocation', key: 'operLocation', width: 140, ellipsis: true, title: '操作地点', visible: true},
  {dataIndex: 'status', key: 'status', width: 90, title: '操作状态', visible: true},
  {dataIndex: 'operTime', key: 'operTime', width: 200, title: '操作时间', visible: true},
  {dataIndex: 'costTime', key: 'costTime', width: 100, title: '消耗时间', visible: true},
  {key: 'operation', resizable: false, width: 96, title: '操作', visible: true},
]);

const visibleColumns = computed(() => {
  const out: TableColumn[] = [];
  for (const col of columns.value) {
    if (col.visible !== false) out.push(col);
  }
  return out;
});


function buildQuery() {
  return {
    beginTime: query.beginTime || undefined,
    businessType: query.businessType,
    endTime: query.endTime || undefined,
    operIp: query.operIp || undefined,
    operName: query.operName || undefined,
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    status: query.status,
    title: query.title || undefined,
  };
}

function getBusinessTypeLabel(value?: number | string) {
  return businessTypeOptions.find((item) => item.value === Number(value))?.label ?? '其它';
}

function formatDateTime(value?: string) {
  return value || '-';
}

function handleDateRangeChange(_dates: unknown, dateStrings: [string, string]) {
  [query.beginTime, query.endTime] = dateStrings;
}

async function loadData() {
  loading.value = true;
  try {
    const response = await listOperLogApi(buildQuery());
    tableData.value = response.rows ?? [];
    total.value = response.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  Object.assign(query, {
    beginTime: '',
    businessType: undefined,
    endTime: '',
    operIp: '',
    operName: '',
    pageNum: 1,
    pageSize: 10,
    status: undefined,
    title: '',
  });
  loadData();
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? query.pageSize;
  loadData();
}

function handleSelectionChange(keys: TableKey[]) {
  selectedRowKeys.value = keys;
}

function openDetail(record: SysOperLog) {
  currentLog.value = record;
  detailVisible.value = true;
}

function handleDelete(record?: SysOperLog) {
  const ids = record?.operId ? [record.operId] : selectedRowKeys.value;
  if (ids.length === 0) {
    message.warning('请至少选择一条操作日志');
    return;
  }

  Modal.confirm({
    content: '删除后不可恢复，确定要删除选中的操作日志吗？',
    okText: '确定',
    title: '删除操作日志',
    async onOk() {
      await deleteOperLogApi(ids);
      message.success('删除成功');
      selectedRowKeys.value = [];
      await loadData();
    },
  });
}

function handleClean() {
  Modal.confirm({
    content: '清空后无法恢复，确定清空全部操作日志吗？',
    okText: '确定',
    title: '清空操作日志',
    async onOk() {
      await cleanOperLogApi();
      message.success('清空成功');
      selectedRowKeys.value = [];
      await loadData();
    },
  });
}

async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `operlog_${Date.now()}.xlsx`,
      path: '/monitor/operlog/export'
    });
    message.success('导出成功');
  } catch {
    message.error('导出失败，请稍后重试');
  }
}

/** JSON 美化：请求/返回参数通常是 JSON 字符串，格式化后更易读 */
function formatJson(str?: string): string {
  const value = String(str ?? '');
  if (value.trim() === '') return '（无数据）';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

async function copyText(str?: string) {
  const text = formatJson(str);
  try {
    await navigator.clipboard.writeText(text);
    message.success('已复制');
  } catch {
    message.error('复制失败，请手动选择复制');
  }
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height>
    <div class="operlog-page">
      <Card :bordered="false" class="operlog-card">
        <section v-show="showSearch" class="search-panel">
          <Form layout="inline" @submit.prevent="handleSearch">
            <Form.Item label="系统模块">
              <Input
                v-model:value="query.title" allow-clear placeholder="请输入系统模块"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="操作人员">
              <Input
                v-model:value="query.operName" allow-clear placeholder="请输入操作人员"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="主机地址">
              <Input
                v-model:value="query.operIp" allow-clear placeholder="请输入主机地址"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="操作类型">
              <Select
                v-model:value="query.businessType" :options="businessTypeOptions" allow-clear
                placeholder="请选择操作类型"
              />
            </Form.Item>
            <Form.Item label="操作状态">
              <Select
                v-model:value="query.status" :options="statusOptions" allow-clear
                placeholder="请选择操作状态"
              />
            </Form.Item>
            <Form.Item label="操作时间">
              <DatePicker.RangePicker show-time @change="handleDateRangeChange"/>
            </Form.Item>
            <Form.Item>
              <Space :size="8">
                <Button type="primary" @click="handleSearch">
                  <IconifyIcon icon="ant-design:search-outlined"/>
                  搜索
                </Button>
                <Button @click="handleReset">
                  <IconifyIcon icon="ant-design:reload-outlined"/>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </section>

        <section class="toolbar">
          <Space :size="10" wrap>
            <Button
              v-if="hasPermi('monitor:operlog:remove')" :disabled="!selectedRowKeys.length"
              danger @click="handleDelete()"
            >
              <IconifyIcon icon="ant-design:delete-outlined"/>
              删除
            </Button>
            <Button v-if="hasPermi('monitor:operlog:remove')" @click="handleClean">
              <IconifyIcon icon="ant-design:clear-outlined"/>
              清空
            </Button>
            <Button v-if="hasPermi('monitor:operlog:export')" @click="handleExport">
              <IconifyIcon icon="ant-design:export-outlined"/>
              导出
            </Button>
          </Space>
          <Space :size="8">
            <Button size="small" @click="showSearch = !showSearch">
              {{ showSearch ? '隐藏搜索' : '显示搜索' }}
            </Button>
            <Button size="small" @click="loadData">
              <IconifyIcon icon="ant-design:reload-outlined"/>
              刷新
            </Button>
            <ColumnSetting
              :columns="columns"
              :filter="(c) => c.key !== 'operation'"
              storage-key="monitor-operlog-columns"
            />
          </Space>
        </section>

        <ResizableTable
          :columns="visibleColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="{
            current: query.pageNum,
            pageSize: query.pageSize,
            showSizeChanger: true,
            showTotal: (value: number) => `共 ${value} 条`,
            total,
          }"
          :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"

          row-key="operId"
          size="middle"
          storage-key="monitor-operlog-columns"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'businessType'">
              {{ getBusinessTypeLabel(record.businessType) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status == '0' ? 'success' : 'error'">
                {{ record.status == '0' ? '成功' : '失败' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'operTime'">
              {{ formatDateTime(record.operTime) }}
            </template>
            <template v-else-if="column.key === 'costTime'">
              {{ record.costTime ?? 0 }} ms
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="4">
                <Button size="small" type="link" @click="openDetail(record)">详情</Button>
                <Button
                  v-if="hasPermi('monitor:operlog:remove')" danger size="small" type="link"
                  @click="handleDelete(record)"
                >
                  删除
                </Button>
              </Space>
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>

    <Drawer v-model:open="detailVisible" :width="780" title="操作日志详情">
      <div v-if="currentLog" class="detail-wrap">
        <!-- 基本信息 -->
        <section class="detail-card">
          <div class="detail-card-title">
            <IconifyIcon class="title-icon" icon="lucide:info"/>
            基本信息
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">操作模块</span><span
              class="detail-value"
            >{{ currentLog.title || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">业务类型</span><span
              class="detail-value"
            >{{ getBusinessTypeLabel(currentLog.businessType) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作时间</span><span
              class="detail-value"
            >{{ formatDateTime(currentLog.operTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">执行状态</span>
              <span class="detail-value">
                <Tag :color="currentLog.status === '0' ? 'success' : 'error'">
                  <IconifyIcon
                    :icon="currentLog.status === '0' ? 'lucide:check' : 'lucide:x'"
                    class="tag-icon"
                  />
                  {{ currentLog.status === '0' ? '正常' : '异常' }}
                </Tag>
              </span>
            </div>
          </div>
        </section>

        <!-- 操作人员 -->
        <section class="detail-card">
          <div class="detail-card-title">
            <IconifyIcon class="title-icon" icon="lucide:user"/>
            操作人员
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">操作人员</span><span
              class="detail-value"
            >{{ currentLog.operName || '-' }}</span>
            </div>
            <div v-if="currentLog.deptName" class="detail-item">
<span
  class="detail-label"
>所属部门</span><span class="detail-value">{{
                currentLog.deptName
              }}</span>
            </div>
            <div class="detail-item detail-item__full">
              <span class="detail-label">操作地址</span>
              <span class="detail-value">
                {{ currentLog.operIp || '-' }}<span
                v-if="currentLog.operLocation"
                class="detail-location"
              >（{{
                  currentLog.operLocation
                }}）</span>
              </span>
            </div>
          </div>
        </section>

        <!-- 请求信息 -->
        <section class="detail-card">
          <div class="detail-card-title">
            <IconifyIcon class="title-icon" icon="lucide:send"/>
            请求信息
          </div>
          <div class="detail-grid">
            <div class="detail-item detail-item__full">
              <span class="detail-label">请求地址</span>
              <span class="detail-value">
                <span
                  v-if="currentLog.requestMethod"
                  :class="`method-tag method-${currentLog.requestMethod.toUpperCase()}`"
                >{{
                    currentLog.requestMethod.toUpperCase()
                  }}</span>
                {{ currentLog.operUrl || '-' }}
              </span>
            </div>
            <div class="detail-item detail-item__full">
              <span class="detail-label">操作方法</span><span
              class="detail-value mono"
            >{{ currentLog.method || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">消耗时间</span><span
              class="detail-value"
            >{{ currentLog.costTime ?? 0 }} 毫秒</span>
            </div>
          </div>
        </section>

        <!-- 请求参数 -->
        <section class="detail-card">
          <div class="detail-card-title">
            <IconifyIcon class="title-icon" icon="lucide:upload"/>
            请求参数
          </div>
          <div class="code-body">
            <div class="code-wrap">
              <div class="code-action">
                <Button size="small" @click="copyText(currentLog.operParam)">
                  <IconifyIcon class="btn-icon" icon="lucide:copy"/>
                  复制
                </Button>
              </div>
              <pre class="code-pre">{{ formatJson(currentLog.operParam) }}</pre>
            </div>
          </div>
        </section>

        <!-- 返回参数 -->
        <section class="detail-card">
          <div class="detail-card-title">
            <IconifyIcon class="title-icon" icon="lucide:download"/>
            返回参数
          </div>
          <div class="code-body">
            <div class="code-wrap">
              <div class="code-action">
                <Button size="small" @click="copyText(currentLog.jsonResult)">
                  <IconifyIcon class="btn-icon" icon="lucide:copy"/>
                  复制
                </Button>
              </div>
              <pre class="code-pre">{{ formatJson(currentLog.jsonResult) }}</pre>
            </div>
          </div>
        </section>

        <!-- 异常信息 -->
        <section v-if="currentLog.status !== '0'" class="detail-card">
          <div class="detail-card-title error-title">
            <IconifyIcon class="title-icon" icon="lucide:alert-triangle"/>
            异常信息
          </div>
          <div class="error-body">
            <div class="error-msg">{{ currentLog.errorMsg || '（无异常信息）' }}</div>
          </div>
        </section>
      </div>
    </Drawer>
  </Page>
</template>

<style scoped>
/* 页面填满 + 卡片体 flex 撑满，表格在内部滚动（与其他功能页一致，不出现整页滚动） */
.operlog-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.operlog-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.operlog-page :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.search-panel {
  flex-shrink: 0;
  margin-bottom: 16px;
  padding: 16px 18px 2px;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: var(--ant-color-fill-quaternary);
}

.search-panel :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.search-panel :deep(.ant-input),
.search-panel :deep(.ant-picker),
.search-panel :deep(.ant-select) {
  width: 180px;
}

.search-panel :deep(.ant-picker-range) {
  width: 300px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  gap: 12px;
  margin-bottom: 14px;
}

/* ===== 详情卡片布局（对齐原 Element UI 版本） ===== */
.detail-wrap {
  padding: 0 4px;
}

.detail-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 14px;
  overflow: hidden;
}

.detail-card-title {
  display: flex;
  align-items: center;
  background: #f7f9fb;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #ebeef5;
}

.title-icon {
  margin-right: 5px;
  color: #409eff;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  padding: 10px 8px;
  font-size: 13px;
  border-bottom: 1px solid #f5f7fa;
}

.detail-item__full {
  grid-column: 1 / -1;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  flex-shrink: 0;
  width: 72px;
  color: #909399;
  margin-right: 12px;
}

.detail-value {
  flex: 1;
  color: #303133;
  word-break: break-all;
}

.detail-location {
  color: #999;
  font-size: 12px;
}

.mono {
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 12px;
}

.tag-icon {
  margin-right: 4px;
}

/* HTTP 方法色块 */
.method-tag {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
  margin-right: 6px;
  vertical-align: middle;
  background: #f5f5f5;
  color: #616161;
}

.method-GET {
  background: #e8f5e9;
  color: #27ae60;
}

.method-POST {
  background: #e3f2fd;
  color: #1565c0;
}

.method-PUT {
  background: #fff3e0;
  color: #e65100;
}

.method-DELETE {
  background: #ffebee;
  color: #c62828;
}

/* JSON 代码块 */
.code-body {
  padding: 14px;
}

.code-wrap {
  position: relative;
  background: #f7f9fb;
  border: 1px solid #e8ecf0;
  border-radius: 4px;
  overflow: hidden;
}

.code-action {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}

.code-pre {
  display: block;
  max-height: 240px;
  margin: 0;
  padding: 12px 14px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  font-family: Consolas, 'SFMono-Regular', monospace;
  color: #444;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 异常信息 */
.error-title {
  color: #c0392b;
}

.error-title .title-icon {
  color: #c0392b;
}

.error-body {
  padding: 12px 16px;
}

.error-msg {
  padding: 8px 12px;
  border-left: 3px solid #e74c3c;
  border-radius: 3px;
  background: #fff8f8;
  color: #c0392b;
  font-size: 12px;
  line-height: 1.7;
  word-break: break-all;
}

@media (max-width: 900px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
