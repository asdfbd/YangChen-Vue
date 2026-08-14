<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';

import {useAppConfig} from '@vben/hooks';
import {IconifyIcon} from '@vben/icons';
import {Page} from '@vben/common-ui';
import {useAccessStore} from '@vben/stores';

import type {TableColumnType} from 'ant-design-vue';
import {
  Button,
  Card,
  Checkbox,
  DatePicker,
  Descriptions,
  Divider,
  Drawer,
  Form,
  Input,
  message,
  Modal,
  Radio,
  Select,
  Space,
  Switch,
  Tag,
  TreeSelect,
  Upload,
} from 'ant-design-vue';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';
import TreePanel from '#/components/tree-panel/index.vue';

import {
  addUserApi,
  changeUserStatusApi,
  deleteUserApi,
  getDeptTreeApi,
  getUserApi,
  getUserListApi,
  resetUserPwdApi,
  type SysUser,
  type SysUserInfo,
  updateUserApi,
} from '#/api/system/user';
import type {SysPost} from '#/api/system/post';
import type {SysRole, TreeSelectNode} from '#/api/system/role';
import {downloadBlob} from '#/utils/download';

defineOptions({name: 'SystemUser'});

const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);
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

const sexOptions = [
  {label: '男', value: '0'},
  {label: '女', value: '1'},
  {label: '未知', value: '2'},
];

const sexMap: Record<string, string> = {'0': '男', '1': '女', '2': '未知'};
const statusMap: Record<string, string> = {'0': '正常', '1': '停用'};

type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

// ===== 列表 =====
const loading = ref(false);
const userList = ref<SysUser[]>([]);
const total = ref(0);
const showSearch = ref(true);
const selectedRowKeys = ref<string[]>([]);
const deptOptions = ref<TreeSelectNode[]>([]);
const dateRange = ref<[import('dayjs').Dayjs, import('dayjs').Dayjs] | undefined>(
  undefined,
);

const query = reactive({
  beginTime: undefined as string | undefined,
  deptId: undefined as string | undefined,
  endTime: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
  phonenumber: undefined as string | undefined,
  status: undefined as string | undefined,
  userName: undefined as string | undefined,
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
    const {rows, total: t} = await getUserListApi(buildQuery());
    userList.value = rows;
    total.value = t;
  } finally {
    loading.value = false;
  }
}

