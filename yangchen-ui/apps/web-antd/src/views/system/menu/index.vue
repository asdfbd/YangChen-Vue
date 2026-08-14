<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';

import {IconPicker, Page} from '@vben/common-ui';
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

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';

import type {SysMenu} from '#/api/system/menu';
import {
  addMenuApi,
  deleteMenuApi,
  getMenuApi,
  listMenuApi,
  updateMenuApi,
  updateMenuSortApi,
} from '#/api/system/menu';

defineOptions({name: 'SystemMenu'});

type Col = TableColumnType & { resizable?: boolean; visible?: boolean };
type Key = number | string;

const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const statusOptions = [
  {label: '正常', value: '0'},
  {label: '停用', value: '1'},
];

const visibilityOptions = [
  {label: '显示', value: '0'},
  {label: '隐藏', value: '1'},
];

const menuTypeOptions = [
  {label: '目录', value: 'M'},
  {label: '菜单', value: 'C'},
  {label: '按钮', value: 'F'},
];

const loading = ref(false);
const saving = ref(false);
const showSearch = ref(true);
const menuList = ref<SysMenu[]>([]);
const originalOrders = ref<Record<string, number>>({});
const expandedRowKeys = ref<Key[]>([]);
const modalOpen = ref(false);
const modalTitle = ref('');
const isEdit = ref(false);
const formRef = ref();
const parentOptions = ref<any[]>([]);

const query = reactive({
  menuName: undefined as string | undefined,
  status: undefined as string | undefined,
});

const form = reactive<SysMenu>({});

const rules: Record<string, any> = {
  menuName: [{required: true, message: '菜单名称不能为空', trigger: 'blur'}],
  orderNum: [{required: true, message: '显示排序不能为空', trigger: 'change'}],
  parentId: [{required: true, message: '上级菜单不能为空', trigger: 'change'}],
  path: [{required: true, message: '路由地址不能为空', trigger: 'blur'}],
};

const columns = ref<Col[]>([
  {
    dataIndex: 'menuName',
    ellipsis: true,
    key: 'menuName',
    title: '菜单名称',
    visible: true,
    width: 260
  },
  {align: 'center', key: 'menuType', title: '类型', visible: true, width: 110},
  {align: 'center', key: 'orderNum', title: '排序', visible: true, width: 120},
  {dataIndex: 'perms', ellipsis: true, key: 'perms', title: '权限标识', visible: true, width: 210},
  {
    dataIndex: 'component',
    ellipsis: true,
    key: 'component',
    title: '组件路径',
    visible: true,
    width: 220
  },
  {align: 'center', key: 'status', title: '状态', visible: true, width: 100},
  {align: 'center', key: 'operation', resizable: false, title: '操作', visible: true, width: 190},
]);

const visibleColumns = computed(() => columns.value.filter((column) => column.visible !== false));

function normalizeTree(items: SysMenu[]): SysMenu[] {
  const flat: SysMenu[] = [];
  const collect = (list: SysMenu[]) => {
    for (const item of list) {
      const {children, ...node} = item;
      flat.push(node);
      if (children?.length) collect(children);
    }
  };
  collect(items);

  const nodes = flat.map((item) => ({...item, children: [] as SysMenu[]}));
  const nodeMap = new Map(nodes.map((item) => [String(item.menuId), item]));
  const roots: SysMenu[] = [];
  for (const item of nodes) {
    const parent = nodeMap.get(String(item.parentId));
    if (parent && parent !== item) parent.children?.push(item);
    else roots.push(item);
  }

  const sortAndCleanup = (list: SysMenu[]) => {
    list.sort((left, right) => Number(left.orderNum ?? 0) - Number(right.orderNum ?? 0));
    for (const item of list) {
      if (item.children?.length) sortAndCleanup(item.children);
      else delete item.children;
    }
  };
  sortAndCleanup(roots);
  return roots;
}

function collectParentKeys(items: SysMenu[]): Key[] {
  const keys: Key[] = [];
  const walk = (list: SysMenu[]) => {
    for (const item of list) {
      if (item.children?.length) {
        keys.push(item.menuId as Key);
        walk(item.children);
      }
    }
  };
  walk(items);
  return keys;
}

function recordOriginalOrders(items: SysMenu[]) {
  const orders: Record<string, number> = {};
  const walk = (list: SysMenu[]) => {
    for (const item of list) {
      orders[String(item.menuId)] = Number(item.orderNum ?? 0);
      if (item.children?.length) walk(item.children);
    }
  };
  walk(items);
  originalOrders.value = orders;
}

