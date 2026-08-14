<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {useTabs} from '@vben/hooks';
import {useAccessStore} from '@vben/stores';

import {Button, Card, Form, Input, message, Modal, Space, Table, Tag,} from 'ant-design-vue';

import {allocatedUserListApi, cancelAuthUserAllApi, cancelAuthUserApi,} from '#/api/system/role';
import type {SysUser} from '#/api/system/user';

import SelectUser from './select-user.vue';

defineOptions({name: 'SystemRoleAuthUser'});

type Key = string | number;

const route = useRoute();
const router = useRouter();
const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const loading = ref(false);
const userList = ref<SysUser[]>([]);
const total = ref(0);
const showSearch = ref(true);
const selectedRowKeys = ref<Key[]>([]);
// 雪花 ID 超过 JS Number.MAX_SAFE_INTEGER，必须保持字符串（Spring 转 Long 无损），不能 Number() 转换
const roleId = String(route.params.roleId);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  phonenumber: undefined as string | undefined,
  roleId,
  userName: undefined as string | undefined,
});

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.includes('T') ? value.replace('T', ' ').slice(0, 19) : value.slice(0, 19);
}

function formatStatus(status?: string) {
  return status === '0'
    ? {label: '正常', color: 'success'}
    : {label: '停用', color: 'error'};
}

async function loadData() {
  loading.value = true;
  try {
    const {rows, total: t} = await allocatedUserListApi(query);
    userList.value = rows;
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
  query.phonenumber = undefined;
  query.userName = undefined;
  handleSearch();
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? 10;
  loadData();
}

function handleSelectionChange(keys: Key[]) {
  selectedRowKeys.value = keys;
}

const columns = [
  {dataIndex: 'userName', ellipsis: true, key: 'userName', title: '用户名称'},
  {dataIndex: 'nickName', ellipsis: true, key: 'nickName', title: '用户昵称'},
  {dataIndex: 'email', ellipsis: true, key: 'email', title: '邮箱'},
  {
    dataIndex: 'phonenumber',
    ellipsis: true,
    key: 'phonenumber',
    title: '手机',
  },
  {align: 'center' as const, key: 'status', title: '状态', width: 90},
  {
    align: 'center' as const,
    dataIndex: 'createTime',
    key: 'createTime',
    title: '创建时间',
    width: 180,
  },
  {align: 'center' as const, key: 'operation', title: '操作', width: 120},
];

// ===== 取消授权 =====
function handleCancelAuth(row: SysUser) {
  Modal.confirm({
    content: `确认要取消该用户"${row.userName}"角色吗？`,
    okText: '确定',
    onOk: async () => {
      await cancelAuthUserApi({roleId, userId: String(row.userId ?? '')});
      message.success('取消授权成功');
      loadData();
    },
    title: '系统提示',
  });
}

function handleCancelAuthAll() {
  const userIds = selectedRowKeys.value as number[];
  Modal.confirm({
    content: '是否取消选中用户授权数据项？',
    okText: '确定',
    onOk: async () => {
      await cancelAuthUserAllApi({
        roleId,
        userIds: userIds.join(','),
      });
      message.success('取消授权成功');
      selectedRowKeys.value = [];
      loadData();
    },
    title: '系统提示',
  });
}

// ===== 选择用户弹窗 =====
const selectUserRef = ref<InstanceType<typeof SelectUser>>();

function openSelectUser() {
  selectUserRef.value?.open(roleId);
}

// ===== 关闭 =====
const {closeTabByKey} = useTabs();

async function handleClose() {
  const currentPath = route.path;
  await router.push('/system/role');
  closeTabByKey(currentPath);
}

onMounted(loadData);
</script>

<template>
  <Page title="分配用户">
    <Card>
      <!-- 搜索区（可折叠） -->
      <div v-show="showSearch" class="search-panel mb-4">
        <Form :model="query" class="search-form" layout="inline">
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
            @click="openSelectUser"
          >
            <svg
              class="btn-icon"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              viewBox="0 0 24 24"
            >
              <path d="M5 12h14"/>
              <path d="M12 5v14"/>
            </svg>
            添加用户
          </Button>
          <Button
            v-if="hasPermi('system:role:remove')"
            :disabled="selectedRowKeys.length === 0"
            danger
            @click="handleCancelAuthAll"
          >
            <svg
              class="btn-icon"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              viewBox="0 0 24 24"
            >
              <circle cx="12" cy="12" r="10"/>
              <path d="M15 9l-6 6"/>
              <path d="M9 9l6 6"/>
            </svg>
            批量取消授权
          </Button>
          <Button @click="handleClose">
            <svg
              class="btn-icon"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              viewBox="0 0 24 24"
            >
              <path d="M18 6L6 18"/>
              <path d="M6 6l12 12"/>
            </svg>
            关闭
          </Button>
        </Space>
        <Space :size="8">
          <Button size="small" @click="showSearch = !showSearch">
            <svg
              class="btn-icon"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              viewBox="0 0 24 24"
            >
              <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            {{ showSearch ? '隐藏搜索' : '显示搜索' }}
          </Button>
          <Button size="small" @click="loadData">
            <svg
              class="btn-icon"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              viewBox="0 0 24 24"
            >
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
            </svg>
            刷新
          </Button>
        </Space>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="userList"
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
        row-key="userId"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="formatStatus(record.status).color">
              {{ formatStatus(record.status).label }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDateTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'operation'">
            <Button
              v-if="hasPermi('system:role:remove')"
              danger
              size="small"
              type="link"
              @click="handleCancelAuth(record)"
            >
              取消授权
            </Button>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 选择用户弹窗 -->
    <SelectUser ref="selectUserRef" @ok="handleSearch"/>
  </Page>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.search-panel {
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