async function loadDeptTree() {
  deptOptions.value = await getDeptTreeApi();
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

/** 重置：清空搜索字段、部门树选中并刷新 */
function handleReset() {
  query.userName = undefined;
  query.phonenumber = undefined;
  query.status = undefined;
  query.beginTime = undefined;
  query.endTime = undefined;
  query.deptId = undefined;
  dateRange.value = undefined;
  query.pageNum = 1;
  loadData();
}

function handleDateRangeChange(_dates: unknown, dateStrings: [string, string]) {
  query.beginTime = dateStrings?.[0] || undefined;
  query.endTime = dateStrings?.[1] || undefined;
}

function handleNodeClick(node: TreeSelectNode) {
  query.deptId = String(node.id);
  handleSearch();
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? 10;
  loadData();
}

function handleSelectionChange(keys: string[]) {
  selectedRowKeys.value = keys;
}

const columns = ref<Col[]>([
  {
    dataIndex: 'userName',
    ellipsis: true,
    key: 'userName',
    title: '用户名称',
    visible: true,
    width: 140,
  },
  {
    dataIndex: 'nickName',
    ellipsis: true,
    key: 'nickName',
    title: '用户昵称',
    visible: true,
    width: 130,
  },
  {
    dataIndex: ['dept', 'deptName'],
    ellipsis: true,
    key: 'dept',
    title: '部门',
    visible: true,
    width: 150,
  },
  {
    dataIndex: 'phonenumber',
    key: 'phonenumber',
    title: '手机号码',
    visible: true,
    width: 130,
  },
  {
    align: 'center' as const,
    key: 'status',
    title: '状态',
    visible: true,
    width: 90,
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
    key: 'operation',
    resizable: false,
    title: '操作',
    visible: true,
    width: 200,
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
function handleStatusChange(row: SysUser, checked: boolean) {
  const text = checked ? '启用' : '停用';
  const original = row.status;
  Modal.confirm({
    cancelText: '取消',
    content: `确认要"${text}""${row.userName}"用户吗？`,
    okText: '确定',
    onCancel: () => {
      // 取消时回滚开关到原状态
      row.status = original;
    },
    onOk: async () => {
      await changeUserStatusApi(row.userId as number, checked ? '0' : '1');
      row.status = checked ? '0' : '1';
      message.success(`${text}成功`);
    },
    title: '系统提示',
  });
}

// ===== 删除（确认提示用户名称，实际传用户 ID） =====
/** 由选中的用户 ID 反查名称（用于确认提示文案） */
function selectedUserNames(): string[] {
  return (selectedRowKeys.value as string[]).map((id) => {
    const row = userList.value.find(
      (item) => String(item.userId) === String(id),
    );
    return row?.nickName || row?.userName || String(id);
  });
}

function handleDelete(row?: SysUser) {
  const userIds = row
    ? [String(row.userId)]
    : (selectedRowKeys.value as string[]);
  const names = row
    ? [row.nickName || row.userName || String(row.userId)]
    : selectedUserNames();
  Modal.confirm({
    content: `是否确认删除用户"${names.join('、')}"的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteUserApi(userIds);
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
const roleOptions = ref<SysRole[]>([]);
const postOptions = ref<SysPost[]>([]);

const form = reactive<Record<string, any>>({
  deptId: undefined,
  email: undefined,
  nickName: undefined,
  password: undefined,
  phonenumber: undefined,
  postIds: [],
  remark: undefined,
  roleIds: [],
  sex: '0',
  status: '0',
  userId: undefined,
  userName: undefined,
});

const rules: Record<string, any> = {
  email: [
    {type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change']},
  ],
  nickName: [{required: true, message: '用户昵称不能为空', trigger: 'blur'}],
  password: [{required: true, message: '用户密码不能为空', trigger: 'blur'}],
  phonenumber: [
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur'},
  ],
  userName: [{required: true, message: '用户名称不能为空', trigger: 'blur'}],
};

/** 归属部门树选择数据（停用部门禁用不可选） */
const deptTreeSelectData = computed(() => toTreeSelectData(deptOptions.value));

function toTreeSelectData(nodes: TreeSelectNode[]): any[] {
  return (nodes ?? []).map((node) => ({
    children: node.children?.length ? toTreeSelectData(node.children) : undefined,
    disabled: node.disabled,
    title: node.label,
    value: node.id,
  }));
}

const roleSelectOptions = computed(() =>
  roleOptions.value.map((role) => ({
    disabled: role.status === '1',
    label: role.roleName ?? '',
    value: role.roleId,
  })),
);

const postSelectOptions = computed(() =>
  postOptions.value.map((post) => ({
    disabled: post.status === '1',
    label: post.postName ?? '',
    value: post.postId,
  })),
);

function resetForm() {
  Object.assign(form, {
    deptId: undefined,
    email: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    postIds: [],
    remark: undefined,
    roleIds: [],
    sex: '0',
    status: '0',
    userId: undefined,
    userName: undefined,
  });
  formRef.value?.clearValidate?.();
}

async function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增用户';
  resetForm();
  const info = await getUserApi();
  roleOptions.value = info.roles;
  postOptions.value = info.posts;
  modalOpen.value = true;
}

async function openEdit(row: SysUser) {
  isEdit.value = true;
  modalTitle.value = '修改用户';
  resetForm();
  const info = await getUserApi(row.userId as number);
  Object.assign(form, info.data, {
    postIds: info.postIds ?? [],
    roleIds: info.roleIds ?? [],
  });
  roleOptions.value = info.roles;
  postOptions.value = info.posts;
  modalOpen.value = true;
}

function handleUpdate() {
  const id = selectedRowKeys.value[0];
  const row = userList.value.find((item) => String(item.userId) === String(id));
  if (row) openEdit(row);
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  const payload: Record<string, any> = {...form};
  // 修改场景不涉及密码（后端对 null 密码不更新）
  if (isEdit.value) delete payload.password;
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateUserApi(payload);
      message.success('修改成功');
    } else {
      await addUserApi(payload);
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

// ===== 重置密码 =====
const resetOpen = ref(false);
const resetUserId = ref<number | string | undefined>(undefined);
const resetUserName = ref('');
const resetPassword = ref('');
const resetSaving = ref(false);

function openResetPwd(row: SysUser) {
  resetUserId.value = row.userId;
  resetUserName.value = row.userName ?? '';
  resetPassword.value = '';
  resetOpen.value = true;
}

async function handleResetPwd() {
  if (!resetPassword.value) {
    message.warning('请输入新密码');
    return;
  }
  resetSaving.value = true;
  try {
    await resetUserPwdApi(resetUserId.value as number, resetPassword.value);
    message.success(`修改成功，新密码是：${resetPassword.value}`);
    resetOpen.value = false;
  } finally {
    resetSaving.value = false;
  }
}

// ===== 用户详情抽屉 =====
const viewOpen = ref(false);
const viewLoading = ref(false);
const viewInfo = ref<SysUserInfo | null>(null);
const viewRow = ref<SysUser | null>(null);

async function openView(row: SysUser) {
  viewRow.value = row;
  viewOpen.value = true;
  viewLoading.value = true;
  try {
    viewInfo.value = await getUserApi(row.userId as number);
  } finally {
    viewLoading.value = false;
  }
}

/** 抽屉显示用派生数据 */
const viewAvatar = computed(() => {
  const name = viewInfo.value?.data?.userName || viewRow.value?.userName || '?';
  return name.slice(0, 1).toUpperCase();
});

const viewDeptName = computed(
  () =>
    viewInfo.value?.data?.dept?.deptName ||
    viewRow.value?.dept?.deptName ||
    '-',
);

const viewStatusText = computed(() => {
  const status = viewInfo.value?.data?.status;
  return status ? statusMap[status] ?? '-' : '-';
});

const viewSexText = computed(() => {
  const sex = viewInfo.value?.data?.sex;
  return sex ? sexMap[sex] ?? '-' : '-';
});

const viewLoginInfo = computed(() => {
  const info = viewInfo.value?.data;
  const date = formatDateTime(info?.loginDate);
  const ip = info?.loginIp || '-';
  return date === '-' ? `IP：${ip}` : `${date} / ${ip}`;
});

function roleTags(): string[] {
  const info = viewInfo.value;
  if (!info) return [];
  const ids = (info.roleIds ?? []).map(String);
  return info.roles
    .filter((role) => ids.includes(String(role.roleId)))
    .map((role) => role.roleName)
    .filter(Boolean) as string[];
}

function postTags(): string[] {
  const info = viewInfo.value;
  if (!info) return [];
  const ids = (info.postIds ?? []).map(String);
  return info.posts
    .filter((post) => ids.includes(String(post.postId)))
    .map((post) => post.postName)
    .filter(Boolean) as string[];
}

const viewRoleTags = computed(() => roleTags());
const viewPostTags = computed(() => postTags());

// ===== 导入 / 导出 =====
const importOpen = ref(false);
const importLoading = ref(false);
const updateSupport = ref(true);

function openImport() {
  importOpen.value = true;
}

async function customImport({file, onError, onSuccess}: any) {
  importLoading.value = true;
  try {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('updateSupport', String(updateSupport.value));
    const res = await fetch(`${apiURL}/system/user/importData`, {
      body: fd,
      headers: {Authorization: `Bearer ${accessStore.accessToken}`},
      method: 'POST',
    });
    const data = await res.json();
    if (data.code !== 200) {
      throw new Error(data.msg || '导入失败');
    }
    onSuccess?.(data);
    message.success(data.msg || '导入成功');
    loadData();
  } catch (error: any) {
    onError?.(error);
    message.error(error?.message ?? '导入失败');
  } finally {
    importLoading.value = false;
  }
}

async function handleDownloadTemplate() {
  try {
    await downloadBlob({
      filename: 'user_template.xlsx',
      path: '/system/user/importTemplate',
    });
    message.success('模板下载成功');
  } catch (error: any) {
    message.error(error?.message ?? '模板下载失败');
  }
}

async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `user_${Date.now()}.xlsx`,
      path: '/system/user/export',
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message ?? '导出失败');
  }
}

onMounted(() => {
  loadData();
  loadDeptTree();
});
</script>

<template>
  <Page auto-content-height title="用户管理">
    <div class="user-layout">
      <TreePanel
        :selected-key="query.deptId"
        :tree-data="deptOptions"
        search-placeholder="请输入部门名称"
        storage-key="user-dept-sidebar"
        title="组织机构"
        @refresh="loadDeptTree"
        @node-click="handleNodeClick"
      />

      <div class="user-layout__main">
        <Card :bordered="false">
          <!-- 搜索区（可折叠） -->
          <div v-show="showSearch" class="search-panel mb-4">
            <Form
              :model="query"
              class="search-form"
              layout="inline"
              @submit.prevent
            >
              <Form.Item label="用户名称" name="userName">
                <Input
                  v-model:value="query.userName"
                  allow-clear
                  placeholder="请输入用户名称"
                  style="width: 200px"
                  @press-enter="handleSearch"
                />
              </Form.Item>
              <Form.Item label="手机号码" name="phonenumber">
                <Input
                  v-model:value="query.phonenumber"
                  allow-clear
                  placeholder="请输入手机号码"
                  style="width: 200px"
                  @press-enter="handleSearch"
                />
              </Form.Item>
              <Form.Item label="状态" name="status">
                <Select
                  v-model:value="query.status"
                  :options="statusOptions"
                  allow-clear
                  placeholder="用户状态"
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
                  <Button @click="handleReset">重置</Button>
                </Space>
              </Form.Item>
            </Form>
          </div>

          <!-- 工具栏：左侧操作组 + 右侧视图控制组 -->
          <div class="toolbar mb-4">
            <Space :size="12">
              <Button
                v-if="hasPermi('system:user:add')"
                type="primary"
                @click="openAdd"
              >
                <IconifyIcon class="btn-icon" icon="lucide:plus"/>
                新增
              </Button>
              <Button
                v-if="hasPermi('system:user:edit')"
                :disabled="selectedRowKeys.length !== 1"
                @click="handleUpdate"
              >
                <IconifyIcon class="btn-icon" icon="lucide:pencil"/>
                修改
              </Button>
              <Button
                v-if="hasPermi('system:user:remove')"
                :disabled="selectedRowKeys.length === 0"
                danger
                @click="handleDelete()"
              >
                <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
                删除
              </Button>
              <Button
                v-if="hasPermi('system:user:import')"
                @click="openImport"
              >
                <IconifyIcon class="btn-icon" icon="lucide:upload"/>
                导入
              </Button>
              <Button
                v-if="hasPermi('system:user:export')"
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
                storage-key="system-user-columns"
              />
            </Space>
          </div>

          <!-- 表格（列支持拖拽调宽，宽度持久化到 localStorage） -->
          <ResizableTable
            :columns="visibleColumns"
            :data-source="userList"
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
            row-key="userId"
            storage-key="system-user-columns"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'userName'">
                <a class="link-type" @click="openView(record)">{{ record.userName }}</a>
              </template>
              <template v-else-if="column.key === 'status'">
                <Switch
                  :checked="record.status === '0'"
                  :disabled="
                    Number(record.userId) === 1 || !hasPermi('system:user:edit')
                  "
                  @change="(checked) => handleStatusChange(record, Boolean(checked))"
                />
              </template>
              <template v-else-if="column.key === 'createTime'">
                {{ formatDateTime(record.createTime) }}
              </template>
              <template v-else-if="column.key === 'operation'">
                <!-- 超级管理员（userId=1）不允许操作 -->
                <Space v-if="String(record.userId) !== '1'" :size="4">
                  <Button
                    v-if="hasPermi('system:user:edit')"
                    size="small"
                    type="link"
                    @click="openEdit(record)"
                  >
                    修改
                  </Button>
                  <Button
                    v-if="hasPermi('system:user:resetPwd')"
                    size="small"
                    type="link"
                    @click="openResetPwd(record)"
                  >
                    重置密码
                  </Button>
                  <Button
                    v-if="hasPermi('system:user:remove')"
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
    </div>

    <!-- 新增 / 修改用户 -->
    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      width="640px"
      @ok="handleSubmit"
    >
      <Form
        ref="formRef"
        :label-col="{ flex: '80px' }"
        :model="form"
        :rules="rules"
      >
        <div class="form-grid">
          <Form.Item label="用户昵称" prop="nickName">
            <Input v-model:value="form.nickName" placeholder="请输入用户昵称"/>
          </Form.Item>
          <Form.Item label="归属部门">
            <TreeSelect
              v-model:value="form.deptId"
              :tree-data="deptTreeSelectData"
              allow-clear
              placeholder="请选择归属部门"
            />
          </Form.Item>
          <Form.Item label="手机号码" prop="phonenumber">
            <Input v-model:value="form.phonenumber" placeholder="请输入手机号码"/>
          </Form.Item>
          <Form.Item label="邮箱" prop="email">
            <Input v-model:value="form.email" placeholder="请输入邮箱"/>
          </Form.Item>
          <template v-if="!isEdit">
            <Form.Item label="用户名称" prop="userName">
              <Input v-model:value="form.userName" placeholder="请输入用户名称"/>
            </Form.Item>
            <Form.Item label="用户密码" prop="password">
              <Input.Password
                v-model:value="form.password"
                placeholder="请输入用户密码"
              />
            </Form.Item>
          </template>
          <Form.Item label="用户性别">
            <Select v-model:value="form.sex" :options="sexOptions"/>
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
          <Form.Item label="岗位">
            <Select
              v-model:value="form.postIds"
              :options="postSelectOptions"
              mode="multiple"
              placeholder="请选择岗位"
            />
          </Form.Item>
          <Form.Item label="角色">
            <Select
              v-model:value="form.roleIds"
              :options="roleSelectOptions"
              mode="multiple"
              placeholder="请选择角色"
            />
          </Form.Item>
          <Form.Item class="form-grid__full" label="备注">
            <Input.TextArea v-model:value="form.remark" placeholder="请输入内容"/>
          </Form.Item>
        </div>
      </Form>
    </Modal>

    <!-- 重置密码 -->
    <Modal
      v-model:open="resetOpen"
      :confirm-loading="resetSaving"
      :title="`重置密码 - ${resetUserName}`"
      @ok="handleResetPwd"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <Form.Item label="新密码" required>
          <Input.Password
            v-model:value="resetPassword"
            placeholder="请输入新密码"
            @press-enter="handleResetPwd"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 用户导入 -->
    <Modal v-model:open="importOpen" :footer="null" title="用户导入" width="520px">
      <div class="import-tip">
        <p>1. 点击「下载模板」下载用户数据导入模板。</p>
        <p>2. 按模板格式填写数据后，将文件拖拽到下方区域上传。</p>
        <Space class="mb-3">
          <Button size="small" @click="handleDownloadTemplate">
            <IconifyIcon class="btn-icon" icon="lucide:download"/>
            下载模板
          </Button>
          <Checkbox v-model:checked="updateSupport">
            是否更新已经存在的用户数据
          </Checkbox>
        </Space>
      </div>
      <Upload.Dragger
        :custom-request="customImport"
        :disabled="importLoading"
        :show-upload-list="false"
        accept=".xls,.xlsx"
      >
        <IconifyIcon class="upload-icon" icon="lucide:upload"/>
        <p class="ant-upload-text">点击或拖拽文件到此处上传</p>
        <p class="ant-upload-hint">仅支持 .xls / .xlsx 格式</p>
      </Upload.Dragger>
    </Modal>

    <!-- 用户详情抽屉 -->
    <Drawer v-model:open="viewOpen" title="用户详情" width="60%">
      <div v-if="viewLoading" class="view-loading">加载中...</div>
      <template v-else-if="viewInfo">
        <!-- 个人资料头部 -->
        <div class="user-view__profile">
          <div class="user-view__avatar">{{ viewAvatar }}</div>
          <div class="user-view__brief">
            <div class="user-view__name">
              {{ viewInfo.data?.nickName || viewInfo.data?.userName || '-' }}
            </div>
            <div class="user-view__meta">
              <span>{{ viewDeptName }}</span>
              <Tag
                :color="viewInfo.data?.status === '0' ? 'success' : 'error'"
              >
                {{ viewStatusText }}
              </Tag>
            </div>
          </div>
        </div>

        <Divider class="user-view__divider"/>

        <!-- 基本信息 -->
        <div class="user-view__section">
          <div class="user-view__section-title">基本信息</div>
          <Descriptions :column="2" size="small">
            <Descriptions.Item label="用户名称">
              {{ viewInfo.data?.userName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="手机号码">
              {{ viewInfo.data?.phonenumber || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="邮箱">
              {{ viewInfo.data?.email || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="性别">
              {{ viewSexText }}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {{ formatDateTime(viewInfo.data?.createTime) }}
            </Descriptions.Item>
            <Descriptions.Item label="上次登录">
              {{ viewLoginInfo }}
            </Descriptions.Item>
          </Descriptions>
        </div>

        <!-- 角色 -->
        <div class="user-view__section">
          <div class="user-view__section-title">角色</div>
          <div class="user-view__tags">
            <template v-if="viewRoleTags.length">
              <Tag v-for="name in viewRoleTags" :key="name" color="blue">
                {{ name }}
              </Tag>
            </template>
            <span v-else class="user-view__empty">-</span>
          </div>
        </div>

        <!-- 岗位 -->
        <div class="user-view__section">
          <div class="user-view__section-title">岗位</div>
          <div class="user-view__tags">
            <template v-if="viewPostTags.length">
              <Tag v-for="name in viewPostTags" :key="name" color="geekblue">
                {{ name }}
              </Tag>
            </template>
            <span v-else class="user-view__empty">-</span>
          </div>
        </div>

        <!-- 备注 -->
        <div class="user-view__section">
          <div class="user-view__section-title">备注</div>
          <div class="user-view__remark">{{ viewInfo.data?.remark || '-' }}</div>
        </div>
      </template>
    </Drawer>
  </Page>
</template>

<style scoped>
/* 左部门树 + 右内容布局：占满整个内容区高度 */
.user-layout {
  display: flex;
  align-items: stretch;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.user-layout__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

/* 右侧卡片撑满高度；表格区 flex 占满剩余空间并在内部滚动，避免整页滚动 */
.user-layout__main :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.user-layout__main :deep(.ant-card-body) {
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

/* 新增/修改表单：两列网格 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.form-grid__full {
  grid-column: 1 / -1;
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

.import-tip p {
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.65);
}

.upload-icon {
  width: 36px;
  height: 36px;
  color: rgba(0, 0, 0, 0.45);
}

.view-loading {
  padding: 40px 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
}

/* —— 用户详情抽屉 —— */
.user-view__profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 0 8px;
}

.user-view__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #4096ff, #1677ff);
  border-radius: 12px;
}

.user-view__name {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
  color: #1f2329;
}

.user-view__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
}

.user-view__divider {
  margin: 8px 0 0;
}

.user-view__section {
  margin-top: 20px;
}

.user-view__section-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.75);
}

.user-view__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-view__empty {
  color: rgba(0, 0, 0, 0.45);
}

.user-view__remark {
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(0, 0, 0, 0.75);
  background: #f6f8fa;
  border-radius: 6px;
}
</style>
