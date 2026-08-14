<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {useAppConfig} from '@vben/hooks';
import {IconifyIcon} from '@vben/icons';
import {type ImageUploadOptions, VbenTiptap, VbenTiptapPreview,} from '@vben/plugins/tiptap';
import {useAccessStore} from '@vben/stores';

import type {TableColumnType} from 'ant-design-vue';
import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  message,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import ColumnSetting from '#/components/column-setting/index.vue';
import ResizableTable from '#/components/resizable-table/index.vue';

import {
  addNoticeApi,
  deleteNoticeApi,
  getNoticeApi,
  listNoticeApi,
  type NoticeReadUser,
  readUsersApi,
  type SysNotice,
  updateNoticeApi,
} from '#/api/system/notice';
import {requestClient} from '#/api/request';
import {normalizeContentHtml} from '#/utils/rich-content';

defineOptions({name: 'SystemNotice'});

const accessStore = useAccessStore();
const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);

/**
 * 富文本图片上传：调用 RuoYi /common/upload。
 * 返回的 url 是绝对地址（http://localhost:8080/profile/...），存储前改成当前代理相对路径
 * （/api/profile/...），保证渲染/换环境都能经前端代理访问。
 */
const imageUpload: ImageUploadOptions = {
  maxSize: 5 * 1024 * 1024,
  async upload(file, onProgress) {
    const formData = new FormData();
    formData.append('file', file);
    const resp = await requestClient.post<{ url: string }>(
      '/common/upload',
      formData,
      {
        onUploadProgress: (event) => {
          if (event.total) {
            onProgress?.(Math.round((event.loaded / event.total) * 100));
          }
        },
      },
    );
    const path = resp.url.replace(/^https?:\/\/[^/]+/, '');
    return `${apiURL}${path}`;
  },
};

/** 权限码校验（管理员 *:*:* 拥有全部权限） */
function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const noticeTypeOptions = [
  {label: '通知', value: '1'},
  {label: '公告', value: '2'},
];
const statusOptions = [
  {label: '正常', value: '0'},
  {label: '关闭', value: '1'},
];

const noticeTypeMap: Record<string, string> = {'1': '通知', '2': '公告'};
const statusMap: Record<string, string> = {'0': '正常', '1': '关闭'};

type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

// ===== 列表 =====
const loading = ref(false);
const noticeList = ref<SysNotice[]>([]);
const total = ref(0);
const showSearch = ref(true);
const selectedRowKeys = ref<string[]>([]);

