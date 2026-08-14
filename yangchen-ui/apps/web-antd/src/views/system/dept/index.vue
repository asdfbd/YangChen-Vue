<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';

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
  Tag,
  TreeSelect,
} from 'ant-design-vue';
import type {SysDept} from '#/api/system/dept';
import {
  addDeptApi,
  deleteDeptApi,
  getDeptApi,
  getDeptTreeSelectApi,
  listDeptApi,
  updateDeptApi,
  updateDeptSortApi,
} from '#/api/system/dept';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';

defineOptions({name: 'SystemDept'});

type Key = number | string;
type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const statusOptions = [
  {label: '正常', value: '0'},
  {label: '停用', value: '1'},
];

const loading = ref(false);
const saving = ref(false);
const showSearch = ref(true);
const deptList = ref<SysDept[]>([]);
const originalOrders = ref<Record<string, number>>({});
/** 受控展开的节点 key（默认加载后全部展开） */
const expandedRowKeys = ref<Key[]>([]);
const query = reactive({
  deptName: undefined as string | undefined,
  status: undefined as string | undefined,
});

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.includes('T') ? value.replace('T', ' ').slice(0, 19) : value.slice(0, 19);
}

/**
 * 接口在不同环境可能返回扁平列表或已组装的树。
 * 先扁平化保留所有子节点，再按 parentId 统一建树，避免覆盖后端的 children。
 */
function normalizeTree(items: SysDept[]): SysDept[] {
  const flatNodes: SysDept[] = [];
  const walk = (list: SysDept[]) => {
    for (const item of list) {
      const {children, ...node} = item;
      flatNodes.push(node);
      if (children?.length) walk(children);
    }
  };
  walk(items);

  const nodes = flatNodes.map((item) => ({...item, children: [] as SysDept[]}));
  const map = new Map(nodes.map((item) => [String(item.deptId), item]));
  const roots: SysDept[] = [];

  for (const item of nodes) {
    const parent = map.get(String(item.parentId));
    if (parent && parent !== item) {
      parent.children?.push(item);
    } else {
      roots.push(item);
    }
  }

  const cleanup = (list: SysDept[]) => {
    list.sort((a, b) => Number(a.orderNum ?? 0) - Number(b.orderNum ?? 0));
    for (const item of list) {
      if (item.children?.length) cleanup(item.children);
      else delete item.children;
    }
  };
  cleanup(roots);
  return roots;
}

function recordOriginalOrders(list: SysDept[]) {
  const orders: Record<string, number> = {};
  const walk = (items: SysDept[]) => {
    for (const item of items) {
      orders[String(item.deptId)] = Number(item.orderNum ?? 0);
      if (item.children?.length) walk(item.children);
    }
  };
  walk(list);
  originalOrders.value = orders;
}

/** 收集所有「有子节点」的节点 key，用于展开/折叠全部 */
function collectAllKeys(items: SysDept[]): Key[] {
  const keys: Key[] = [];
  const walk = (list: SysDept[]) => {
    for (const item of list) {
      if (item.children?.length) {
        keys.push(item.deptId as Key);
        walk(item.children);
      }
    }
  };
  walk(items);
  return keys;
}

