<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {preferences} from '@vben/preferences';
import {useUserStore} from '@vben/stores';

import {Avatar, Card, Spin, Tag} from 'ant-design-vue';

import {getUserProfileApi, type SysUser} from '#/api/system/user';

import AvatarUpload from './avatar-upload.vue';
import ProfileBase from './base-setting.vue';
import ProfilePassword from './password-setting.vue';

defineOptions({name: 'Profile'});

const userStore = useUserStore();

const loading = ref(true);
const user = ref<SysUser>({});
const roleGroup = ref('');
const postGroup = ref('');
const tab = ref<'base' | 'pwd'>('base');
const avatarUploadRef = ref<InstanceType<typeof AvatarUpload>>();

const tabs = [
  {key: 'base', label: '基本资料'},
  {key: 'pwd', label: '修改密码'},
] as const;

const avatarSrc = computed(
  () => userStore.userInfo?.avatar || preferences.app.defaultAvatar,
);

const roleChips = computed(() =>
  roleGroup.value
    ? roleGroup.value
      .split(',')
      .map((r) => r.trim())
      .filter(Boolean)
    : [],
);

const postChips = computed(() =>
  postGroup.value
    ? postGroup.value
      .split(',')
      .map((p) => p.trim())
      .filter(Boolean)
    : [],
);

const facts = computed(() => [
  {label: '手机号码', value: user.value.phonenumber || '—'},
  {label: '邮箱', value: user.value.email || '—'},
  {
    label: '所属部门',
    value: user.value.dept?.deptName || postChips.value[0] || '—',
  },
  {label: '岗位', value: postChips.value.join(' / ') || '—'},
  {label: '创建日期', value: formatDate(user.value.createTime)},
]);

function formatDate(value?: string) {
  if (!value) return '—';
  return value.includes('T')
    ? value.replace('T', ' ').slice(0, 19)
    : value.slice(0, 19);
}

onMounted(async () => {
  try {
    const res = await getUserProfileApi();
    user.value = res.data;
    roleGroup.value = res.roleGroup;
    postGroup.value = res.postGroup;
  } finally {
    loading.value = false;
  }
});

/** 基本资料保存后：同步侧栏展示 + 顶栏昵称 */
function handleBaseSaved(values: SysUser) {
  Object.assign(user.value, values);
  const info = userStore.userInfo;
  if (info && values.nickName && values.nickName !== info.realName) {
    userStore.setUserInfo({...info, realName: values.nickName});
  }
}
</script>

<template>
  <Page auto-content-height title="个人中心">
    <Spin :spinning="loading" wrapper-class-name="profile-spin">
      <div class="profile-page">
        <!-- 身份侧栏 -->
        <Card :bordered="false" class="profile-rail">
          <div
            class="profile-rail__avatar-wrap"
            role="button"
            tabindex="0"
            @click="avatarUploadRef?.open()"
            @keydown.enter.prevent="avatarUploadRef?.open()"
          >
            <Avatar
              :size="96"
              :src="avatarSrc"
              class="profile-rail__avatar"
            />
            <div class="profile-rail__avatar-mask">
              <IconifyIcon
                class="profile-rail__avatar-mask-icon"
                icon="lucide:camera"
              />
              <span>更换头像</span>
            </div>
          </div>

          <div class="profile-rail__name">
            {{ user.nickName || user.userName || '—' }}
          </div>
          <div class="profile-rail__username">@{{ user.userName }}</div>

          <div v-if="roleChips.length" class="profile-rail__roles">
            <Tag
              v-for="r in roleChips"
              :key="r"
              class="profile-rail__role-tag"
              color="blue"
            >
              {{ r }}
            </Tag>
          </div>

          <div class="profile-rail__divider"></div>

          <dl class="profile-rail__facts">
            <div v-for="fact in facts" :key="fact.label" class="profile-fact">
              <dt class="profile-fact__label">{{ fact.label }}</dt>
              <dd class="profile-fact__value">{{ fact.value }}</dd>
            </div>
          </dl>
        </Card>

        <!-- 工作区 -->
        <Card :bordered="false" class="profile-workspace">
          <div class="profile-tabs" role="tablist">
            <button
              v-for="t in tabs"
              :key="t.key"
              :aria-selected="tab === t.key"
              :class="['profile-tabs__item', {active: tab === t.key}]"
              role="tab"
              type="button"
              @click="tab = t.key"
            >
              {{ t.label }}
            </button>
          </div>

          <div class="profile-workspace__content">
            <ProfileBase
              v-if="tab === 'base'"
              :user="user"
              @saved="handleBaseSaved"
            />
            <ProfilePassword v-else/>
          </div>
        </Card>
      </div>
    </Spin>

    <AvatarUpload ref="avatarUploadRef"/>
  </Page>
