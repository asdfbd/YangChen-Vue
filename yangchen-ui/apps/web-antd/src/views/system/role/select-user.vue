<script lang="ts" setup>
import {reactive, ref} from 'vue';

import {Button, Form, Input, message, Modal, Space, Table, Tag,} from 'ant-design-vue';

import {selectAuthUserAllApi, unallocatedUserListApi,} from '#/api/system/role';
import type {SysUser} from '#/api/system/user';

defineOptions({name: 'SystemRoleSelectUser'});

type Key = string | number;

const emit = defineEmits<{ ok: [] }>();

const open = ref(false);
const loading = ref(false);
const userList = ref<SysUser[]>([]);
const total = ref(0);
const selectedRowKeys = ref<Key[]>([]);
const roleId = ref<string | number | undefined>(undefined);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  phonenumber: undefined as string | undefined,
  roleId: undefined as string | number | undefined,
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
    const {rows, total: t} = await unallocatedUserListApi(query);
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
];

function openDialog(id: string | number) {
  roleId.value = id;
  query.roleId = id;
  selectedRowKeys.value = [];
  handleSearch();
  open.value = true;
}

function handleSelectUser() {
  const userIds = (selectedRowKeys.value as number[]).join(',');
  if (!userIds) {
    message.error('请选择要分配的用户');
    return;
  }
  selectAuthUserAllApi({roleId: roleId.value, userIds}).then((res: any) => {
    message.success(res?.msg ?? '分配成功');
    open.value = false;
    emit('ok');
  });
}

defineExpose({open: openDialog});
</script>

<template>
  <Modal
    v-model:open="open"
    :footer="null"
    title="选择用户"
    width="800px"
  >
    <!-- 搜索区 -->
    <Form :model="query" class="mb-4" layout="inline">
      <Form.Item label="用户名称" name="userName">
        <Input
          v-model:value="query.userName"
          allow-clear
          placeholder="请输入用户名称"
          style="width: 180px"
          @press-enter="handleSearch"
        />
      </Form.Item>
      <Form.Item label="手机号码" name="phonenumber">
        <Input
          v-model:value="query.phonenumber"
          allow-clear
          placeholder="请输入手机号码"
          style="width: 180px"
          @press-enter="handleSearch"
        />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" @click="handleSearch">搜索</Button>
          <Button @click="resetQuery">重置</Button>
        </Space>
      </Form.Item>
    </Form>

    <!-- 未授权用户表格 -->
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
      </template>
    </Table>

    <!-- 弹窗底部 -->
    <div class="mt-4 flex justify-end">
      <Space>
        <Button type="primary" @click="handleSelectUser">确 定</Button>
        <Button @click="open = false">取 消</Button>
      </Space>
    </div>
  </Modal>
</template>
