<script lang="ts" setup>
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
  Radio,
  Select,
  Space,
  Tag,
} from 'ant-design-vue';
import type {SysConfig} from '#/api/system/config';
import {
  addConfigApi,
  deleteConfigApi,
  getConfigApi,
  listConfigApi,
  refreshConfigCacheApi,
  updateConfigApi,
} from '#/api/system/config';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';
import {downloadBlob} from '#/utils/download';

defineOptions({name: 'SystemConfig'});

type Key = number | string;
type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const yesNoOptions = [
  {label: '是', value: 'Y'},
  {label: '否', value: 'N'},
];

const loading = ref(false);
const saving = ref(false);
const showSearch = ref(true);
const configList = ref<SysConfig[]>([]);
const total = ref(0);
const selectedRowKeys = ref<Key[]>([]);
const dateRange = ref<[import('dayjs').Dayjs, import('dayjs').Dayjs] | undefined>();
const query = reactive({
  beginTime: undefined as string | undefined,
  configKey: undefined as string | undefined,
  configName: undefined as string | undefined,
  configType: undefined as string | undefined,
  endTime: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
});

function buildQuery() {
  const {beginTime, endTime, ...rest} = query;
  return {...rest, params: {beginTime, endTime}};
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.includes('T') ? value.replace('T', ' ').slice(0, 19) : value.slice(0, 19);
}

async function loadData() {
  loading.value = true;
  try {
    const {rows, total: count} = await listConfigApi(buildQuery());
    configList.value = rows;
    total.value = count;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.beginTime = undefined;
  query.configKey = undefined;
  query.configName = undefined;
  query.configType = undefined;
  query.endTime = undefined;
  query.pageNum = 1;
  dateRange.value = undefined;
  loadData();
}

function handleDateRangeChange(_dates: unknown, dateStrings: [string, string]) {
  query.beginTime = dateStrings?.[0] || undefined;
  query.endTime = dateStrings?.[1] || undefined;
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? 10;
  loadData();
}

function handleSelectionChange(keys: Key[]) {
  selectedRowKeys.value = keys;
}

const columns = ref<Col[]>([
  {
    align: 'center',
    dataIndex: 'configId',
    key: 'configId',
    title: '参数主键',
    visible: true,
    width: 100
  },
  {
    dataIndex: 'configName',
    ellipsis: true,
    key: 'configName',
    title: '参数名称',
    visible: true,
    width: 180
  },
  {
    dataIndex: 'configKey',
    ellipsis: true,
    key: 'configKey',
    title: '参数键名',
    visible: true,
    width: 220
  },
  {
    dataIndex: 'configValue',
    ellipsis: true,
    key: 'configValue',
    title: '参数键值',
    visible: true,
    width: 220
  },
  {align: 'center', key: 'configType', title: '系统内置', visible: true, width: 110},
  {dataIndex: 'remark', ellipsis: true, key: 'remark', title: '备注', visible: true, width: 180},
  {
    align: 'center',
    dataIndex: 'createTime',
    key: 'createTime',
    title: '创建时间',
    visible: true,
    width: 180
  },
  {align: 'center', key: 'operation', resizable: false, title: '操作', visible: true, width: 150},
]);

const visibleColumns = computed(() => {
  const visible: Col[] = [];
  for (const column of columns.value) {
    if (column.visible !== false) visible.push(column);
  }
  return visible;
});

const modalOpen = ref(false);
const modalTitle = ref('');
const isEdit = ref(false);
const formRef = ref();
const form = reactive<SysConfig>({});
const rules: Record<string, any> = {
  configKey: [{required: true, message: '参数键名不能为空', trigger: 'blur'}],
  configName: [{required: true, message: '参数名称不能为空', trigger: 'blur'}],
  configValue: [{required: true, message: '参数键值不能为空', trigger: 'blur'}],
};

function resetForm() {
  Object.assign(form, {
    configId: undefined,
    configKey: undefined,
    configName: undefined,
    configType: 'Y',
    configValue: undefined,
    remark: undefined,
  });
  formRef.value?.clearValidate?.();
}

function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增参数';
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: SysConfig) {
  isEdit.value = true;
  modalTitle.value = '修改参数';
  resetForm();
  Object.assign(form, await getConfigApi(row.configId as Key));
  modalOpen.value = true;
}

