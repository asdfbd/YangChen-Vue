<script lang="ts" setup>
/**
 * 代码生成管理页：
 * - 列表：数据库表 -> 生成配置 -> 下载/生成代码
 * - 功能：导入表结构、修改配置、删除、预览、下载、同步、批量生成
 */
import {computed, onMounted, reactive, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {useAccessStore} from '@vben/stores';

import type {TableColumnType} from 'ant-design-vue';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  message,
  Modal,
  Space,
} from 'ant-design-vue';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';

import {
  batchGenCodeApi,
  deleteGenTableApi,
  downloadCodeApi,
  genCodeToPathApi,
  getGenTableListApi,
  synchDbApi,
  type GenTable,
} from '#/api/tool/gen';

import EditGenDrawer from './components/edit-gen-drawer.vue';
import ImportTableModal from './components/import-table-modal.vue';
import PreviewCodeModal from './components/preview-code-modal.vue';

defineOptions({name: 'ToolGen'});

const accessStore = useAccessStore();

/** 权限码校验（管理员 *:*:* 拥有全部权限） */
function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

// ===== 列表 =====
const loading = ref(false);
const tableList = ref<GenTable[]>([]);
const total = ref(0);
const showSearch = ref(true);
const selectedRowKeys = ref<(string | number)[]>([]);
const dateRange = ref<[import('dayjs').Dayjs, import('dayjs').Dayjs] | undefined>(
  undefined,
);

const query = reactive({
  beginTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
  tableComment: undefined as string | undefined,
  tableName: undefined as string | undefined,
});

/** 组装查询参数：创建时间作为 params.beginTime/endTime 传给后端 */
function buildQuery() {
  const {beginTime, endTime, ...rest} = query;
  return {...rest, params: {beginTime, endTime}};
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.includes('T')
    ? value.replace('T', ' ').slice(0, 19)
    : value.slice(0, 19);
}

async function loadData() {
  loading.value = true;
  try {
    const {rows, total: t} = await getGenTableListApi(buildQuery());
    tableList.value = rows;
    total.value = t;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.tableComment = undefined;
  query.tableName = undefined;
  query.beginTime = undefined;
  query.endTime = undefined;
  dateRange.value = undefined;
  query.pageNum = 1;
  loadData();
}

function handleDateRangeChange(_dates: unknown, dateStrings: [string, string]) {
  query.beginTime = dateStrings?.[0] || undefined;
  query.endTime = dateStrings?.[1] || undefined;
}

function handleTableChange(pagination: {current?: number; pageSize?: number}) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? 10;
  loadData();
}

function handleSelectionChange(keys: (string | number)[]) {
  selectedRowKeys.value = keys;
}

type Col = TableColumnType & {resizable?: boolean; visible?: boolean};

const columns = ref<Col[]>([
  {
    dataIndex: 'tableName',
    ellipsis: true,
    key: 'tableName',
    title: '表名称',
    visible: true,
    width: 180,
  },
  {
    dataIndex: 'tableComment',
    ellipsis: true,
    key: 'tableComment',
    title: '表描述',
    visible: true,
    width: 200,
  },
  {
    dataIndex: 'className',
    key: 'className',
    title: '实体类名称',
    visible: true,
    width: 150,
  },
  {
    align: 'center' as const,
    key: 'createTime',
    title: '创建时间',
    visible: true,
    width: 170,
  },
  {
    align: 'center' as const,
    key: 'updateTime',
    title: '更新时间',
    visible: true,
    width: 170,
  },
  {
    align: 'center' as const,
    key: 'operation',
    resizable: false,
    title: '操作',
    visible: true,
    width: 280,
  },
]);

const visibleColumns = computed(() => {
  const out: Col[] = [];
  for (const col of columns.value) {
    if (col.visible !== false) out.push(col);
  }
  return out;
});

// ===== 删除（确认提示表名称，实际传表 ID） =====
/** 由选中的表 ID 反查表名称（用于确认提示文案） */
function selectedTableNames(): string[] {
  return selectedRowKeys.value.map((id) => {
    const row = tableList.value.find(
      (item) => String(item.tableId) === String(id),
    );
    return row?.tableName ?? String(id);
  });
}

