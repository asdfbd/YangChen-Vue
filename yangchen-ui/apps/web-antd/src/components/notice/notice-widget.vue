<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';

import {Bell, IconifyIcon, MailCheck} from '@vben/icons';

import {Badge, Button, Empty, Popover, Spin} from 'ant-design-vue';

import {
  listNoticeTopApi,
  markNoticeReadAllApi,
  markNoticeReadApi,
  type SysNotice,
} from '#/api/system/notice';

import NoticeDetailDrawer from './notice-detail-drawer.vue';

defineOptions({name: 'NoticeWidget'});

const router = useRouter();

const open = ref(false);
const loading = ref(false);
const noticeList = ref<SysNotice[]>([]);
const unreadCount = ref(0);
const detailDrawerRef = ref<InstanceType<typeof NoticeDetailDrawer>>();

/** 公告类型 monogram：通知=琥珀铃铛 / 公告=翠绿喇叭（与公告详情同色语言） */
const noticeTypeStyle = (item: SysNotice) =>
  item.noticeType === '2' ? 'announce' : 'notify';

const monogramIcon = (item: SysNotice) =>
  item.noticeType === '2' ? 'lucide:megaphone' : 'lucide:bell';

const hasList = computed(() => noticeList.value.length > 0);

function formatDate(value?: string) {
  if (!value) return '';
  const s = value.includes('T') ? value.replace('T', ' ') : value;
  const [date, time] = s.split(' ');
  const now = new Date();
  const sameYear = now.getFullYear() === Number(date?.slice(0, 4));
  const today = s.slice(0, 10) === formatLocalDate(now);
  if (today && time) return time.slice(0, 5);
  if (sameYear) return `${date?.slice(5)} ${time?.slice(0, 5) ?? ''}`;
  return date ?? '';
}