async function loadData() {
  loading.value = true;
  try {
    const data = await listMenuApi(query);
    menuList.value = normalizeTree(data);
    recordOriginalOrders(menuList.value);
    expandedRowKeys.value = collectParentKeys(menuList.value);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  loadData();
}

function handleReset() {
  query.menuName = undefined;
  query.status = undefined;
  loadData();
}

function toggleExpandAll() {
  expandedRowKeys.value = expandedRowKeys.value.length > 0 ? [] : collectParentKeys(menuList.value);
}

function resetForm() {
  Object.assign(form, {
    alwaysShow: '0',
    component: undefined,
    icon: undefined,
    isCache: '0',
    isFrame: '1',
    menuId: undefined,
    menuName: undefined,
    menuType: 'M',
    orderNum: 0,
    parentId: 0,
    path: undefined,
    perms: undefined,
    query: undefined,
    routeName: undefined,
    status: '0',
    visible: '0',
  });
}

function descendantsOf(menuId: Key, items: SysMenu[]) {
  const result = new Set<string>([String(menuId)]);
  const find = (list: SysMenu[]) => {
    for (const item of list) {
      if (String(item.parentId) === String(menuId)) {
        result.add(String(item.menuId));
        find(item.children ?? []);
      } else if (item.children?.length) {
        find(item.children);
      }
    }
  };
  find(items);
  return result;
}

function toTreeSelectData(items: SysMenu[], excludeIds = new Set<string>()): any[] {
  return items
    .filter((item) => !excludeIds.has(String(item.menuId)))
    .map((item) => ({
      children: item.children?.length ? toTreeSelectData(item.children, excludeIds) : undefined,
      title: item.menuName,
      value: item.menuId,
    }));
}

async function loadParentOptions(menuId?: Key) {
  const data = normalizeTree(await listMenuApi());
  const root = {
    children: toTreeSelectData(data, menuId === undefined ? new Set() : descendantsOf(menuId, data)),
    title: '主类目',
    value: 0
  };
  parentOptions.value = [root];
}

async function openAdd(parent?: SysMenu) {
  isEdit.value = false;
  modalTitle.value = '新增菜单';
  resetForm();
  form.parentId = parent?.menuId ?? 0;
  await loadParentOptions();
  modalOpen.value = true;
}

async function openEdit(row: SysMenu) {
  isEdit.value = true;
  modalTitle.value = '修改菜单';
  resetForm();
  const [detail] = await Promise.all([getMenuApi(row.menuId as Key), loadParentOptions(row.menuId as Key)]);
  Object.assign(form, detail, {orderNum: Number(detail.orderNum ?? 0)});
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
    const payload = {...form};
    if (payload.menuType === 'F') {
      payload.component = undefined;
      payload.isCache = '1';
      payload.isFrame = '1';
      payload.path = undefined;
      payload.visible = '0';
    } else if (payload.menuType === 'M') {
      payload.component = undefined;
      payload.isCache = '1';
      payload.perms = undefined;
      payload.query = undefined;
      payload.routeName = undefined;
    }
    if (isEdit.value) {
      await updateMenuApi(payload);
      message.success('修改成功');
    } else {
      await addMenuApi(payload);
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
  const walk = (items: SysMenu[]) => {
    for (const item of items) {
      const current = Number(item.orderNum ?? 0);
      if (originalOrders.value[String(item.menuId)] !== current) {
        ids.push(item.menuId as Key);
        orderNums.push(current);
      }
      if (item.children?.length) walk(item.children);
    }
  };
  walk(menuList.value);
  if (ids.length === 0) {
    message.warning('未检测到排序修改');
    return;
  }
  await updateMenuSortApi({menuIds: ids.join(','), orderNums: orderNums.join(',')});
  recordOriginalOrders(menuList.value);
  message.success('排序保存成功');
}

function handleDelete(row: SysMenu) {
  Modal.confirm({
    cancelText: '取消',
    content: `是否确认删除名称为“${row.menuName}”的菜单？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteMenuApi(row.menuId as Key);
      message.success('删除成功');
      loadData();
    },
    title: '系统提示',
  });
}

function getMenuType(row: SysMenu) {
  if (row.menuType === 'F') return {color: 'gold', text: '按钮'};
  if (row.isFrame === '0') return {color: 'red', text: '外链'};
  return row.menuType === 'M' ? {color: 'blue', text: '目录'} : {color: 'green', text: '菜单'};
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="菜单管理">
    <div class="menu-page">
      <Card :bordered="false">
        <div v-show="showSearch" class="search-panel mb-4">
          <Form :model="query" class="search-form" layout="inline">
            <Form.Item label="菜单名称" name="menuName">
              <Input v-model:value="query.menuName" allow-clear placeholder="请输入菜单名称"
                     style="width: 220px" @press-enter="handleSearch"/>
            </Form.Item>
            <Form.Item label="状态" name="status">
              <Select v-model:value="query.status" :options="statusOptions" allow-clear
                      placeholder="菜单状态" style="width: 180px"/>
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
            <Button v-if="hasPermi('system:menu:add')" type="primary" @click="openAdd()">
              <IconifyIcon class="btn-icon" icon="lucide:plus"/>
              新增菜单
            </Button>
            <Button v-if="hasPermi('system:menu:edit')" @click="handleSaveSort">
              <IconifyIcon class="btn-icon" icon="lucide:save"/>
              保存排序
            </Button>
            <Button @click="toggleExpandAll">
              <IconifyIcon
                :icon="expandedRowKeys.length ? 'lucide:chevrons-up' : 'lucide:chevrons-down'"
                class="btn-icon"/>
              {{ expandedRowKeys.length ? '折叠全部' : '展开全部' }}
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
                           storage-key="system-menu-columns"/>
          </Space>
        </div>

        <ResizableTable v-model:expandedRowKeys="expandedRowKeys" :columns="visibleColumns"
                        :data-source="menuList" :loading="loading" :pagination="false"
                        row-key="menuId" storage-key="system-menu-columns">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'menuName'">
              <Space :size="6">
                <IconifyIcon v-if="record.icon" :icon="record.icon"/>
                <span>{{ record.menuName }}</span></Space>
            </template>
            <template v-else-if="column.key === 'menuType'">
              <Tag :color="getMenuType(record).color">{{ getMenuType(record).text }}</Tag>
            </template>
            <template v-else-if="column.key === 'orderNum'">
              <InputNumber v-model:value="record.orderNum" :min="0" :precision="0" size="small"
                           style="width: 88px"/>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === '0' ? 'blue' : 'default'">
                {{ record.status === '0' ? '正常' : '停用' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="2">
                <Button v-if="hasPermi('system:menu:edit')" size="small" type="link"
                        @click="openEdit(record)">修改
                </Button>
                <Button v-if="hasPermi('system:menu:add')" size="small" type="link"
                        @click="openAdd(record)">新增下级
                </Button>
                <Button v-if="hasPermi('system:menu:remove')" danger size="small" type="link"
                        @click="handleDelete(record)">删除
                </Button>
              </Space>
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>

    <Modal v-model:open="modalOpen" :confirm-loading="saving" :title="modalTitle" width="760px"
           @ok="handleSubmit">
      <Form ref="formRef" :label-col="{ flex: '88px' }" :model="form" :rules="rules">
        <div class="form-grid">
          <Form.Item class="form-grid__full" label="上级菜单" name="parentId">
            <TreeSelect v-model:value="form.parentId" :tree-data="parentOptions"
                        placeholder="请选择上级菜单" tree-default-expand-all/>
          </Form.Item>
          <Form.Item class="form-grid__full" label="菜单类型" name="menuType">
            <Radio.Group v-model:value="form.menuType">
              <Radio v-for="option in menuTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item v-if="form.menuType !== 'F'" label="菜单图标" name="icon">
            <IconPicker
              :model-value="form.icon ?? ''"
              prefix="lucide"
              @update:model-value="form.icon = $event"
            />
          </Form.Item>
          <Form.Item label="显示排序" name="orderNum">
            <InputNumber v-model:value="form.orderNum" :min="0" :precision="0" style="width: 100%"/>
          </Form.Item>
          <Form.Item label="菜单名称" name="menuName"><Input v-model:value="form.menuName"
                                                             :maxlength="50"
                                                             placeholder="请输入菜单名称"/>
          </Form.Item>
          <Form.Item v-if="form.menuType === 'C'" label="路由名称" name="routeName"><Input
            v-model:value="form.routeName" :maxlength="50" placeholder="请输入路由名称"/>
          </Form.Item>
          <Form.Item v-if="form.menuType !== 'F'" label="是否外链" name="isFrame">
            <Radio.Group v-model:value="form.isFrame">
              <Radio value="0">是</Radio>
              <Radio value="1">否</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item v-if="form.menuType !== 'F'" label="路由地址" name="path"><Input
            v-model:value="form.path" placeholder="请输入路由地址"/></Form.Item>
          <Form.Item v-if="form.menuType === 'C'" label="组件路径" name="component"><Input
            v-model:value="form.component" placeholder="例如 system/user/index"/></Form.Item>
          <Form.Item v-if="form.menuType !== 'M'" label="权限字符" name="perms"><Input
            v-model:value="form.perms" :maxlength="100" placeholder="请输入权限字符"/></Form.Item>
          <Form.Item v-if="form.menuType === 'C'" label="路由参数" name="query"><Input
            v-model:value="form.query" :maxlength="255" placeholder='例如 {"id": 1}'/></Form.Item>
          <Form.Item v-if="form.menuType === 'C'" label="是否缓存" name="isCache">
            <Radio.Group v-model:value="form.isCache">
              <Radio value="0">缓存</Radio>
              <Radio value="1">不缓存</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item v-if="form.menuType !== 'F'" label="显示状态" name="visible">
            <Radio.Group v-model:value="form.visible">
              <Radio v-for="option in visibilityOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="菜单状态" name="status">
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
.menu-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.menu-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.menu-page :deep(.ant-card-body) {
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
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid__full {
    grid-column: auto;
  }
}
</style>
