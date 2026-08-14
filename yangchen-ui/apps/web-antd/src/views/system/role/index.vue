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
  Checkbox,
  DatePicker,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Menu,
  MenuItem,
  message,
  Modal,
  Radio,
  Select,
  Space,
  Switch,
  Tooltip,
  Tree,
} from 'ant-design-vue';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';

import {getMenuTreeSelectApi, getRoleMenuTreeSelectApi,} from '#/api/system/menu';
import {
  addRoleApi,
  changeRoleStatusApi,
  dataScopeApi,
  deleteRoleApi,
  deptTreeSelectApi,
  getRoleApi,
  listRoleApi,
  type SysRole,
  type TreeSelectNode,
  updateRoleApi,
} from '#/api/system/role';
import {downloadBlob} from '#/utils/download';

defineOptions({name: 'SystemRole'});

type Key = string | number;

type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

const router = useRouter();
const accessStore = useAccessStore();

/** 权限码校验（管理员 *:*:* 拥有全部权限） */
function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const statusOptions = [
  {label: '正常', value: '0'},
  {label: '停用', value: '1'},
];

const dataScopeOptions = [
  {label: '全部数据权限', value: '1'},
  {label: '自定数据权限', value: '2'},
  {label: '本部门数据权限', value: '3'},
  {label: '本部门及以下数据权限', value: '4'},
  {label: '仅本人数据权限', value: '5'},
];

// ===== 列表 =====
const loading = ref(false);
const roleList = ref<SysRole[]>([]);
const total = ref(0);
const showSearch = ref(true);
const selectedRowKeys = ref<Key[]>([]);
const dateRange = ref<[import('dayjs').Dayjs, import('dayjs').Dayjs] | undefined>(
  undefined,
);
const query = reactive({
  beginTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
  roleKey: undefined as string | undefined,
  roleName: undefined as string | undefined,
  status: undefined as string | undefined,
});

/** 组装查询参数：创建时间作为 params.beginTime/endTime 传给后端 */
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
    const {rows, total: t} = await listRoleApi(buildQuery());
    roleList.value = rows;
    total.value = t;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function resetQuery() {
  query.beginTime = undefined;
  query.endTime = undefined;
  query.roleKey = undefined;
  query.roleName = undefined;
  query.status = undefined;
  dateRange.value = undefined;
  handleSearch();
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
    dataIndex: 'roleName',
    ellipsis: true,
    key: 'roleName',
    title: '角色名称',
    visible: true,
    width: 150,
  },
  {
    dataIndex: 'roleKey',
    ellipsis: true,
    key: 'roleKey',
    title: '权限字符',
    visible: true,
    width: 150,
  },
  {
    align: 'center' as const,
    dataIndex: 'roleSort',
    key: 'roleSort',
    title: '显示顺序',
    visible: true,
    width: 100,
  },
  {
    align: 'center' as const,
    key: 'status',
    title: '状态',
    visible: true,
    width: 100,
  },
  {
    align: 'center' as const,
    dataIndex: 'createTime',
    key: 'createTime',
    title: '创建时间',
    visible: true,
    width: 180,
  },
  {
    align: 'center' as const,
    key: 'operation',
    resizable: false,
    title: '操作',
    visible: true,
    width: 180,
  },
]);

const visibleColumns = computed(() => {
  const out: Col[] = [];
  for (const col of columns.value) {
    if (col.visible !== false) out.push(col);
  }
  return out;
});

// ===== 状态切换 =====
function handleStatusChange(row: SysRole, checked: boolean) {
  const text = checked ? '启用' : '停用';
  const original = row.status;
  Modal.confirm({
    cancelText: '取消',
    content: `确认要"${text}""${row.roleName}"角色吗？`,
    okText: '确定',
    onCancel: () => {
      // 取消时回滚开关到原状态
      row.status = original;
    },
    onOk: async () => {
      await changeRoleStatusApi(row.roleId as number, checked ? '0' : '1');
      // 成功后同步本地状态，让受控开关即时反映
      row.status = checked ? '0' : '1';
      message.success(`${text}成功`);
    },
    title: '系统提示',
  });
}

// ===== 删除（确认提示角色名称，实际传角色 ID） =====
/** 由选中的角色 ID 反查名称（用于确认提示文案） */
function selectedRoleNames(): string[] {
  return selectedRowKeys.value.map((id) => {
    const row = roleList.value.find(
      (item) => String(item.roleId) === String(id),
    );
    return row?.roleName || String(id);
  });
}

