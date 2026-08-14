<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useRouter} from 'vue-router';

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
  Tag
} from 'ant-design-vue';
import type {SysDictType} from '#/api/system/dict';
import {
  addDictTypeApi,
  deleteDictTypeApi,
  getDictTypeApi,
  listDictTypeApi,
  refreshDictCacheApi,
  updateDictTypeApi
} from '#/api/system/dict';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';
import {downloadBlob} from '#/utils/download';

defineOptions({name: 'SystemDict'});

type Key = number | string;
type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

const router = useRouter();
const accessStore = useAccessStore();
const statusOptions = [{label: '正常', value: '0'}, {label: '停用', value: '1'}];

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const loading = ref(false);
const saving = ref(false);
const showSearch = ref(true);
const typeList = ref<SysDictType[]>([]);
const total = ref(0);
const selectedRowKeys = ref<Key[]>([]);
const dateRange = ref<[import('dayjs').Dayjs, import('dayjs').Dayjs] | undefined>();
const query = reactive({
  beginTime: undefined as string | undefined,
  dictName: undefined as string | undefined,
  dictType: undefined as string | undefined,
  endTime: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
  status: undefined as string | undefined
});

function buildQuery() {
  const {beginTime, endTime, ...rest} = query;
  return {...rest, params: {beginTime, endTime}};
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  if (value.includes('T')) return value.replace('T', ' ').slice(0, 19);
  return value.slice(0, 19);
}

async function loadData() {
  loading.value = true;
  try {
    const {rows, total: count} = await listDictTypeApi(buildQuery());
    typeList.value = rows;
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
  Object.assign(query, {
    beginTime: undefined,
    dictName: undefined,
    dictType: undefined,
    endTime: undefined,
    pageNum: 1,
    status: undefined
  });
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
    dataIndex: 'dictId',
    key: 'dictId',
    title: '字典编号',
    visible: true,
    width: 100
  },
  {
    dataIndex: 'dictName',
    ellipsis: true,
    key: 'dictName',
    title: '字典名称',
    visible: true,
    width: 180
  },
  {
    dataIndex: 'dictType',
    ellipsis: true,
    key: 'dictType',
    title: '字典类型',
    visible: true,
    width: 220
  },
  {align: 'center', key: 'status', title: '状态', visible: true, width: 100},
  {dataIndex: 'remark', ellipsis: true, key: 'remark', title: '备注', visible: true, width: 200},
  {
    align: 'center',
    dataIndex: 'createTime',
    key: 'createTime',
    title: '创建时间',
    visible: true,
    width: 180
  },
  {align: 'center', key: 'operation', resizable: false, title: '操作', visible: true, width: 210},
]);
const visibleColumns = computed(() => columns.value.filter((column) => column.visible !== false) as Col[]);

const modalOpen = ref(false);
const modalTitle = ref('');
const isEdit = ref(false);
const formRef = ref();
const form = reactive<SysDictType>({});
const rules: Record<string, any> = {
  dictName: [{
    required: true,
    message: '字典名称不能为空',
    trigger: 'blur'
  }], dictType: [{required: true, message: '字典类型不能为空', trigger: 'blur'}]
};

function resetForm() {
  Object.assign(form, {
    dictId: undefined,
    dictName: undefined,
    dictType: undefined,
    remark: undefined,
    status: '0'
  });
  formRef.value?.clearValidate?.();
}

function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增字典类型';
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: SysDictType) {
  isEdit.value = true;
  modalTitle.value = '修改字典类型';
  resetForm();
  Object.assign(form, await getDictTypeApi(row.dictId as Key));
  modalOpen.value = true;
}

