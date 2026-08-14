<script lang="ts" setup>
/**
 * 导入表结构弹窗
 * - 「导入表」：从数据库表列表勾选导入（tool:gen:import）
 * - 「创建表」：粘贴建表 SQL 创建并导入（需 admin 角色，tool:gen:list）
 */
import {reactive, ref, watch} from 'vue';

import {IconifyIcon} from '@vben/icons';
import {message} from 'ant-design-vue';
import {
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
} from 'ant-design-vue';

import {
  createTableApi,
  getDbTableListApi,
  importTableApi,
  type GenTable,
} from '#/api/tool/gen';

import {tplWebTypeOptions} from '../options';
defineOptions({name: 'GenImportTableModal'});

const props = defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  'update:open': [value: boolean];
  success: [];
}>();

// ===== 通用 =====
const activeTab = ref<'table' | 'create'>('table');
const tplWebType = ref('antd-vue-typescript');
const importing = ref(false);
const creating = ref(false);

// ===== 导入表 =====
const loading = ref(false);
const tableList = ref<GenTable[]>([]);
const total = ref(0);
const selectedRowKeys = ref<string[]>([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  tableComment: undefined as string | undefined,
  tableName: undefined as string | undefined,
});

async function loadData() {
  loading.value = true;
  try {
    const {rows, total: t} = await getDbTableListApi(query);
    tableList.value = rows;
    total.value = t;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.tableComment = undefined;
  query.tableName = undefined;
  query.pageNum = 1;
  loadData();
}

function handleTableChange(pagination: {current?: number; pageSize?: number}) {
  query.pageNum = pagination.current ?? 1;
  query.pageSize = pagination.pageSize ?? 10;
  loadData();
}

/**
 * 表格勾选变化。antd 的 Key 类型为 string | number，统一转为字符串保存
 * （本场景 row-key 为表名字符串）。
 */
function handleSelectionChange(keys: (string | number)[]) {
  selectedRowKeys.value = keys.map(String);
}

// ===== 导入 =====
async function handleImport() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要导入的数据表');
    return;
  }
  importing.value = true;
  try {
    await importTableApi(selectedRowKeys.value.join(','), tplWebType.value);
    message.success('导入成功');
    emit('success');
    close();
  } finally {
    importing.value = false;
  }
}

// ===== 创建表 =====
const createSql = ref('');

async function handleCreate() {
  if (!createSql.value.trim()) {
    message.warning('请输入建表 SQL 语句');
    return;
  }
  creating.value = true;
  try {
    await createTableApi(createSql.value, tplWebType.value);
    message.success('创建成功');
    createSql.value = '';
    emit('success');
    close();
  } finally {
    creating.value = false;
  }
}

// ===== 弹窗开关 =====
function close() {
  emit('update:open', false);
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      activeTab.value = 'table';
      selectedRowKeys.value = [];
      createSql.value = '';
      loadData();
    }
  },
);
</script>

<template>
  <Modal :footer="null" :open="open" title="导入表结构" width="780px" @cancel="close">
    <div class="mb-3">
      <Space>
        <span class="field-label">前端类型</span>
        <Select
          v-model:value="tplWebType"
          :options="tplWebTypeOptions"
          style="width: 220px"
        />
      </Space>
    </div>

    <Tabs v-model:activeKey="activeTab">
      <!-- 导入表 -->
      <Tabs.TabPane key="table" tab="导入表">
        <Form class="mb-3" layout="inline" @submit.prevent>
          <Form.Item label="表名称">
            <Input
              v-model:value="query.tableName"
              allow-clear
              placeholder="请输入表名称"
              style="width: 180px"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item label="表描述">
            <Input
              v-model:value="query.tableComment"
              allow-clear
              placeholder="请输入表描述"
              style="width: 180px"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item>
            <Space :size="8">
              <Button type="primary" @click="handleSearch">搜索</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Form.Item>
        </Form>

        <Table
          :data-source="tableList"
          :loading="loading"
          :pagination="{
            current: query.pageNum,
            pageSize: query.pageSize,
            showSizeChanger: true,
            showTotal: (t: number) => `共 ${t} 条`,
            size: 'small',
            total,
          }"
          :row-selection="{
            selectedRowKeys,
            onChange: handleSelectionChange,
          }"
          row-key="tableName"
          size="small"
          @change="handleTableChange"
        >
          <Table.Column dataIndex="tableName" title="表名称"/>
          <Table.Column dataIndex="tableComment" ellipsis title="表描述"/>
          <Table.Column dataIndex="createTime" title="创建时间" width="170"/>
        </Table>
      </Tabs.TabPane>

      <!-- 创建表 -->
      <Tabs.TabPane key="create" tab="创建表">
        <div class="create-tip">
          根据创建表结构语句生成代码。注意：创建表需要 admin 角色权限。
        </div>
        <Input.TextArea
          v-model:value="createSql"
          :rows="10"
          placeholder="请输入创建表的 SQL 语句，例如：CREATE TABLE t_user (id bigint PRIMARY KEY, name varchar(50));"
        />
        <Button
          class="mt-2"
          :loading="creating"
          type="primary"
          @click="handleCreate"
        >
          <IconifyIcon class="btn-icon" icon="lucide:hammer"/>
          创建
        </Button>
      </Tabs.TabPane>
    </Tabs>

    <div class="modal-footer">
      <Space>
        <Button @click="close">取消</Button>
        <Button
          v-if="activeTab === 'table'"
          :disabled="!selectedRowKeys.length"
          :loading="importing"
          type="primary"
          @click="handleImport"
        >
          确定
        </Button>
      </Space>
    </div>
  </Modal>
</template>

<style scoped>
.field-label {
  color: rgba(0, 0, 0, 0.75);
}

.create-tip {
  margin-bottom: 12px;
  padding: 8px 12px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  background: #f6f8fa;
  border: 1px solid #eceff3;
  border-radius: 6px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 4px;
  vertical-align: -2px;
}

.mt-2 {
  margin-top: 12px;
}
</style>