function handleUpdate() {
  const key = selectedRowKeys.value[0];
  const row = configList.value.find((item) => String(item.configId) === String(key));
  if (row) openEdit(row);
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateConfigApi({...form});
      message.success('修改成功');
    } else {
      await addConfigApi({...form});
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row?: SysConfig) {
  const configIds = row ? [row.configId as Key] : selectedRowKeys.value;
  Modal.confirm({
    cancelText: '取消',
    content: `是否确认删除参数编号为“${configIds.join(',')}”的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteConfigApi(configIds);
      selectedRowKeys.value = [];
      message.success('删除成功');
      loadData();
    },
    title: '系统提示',
  });
}

async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `config_${Date.now()}.xlsx`,
      path: '/system/config/export'
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message ?? '导出失败');
  }
}

function handleRefreshCache() {
  Modal.confirm({
    cancelText: '取消',
    content: '刷新后将重新加载全部系统参数缓存，是否继续？',
    okText: '刷新缓存',
    onOk: async () => {
      await refreshConfigCacheApi();
      message.success('缓存刷新成功');
    },
    title: '系统提示',
  });
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="参数设置">
    <div class="config-page">
      <Card :bordered="false">
        <div v-show="showSearch" class="search-panel mb-4">
          <Form :model="query" class="search-form" layout="inline" @submit.prevent>
            <Form.Item label="参数名称" name="configName"><Input v-model:value="query.configName"
                                                                 allow-clear
                                                                 placeholder="请输入参数名称"
                                                                 style="width: 200px"
                                                                 @press-enter="handleSearch"/>
            </Form.Item>
            <Form.Item label="参数键名" name="configKey"><Input v-model:value="query.configKey"
                                                                allow-clear
                                                                placeholder="请输入参数键名"
                                                                style="width: 200px"
                                                                @press-enter="handleSearch"/>
            </Form.Item>
            <Form.Item label="系统内置" name="configType"><Select v-model:value="query.configType"
                                                                  :options="yesNoOptions"
                                                                  allow-clear
                                                                  placeholder="系统内置"
                                                                  style="width: 160px"/></Form.Item>
            <Form.Item label="创建时间" name="dateRange">
              <DatePicker.RangePicker v-model:value="dateRange" style="width: 260px"
                                      @change="handleDateRangeChange"/>
            </Form.Item>
            <Form.Item>
              <Space :size="8">
                <Button type="primary" @click="handleSearch">搜索</Button>
                <Button @click="handleReset">重置</Button>
              </Space>
            </Form.Item>
          </Form>
        </div>

        <div class="toolbar mb-4">
          <Space :size="12" wrap>
            <Button v-if="hasPermi('system:config:add')" type="primary" @click="openAdd">
              <IconifyIcon class="btn-icon" icon="lucide:plus"/>
              新增
            </Button>
            <Button v-if="hasPermi('system:config:edit')" :disabled="selectedRowKeys.length !== 1"
                    @click="handleUpdate">
              <IconifyIcon class="btn-icon" icon="lucide:pencil"/>
              修改
            </Button>
            <Button v-if="hasPermi('system:config:remove')" :disabled="selectedRowKeys.length === 0"
                    danger @click="handleDelete()">
              <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
              删除
            </Button>
            <Button v-if="hasPermi('system:config:export')" @click="handleExport">
              <IconifyIcon class="btn-icon" icon="lucide:download"/>
              导出
            </Button>
            <Button v-if="hasPermi('system:config:remove')" @click="handleRefreshCache">
              <IconifyIcon class="btn-icon" icon="lucide:refresh-ccw"/>
              刷新缓存
            </Button>
          </Space>
          <Space :size="8">
            <Button size="small" @click="showSearch = !showSearch">
              <IconifyIcon :icon="showSearch ? 'lucide:eye-off' : 'lucide:eye'" class="btn-icon"/>
              {{ showSearch ? '隐藏搜索' : '显示搜索' }}
            </Button>
            <Button size="small" @click="loadData">
              <IconifyIcon class="btn-icon" icon="lucide:refresh-cw"/>
              刷新
            </Button>
            <ColumnSetting :columns="columns" :filter="(column) => column.key !== 'operation'"
                           storage-key="system-config-columns"/>
          </Space>
        </div>

        <ResizableTable
          :columns="visibleColumns"
          :data-source="configList"
          :loading="loading"
          :pagination="{ current: query.pageNum, pageSize: query.pageSize, showSizeChanger: true, showTotal: (count: number) => `共 ${count} 条`, total }"
          :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
          row-key="configId"
          storage-key="system-config-columns"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'configType'">
              <Tag :color="record.configType === 'Y' ? 'blue' : 'default'">
                {{ record.configType === 'Y' ? '是' : '否' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createTime'">{{
                formatDateTime(record.createTime)
              }}
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="4">
                <Button v-if="hasPermi('system:config:edit')" size="small" type="link"
                        @click="openEdit(record)">修改
                </Button>
                <Button v-if="hasPermi('system:config:remove')" danger size="small" type="link"
                        @click="handleDelete(record)">删除
                </Button>
              </Space>
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>

    <Modal v-model:open="modalOpen" :confirm-loading="saving" :title="modalTitle" width="560px"
           @ok="handleSubmit">
      <Form ref="formRef" :label-col="{ span: 5 }" :model="form" :rules="rules"
            :wrapper-col="{ span: 18 }">
        <Form.Item label="参数名称" name="configName"><Input v-model:value="form.configName"
                                                             placeholder="请输入参数名称"/>
        </Form.Item>
        <Form.Item label="参数键名" name="configKey"><Input v-model:value="form.configKey"
                                                            placeholder="请输入参数键名"/>
        </Form.Item>
        <Form.Item label="参数键值" name="configValue">
          <Input.TextArea v-model:value="form.configValue" :auto-size="{ minRows: 3, maxRows: 6 }"
                          placeholder="请输入参数键值"/>
        </Form.Item>
        <Form.Item label="系统内置" name="configType">
          <Radio.Group v-model:value="form.configType">
            <Radio v-for="option in yesNoOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea v-model:value="form.remark" :auto-size="{ minRows: 3, maxRows: 6 }"
                          placeholder="请输入备注"/>
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.config-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.config-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.config-page :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  flex-shrink: 0;
  gap: 8px 16px;
}

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

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
