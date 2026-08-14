<script lang="ts" setup>
import type {TableColumnType} from 'ant-design-vue';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Tag,
} from 'ant-design-vue';

import type {SysLogininfor} from '#/api/monitor/log';
import {
  cleanLoginLogApi,
  deleteLoginLogApi,
  listLoginLogApi,
  unlockLoginApi,
} from '#/api/monitor/log';

import {computed, onMounted, reactive, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {useAccessStore} from '@vben/stores';
import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';
import {downloadBlob} from '#/utils/download';

type TableColumn = TableColumnType & { resizable?: boolean; visible?: boolean };
type TableKey = number | string;

defineOptions({name: 'MonitorLogininfor'});

const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const statusOptions = [
  {label: '成功', value: '0'},
  {label: '失败', value: '1'},
];

const loading = ref(false);
const showSearch = ref(true);
const selectedRowKeys = ref<TableKey[]>([]);
const selectedRows = ref<SysLogininfor[]>([]);
const total = ref(0);
const tableData = ref<SysLogininfor[]>([]);

const query = reactive({
  beginTime: '',
  endTime: '',
  ipaddr: '',
  pageNum: 1,
  pageSize: 10,
  status: undefined as string | undefined,
  userName: '',
});

const columns = ref<TableColumn[]>([
  {dataIndex: 'userName', key: 'userName', width: 120, ellipsis: true, title: '用户名称', visible: true},
  {dataIndex: 'ipaddr', key: 'ipaddr', width: 135, title: '登录地址', visible: true},
  {dataIndex: 'loginLocation', key: 'loginLocation', width: 140, ellipsis: true, title: '登录地点', visible: true},
  {dataIndex: 'browser', key: 'browser', width: 130, ellipsis: true, title: '浏览器', visible: true},
  {dataIndex: 'os', key: 'os', width: 130, title: '操作系统', visible: true},
  {dataIndex: 'status', key: 'status', width: 90, title: '登录状态', visible: true},
  {dataIndex: 'msg', key: 'msg', width: 170, ellipsis: true, title: '操作信息', visible: true},
  {dataIndex: 'loginTime', key: 'loginTime', width: 200, title: '登录时间', visible: true},
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
    endTime: query.endTime || undefined,
    ipaddr: query.ipaddr || undefined,
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    status: query.status,
    userName: query.userName || undefined,
  };
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
    const response = await listLoginLogApi(buildQuery());
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
    endTime: '',
    ipaddr: '',
    pageNum: 1,
    pageSize: 10,
    status: undefined,
    userName: '',
  });
  loadData();
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? query.pageSize;
  loadData();
}

function handleSelectionChange(keys: TableKey[], rows: SysLogininfor[]) {
  selectedRowKeys.value = keys;
  selectedRows.value = rows;
}

function handleDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请至少选择一条登录日志');
    return;
  }

  Modal.confirm({
    content: '删除后不可恢复，确定要删除选中的登录日志吗？',
    okText: '确定',
    title: '删除登录日志',
    async onOk() {
      await deleteLoginLogApi(selectedRowKeys.value);
      message.success('删除成功');
      selectedRowKeys.value = [];
      selectedRows.value = [];
      await loadData();
    },
  });
}

function handleClean() {
  Modal.confirm({
    content: '清空后无法恢复，确定清空全部登录日志吗？',
    okText: '确定',
    title: '清空登录日志',
    async onOk() {
      await cleanLoginLogApi();
      message.success('清空成功');
      selectedRowKeys.value = [];
      selectedRows.value = [];
      await loadData();
    },
  });
}

function handleUnlock() {
  if (selectedRows.value.length !== 1 || !selectedRows.value[0]?.userName) {
    message.warning('请选择一条包含用户名的登录日志');
    return;
  }

  const userName = selectedRows.value[0].userName;
  Modal.confirm({
    content: `确定解除用户“${userName}”的登录锁定吗？`,
    okText: '确定',
    title: '解除锁定',
    async onOk() {
      await unlockLoginApi(userName);
      message.success('解除锁定成功');
    },
  });
}

async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `logininfor_${Date.now()}.xlsx`,
      path: '/monitor/logininfor/export'
    });
    message.success('导出成功');
  } catch {
    message.error('导出失败，请稍后重试');
  }
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height>
    <div class="logininfor-page">
      <Card :bordered="false" class="logininfor-card">
        <section v-show="showSearch" class="search-panel">
          <Form layout="inline" @submit.prevent="handleSearch">
            <Form.Item label="用户名称">
              <Input
                v-model:value="query.userName" allow-clear placeholder="请输入用户名称"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="登录地址">
              <Input
                v-model:value="query.ipaddr" allow-clear placeholder="请输入登录地址"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="登录状态">
              <Select
                v-model:value="query.status" :options="statusOptions" allow-clear
                placeholder="请选择登录状态"
              />
            </Form.Item>
            <Form.Item label="登录时间">
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
              v-if="hasPermi('monitor:logininfor:remove')" :disabled="!selectedRowKeys.length"
              danger @click="handleDelete"
            >
              <IconifyIcon icon="ant-design:delete-outlined"/>
              删除
            </Button>
            <Button v-if="hasPermi('monitor:logininfor:remove')" @click="handleClean">
              <IconifyIcon icon="ant-design:clear-outlined"/>
              清空
            </Button>
            <Button
              v-if="hasPermi('monitor:logininfor:unlock')"
              :disabled="selectedRows.length !== 1" @click="handleUnlock"
            >
              <IconifyIcon icon="ant-design:unlock-outlined"/>
              解除锁定
            </Button>
            <Button v-if="hasPermi('monitor:logininfor:export')" @click="handleExport">
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
            <ColumnSetting :columns="columns" storage-key="monitor-logininfor-columns"/>
          </Space>
        </section>

        <ResizableTable
          :columns="visibleColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="{
            current: query.pageNum,
            pageSize: query.pageSize,
            showQuickJumper: true,
            showSizeChanger: true,
            showTotal: (value: number) => `共 ${value} 条`,
            total,
          }"
          :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
          :scroll="{ x: 1200 }"
          row-key="infoId"
          size="middle"
          storage-key="monitor-logininfor-columns"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="record.status == '0' ? 'success' : 'error'">
                {{ record.status == '0' ? '成功' : '失败' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'loginTime'">
              {{ formatDateTime(record.loginTime) }}
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>
  </Page>
</template>

<style scoped>
/* 页面填满 + 卡片体 flex 撑满，表格在内部滚动（与其他功能页一致，不出现整页滚动） */
.logininfor-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.logininfor-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.logininfor-page :deep(.ant-card-body) {
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

@media (max-width: 900px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