</template>

<style scoped>
/* Spin 包装链保持全高，使内部 .profile-page 的 height:100% 生效 */
.profile-spin,
.profile-spin :deep(.ant-spin-container) {
  height: 100%;
}

.profile-page {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

/* ===== 身份侧栏 ===== */
.profile-rail {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
}

.profile-rail :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 22px 16px;
  height: 100%;
  box-sizing: border-box;
}

/* 头像：悬停浮现更换遮罩 */
.profile-rail__avatar-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  cursor: pointer;
}

.profile-rail__avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(17, 24, 39, 0.06), 0 6px 18px rgba(17, 24, 39, 0.1);
}

.profile-rail__avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #fff;
  font-size: 12px;
  background: rgba(17, 24, 39, 0.55);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.profile-rail__avatar-wrap:hover .profile-rail__avatar-mask,
.profile-rail__avatar-wrap:focus-visible .profile-rail__avatar-mask {
  opacity: 1;
}

.profile-rail__avatar-wrap:focus-visible {
  border-radius: 50%;
  outline: 2px solid #1677ff;
  outline-offset: 3px;
}

.profile-rail__avatar-mask-icon {
  width: 18px;
  height: 18px;
}

.profile-rail__name {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
  text-align: center;
}

.profile-rail__username {
  margin-top: 2px;
  font-size: 13px;
  color: #6b7280;
}

.profile-rail__roles {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
}

.profile-rail__role-tag {
  margin-inline-end: 0;
  border-radius: 4px;
}

.profile-rail__divider {
  width: 100%;
  margin: 18px 0 10px;
  border-top: 1px solid #eef1f5;
}

.profile-rail__facts {
  width: 100%;
  margin: 0;
}

.profile-fact {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed #eef1f5;
}

.profile-fact:last-child {
  border-bottom: none;
}

.profile-fact__label {
  flex-shrink: 0;
  font-size: 12px;
  color: #9ca3af;
}

.profile-fact__value {
  min-width: 0;
  font-size: 13px;
  color: #374151;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ===== 工作区 ===== */
.profile-workspace {
  flex: 1;
  min-width: 0;
  background: #fff;
}

.profile-workspace :deep(.ant-card-body) {
  padding: 24px 28px 28px;
  height: 100%;
  box-sizing: border-box;
}

/* 自定义 Tab：细线滑动指示 */
.profile-tabs {
  display: flex;
  gap: 28px;
  border-bottom: 1px solid #eef1f5;
}

.profile-tabs__item {
  position: relative;
  padding: 6px 2px 14px;
  font-size: 14px;
  color: #6b7280;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s ease;
}

.profile-tabs__item::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  content: '';
  background: transparent;
  border-radius: 2px;
  transition: background 0.2s ease;
}

.profile-tabs__item:hover {
  color: #374151;
}

.profile-tabs__item.active {
  color: #1677ff;
  font-weight: 600;
}

.profile-tabs__item.active::after {
  background: #1677ff;
}

.profile-workspace__content {
  padding-top: 24px;
}

/* 窄屏：纵向堆叠 */
@media (max-width: 768px) {
  .profile-page {
    flex-direction: column;
    height: auto;
  }

  .profile-rail {
    width: 100%;
  }
}
</style>
