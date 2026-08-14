<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {Button, Card, Form, Input, message, Modal, Space, Table, Tag} from 'ant-design-vue';

import {forceLogoutApi, listOnlineUserApi, type OnlineUser} from '#/api/monitor/online';

defineOptions({name: 'MonitorOnline'});

const loading = ref(false);
const users = ref<OnlineUser[]>([]);
const total = ref(0);
const query = reactive({ipaddr: '', pageNum: 1, pageSize: 10, userName: ''});

const columns = [
  {dataIndex: 'userName', title: '用户名称', width: 150},
  {dataIndex: 'deptName', title: '部门名称', width: 160},
  {dataIndex: 'ipaddr', title: '主机地址', width: 150},
  {dataIndex: 'loginLocation', title: '登录地点', width: 160},
  {dataIndex: 'os', title: '操作系统', width: 170},
  {dataIndex: 'loginTime', title: '登录时间', width: 190},
  {key: 'operation', title: '操作', width: 100},
];

async function loadData() {
  loading.value = true;
  try {
    const result = await listOnlineUserApi(query);
    users.value = result.rows ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  loadData();
}

function reset() {
  query.ipaddr = '';
  query.userName = '';
  search();
}

function changePage(page: { current?: number; pageSize?: number }) {
  query.pageNum = page.current ?? 1;
  query.pageSize = page.pageSize ?? query.pageSize;
  loadData();
}

function logout(record: OnlineUser) {
  Modal.confirm({
    content: `确定强制退出用户“${record.userName}”吗？`,
    okText: '强退',
    okType: 'danger',
    title: '系统提示',
    async onOk() {
      await forceLogoutApi(record.tokenId ?? '');
      message.success('已强制退出');
      await loadData();
    },
  });
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height>
    <Card :bordered="false">
      <div class="search-panel">
        <Form layout="inline" @submit.prevent="search">
          <Form.Item label="用户名称"><Input v-model:value="query.userName" allow-clear
                                             placeholder="请输入用户名称" @press-enter="search"/>
          </Form.Item>
          <Form.Item label="主机地址"><Input v-model:value="query.ipaddr" allow-clear
                                             placeholder="请输入主机地址" @press-enter="search"/>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" @click="search">
                <IconifyIcon icon="lucide:search"/>
                搜索
              </Button>
              <Button @click="reset">重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </div>
      <div class="toolbar"><span>当前在线 {{ total }} 人</span>
        <Button size="small" @click="loadData">
          <IconifyIcon icon="lucide:refresh-cw"/>
          刷新
        </Button>
      </div>
      <Table :columns="columns" :data-source="users" :loading="loading"
             :pagination="{current: query.pageNum, pageSize: query.pageSize, showSizeChanger: true, total}"
             :scroll="{x: 1050}" row-key="tokenId" @change="changePage">
        <template #bodyCell="{column, record}">
          <template v-if="column.key === 'operation'">
            <Button danger size="small" type="link" @click="logout(record)">强退</Button>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <Tag :color="record.status === '0' ? 'success' : 'default'">
              {{ record.status === '0' ? '在线' : '离线' }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>

<style scoped>.search-panel {
  margin-bottom: 16px;
  padding: 16px 16px 2px;
  background: #f6f8fa;
  border: 1px solid #eceff3;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  color: #667085;
}</style>
