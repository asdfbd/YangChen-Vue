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
} from 'ant-design-vue';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';

import {
  addPostApi,
  deletePostApi,
  getPostApi,
  listPostApi,
  type SysPost,
  updatePostApi,
} from '#/api/system/post';
import {downloadBlob} from '#/utils/download';

defineOptions({name: 'SystemPost'});

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

const statusMap: Record<string, string> = {'0': '正常', '1': '停用'};

type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

// ===== 列表 =====
const loading = ref(false);
const postList = ref<SysPost[]>([]);
const total = ref(0);
const showSearch = ref(true);
const selectedRowKeys = ref<string[]>([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  postCode: undefined as string | undefined,
  postName: undefined as string | undefined,
  status: undefined as string | undefined,
});

function buildQuery() {
  return {...query};
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return value.includes('T') ? value.replace('T', ' ').slice(0, 19) : value.slice(0, 19);
}

async function loadData() {
  loading.value = true;
  try {
    const {rows, total: t} = await listPostApi(buildQuery());
    postList.value = rows;
    total.value = t;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

/** 重置：清空搜索字段并刷新 */
function handleReset() {
  query.postCode = undefined;
  query.postName = undefined;
  query.status = undefined;
  query.pageNum = 1;
  loadData();
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
    dataIndex: 'postCode',
    ellipsis: true,
    key: 'postCode',
    title: '岗位编码',
    visible: true,
    width: 130,
  },
  {
    dataIndex: 'postName',
    ellipsis: true,
    key: 'postName',
    title: '岗位名称',
    visible: true,
    width: 140,
  },
  {
    align: 'center' as const,
    dataIndex: 'postSort',
    key: 'postSort',
    title: '岗位排序',
    visible: true,
    width: 90,
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
    dataIndex: 'createTime',
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
    width: 150,
  },
]);

const visibleColumns = computed(() => {
  const out: Col[] = [];
  for (const col of columns.value) {
    if (col.visible !== false) out.push(col);
  }
  return out;
});

/** 由选中的岗位 ID 反查名称（用于确认提示文案） */
function selectedPostNames(): string[] {
  return (selectedRowKeys.value as string[]).map((id) => {
    const row = postList.value.find(
      (item) => String(item.postId) === String(id),
    );
    return row?.postName || String(id);
  });
}

// ===== 删除（确认提示岗位名称，实际传岗位 ID） =====
function handleDelete(row?: SysPost) {
  const postIds = row
    ? [String(row.postId)]
    : (selectedRowKeys.value as string[]);
  const names = row
    ? [row.postName || String(row.postId)]
    : selectedPostNames();
  Modal.confirm({
    content: `是否确认删除岗位"${names.join('、')}"的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deletePostApi(postIds);
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

const form = reactive<Record<string, any>>({
  postCode: undefined,
  postId: undefined,
  postName: undefined,
  postSort: 0,
  remark: undefined,
  status: '0',
});

const rules: Record<string, any> = {
  postCode: [{required: true, message: '岗位编码不能为空', trigger: 'blur'}],
  postName: [{required: true, message: '岗位名称不能为空', trigger: 'blur'}],
  postSort: [{required: true, message: '岗位顺序不能为空', trigger: 'blur'}],
};

function resetForm() {
  Object.assign(form, {
    postCode: undefined,
    postId: undefined,
    postName: undefined,
    postSort: 0,
    remark: undefined,
    status: '0',
  });
  formRef.value?.clearValidate?.();
}

function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增岗位';
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: SysPost) {
  isEdit.value = true;
  modalTitle.value = '修改岗位';
  resetForm();
  const data = await getPostApi(row.postId as number);
  Object.assign(form, data, {
    // 后端 Long 序列化为字符串，InputNumber 需要数字
    postSort: Number(data.postSort ?? 0),
  });
  modalOpen.value = true;
}

function handleUpdate() {
  const id = selectedRowKeys.value[0];
  const row = postList.value.find((item) => String(item.postId) === String(id));
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
      await updatePostApi(form);
      message.success('修改成功');
    } else {
      await addPostApi(form);
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

// ===== 导出 =====
async function handleExport() {
  try {
    await downloadBlob({
      body: buildQuery(),
      filename: `post_${Date.now()}.xlsx`,
      path: '/system/post/export',
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message ?? '导出失败');
  }
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="岗位管理">
    <div class="post-page">
      <Card :bordered="false">
        <!-- 搜索区（可折叠） -->
        <div v-show="showSearch" class="search-panel mb-4">
          <Form
            :model="query"
            class="search-form"
            layout="inline"
            @submit.prevent
          >
            <Form.Item label="岗位编码" name="postCode">
              <Input
                v-model:value="query.postCode"
                allow-clear
                placeholder="请输入岗位编码"
                style="width: 200px"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="岗位名称" name="postName">
              <Input
                v-model:value="query.postName"
                allow-clear
                placeholder="请输入岗位名称"
                style="width: 200px"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="状态" name="status">
              <Select
                v-model:value="query.status"
                :options="statusOptions"
                allow-clear
                placeholder="岗位状态"
                style="width: 200px"
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
              v-if="hasPermi('system:post:add')"
              type="primary"
              @click="openAdd"
            >
              <IconifyIcon class="btn-icon" icon="lucide:plus"/>
              新增
            </Button>
            <Button
              v-if="hasPermi('system:post:edit')"
              :disabled="selectedRowKeys.length !== 1"
              @click="handleUpdate"
            >
              <IconifyIcon class="btn-icon" icon="lucide:pencil"/>
              修改
            </Button>
            <Button
              v-if="hasPermi('system:post:remove')"
              :disabled="selectedRowKeys.length === 0"
              danger
              @click="handleDelete()"
            >
              <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
              删除
            </Button>
            <Button
              v-if="hasPermi('system:post:export')"
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
              storage-key="system-post-columns"
            />
          </Space>
        </div>

        <!-- 表格（列支持拖拽调宽，宽度持久化到 localStorage） -->
        <ResizableTable
          :columns="visibleColumns"
          :data-source="postList"
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
          row-key="postId"
          storage-key="system-post-columns"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="record.status === '0' ? 'success' : 'error'">
                {{ statusMap[record.status ?? ''] ?? '-' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="4">
                <Button
                  v-if="hasPermi('system:post:edit')"
                  size="small"
                  type="link"
                  @click="openEdit(record)"
                >
                  修改
                </Button>
                <Button
                  v-if="hasPermi('system:post:remove')"
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

    <!-- 新增 / 修改岗位 -->
    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      width="500px"
      @ok="handleSubmit"
    >
      <Form
        ref="formRef"
        :label-col="{ span: 5 }"
        :model="form"
        :rules="rules"
        :wrapper-col="{ span: 18 }"
      >
        <Form.Item label="岗位名称" prop="postName">
          <Input v-model:value="form.postName" placeholder="请输入岗位名称"/>
        </Form.Item>
        <Form.Item label="岗位编码" prop="postCode">
          <Input v-model:value="form.postCode" placeholder="请输入岗位编码"/>
        </Form.Item>
        <Form.Item label="岗位顺序" prop="postSort">
          <InputNumber v-model:value="form.postSort" :min="0" :precision="0"/>
        </Form.Item>
        <Form.Item label="岗位状态" prop="status">
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
        <Form.Item label="备注" prop="remark">
          <Input.TextArea
            v-model:value="form.remark"
            placeholder="请输入内容"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
/* 占满整个内容区高度：卡片撑满、表格区内部滚动 */
.post-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.post-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.post-page :deep(.ant-card-body) {
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

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