function handleDelete(row?: SysRole) {
  const roleId = row?.roleId;
  const names = row
    ? [row.roleName || String(roleId)]
    : selectedRoleNames();
  Modal.confirm({
    content: `是否确认删除角色"${names.join('、')}"的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteRoleApi(
        roleId ? [roleId] : (selectedRowKeys.value as number[]),
      );
      message.success('删除成功');
      selectedRowKeys.value = [];
      loadData();
    },
    title: '系统提示',
  });
}

// ===== 新增 / 编辑 =====
const modalOpen = ref(false);
const modalTitle = ref('');
const isEdit = ref(false);
const saving = ref(false);
const formRef = ref();
const form = reactive<SysRole>({
  deptCheckStrictly: true,
  menuCheckStrictly: true,
  roleSort: 0,
  status: '0',
});

// antd 的 RuleObject 联合类型较严格（部分成员要求 type 字段），这里宽松标注即可
const rules: Record<string, any> = {
  roleKey: [{required: true, message: '权限字符不能为空', trigger: 'blur'}],
  roleName: [{required: true, message: '角色名称不能为空', trigger: 'blur'}],
  roleSort: [{required: true, message: '角色顺序不能为空', trigger: 'blur'}],
};

// 菜单权限树
const menuOptions = ref<TreeSelectNode[]>([]);
const menuCheckedKeys = ref<Key[]>([]);
const menuHalfCheckedKeys = ref<Key[]>([]);
const menuExpandedKeys = ref<Key[]>([]);
const menuExpand = ref(false);
const menuNodeAll = ref(false);

// 数据权限部门树
const dataScopeOpen = ref(false);
const deptOptions = ref<TreeSelectNode[]>([]);
const deptCheckedKeys = ref<Key[]>([]);
const deptHalfCheckedKeys = ref<Key[]>([]);
const deptExpandedKeys = ref<Key[]>([]);
const deptExpand = ref(true);
const deptNodeAll = ref(false);

/** antd Tree 节点（RuoYi TreeSelect 的 id/label 需映射为 key/title 以满足 DataNode 类型） */
interface AntdTreeNode {
  children?: AntdTreeNode[];
  key: Key;
  title: string;
}

function toTreeData(nodes?: TreeSelectNode[]): AntdTreeNode[] {
  return (nodes ?? []).map((node) => ({
    children: toTreeData(node.children),
    key: node.id,
    title: node.label,
  }));
}

const menuTreeData = computed(() => toTreeData(menuOptions.value));
const deptTreeData = computed(() => toTreeData(deptOptions.value));

/** 收集树所有节点 key */
function collectAllTreeKeys(nodes: TreeSelectNode[]): Key[] {
  const keys: Key[] = [];
  const walk = (list: TreeSelectNode[]) => {
    for (const node of list) {
      keys.push(node.id);
      if (node.children?.length) walk(node.children);
    }
  };
  walk(nodes);
  return keys;
}

// antd Tree 受控 checkedKeys：checkStrictly=true 时需传 {checked, halfChecked} 对象
const menuTreeCheckedKeys = computed(() =>
  form.menuCheckStrictly
    ? menuCheckedKeys.value
    : {checked: menuCheckedKeys.value, halfChecked: menuHalfCheckedKeys.value},
);

const deptTreeCheckedKeys = computed(() =>
  form.deptCheckStrictly
    ? deptCheckedKeys.value
    : {checked: deptCheckedKeys.value, halfChecked: deptHalfCheckedKeys.value},
);

function handleMenuCheck(
  keys: Key[] | { checked: Key[]; halfChecked: Key[] },
  info: { halfCheckedKeys?: Key[] },
) {
  if (Array.isArray(keys)) {
    menuCheckedKeys.value = keys;
    menuHalfCheckedKeys.value = info?.halfCheckedKeys ?? [];
  } else {
    menuCheckedKeys.value = keys.checked ?? [];
    menuHalfCheckedKeys.value = keys.halfChecked ?? [];
  }
}

function handleDeptCheck(
  keys: Key[] | { checked: Key[]; halfChecked: Key[] },
  info: { halfCheckedKeys?: Key[] },
) {
  if (Array.isArray(keys)) {
    deptCheckedKeys.value = keys;
    deptHalfCheckedKeys.value = info?.halfCheckedKeys ?? [];
  } else {
    deptCheckedKeys.value = keys.checked ?? [];
    deptHalfCheckedKeys.value = keys.halfChecked ?? [];
  }
}

function handleMenuExpandAll(expanded: boolean) {
  menuExpand.value = expanded;
  menuExpandedKeys.value = expanded
    ? collectAllTreeKeys(menuOptions.value)
    : [];
}

function handleMenuCheckAll(checked: boolean) {
  menuNodeAll.value = checked;
  menuCheckedKeys.value = checked ? collectAllTreeKeys(menuOptions.value) : [];
  menuHalfCheckedKeys.value = [];
}

function handleDeptExpandAll(expanded: boolean) {
  deptExpand.value = expanded;
  deptExpandedKeys.value = expanded
    ? collectAllTreeKeys(deptOptions.value)
    : [];
}

function handleDeptCheckAll(checked: boolean) {
  deptNodeAll.value = checked;
  deptCheckedKeys.value = checked ? collectAllTreeKeys(deptOptions.value) : [];
  deptHalfCheckedKeys.value = [];
}

function resetForm() {
  Object.assign(form, {
    dataScope: '1',
    deptCheckStrictly: true,
    menuCheckStrictly: true,
    remark: undefined,
    roleId: undefined,
    roleKey: undefined,
    roleName: undefined,
    roleSort: 0,
    status: '0',
  });
  menuCheckedKeys.value = [];
  menuHalfCheckedKeys.value = [];
  menuExpandedKeys.value = [];
  menuExpand.value = false;
  menuNodeAll.value = false;
  deptCheckedKeys.value = [];
  deptHalfCheckedKeys.value = [];
  deptExpandedKeys.value = [];
  deptExpand.value = true;
  deptNodeAll.value = false;
}

async function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增角色';
  resetForm();
  menuOptions.value = await getMenuTreeSelectApi();
  modalOpen.value = true;
}

async function openEdit(row: SysRole) {
  isEdit.value = true;
  modalTitle.value = '修改角色';
  resetForm();
  const roleId = row.roleId as number;
  const [roleRes, menuRes] = await Promise.all([
    getRoleApi(roleId),
    getRoleMenuTreeSelectApi(roleId),
  ]);
  Object.assign(form, roleRes);
  // 后端 Long 序列化为字符串，InputNumber 需要数字
  form.roleSort = Number(roleRes.roleSort ?? 0);
  menuOptions.value = menuRes.menus;
  menuCheckedKeys.value = menuRes.checkedKeys;
  menuHalfCheckedKeys.value = [];
  modalOpen.value = true;
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  const payload: SysRole = {
    menuIds: [...menuCheckedKeys.value, ...menuHalfCheckedKeys.value] as number[],
    remark: form.remark,
    roleId: form.roleId,
    roleKey: form.roleKey,
    roleName: form.roleName,
    roleSort: form.roleSort,
    status: form.status,
  };
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateRoleApi(payload);
      message.success('修改成功');
    } else {
      await addRoleApi(payload);
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

// ===== 数据权限 =====
async function openDataScope(row: SysRole) {
  resetForm();
  const roleId = row.roleId as number;
  const [roleRes, deptRes] = await Promise.all([
    getRoleApi(roleId),
    deptTreeSelectApi(roleId),
  ]);
  Object.assign(form, roleRes);
  deptOptions.value = deptRes.depts;
  deptCheckedKeys.value = deptRes.checkedKeys;
  deptHalfCheckedKeys.value = [];
  deptExpandedKeys.value = collectAllTreeKeys(deptOptions.value);
  dataScopeOpen.value = true;
}

function handleDataScopeChange(value: any) {
  if (value !== '2') {
    deptCheckedKeys.value = [];
    deptHalfCheckedKeys.value = [];
  }
}

async function handleSubmitDataScope() {
  if (form.roleId == null) return;
  saving.value = true;
  try {
    await dataScopeApi({
      dataScope: form.dataScope,
      deptIds: [...deptCheckedKeys.value, ...deptHalfCheckedKeys.value] as number[],
      roleId: form.roleId,
    });
    message.success('修改成功');
    dataScopeOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

// ===== 更多操作 =====
function handleMore(row: SysRole, command: string) {
  if (command === 'dataScope') {
    openDataScope(row);
  } else if (command === 'authUser') {
    router.push(`/system/role-auth/user/${row.roleId}`);
  }
}

function handleUpdate() {
  const id = selectedRowKeys.value[0];
  const row = roleList.value.find((item) => item.roleId === id);
  if (row) openEdit(row);
}

// ===== 导出 =====
async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `role_${Date.now()}.xlsx`,
      path: '/system/role/export',
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message ?? '导出失败');
  }
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="角色管理">
    <div class="role-page">
      <Card>
        <!-- 搜索区（可折叠） -->
        <div v-show="showSearch" class="search-panel mb-4">
          <Form
            :model="query"
            class="search-form"
            layout="inline"
            @submit.prevent
          >
            <Form.Item label="角色名称" name="roleName">
              <Input
                v-model:value="query.roleName"
                allow-clear
                placeholder="请输入角色名称"
                style="width: 200px"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="权限字符" name="roleKey">
              <Input
                v-model:value="query.roleKey"
                allow-clear
                placeholder="请输入权限字符"
                style="width: 200px"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="状态" name="status">
              <Select
                v-model:value="query.status"
                :options="statusOptions"
                allow-clear
                placeholder="角色状态"
                style="width: 200px"
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
                <Button @click="resetQuery">重置</Button>
              </Space>
            </Form.Item>
          </Form>
        </div>

        <!-- 工具栏：左侧操作组 + 右侧视图控制组 -->
        <div class="toolbar mb-4">
          <Space :size="12">
            <Button
              v-if="hasPermi('system:role:add')"
              type="primary"
              @click="openAdd"
            >
              <IconifyIcon class="btn-icon" icon="lucide:plus"/>
              新增
            </Button>
            <Button
              v-if="hasPermi('system:role:edit')"
              :disabled="selectedRowKeys.length !== 1"
              @click="handleUpdate"
            >
              <IconifyIcon class="btn-icon" icon="lucide:pencil"/>
              修改
            </Button>
            <Button
              v-if="hasPermi('system:role:remove')"
              :disabled="selectedRowKeys.length === 0"
              danger
              @click="handleDelete()"
            >
              <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
              删除
            </Button>
            <Button
              v-if="hasPermi('system:role:export')"
              @click="handleExport"
            >
              <IconifyIcon class="btn-icon" icon="lucide:download"/>
              导出
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
              storage-key="system-role-columns"
            />
          </Space>
        </div>

        <!-- 表格（列支持拖拽调宽，宽度持久化到 localStorage） -->
        <ResizableTable
          :columns="visibleColumns"
          :data-source="roleList"
          :loading="loading"
          :pagination="{
          current: query.pageNum,
          pageSize: query.pageSize,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          total: total,
        }"
          :row-selection="{
          selectedRowKeys,
          onChange: handleSelectionChange,
        }"
          row-key="roleId"
          storage-key="system-role-columns"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Switch
                :checked="record.status === '0'"
                :disabled="Number(record.roleId) === 1 || !hasPermi('system:role:edit')"
                @change="(checked) => handleStatusChange(record, Boolean(checked))"
              />
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'operation'">
              <!-- 超级管理员（roleId=1）不允许操作 -->
              <Space :size="8">
                <Button
                  v-if="hasPermi('system:role:edit')"
                  size="small"
                  type="link"
                  @click="openEdit(record)"
                >
                  修改
                </Button>
                <Button
                  v-if="hasPermi('system:role:remove') && Number(record.roleId) !== 1"
                  danger
                  size="small"
                  type="link"
                  @click="handleDelete(record)"
                >
                  删除
                </Button>
                <Dropdown v-if="hasPermi('system:role:edit')">
                  <Button size="small" type="link">
                    更多<span class="caret">▾</span>
                  </Button>
                  <template #overlay>
                    <Menu
                      @click="({ key }) => handleMore(record, String(key))"
                    >
                      <MenuItem key="dataScope">数据权限</MenuItem>
                      <MenuItem key="authUser">分配用户</MenuItem>
                    </Menu>
                  </template>
                </Dropdown>
              </Space>
            </template>
          </template>
        </ResizableTable>
      </Card>
    </div>

    <!-- 新增 / 修改角色 -->
    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      width="600px"
      @ok="handleSubmit"
    >
      <Form
        ref="formRef"
        :label-col="{ span: 5 }"
        :model="form"
        :rules="rules"
        :wrapper-col="{ span: 18 }"
      >
        <Form.Item label="角色名称" prop="roleName">
          <Input v-model:value="form.roleName" placeholder="请输入角色名称"/>
        </Form.Item>
        <Form.Item label="权限字符" prop="roleKey">
          <template #label>
            <span class="inline-flex items-center">
              权限字符
              <Tooltip
                title="控制器中定义的权限字符，如：@PreAuthorize(`@ss.hasRole('admin')`)"
              >
                <span class="question-icon">?</span>
              </Tooltip>
            </span>
          </template>
          <Input v-model:value="form.roleKey" placeholder="请输入权限字符"/>
        </Form.Item>
        <Form.Item label="角色顺序" prop="roleSort">
          <InputNumber
            v-model:value="form.roleSort"
            :min="0"
            :precision="0"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Radio.Group v-model:value="form.status">
            <Radio
              v-for="opt in statusOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="菜单权限">
          <Space class="mb-2">
            <Checkbox
              v-model:checked="menuExpand"
              @change="(e) => handleMenuExpandAll(Boolean(e?.target?.checked))"
            >
              展开/折叠
            </Checkbox>
            <Checkbox
              v-model:checked="menuNodeAll"
              @change="(e) => handleMenuCheckAll(Boolean(e?.target?.checked))"
            >
              全选/全不选
            </Checkbox>
            <Checkbox v-model:checked="form.menuCheckStrictly">
              父子联动
            </Checkbox>
          </Space>
          <div class="tree-border">
            <Tree
              :check-strictly="!form.menuCheckStrictly"
              :checked-keys="menuTreeCheckedKeys"
              :expanded-keys="menuExpandedKeys"
              :selectable="false"
              :tree-data="menuTreeData"
              checkable
              @check="handleMenuCheck"
              @expand="(keys: Key[]) => (menuExpandedKeys = keys)"
            />
          </div>
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="form.remark"
            placeholder="请输入内容"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 分配数据权限 -->
    <Modal
      v-model:open="dataScopeOpen"
      :confirm-loading="saving"
      :title="'分配数据权限'"
      width="560px"
      @ok="handleSubmitDataScope"
    >
      <Form
        :label-col="{ span: 6 }"
        :model="form"
        :wrapper-col="{ span: 16 }"
      >
        <Form.Item label="角色名称">
          <Input :value="form.roleName" disabled/>
        </Form.Item>
        <Form.Item label="权限字符">
          <Input :value="form.roleKey" disabled/>
        </Form.Item>
        <Form.Item label="权限范围">
          <Select
            v-model:value="form.dataScope"
            :options="dataScopeOptions"
            @change="handleDataScopeChange"
          />
        </Form.Item>
        <Form.Item v-show="form.dataScope === '2'" label="数据权限">
          <Space class="mb-2">
            <Checkbox
              v-model:checked="deptExpand"
              @change="(e) => handleDeptExpandAll(Boolean(e?.target?.checked))"
            >
              展开/折叠
            </Checkbox>
            <Checkbox
              v-model:checked="deptNodeAll"
              @change="(e) => handleDeptCheckAll(Boolean(e?.target?.checked))"
            >
              全选/全不选
            </Checkbox>
            <Checkbox v-model:checked="form.deptCheckStrictly">
              父子联动
            </Checkbox>
          </Space>
          <div class="tree-border">
            <Tree
              :check-strictly="!form.deptCheckStrictly"
              :checked-keys="deptTreeCheckedKeys"
              :expanded-keys="deptExpandedKeys"
              :selectable="false"
              :tree-data="deptTreeData"
              checkable
              @check="handleDeptCheck"
              @expand="(keys: Key[]) => (deptExpandedKeys = keys)"
            />
          </div>
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
/* 占满整个内容区高度：卡片撑满、表格区内部滚动（与用户管理一致） */
.role-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.role-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.role-page :deep(.ant-card-body) {
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

.tree-border {
  max-height: 260px;
  overflow: auto;
  padding: 6px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.question-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-left: 4px;
  color: #fff;
  font-size: 12px;
  background: #8c8c8c;
  border-radius: 50%;
  cursor: help;
}

.caret {
  margin-left: 2px;
  font-size: 12px;
}

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
