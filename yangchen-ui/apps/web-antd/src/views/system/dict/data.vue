<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {useAccessStore} from '@vben/stores';
import type {TableColumnType} from 'ant-design-vue';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Radio,
  Select,
  Space,
  Tag
} from 'ant-design-vue';
import type {SysDictData, SysDictType} from '#/api/system/dict';
import {
  addDictDataApi,
  deleteDictDataApi,
  getDictDataApi,
  getDictTypeApi,
  getDictTypeOptionsApi,
  listDictDataApi,
  updateDictDataApi
} from '#/api/system/dict';
import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';
import {downloadBlob} from '#/utils/download';

defineOptions({name: 'SystemDictData'});
type Key = number | string;
type Col = TableColumnType & { resizable?: boolean; visible?: boolean };
const route = useRoute();
const router = useRouter();
const accessStore = useAccessStore();
const statusOptions = [{label: '正常', value: '0'}, {label: '停用', value: '1'}];
const listClassOptions = [{label: '默认', value: 'default'}, {
  label: '主要',
  value: 'primary'
}, {label: '成功', value: 'success'}, {label: '信息', value: 'info'}, {
  label: '警告',
  value: 'warning'
}, {label: '危险', value: 'danger'}];

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const loading = ref(false);
const saving = ref(false);
const showSearch = ref(true);
const dataList = ref<SysDictData[]>([]);
const typeOptions = ref<SysDictType[]>([]);
const currentType = ref<SysDictType>();
const total = ref(0);
const selectedRowKeys = ref<Key[]>([]);
const query = reactive({
  dictLabel: undefined as string | undefined,
  dictType: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
  status: undefined as string | undefined
});

async function loadData() {
  if (!query.dictType) return;
  loading.value = true;
  try {
    const {rows, total: count} = await listDictDataApi({...query});
    dataList.value = rows;
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
  query.dictLabel = undefined;
  query.status = undefined;
  query.pageNum = 1;
  loadData();
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
    dataIndex: 'dictCode',
    key: 'dictCode',
    title: '字典编码',
    visible: true,
    width: 100
  }, {key: 'dictLabel', title: '字典标签', visible: true, width: 160}, {
    dataIndex: 'dictValue',
    ellipsis: true,
    key: 'dictValue',
    title: '字典键值',
    visible: true,
    width: 160
  }, {
    align: 'center',
    dataIndex: 'dictSort',
    key: 'dictSort',
    title: '字典排序',
    visible: true,
    width: 110
  }, {
    align: 'center',
    key: 'status',
    title: '状态',
    visible: true,
    width: 100
  }, {
    dataIndex: 'remark',
    ellipsis: true,
    key: 'remark',
    title: '备注',
    visible: true,
    width: 180
  }, {
    align: 'center',
    dataIndex: 'createTime',
    key: 'createTime',
    title: '创建时间',
    visible: true,
    width: 180
  }, {
    align: 'center',
    key: 'operation',
    resizable: false,
    title: '操作',
    visible: true,
    width: 150
  },
]);
const visibleColumns = computed(() => {
  const visible: Col[] = [];
  for (const col of columns.value) if (col.visible !== false) visible.push(col);
  return visible;
});

function formatDateTime(value?: string) {
  if (!value) return '-';
  if (value.includes('T')) return value.replace('T', ' ').slice(0, 19);
  return value.slice(0, 19);
}

function tagColor(listClass?: string) {
  return ({
    danger: 'error',
    default: undefined,
    info: 'processing',
    primary: 'blue',
    success: 'success',
    warning: 'warning'
  } as Record<string, any>)[listClass ?? 'default'];
}

const modalOpen = ref(false);
const modalTitle = ref('');
const isEdit = ref(false);
const formRef = ref();
const form = reactive<SysDictData>({});
const rules: Record<string, any> = {
  dictLabel: [{
    required: true,
    message: '数据标签不能为空',
    trigger: 'blur'
  }],
  dictSort: [{required: true, message: '数据排序不能为空', trigger: 'change'}],
  dictValue: [{required: true, message: '数据键值不能为空', trigger: 'blur'}]
};

function resetForm() {
  Object.assign(form, {
    cssClass: undefined,
    dictCode: undefined,
    dictLabel: undefined,
    dictSort: 0,
    dictType: query.dictType,
    dictValue: undefined,
    listClass: 'default',
    remark: undefined,
    status: '0'
  });
  formRef.value?.clearValidate?.();
}

function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增字典数据';
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: SysDictData) {
  isEdit.value = true;
  modalTitle.value = '修改字典数据';
  resetForm();
  const detail = await getDictDataApi(row.dictCode as Key);
  Object.assign(form, detail, {dictSort: Number(detail.dictSort ?? 0)});
  modalOpen.value = true;
}