function handleDelete(row?: GenTable) {
  // 实际请求参数：表 ID（后端 DELETE /tool/gen/{tableIds} 需要 Long[]）
  const tableIds = row
    ? [String(row.tableId)]
    : selectedRowKeys.value.map(String);
  // 提示文案：表名称
  const names = row
    ? [row.tableName ?? String(row.tableId)]
    : selectedTableNames();
  Modal.confirm({
    content: `是否确认删除表"${names.join('、')}"的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteGenTableApi(tableIds);
      message.success('删除成功');
      selectedRowKeys.value = [];
      loadData();
    },
    title: '系统提示',
  });
}

// ===== 导入表结构弹窗 =====
const importOpen = ref(false);

// ===== 预览弹窗 =====
const previewOpen = ref(false);
const previewTableId = ref<number | string | undefined>(undefined);
const previewTableName = ref('');

function openPreview(row: GenTable) {
  previewTableId.value = row.tableId;
  previewTableName.value = row.tableName ?? '';
  previewOpen.value = true;
}

// ===== 修改配置抽屉 =====
const editOpen = ref(false);
const editTableId = ref<number | string | undefined>(undefined);

function openEdit(row: GenTable) {
  editTableId.value = row.tableId;
  editOpen.value = true;
}

// ===== 同步数据库 =====
function handleSynchDb(row: GenTable) {
  Modal.confirm({
    content: `确认要强制同步"${row.tableName}"表结构吗？`,
    okText: '确定',
    onOk: async () => {
      await synchDbApi(row.tableName as string);
      message.success('同步成功');
      loadData();
    },
    title: '系统提示',
  });
}

// ===== 下载代码 =====
async function handleDownload(row: GenTable) {
  try {
    await downloadCodeApi(row.tableName as string);
    message.success('下载成功');
  } catch (error: any) {
    message.error(error?.message ?? '下载失败');
  }
}

// ===== 生成代码（自定义路径） =====
function handleGenCode(row: GenTable) {
  Modal.confirm({
    content: `确认要生成表"${row.tableName}"代码吗？`,
    okText: '确定',
    onOk: async () => {
      await genCodeToPathApi(row.tableName as string);
      message.success('生成成功');
    },
    title: '系统提示',
  });
}

// ===== 批量生成代码（下载 zip） =====
function handleBatchGenCode() {
  const tableIds = selectedRowKeys.value;
  if (!tableIds.length) {
    message.warning('请先勾选要生成的数据表');
    return;
  }
  // 生成代码接口按表名生成（GET /tool/gen/batchGenCode?tables=xxx）
  const tableNames = selectedTableNames();
  Modal.confirm({
    content: `确定要生成选中表代码吗？共 ${tableIds.length} 张表（${tableNames.join('、')}）。`,
    okText: '确定',
    onOk: async () => {
      await batchGenCodeApi(tableNames);
      message.success('下载成功');
    },
    title: '系统提示',
  });
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page auto-content-height title="代码生成">
    <div class="gen-page">
      <Card :bordered="false">
      <!-- 搜索区（可折叠） -->
      <div v-show="showSearch" class="search-panel mb-4">
        <Form class="search-form" layout="inline" @submit.prevent>
          <Form.Item label="表名称" name="tableName">
            <Input
              v-model:value="query.tableName"
              allow-clear
              placeholder="请输入表名称"
              style="width: 200px"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item label="表描述" name="tableComment">
            <Input
              v-model:value="query.tableComment"
              allow-clear
              placeholder="请输入表描述"
              style="width: 200px"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item label="创建时间" name="dateRange">
            <DatePicker.RangePicker
              v-model:value="dateRange"
              style="width: 260px"
              @change="handleDateRangeChange"
            />
          </Form.Item>
          <Form.Item class="search-actions">
            <Space :size="8">
              <Button type="primary" @click="handleSearch">搜索</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      <!-- 工具栏：左侧操作组 + 右侧视图控制组 -->
      <div class="toolbar mb-4">
        <Space :size="12">
          <Button
            v-if="hasPermi('tool:gen:import')"
            type="primary"
            @click="importOpen = true"
          >
            <IconifyIcon class="btn-icon" icon="lucide:upload"/>
            导入
          </Button>
          <Button
            v-if="hasPermi('tool:gen:remove')"
            :disabled="selectedRowKeys.length === 0"
            danger
            @click="handleDelete()"
          >
            <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
            删除
          </Button>
          <Button
            v-if="hasPermi('tool:gen:code')"
            :disabled="selectedRowKeys.length === 0"
            @click="handleBatchGenCode"
          >
            <IconifyIcon class="btn-icon" icon="lucide:code"/>
            生成代码
          </Button>
        </Space>
        <Space :size="8">
          <Button size="small" @click="showSearch = !showSearch">
            <IconifyIcon
              :icon="showSearch ? 'lucide:eye' : 'lucide:eye-off'"
              class="btn-icon"
            />
            {{ showSearch ? '隐藏搜索' : '显示搜索' }}
          </Button>
          <Button size="small" @click="loadData">
            <IconifyIcon class="btn-icon" icon="lucide:refresh-cw"/>
            刷新
          </Button>
          <ColumnSetting
            :columns="columns"
            :filter="(c) => c.key !== 'operation'"
            storage-key="tool-gen-columns"
          />
        </Space>
      </div>

      <!-- 表格 -->
      <ResizableTable
        :columns="visibleColumns"
        :data-source="tableList"
        :loading="loading"
        :pagination="{
          current: query.pageNum,
          pageSize: query.pageSize,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          total,
        }"
        :row-selection="{
          selectedRowKeys,
          onChange: handleSelectionChange,
        }"
        row-key="tableId"
        storage-key="tool-gen-columns"
        @change="handleTableChange"
      >
        <template #bodyCell="{column, record}">
          <template v-if="column.key === 'tableName'">
            <a class="link-type" @click="openEdit(record)">{{ record.tableName }}</a>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDateTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'updateTime'">
            {{ formatDateTime(record.updateTime) }}
          </template>
          <template v-else-if="column.key === 'operation'">
            <Space :size="4">
              <Button
                v-if="hasPermi('tool:gen:edit')"
                size="small"
                type="link"
                @click="openEdit(record)"
              >
                修改
              </Button>
              <Button
                v-if="hasPermi('tool:gen:preview')"
                size="small"
                type="link"
                @click="openPreview(record)"
              >
                预览
              </Button>
              <Button
                v-if="hasPermi('tool:gen:code')"
                size="small"
                type="link"
                @click="handleDownload(record)"
              >
                下载
              </Button>
              <Button
                v-if="hasPermi('tool:gen:edit')"
                size="small"
                type="link"
                @click="handleSynchDb(record)"
              >
                同步
              </Button>
              <Button
                v-if="hasPermi('tool:gen:code')"
                size="small"
                type="link"
                @click="handleGenCode(record)"
              >
                生成代码
              </Button>
              <Button
                v-if="hasPermi('tool:gen:remove')"
                danger
                size="small"
                type="link"
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

    <!-- 导入表结构 -->
    <ImportTableModal v-model:open="importOpen" @success="loadData"/>

    <!-- 预览代码 -->
    <PreviewCodeModal
      v-model:open="previewOpen"
      :table-id="previewTableId"
      :table-name="previewTableName"
    />

    <!-- 修改配置 -->
    <EditGenDrawer
      v-model:open="editOpen"
      :table-id="editTableId"
      @success="loadData"
    />
  </Page>
</template>

<style scoped>
/* 外层 flex 容器撑满内容区：卡片立即占满整个容器高度（而非由内容撑开） */
.gen-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.gen-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.gen-page :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* 搜索区：独立浅色面板，与表格区隔开 */
.search-panel {
  flex-shrink: 0;
  padding: 16px 16px 4px;
  background: #f6f8fa;
  border: 1px solid #eceff3;
  border-radius: 8px;
}

.search-form .ant-form-item {
  margin-right: 24px;
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.link-type {
  color: #1677ff;
  cursor: pointer;
}

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