function handleUpdate() {
  const row = typeList.value.find((item) => String(item.dictId) === String(selectedRowKeys.value[0]));
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
      await updateDictTypeApi({...form});
      message.success('修改成功');
    } else {
      await addDictTypeApi({...form});
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row?: SysDictType) {
  const dictIds = row ? [row.dictId as Key] : selectedRowKeys.value;
  Modal.confirm({
    cancelText: '取消',
    content: `是否确认删除字典编号为“${dictIds.join(',')}”的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteDictTypeApi(dictIds);
      selectedRowKeys.value = [];
      message.success('删除成功');
      loadData();
    },
    title: '系统提示'
  });
}

function openDataList(row: SysDictType) {
  router.push(`/system/dict-data/index/${row.dictId}`);
}

async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `dict_type_${Date.now()}.xlsx`,
      path: '/system/dict/type/export'
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message ?? '导出失败');
  }
}

function handleRefreshCache() {
  Modal.confirm({
    cancelText: '取消',
    content: '刷新后将重新加载全部字典缓存，是否继续？',
    okText: '刷新缓存',
    onOk: async () => {
      await refreshDictCacheApi();
      message.success('缓存刷新成功');
    },
    title: '系统提示'
  });
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="字典管理">
    <div class="dict-page">
      <Card :bordered="false">
        <div v-show="showSearch" class="search-panel mb-4">
          <Form :model="query" class="search-form" layout="inline" @submit.prevent>
            <Form.Item label="字典名称"><Input v-model:value="query.dictName" allow-clear
                                               placeholder="请输入字典名称" style="width: 200px"
                                               @press-enter="handleSearch"/></Form.Item>
            <Form.Item label="字典类型"><Input v-model:value="query.dictType" allow-clear
                                               placeholder="请输入字典类型" style="width: 200px"
                                               @press-enter="handleSearch"/></Form.Item>
            <Form.Item label="状态"><Select v-model:value="query.status" :options="statusOptions"
                                            allow-clear placeholder="字典状态"
                                            style="width: 160px"/></Form.Item>
            <Form.Item label="创建时间">
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
            <Button v-if="hasPermi('system:dict:add')" type="primary" @click="openAdd">
              <IconifyIcon class="btn-icon" icon="lucide:plus"/>
              新增
            </Button>
            <Button v-if="hasPermi('system:dict:edit')" :disabled="selectedRowKeys.length !== 1"
                    @click="handleUpdate">
              <IconifyIcon class="btn-icon" icon="lucide:pencil"/>
              修改
            </Button>
            <Button v-if="hasPermi('system:dict:remove')" :disabled="selectedRowKeys.length === 0"
                    danger @click="handleDelete()">
              <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
              删除
            </Button>
            <Button v-if="hasPermi('system:dict:export')" @click="handleExport">
              <IconifyIcon class="btn-icon" icon="lucide:download"/>
              导出
            </Button>
            <Button v-if="hasPermi('system:dict:remove')" @click="handleRefreshCache">
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
                           storage-key="system-dict-columns"/>
          </Space>
        </div>
        <ResizableTable :columns="visibleColumns" :data-source="typeList" :loading="loading"
                        :pagination="{ current: query.pageNum, pageSize: query.pageSize, showSizeChanger: true, showTotal: (count: number) => `共 ${count} 条`, total }"
                        :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
                        row-key="dictId" storage-key="system-dict-columns"
                        @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'dictType'"><a class="link-type"
                                                          @click="openDataList(record)">{{
                record.dictType
              }}</a></template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === '0' ? 'success' : 'error'">
                {{ record.status === '0' ? '正常' : '停用' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createTime'">{{
                formatDateTime(record.createTime)
              }}
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="4">
                <Button v-if="hasPermi('system:dict:edit')" size="small" type="link"
                        @click="openEdit(record)">修改
                </Button>
                <Button v-if="hasPermi('system:dict:list')" size="small" type="link"
                        @click="openDataList(record)">字典数据
                </Button>
                <Button v-if="hasPermi('system:dict:remove')" danger size="small" type="link"
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
        <Form.Item label="字典名称" name="dictName"><Input v-model:value="form.dictName"
                                                           placeholder="请输入字典名称"/>
        </Form.Item>
        <Form.Item label="字典类型" name="dictType"><Input v-model:value="form.dictType"
                                                           placeholder="请输入字典类型，如：sys_user_sex"/>
        </Form.Item>
        <Form.Item label="状态" name="status">
          <Radio.Group v-model:value="form.status">
            <Radio v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea v-model:value="form.remark" :auto-size="{ minRows: 3, maxRows: 6 }"
                          placeholder="请输入备注"/>
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.dict-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.dict-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.dict-page :deep(.ant-card-body) {
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

.link-type {
  color: #1677ff;
  cursor: pointer;
}
</style>