async function loadData() {
  loading.value = true;
  try {
    const data = await listDeptApi(query);
    deptList.value = normalizeTree(data);
    recordOriginalOrders(deptList.value);
    // 受控展开：加载后默认全部展开（也保证搜索后重新全展开）
    expandedRowKeys.value = collectAllKeys(deptList.value);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  loadData();
}

function handleReset() {
  query.deptName = undefined;
  query.status = undefined;
  loadData();
}

function toggleExpandAll() {
  expandedRowKeys.value = expandedRowKeys.value.length
    ? []
    : collectAllKeys(deptList.value);
}

const columns = ref<Col[]>([
  {
    dataIndex: 'deptName',
    ellipsis: true,
    key: 'deptName',
    title: '部门名称',
    visible: true,
    width: 280
  },
  {align: 'center', key: 'orderNum', title: '排序', visible: true, width: 130},
  {align: 'center', key: 'status', title: '状态', visible: true, width: 110},
  {
    align: 'center',
    dataIndex: 'leader',
    ellipsis: true,
    key: 'leader',
    title: '负责人',
    visible: true,
    width: 130
  },
  {
    align: 'center',
    dataIndex: 'phone',
    ellipsis: true,
    key: 'phone',
    title: '联系电话',
    visible: true,
    width: 150
  },
  {
    align: 'center',
    dataIndex: 'createTime',
    key: 'createTime',
    title: '创建时间',
    visible: true,
    width: 180
  },
  {align: 'center', key: 'operation', resizable: false, title: '操作', visible: true, width: 190},
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
const parentOptions = ref<any[]>([]);
const form = reactive<SysDept>({});

const rules: Record<string, any> = {
  deptName: [{required: true, message: '部门名称不能为空', trigger: 'blur'}],
  orderNum: [{required: true, message: '显示排序不能为空', trigger: 'change'}],
  parentId: [{required: true, message: '上级部门不能为空', trigger: 'change'}],
  email: [{type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}],
  phone: [{pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur'}],
};

function resetForm() {
  Object.assign(form, {
    deptId: undefined,
    deptName: undefined,
    email: undefined,
    leader: undefined,
    orderNum: 0,
    parentId: 0,
    phone: undefined,
    status: '0',
  });
}

function toTreeSelectData(nodes: any[]): any[] {
  return nodes.map((node) => ({
    children: node.children?.length ? toTreeSelectData(node.children) : undefined,
    title: node.deptName ?? node.label,
    value: node.deptId ?? node.id,
  }));
}

async function loadParentOptions(deptId?: Key) {
  const data = await getDeptTreeSelectApi(deptId);
  parentOptions.value = toTreeSelectData(normalizeTree(data as SysDept[]));
}

async function openAdd(parent?: SysDept) {
  isEdit.value = false;
  modalTitle.value = '新增部门';
  resetForm();
  form.parentId = parent?.deptId ?? 0;
  await loadParentOptions();
  modalOpen.value = true;
}

async function openEdit(row: SysDept) {
  isEdit.value = true;
  modalTitle.value = '修改部门';
  resetForm();
  const [detail] = await Promise.all([getDeptApi(row.deptId as Key), loadParentOptions(row.deptId)]);
  Object.assign(form, detail, {orderNum: Number(detail.orderNum ?? 0)});
  if (parentOptions.value.length === 0 && form.parentId !== undefined) {
    parentOptions.value = [{title: detail.parentName ?? '上级部门', value: form.parentId}];
  }
  modalOpen.value = true;
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
      await updateDeptApi({...form});
      message.success('修改成功');
    } else {
      await addDeptApi({...form});
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

async function handleSaveSort() {
  const ids: Key[] = [];
  const orderNums: number[] = [];
  const walk = (items: SysDept[]) => {
    for (const item of items) {
      const current = Number(item.orderNum ?? 0);
      if (originalOrders.value[String(item.deptId)] !== current) {
        ids.push(item.deptId as Key);
        orderNums.push(current);
      }
      if (item.children?.length) walk(item.children);
    }
  };
  walk(deptList.value);
  if (ids.length === 0) {
    message.warning('未检测到排序修改');
    return;
  }
  await updateDeptSortApi({deptIds: ids.join(','), orderNums: orderNums.join(',')});
  recordOriginalOrders(deptList.value);
  message.success('排序保存成功');
}

function handleDelete(row: SysDept) {
  Modal.confirm({
    cancelText: '取消',
    content: `是否确认删除名称为“${row.deptName}”的部门？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteDeptApi(row.deptId as Key);
      message.success('删除成功');
      loadData();
    },
    title: '系统提示',
  });
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="部门管理">
    <div class="dept-page">
      <Card :bordered="false">
        <div v-show="showSearch" class="search-panel mb-4">
          <Form :model="query" class="search-form" layout="inline">
            <Form.Item label="部门名称" name="deptName">
              <Input v-model:value="query.deptName" allow-clear placeholder="请输入部门名称"
                     style="width: 220px" @press-enter="handleSearch"/>
            </Form.Item>
            <Form.Item label="状态" name="status">
              <Select v-model:value="query.status" :options="statusOptions" allow-clear
                      placeholder="部门状态" style="width: 180px"/>
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
            <Button v-if="hasPermi('system:dept:add')" type="primary" @click="openAdd()">新增部门
            </Button>
            <Button v-if="hasPermi('system:dept:edit')" @click="handleSaveSort">保存排序</Button>
            <Button @click="toggleExpandAll">{{
                expandedRowKeys.length ? '折叠全部' : '展开全部'
              }}
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
                           storage-key="system-dept-columns"/>
          </Space>
        </div>

        <ResizableTable
          v-model:expandedRowKeys="expandedRowKeys"
          :columns="visibleColumns"
          :data-source="deptList"
          :loading="loading"
          :pagination="false"
          row-key="deptId"
          storage-key="system-dept-columns"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'orderNum'">
              <InputNumber v-model:value="record.orderNum" :min="0" :precision="0" size="small"
                           style="width: 88px"/>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === '0' ? 'blue' : 'default'">
                {{ record.status === '0' ? '正常' : '停用' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createTime'">{{
                formatDateTime(record.createTime)
              }}
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="2">
                <Button v-if="hasPermi('system:dept:edit')" size="small" type="link"
                        @click="openEdit(record)">修改
                </Button>
                <Button v-if="hasPermi('system:dept:add')" size="small" type="link"
                        @click="openAdd(record)">新增下级
                </Button>
                <Button v-if="record.parentId != 0 && hasPermi('system:dept:remove')" danger
                        size="small" type="link" @click="handleDelete(record)">删除
                </Button>
              </Space>
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>

    <Modal v-model:open="modalOpen" :confirm-loading="saving" :title="modalTitle" width="680px"
           @ok="handleSubmit">
      <Form ref="formRef" :label-col="{ flex: '88px' }" :model="form" :rules="rules">
        <div class="form-grid">
          <Form.Item class="form-grid__full" label="上级部门" name="parentId">
            <TreeSelect v-model:value="form.parentId" :tree-data="parentOptions"
                        placeholder="请选择上级部门" tree-default-expand-all/>
          </Form.Item>
          <Form.Item label="部门名称" name="deptName"><Input v-model:value="form.deptName"
                                                             placeholder="请输入部门名称"/>
          </Form.Item>
          <Form.Item label="显示排序" name="orderNum">
            <InputNumber v-model:value="form.orderNum" :min="0" :precision="0" style="width: 100%"/>
          </Form.Item>
          <Form.Item label="负责人" name="leader"><Input v-model:value="form.leader" :maxlength="20"
                                                         placeholder="请输入负责人"/></Form.Item>
          <Form.Item label="联系电话" name="phone"><Input v-model:value="form.phone" :maxlength="11"
                                                          placeholder="请输入联系电话"/></Form.Item>
          <Form.Item label="邮箱" name="email"><Input v-model:value="form.email" :maxlength="50"
                                                      placeholder="请输入邮箱"/></Form.Item>
          <Form.Item label="部门状态" name="status">
            <Radio.Group v-model:value="form.status">
              <Radio v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </Radio.Group>
          </Form.Item>
        </div>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.dept-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.dept-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.dept-page :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
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

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  gap: 8px 16px;
}

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.form-grid__full {
  grid-column: 1 / -1;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid__full {
    grid-column: auto;
  }
}
</style>