const query = reactive({
  createBy: undefined as string | undefined,
  noticeTitle: undefined as string | undefined,
  noticeType: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
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
    const {rows, total: t} = await listNoticeApi(buildQuery());
    noticeList.value = rows;
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
  query.createBy = undefined;
  query.noticeTitle = undefined;
  query.noticeType = undefined;
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
    dataIndex: 'noticeTitle',
    ellipsis: true,
    key: 'noticeTitle',
    title: '公告标题',
    visible: true,
    width: 240,
  },
  {
    align: 'center' as const,
    dataIndex: 'noticeType',
    key: 'noticeType',
    title: '公告类型',
    visible: true,
    width: 100,
  },
  {
    align: 'center' as const,
    dataIndex: 'status',
    key: 'status',
    title: '状态',
    visible: true,
    width: 90,
  },
  {
    align: 'center' as const,
    dataIndex: 'createBy',
    ellipsis: true,
    key: 'createBy',
    title: '创建者',
    visible: true,
    width: 110,
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

// ===== 删除 =====
function handleDelete(row?: SysNotice) {
  const noticeIds = row
    ? [String(row.noticeId)]
    : (selectedRowKeys.value as string[]);
  Modal.confirm({
    content: `是否确认删除公告编号为"${noticeIds.join(',')}"的数据项？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteNoticeApi(noticeIds);
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
  noticeContent: '',
  noticeId: undefined,
  noticeTitle: undefined,
  noticeType: '1',
  status: '0',
});

const rules: Record<string, any> = {
  noticeTitle: [{required: true, message: '公告标题不能为空', trigger: 'blur'}],
  noticeType: [{required: true, message: '公告类型不能为空', trigger: 'change'}],
};

function resetForm() {
  Object.assign(form, {
    noticeContent: '',
    noticeId: undefined,
    noticeTitle: undefined,
    noticeType: '1',
    status: '0',
  });
  formRef.value?.clearValidate?.();
}

function openAdd() {
  isEdit.value = false;
  modalTitle.value = '新增公告';
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: SysNotice) {
  isEdit.value = true;
  modalTitle.value = '修改公告';
  resetForm();
  const data = await getNoticeApi(row.noticeId as number);
  Object.assign(form, data);
  // 旧数据里图片可能是 /dev-api 前缀，归一化为当前 apiURL，保证编辑器里能看到图
  form.noticeContent = normalizeContentHtml(data.noticeContent);
  modalOpen.value = true;
}

function handleUpdate() {
  const id = selectedRowKeys.value[0];
  const row = noticeList.value.find(
    (item) => String(item.noticeId) === String(id),
  );
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
      await updateNoticeApi(form);
      message.success('修改成功');
    } else {
      await addNoticeApi(form);
      message.success('新增成功');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

// ===== 查看详情 =====
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<SysNotice>({});

async function openDetail(row: SysNotice) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getNoticeApi(row.noticeId as number);
  } finally {
    detailLoading.value = false;
  }
}

/** 是否有正文内容 */
const hasContent = computed(() => {
  const c = detail.value.noticeContent;
  return c != null && String(c).trim() !== '';
});

/** 详情正文：旧前缀归一化，保证图片可显示 */
const normalizedContent = computed(() =>
  normalizeContentHtml(detail.value.noticeContent ?? ''),
);

// ===== 已读用户 =====
const readOpen = ref(false);
const readLoading = ref(false);
const readList = ref<NoticeReadUser[]>([]);
const readTotal = ref(0);
const readNoticeId = ref<number | string | undefined>(undefined);
const readSearch = ref('');
const readPage = reactive({pageNum: 1, pageSize: 10});

const readColumns = [
  {dataIndex: 'username', key: 'username', title: '用户名称', width: 120},
  {dataIndex: 'nickname', key: 'nickname', title: '用户昵称', width: 120},
  {dataIndex: 'deptname', key: 'deptname', title: '部门', width: 140},
  {dataIndex: 'phonenumber', key: 'phonenumber', title: '手机号码', width: 130},
  {dataIndex: 'readtime', key: 'readtime', title: '阅读时间', width: 200},
];

async function loadReadUsers() {
  readLoading.value = true;
  try {
    const {rows, total: t} = await readUsersApi({
      noticeId: readNoticeId.value,
      pageNum: readPage.pageNum,
      pageSize: readPage.pageSize,
      searchValue: readSearch.value || undefined,
    });
    readList.value = rows;
    readTotal.value = t;
  } finally {
    readLoading.value = false;
  }
}

function openReadUsers(row: SysNotice) {
  readNoticeId.value = row.noticeId;
  readPage.pageNum = 1;
  readSearch.value = '';
  readOpen.value = true;
  loadReadUsers();
}

function handleReadTableChange(pagination: {
  current?: number;
  pageSize?: number;
}) {
  readPage.pageNum = pagination.current ?? 1;
  readPage.pageSize = pagination.pageSize ?? 10;
  loadReadUsers();
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height title="通知公告">
    <div class="notice-page">
      <Card :bordered="false">
        <!-- 搜索区（可折叠） -->
        <div v-show="showSearch" class="search-panel mb-4">
          <Form
            :model="query"
            class="search-form"
            layout="inline"
            @submit.prevent
          >
            <Form.Item label="公告标题" name="noticeTitle">
              <Input
                v-model:value="query.noticeTitle"
                allow-clear
                placeholder="请输入公告标题"
                style="width: 200px"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="操作人员" name="createBy">
              <Input
                v-model:value="query.createBy"
                allow-clear
                placeholder="请输入操作人员"
                style="width: 200px"
                @press-enter="handleSearch"
              />
            </Form.Item>
            <Form.Item label="类型" name="noticeType">
              <Select
                v-model:value="query.noticeType"
                :options="noticeTypeOptions"
                allow-clear
                placeholder="公告类型"
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
              v-if="hasPermi('system:notice:add')"
              type="primary"
              @click="openAdd"
            >
              <IconifyIcon class="btn-icon" icon="lucide:plus"/>
              新增
            </Button>
            <Button
              v-if="hasPermi('system:notice:edit')"
              :disabled="selectedRowKeys.length !== 1"
              @click="handleUpdate"
            >
              <IconifyIcon class="btn-icon" icon="lucide:pencil"/>
              修改
            </Button>
            <Button
              v-if="hasPermi('system:notice:remove')"
              :disabled="selectedRowKeys.length === 0"
              danger
              @click="handleDelete()"
            >
              <IconifyIcon class="btn-icon" icon="lucide:trash-2"/>
              删除
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
              storage-key="system-notice-columns"
            />
          </Space>
        </div>

        <!-- 表格（列支持拖拽调宽，宽度持久化到 localStorage） -->
        <ResizableTable
          :columns="visibleColumns"
          :data-source="noticeList"
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
          row-key="noticeId"
          storage-key="system-notice-columns"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'noticeTitle'">
              <a class="link-type" @click="openDetail(record)">
                {{ record.noticeTitle }}
              </a>
            </template>
            <template v-else-if="column.key === 'noticeType'">
              <Tag
                :color="record.noticeType === '1' ? 'blue' : 'purple'"
              >
                {{ noticeTypeMap[record.noticeType ?? ''] ?? '-' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag
                :color="record.status === '0' ? 'success' : 'default'"
              >
                {{ statusMap[record.status ?? ''] ?? '-' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'operation'">
              <Space :size="2">
                <Button
                  v-if="hasPermi('system:notice:list')"
                  size="small"
                  type="link"
                  @click="openReadUsers(record)"
                >
                  阅读用户
                </Button>
                <Button
                  v-if="hasPermi('system:notice:edit')"
                  size="small"
                  type="link"
                  @click="openEdit(record)"
                >
                  修改
                </Button>
                <Button
                  v-if="hasPermi('system:notice:remove')"
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

    <!-- 新增 / 修改公告 -->
    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      width="780px"
      @ok="handleSubmit"
    >
      <Form
        ref="formRef"
        :label-col="{ flex: '80px' }"
        :model="form"
        :rules="rules"
      >
        <div class="notice-form">
          <Form.Item label="公告标题" prop="noticeTitle">
            <Input
              v-model:value="form.noticeTitle"
              placeholder="请输入公告标题"
            />
          </Form.Item>
          <Form.Item label="公告类型" prop="noticeType">
            <Select
              v-model:value="form.noticeType"
              :options="noticeTypeOptions"
              placeholder="请选择公告类型"
            />
          </Form.Item>
        </div>
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
        <Form.Item label="内容" required>
          <VbenTiptap
            v-model="form.noticeContent"
            :image-upload="imageUpload"
            :min-height="192"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 公告详情抽屉（对齐原 Element UI 公告文章布局） -->
    <Drawer
      v-model:open="detailOpen"
      title="公告详情"
      width="680px"
    >
      <div v-if="detailLoading" class="view-loading">加载中...</div>
      <div v-else class="notice-detail-body">
        <div v-if="!detail.noticeId" class="notice-empty">
          <IconifyIcon class="notice-empty__icon" icon="lucide:file-text"/>
          <span>暂无数据</span>
        </div>
        <div v-else class="notice-article">
          <span
            :class="[
              'notice-type-tag',
              detail.noticeType === '2' ? 'type-announce' : 'type-notify',
            ]"
          >
            <IconifyIcon
              :icon="detail.noticeType === '2' ? 'lucide:megaphone' : 'lucide:bell'"
              class="notice-type-tag__icon"
            />
            {{ noticeTypeMap[detail.noticeType ?? ''] ?? '通知' }}
          </span>

          <h1 class="notice-title">{{ detail.noticeTitle }}</h1>

          <div class="notice-meta">
            <span class="meta-item">
              <IconifyIcon class="meta-item__icon" icon="lucide:user"/>
              <span>{{ detail.createBy || '—' }}</span>
            </span>
            <span class="meta-item">
              <IconifyIcon class="meta-item__icon" icon="lucide:clock"/>
              <span>{{ formatDateTime(detail.createTime) }}</span>
            </span>
            <span class="meta-item">
              <span
                :class="[
                  'status-dot',
                  detail.status === '0' ? 'status-ok' : 'status-off',
                ]"
              />
              <span>{{ detail.status === '0' ? '正常' : '已关闭' }}</span>
            </span>
          </div>

          <div class="notice-divider">
            <span class="notice-divider-dot"></span>
            <span class="notice-divider-dot"></span>
            <span class="notice-divider-dot"></span>
          </div>

          <div class="notice-body">
            <VbenTiptapPreview
              v-if="hasContent"
              :content="normalizedContent"
              :min-height="120"
            />
            <div v-else class="notice-empty notice-empty--inner">
              <IconifyIcon class="notice-empty__icon" icon="lucide:file-text"/>
              <span>暂无内容</span>
            </div>
          </div>
        </div>
      </div>
    </Drawer>

    <!-- 已读用户 -->
    <Modal
      v-model:open="readOpen"
      :footer="null"
      title="已读用户"
      width="680px"
    >
      <Space class="mb-3">
        <Input
          v-model:value="readSearch"
          allow-clear
          placeholder="用户名称/昵称"
          style="width: 200px"
          @press-enter="loadReadUsers"
        />
        <Button type="primary" @click="loadReadUsers">搜索</Button>
      </Space>
      <Table
        :columns="readColumns"
        :data-source="readList"
        :loading="readLoading"
        :pagination="{
          current: readPage.pageNum,
          pageSize: readPage.pageSize,
          showSizeChanger: false,
          showTotal: (t: number) => `共 ${t} 条`,
          total: readTotal,
        }"
        row-key="userId"
        size="small"
        @change="handleReadTableChange"
      />
    </Modal>
  </Page>
</template>

<style scoped>
/* 占满整个内容区高度：卡片撑满、表格区内部滚动 */
.notice-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.notice-page :deep(.ant-card) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.notice-page :deep(.ant-card-body) {
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

.link-type {
  color: #1677ff;
  cursor: pointer;
}

/* 编辑表单：标题/类型并排 */
.notice-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

/* ===== 公告详情（对齐原 Element UI 公告文章布局） ===== */
.notice-detail-body {
  height: 100%;
  overflow: auto;
  padding: 10px 16px 22px;
  background: #f5f6f8;
}

.notice-article {
  max-width: 760px;
  margin: 0 auto;
  padding: 8px 8px 20px;
  animation: notice-fade-up 0.28s ease both;
}

@keyframes notice-fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notice-type-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 14px;
}

.notice-type-tag__icon {
  width: 13px;
  height: 13px;
}

.type-notify {
  background: #fff8e6;
  color: #b7791f;
  border-left: 3px solid #d97706;
}

.type-announce {
  background: #e8f5e9;
  color: #276749;
  border-left: 3px solid #38a169;
}

.notice-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a202c;
  line-height: 1.45;
  margin: 0 0 16px;
  letter-spacing: -0.2px;
  word-break: break-word;
}

.notice-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
  margin-bottom: 28px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #718096;
}

.meta-item__icon {
  width: 13px;
  height: 13px;
  color: #a0aec0;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.status-ok {
  background: #38a169;
}

.status-off {
  background: #e53e3e;
}

.notice-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.notice-divider::before,
.notice-divider::after {
  flex: 1;
  height: 1px;
  content: '';
  background: linear-gradient(to right, transparent, #dee2e6, transparent);
}

.notice-divider-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e0;
}

.notice-body {
  min-height: 120px;
  padding: 24px 28px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(0, 0, 0, 0.04);
}

/* 富文本正文排版（对齐原 Element UI 详情样式） */
.notice-body :deep(.vben-tiptap-content) {
  font-size: 14px;
  line-height: 1.85;
  color: #2d3748;
  word-break: break-word;
}

.notice-body :deep(.vben-tiptap-content p) {
  margin: 0 0 1em;
}

.notice-body :deep(.vben-tiptap-content h1),
.notice-body :deep(.vben-tiptap-content h2),
.notice-body :deep(.vben-tiptap-content h3) {
  font-weight: 700;
  color: #1a202c;
  margin: 1.4em 0 0.6em;
}

.notice-body :deep(.vben-tiptap-content h1) {
  font-size: 18px;
}

.notice-body :deep(.vben-tiptap-content h2) {
  font-size: 16px;
}

.notice-body :deep(.vben-tiptap-content h3) {
  font-size: 14px;
}

.notice-body :deep(.vben-tiptap-content a) {
  color: #3182ce;
  text-decoration: underline;
}

.notice-body :deep(.vben-tiptap-content img) {
  display: inline-block;
  max-width: 100%;
  height: auto;
  margin: 8px 0;
  border-radius: 4px;
}

.notice-body :deep(.vben-tiptap-content ul),
.notice-body :deep(.vben-tiptap-content ol) {
  padding-left: 20px;
  margin: 0 0 1em;
}

.notice-body :deep(.vben-tiptap-content li) {
  margin-bottom: 4px;
}

.notice-body :deep(.vben-tiptap-content blockquote) {
  margin: 1em 0;
  padding: 6px 16px;
  color: #718096;
  background: #f7fafc;
  border-left: 3px solid #cbd5e0;
}

.notice-body :deep(.vben-tiptap-content table) {
  width: 100%;
  margin: 1em 0;
  font-size: 13px;
  border-collapse: collapse;
}

.notice-body :deep(.vben-tiptap-content th),
.notice-body :deep(.vben-tiptap-content td) {
  padding: 7px 12px;
  border: 1px solid #e2e8f0;
}

.notice-body :deep(.vben-tiptap-content th) {
  font-weight: 600;
  background: #f7fafc;
}

.notice-empty {
  padding: 40px 0;
  font-size: 13px;
  color: #a0aec0;
  text-align: center;
}

.notice-empty__icon {
  display: block;
  width: 28px;
  height: 28px;
  margin: 0 auto 10px;
}

.notice-empty--inner {
  padding: 32px 0;
}

.view-loading {
  padding: 40px 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
}
</style>