function handleUpdate() {
  const row = dataList.value.find((item) => String(item.dictCode) === String(selectedRowKeys.value[0]));
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
      await updateDictDataApi({...form});
      message.success('修改成功');
    } else {
      await addDictDataApi({...form});
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row?: SysDictData) {
  const codes = row ? [row.dictCode as Key] : selectedRowKeys.value;
  Modal.confirm({
    cancelText: '取消',
    content: `是否确认删除字典编码为“${codes.join(',')}”的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteDictDataApi(codes);
      selectedRowKeys.value = [];
      message.success('删除成功');
      loadData();
    },
    title: '系统提示'
  });
}

async function handleExport() {
  try {
    await downloadBlob({
      body: {...query},
      filename: `dict_data_${Date.now()}.xlsx`,
      path: '/system/dict/data/export'
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message ?? '导出失败');
  }
}

async function init() {
  const id = route.params.dictId as string;
  const [type, options] = await Promise.all([getDictTypeApi(id), getDictTypeOptionsApi()]);
  currentType.value = type;
  typeOptions.value = options;
  query.dictType = type.dictType;
  await loadData();
}

onMounted(init);
</script>

<template>
  <Page :title="`字典数据${currentType?.dictName ? ` · ${currentType.dictName}` : ''}`"
        auto-content-height>
    <div class="dict-data-page">
      <Card :bordered="false">
        <div v-show="showSearch" class="search-panel mb-4">
          <Form :model="query" class="search-form" layout="inline" @submit.prevent>
            <Form.Item label="字典名称"><Select v-model:value="query.dictType"
                                                :options="typeOptions.map((type) => ({ label: type.dictName, value: type.dictType }))"
                                                style="width: 220px" @change="handleSearch"/>
            </Form.Item>
            <Form.Item label="字典标签"><Input v-model:value="query.dictLabel" allow-clear
                                               placeholder="请输入字典标签" style="width: 200px"
                                               @press-enter="handleSearch"/></Form.Item>
            <Form.Item label="状态"><Select v-model:value="query.status" :options="statusOptions"
                                            allow-clear placeholder="数据状态"
                                            style="width: 160px"/></Form.Item>
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
            <Button @click="router.push('/system/dict')">
              <IconifyIcon class="btn-icon" icon="lucide:arrow-left"/>
              返回字典类型
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
                           storage-key="system-dict-data-columns"/>
          </Space>
        </div>
        <ResizableTable :columns="visibleColumns" :data-source="dataList" :loading="loading"
                        :pagination="{ current: query.pageNum, pageSize: query.pageSize, showSizeChanger: true, showTotal: (count: number) => `共 ${count} 条`, total }"
                        :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
                        row-key="dictCode" storage-key="system-dict-data-columns"
                        @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'dictLabel'"><span
              v-if="!record.listClass || record.listClass === 'default'">{{
                record.dictLabel
              }}</span>
              <Tag v-else :class="record.cssClass" :color="tagColor(record.listClass)">
                {{ record.dictLabel }}
              </Tag>
            </template>
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
                <Button v-if="hasPermi('system:dict:remove')" danger size="small" type="link"
                        @click="handleDelete(record)">删除
                </Button>
              </Space>
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>
    <Modal v-model:open="modalOpen" :confirm-loading="saving" :title="modalTitle" width="580px"
           @ok="handleSubmit">
      <Form ref="formRef" :label-col="{ span: 5 }" :model="form" :rules="rules"
            :wrapper-col="{ span: 18 }">
        <Form.Item label="字典类型"><Input :value="form.dictType" disabled/></Form.Item>
        <Form.Item label="数据标签" name="dictLabel"><Input v-model:value="form.dictLabel"
                                                            placeholder="请输入数据标签"/>
        </Form.Item>
        <Form.Item label="数据键值" name="dictValue"><Input v-model:value="form.dictValue"
                                                            placeholder="请输入数据键值"/>
        </Form.Item>
        <Form.Item label="样式属性"><Input v-model:value="form.cssClass"
                                           placeholder="请输入样式属性"/></Form.Item>
        <Form.Item label="显示排序" name="dictSort">
          <InputNumber v-model:value="form.dictSort" :min="0" :precision="0"/>
        </Form.Item>
        <Form.Item label="回显样式"><Select v-model:value="form.listClass"
                                            :options="listClassOptions"/></Form.Item>
        <Form.Item label="状态">
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
<style scoped>.dict-data-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.dict-data-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.dict-data-page :deep(.ant-card-body) {
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
}</style>
