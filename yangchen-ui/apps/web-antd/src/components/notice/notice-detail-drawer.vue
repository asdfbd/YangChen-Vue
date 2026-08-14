<script lang="ts" setup>
import {computed, ref} from 'vue';

import {IconifyIcon} from '@vben/icons';
import {VbenTiptapPreview} from '@vben/plugins/tiptap';

import {Drawer} from 'ant-design-vue';

import {getNoticeApi, type SysNotice} from '#/api/system/notice';
import {normalizeContentHtml} from '#/utils/rich-content';

defineOptions({name: 'NoticeDetailDrawer'});

const open = ref(false);
const loading = ref(false);
const detail = ref<SysNotice>({});

const isAnnounce = computed(() => detail.value.noticeType === '2');
const typeLabel = computed(() =>
  detail.value.noticeType === '2' ? '公告' : '通知',
);

/** 是否有正文内容 */
const hasContent = computed(() => {
  const c = detail.value.noticeContent;
  return c != null && String(c).trim() !== '';
});

/** 正文：旧接口前缀归一化，保证图片可显示 */
const normalizedContent = computed(() =>
  normalizeContentHtml(detail.value.noticeContent ?? ''),
);

function formatDateTime(value?: string) {
  if (!value) return '—';
  return value.includes('T')
    ? value.replace('T', ' ').slice(0, 19)
    : value.slice(0, 19);
}

/**
 * 打开抽屉。payload 可为公告ID，或已带 noticeContent 的整条记录（免二次请求）。
 */
function openDrawer(payload: number | SysNotice) {
  open.value = true;
  if (payload != null && typeof payload === 'object') {
    detail.value = payload;
    return;
  }
  loading.value = true;
  detail.value = {};
  getNoticeApi(payload as number)
    .then((res) => {
      detail.value = res;
    })
    .catch(() => {
      detail.value = {};
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleClose() {
  open.value = false;
  detail.value = {};
  loading.value = false;
}

defineExpose({open: openDrawer});
</script>

<template>
  <Drawer
    :open="open"
    title="公告详情"
    width="680px"
    @close="handleClose"
  >
    <div v-if="loading" class="notice-loading">加载中...</div>
    <div v-else class="notice-detail-body">
      <div v-if="!detail.noticeId" class="notice-empty">
        <IconifyIcon class="notice-empty__icon" icon="lucide:file-text"/>
        <span>暂无数据</span>
      </div>
      <div v-else class="notice-article">
        <span
          :class="['notice-type-tag', isAnnounce ? 'type-announce' : 'type-notify']"
        >
          <IconifyIcon
            :icon="isAnnounce ? 'lucide:megaphone' : 'lucide:bell'"
            class="notice-type-tag__icon"
          />
          {{ typeLabel }}
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
              :class="['status-dot', detail.status === '0' ? 'status-ok' : 'status-off']"
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
</template>

<style scoped>
/* ===== 公告详情（对齐系统公告管理页的公告文章布局） ===== */
.notice-loading {
  padding: 40px 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
}

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

/* 富文本正文排版 */
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

.notice-body :deep(.vben-tiptap-content img) {
  display: inline-block;
  max-width: 100%;
  height: auto;
  margin: 8px 0;
  border-radius: 4px;
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
</style>
