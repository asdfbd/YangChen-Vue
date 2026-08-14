<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';

import dayjs from 'dayjs';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {useAccessStore, useUserStore} from '@vben/stores';

import {Button, Card, Col, Row, Skeleton, Space} from 'ant-design-vue';

import {listDeptApi} from '#/api/system/dept';
import {listMenuApi} from '#/api/system/menu';
import {listNoticeApi} from '#/api/system/notice';
import {listRoleApi} from '#/api/system/role';
import {getUserListApi} from '#/api/system/user';

defineOptions({name: 'Dashboard'});

const router = useRouter();
const userStore = useUserStore();
const accessStore = useAccessStore();

function hasPermi(code: string) {
  const codes = accessStore.accessCodes;
  return codes.includes('*:*:*') || codes.includes(code);
}

const displayName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '朋友',
);

const greeting = computed(() => {
  const hour = dayjs().hour();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const today = computed(() => dayjs().format('YYYY年M月D日 dddd'));

const rolesText = computed(() => userStore.userInfo?.roles?.join('、') || '—');

interface StatItem {
  icon: string;
  label: string;
  loading: boolean;
  value: number | null;
}

const stats = ref<StatItem[]>([
  {icon: 'lucide:users', label: '用户数', loading: true, value: null},
  {icon: 'lucide:shield', label: '角色数', loading: true, value: null},
  {icon: 'lucide:menu', label: '菜单数', loading: true, value: null},
  {icon: 'lucide:building-2', label: '部门数', loading: true, value: null},
]);

const quickLinks = [
  {icon: 'lucide:user-round', name: '用户管理', path: '/system/user', perm: 'system:user:list'},
  {icon: 'lucide:shield-check', name: '角色管理', path: '/system/role', perm: 'system:role:list'},
  {icon: 'lucide:menu', name: '菜单管理', path: '/system/menu', perm: 'system:menu:list'},
  {icon: 'lucide:building-2', name: '部门管理', path: '/system/dept', perm: 'system:dept:list'},
  {icon: 'lucide:book-open', name: '字典管理', path: '/system/dict', perm: 'system:dict:list'},
  {icon: 'lucide:settings-2', name: '参数设置', path: '/system/config', perm: 'system:config:list'},
  {icon: 'lucide:bell', name: '通知公告', path: '/system/notice', perm: 'system:notice:list'},
  {icon: 'lucide:code-2', name: '代码生成', path: '/tool/gen', perm: 'tool:gen:list'},
];

const visibleQuickLinks = computed(() =>
  quickLinks.filter((item) => hasPermi(item.perm)),
);

interface NoticeItem {
  createTime?: string;
  noticeTitle?: string;
}

const notices = ref<NoticeItem[]>([]);

async function loadStats() {
  const tasks: Promise<number | null>[] = [
    hasPermi('system:user:list')
      ? getUserListApi({pageNum: 1, pageSize: 1})
        .then((result) => result.total)
        .catch(() => null)
      : Promise.resolve(null),
    hasPermi('system:role:list')
      ? listRoleApi({pageNum: 1, pageSize: 1})
        .then((result) => result.total)
        .catch(() => null)
      : Promise.resolve(null),
    hasPermi('system:menu:list')
      ? listMenuApi()
        .then((result) => result.length)
        .catch(() => null)
      : Promise.resolve(null),
    hasPermi('system:dept:list')
      ? listDeptApi()
        .then((result) => result.length)
        .catch(() => null)
      : Promise.resolve(null),
  ];
  const values = await Promise.all(tasks);
  stats.value.forEach((item, index) => {
    item.value = values[index] ?? null;
    item.loading = false;
  });
}

async function loadNotices() {
  if (!hasPermi('system:notice:list')) return;
  try {
    const result = await listNoticeApi({pageNum: 1, pageSize: 3});
    notices.value = result.rows;
  } catch {
    // 公告加载失败不阻塞首页
  }
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('MM-DD HH:mm') : '';
}

function go(path: string) {
  router.push(path);
}

onMounted(() => {
  loadStats();
  loadNotices();
});
</script>

<template>
  <Page auto-content-height title="首页">
    <div class="dashboard">
      <Card :bordered="false" class="welcome-card">
        <div class="welcome-inner">
          <div class="welcome-text">
            <div class="welcome-title">{{ greeting }}，{{ displayName }} 👋</div>
            <div class="welcome-sub">今天是 {{ today }}，欢迎回来。</div>
          </div>
          <Space :size="8">
            <Button v-if="hasPermi('system:user:list')" type="primary" @click="go('/system/user')">
              <IconifyIcon class="btn-icon" icon="lucide:user-round"/>
              用户管理
            </Button>
            <Button v-if="hasPermi('tool:gen:list')" @click="go('/tool/gen')">
              <IconifyIcon class="btn-icon" icon="lucide:code-2"/>
              代码生成
            </Button>
          </Space>
        </div>
      </Card>

      <Row :gutter="16">
        <Col v-for="item in stats" :key="item.label" :lg="6" :md="12" :xs="24">
          <Card :bordered="false" class="stat-card">
            <Skeleton v-if="item.loading" active :paragraph="{rows: 1}" :title="false"/>
            <div v-else class="stat-body">
              <div class="stat-icon-wrap">
                <IconifyIcon :icon="item.icon" class="stat-icon"/>
              </div>
              <div class="stat-meta">
                <div class="stat-value">{{ item.value ?? '—' }}</div>
                <div class="stat-label">{{ item.label }}</div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row :gutter="16">
        <Col :lg="15" :md="24" :xs="24">
          <Card :bordered="false" title="快捷入口">
            <div class="quick-grid">
              <button
                v-for="item in visibleQuickLinks"
                :key="item.path"
                class="quick-item"
                type="button"
                @click="go(item.path)"
              >
                <IconifyIcon :icon="item.icon" class="quick-icon"/>
                <span>{{ item.name }}</span>
              </button>
            </div>
          </Card>
        </Col>
        <Col :lg="9" :md="24" :xs="24">
          <Card :bordered="false" class="mb-4" title="系统信息">
            <div class="info-row">
              <span class="info-label">当前用户</span>
              <span>{{ userStore.userInfo?.username || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">姓 名</span>
              <span>{{ userStore.userInfo?.realName || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">角 色</span>
              <span>{{ rolesText }}</span>
            </div>
          </Card>
          <Card :bordered="false" title="最新公告">
            <template v-if="notices.length">
              <div
                v-for="item in notices"
                :key="`${item.noticeTitle}-${item.createTime}`"
                class="notice-item"
              >
                <span class="notice-title">{{ item.noticeTitle }}</span>
                <span class="notice-time">{{ formatTime(item.createTime) }}</span>
              </div>
            </template>
            <div v-else class="empty-tip">暂无公告</div>
          </Card>
        </Col>
      </Row>
    </div>
  </Page>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-card {
  border-radius: 12px;
  background: linear-gradient(120deg, #4f6ef7 0%, #6a5ae0 100%);
}

.welcome-card :deep(.ant-card-body) {
  padding: 24px;
}

.welcome-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.welcome-title {
  color: #fff;
  font-size: 22px;
  font-weight: 600;
}

.welcome-sub {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}

.stat-card {
  border-radius: 12px;
}

.stat-card :deep(.ant-card-body) {
  padding: 20px;
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 10px;
  background: rgba(79, 110, 247, 0.12);
}

.stat-icon {
  width: 24px;
  height: 24px;
  color: #4f6ef7;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-label {
  margin-top: 2px;
  color: #8a94a6;
  font-size: 13px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px 8px;
  border: 1px solid #eceff3;
  border-radius: 10px;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  font-size: 13px;
  color: #303133;
}

.quick-item:hover {
  border-color: #4f6ef7;
  color: #4f6ef7;
  background: rgba(79, 110, 247, 0.06);
  transform: translateY(-2px);
}

.quick-icon {
  width: 22px;
  height: 22px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f2f5;
  font-size: 13px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #8a94a6;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed #f0f2f5;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.notice-time {
  flex-shrink: 0;
  color: #a0a8b5;
  font-size: 12px;
}

.empty-tip {
  padding: 16px 0;
  color: #a0a8b5;
  text-align: center;
  font-size: 13px;
}

@media (max-width: 768px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>