function formatLocalDate(d: Date) {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

/** 静默刷新，不打断已展开的列表 */
async function loadNotice() {
  loading.value = true;
  try {
    const res = await listNoticeTopApi();
    noticeList.value = res.data || [];
    unreadCount.value = res.unreadCount ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleOpenChange(visible: boolean) {
  open.value = visible;
  if (visible) {
    loadNotice();
  }
}

async function handleRowClick(item: SysNotice) {
  if (item.noticeId == null) return;
  if (!item.isRead) {
    markNoticeReadApi(item.noticeId).catch(() => {
    });
    item.isRead = true;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  }
  detailDrawerRef.value?.open(item.noticeId as number);
}

async function handleMarkAll() {
  const ids = noticeList.value.map((n) => n.noticeId).join(',');
  if (!ids) return;
  markNoticeReadAllApi(ids).catch(() => {
  });
  noticeList.value = noticeList.value.map((n) => ({...n, isRead: true}));
  unreadCount.value = 0;
}

function handleViewAll() {
  open.value = false;
  router.push('/system/notice');
}

// 挂载即加载，保证铃铛角标初始正确
loadNotice();
</script>

<template>
  <div class="notice-widget">
    <Popover
      v-model:open="open"
      :overlay-inner-style="{padding: 0}"
      placement="bottomRight"
      trigger="click"
      @open-change="handleOpenChange"
    >
      <template #content>
        <div class="yc-notice">
          <!-- 头部：标题 + 未读数 + 全部已读 -->
          <div class="yc-notice__head">
            <span class="yc-notice__title">通知公告</span>
            <span class="yc-notice__head-right">
              <span v-if="unreadCount > 0" class="yc-notice__unread">
                {{ unreadCount }} 条未读
              </span>
              <Button
                :disabled="!hasList"
                class="yc-notice__mark-all"
                size="small"
                type="link"
                @click="handleMarkAll"
              >
                <MailCheck class="yc-notice__mark-all-icon"/>
                全部已读
              </Button>
            </span>
          </div>

          <!-- 列表 -->
          <Spin :spinning="loading">
            <div v-if="!hasList" class="yc-notice__empty">
              <Empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="暂无公告"/>
            </div>
            <div v-else class="yc-notice__list">
              <div
                v-for="item in noticeList"
                :key="item.noticeId"
                :class="['yc-notice__row', {is_read: item.isRead}]"
                @click="handleRowClick(item)"
              >
                <span
                  :class="['yc-notice__mono', `is-${noticeTypeStyle(item)}`]"
                >
                  <IconifyIcon
                    :class="['yc-notice__mono-icon', `is-${noticeTypeStyle(item)}`]"
                    :icon="monogramIcon(item)"
                  />
                </span>
                <div class="yc-notice__main">
                  <div class="yc-notice__title-text">{{ item.noticeTitle }}</div>
                  <div class="yc-notice__date">
                    {{ formatDate(item.createTime) }}
                  </div>
                </div>
                <span v-if="!item.isRead" class="yc-notice__dot"></span>
              </div>
            </div>
          </Spin>

          <!-- 底部：查看全部 -->
          <div class="yc-notice__foot">
            <Button block size="small" type="link" @click="handleViewAll">
              查看全部
            </Button>
          </div>
        </div>
      </template>

      <!-- 铃铛触发（含未读数量角标） -->
      <Badge
        :count="unreadCount"
        :offset="[-3, 6]"
        :overflow-count="99"
        class="yc-notice__badge"
      >
        <Button class="yc-notice__bell" type="text">
          <Bell class="yc-notice__bell-icon"/>
        </Button>
      </Badge>
    </Popover>

    <NoticeDetailDrawer ref="detailDrawerRef"/>
  </div>
</template>

<style scoped>
.notice-widget {
  display: inline-flex;
  align-items: center;
  height: 100%;
  margin-right: 4px;
}

/* ===== 铃铛触发 ===== */
.yc-notice__bell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 8px;
}

.yc-notice__bell:hover {
  background: hsl(var(--secondary));
}

.yc-notice__bell-icon {
  width: 18px;
  height: 18px;
  color: hsl(var(--foreground) / 0.82);
}

/* ===== 弹出面板 ===== */
.yc-notice {
  width: 360px;
  overflow: hidden;
}

.yc-notice__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.yc-notice__title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.yc-notice__head-right {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.yc-notice__unread {
  font-size: 12px;
  color: #b7791f;
}

.yc-notice__mark-all {
  padding: 0;
  font-size: 12px;
  color: #1677ff;
}

.yc-notice__mark-all-icon {
  width: 13px;
  height: 13px;
  margin-right: 2px;
  vertical-align: -2px;
}

.yc-notice__list {
  max-height: 320px;
  overflow: auto;
}

.yc-notice__empty {
  padding: 28px 0;
}

/* 公告行 */
.yc-notice__row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.yc-notice__row:hover {
  background: #f6f8fa;
}

/* 已读：整体降饱和 */
.yc-notice__row.is_read .yc-notice__title-text,
.yc-notice__row.is_read .yc-notice__date,
.yc-notice__row.is_read .yc-notice__mono {
  opacity: 0.45;
  filter: grayscale(1);
}

/* 类型 monogram */
.yc-notice__mono {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
}

.yc-notice__mono.is-notify {
  background: #fff8e6;
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, 0.18);
}

.yc-notice__mono.is-announce {
  background: #e8f5e9;
  box-shadow: inset 0 0 0 1px rgba(56, 161, 105, 0.2);
}

.yc-notice__mono-icon {
  width: 16px;
  height: 16px;
}

.yc-notice__mono-icon.is-notify {
  color: #d97706;
}

.yc-notice__mono-icon.is-announce {
  color: #38a169;
}

.yc-notice__main {
  min-width: 0;
  flex: 1;
}

.yc-notice__title-text {
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  line-height: 1.5;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.yc-notice__date {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
}

/* 未读红点 */
.yc-notice__dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f5222d;
  box-shadow: 0 0 0 2px rgba(245, 34, 45, 0.12);
}

.yc-notice__foot {
  border-top: 1px solid #f0f0f0;
}
</style>
