<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {Page} from '@vben/common-ui';
import {
  Button,
  Card,
  Col,
  message,
  Modal,
  Row,
  Space,
  Spin,
  TypographyParagraph
} from 'ant-design-vue';
import {
  clearCacheAllApi,
  clearCacheKeyApi,
  clearCacheNameApi,
  getCacheValueApi,
  listCacheKeyApi,
  listCacheNameApi,
  type CacheName
} from '#/api/monitor/cache';

defineOptions({name: 'MonitorCacheList'});
const loading = ref(false);
const names = ref<CacheName[]>([]);
const keys = ref<string[]>([]);
const value = ref('');
const selectedName = ref<CacheName>({});
const selectedKey = ref('');

function stringify(data: unknown) {
  return typeof data === 'string' ? data : JSON.stringify(data, null, 2);
}

async function loadNames() {
  loading.value = true;
  try {
    names.value = await listCacheNameApi();
    const active = selectedName.value.cacheName;
    if (active && !names.value.some((n) => n.cacheName === active)) {
      selectedName.value = {};
      keys.value = [];
      value.value = '';
    }
  } finally {
    loading.value = false;
  }
}

async function chooseName(name: CacheName) {
  selectedName.value = name;
  selectedKey.value = '';
  value.value = '';
  keys.value = await listCacheKeyApi(name.cacheName ?? '');
}

async function chooseKey(key: string) {
  selectedKey.value = key;
  const {cacheValue} = await getCacheValueApi(selectedName.value.cacheName ?? '', key);
  value.value = stringify(cacheValue);
}

function clearName() {
  const {cacheName = '', remark = ''} = selectedName.value;
  Modal.confirm({
    content: `确定清理缓存“${remark || cacheName}”吗？`,
    okType: 'danger',
    title: '清理缓存',
    async onOk() {
      await clearCacheNameApi(cacheName);
      message.success('清理成功');
      await chooseName(selectedName.value);
    }
  });
}

function clearKey() {
  Modal.confirm({
    content: '确定清理当前缓存键吗？',
    okType: 'danger',
    title: '清理缓存',
    async onOk() {
      await clearCacheKeyApi(selectedKey.value);
      message.success('清理成功');
      await chooseName(selectedName.value);
    }
  });
}

function clearAll() {
  Modal.confirm({
    content: '确定清理全部缓存吗？此操作不可恢复。',
    okType: 'danger',
    title: '清理全部缓存',
    async onOk() {
      await clearCacheAllApi();
      message.success('清理成功');
      selectedName.value = {};
      keys.value = [];
      value.value = '';
      await loadNames();
    }
  });
}

onMounted(loadNames);
</script>
<template>
  <Page auto-content-height>
    <Card :bordered="false">
      <div class="toolbar">
        <Space>
          <Button danger @click="clearAll">清理全部缓存</Button>
          <Button @click="loadNames">刷新</Button>
        </Space>
      </div>
      <Spin :spinning="loading">
        <Row :gutter="16">
          <Col :lg="7" :xs="24">
            <Card size="small" title="缓存名称">
              <div v-for="name in names" :key="name.cacheName"
                   :class="['list-item', {active: selectedName.cacheName === name.cacheName}]"
                   @click="chooseName(name)">
                <div class="list-item-title">{{ name.remark || name.cacheName }}</div>
                <div class="list-item-sub">{{ name.cacheName }}</div>
              </div>
              <div v-if="!names.length" class="empty">暂无缓存</div>
            </Card>
          </Col>
          <Col :lg="8" :xs="24">
            <Card size="small" title="缓存键">
              <template #extra>
                <Button v-if="selectedName.cacheName" danger size="small" type="link"
                        @click="clearName">清空名称
                </Button>
              </template>
              <div v-for="key in keys" :key="key"
                   :class="['list-item', {active: selectedKey === key}]" @click="chooseKey(key)">
                {{ key }}
              </div>
              <div v-if="selectedName.cacheName && !keys.length" class="empty">暂无缓存键</div>
            </Card>
          </Col>
          <Col :lg="9" :xs="24">
            <Card size="small" title="缓存内容">
              <template #extra>
                <Button v-if="selectedKey" danger size="small" type="link" @click="clearKey">
                  删除此键
                </Button>
              </template>
              <TypographyParagraph v-if="value" class="content">
                <pre>{{ value }}</pre>
              </TypographyParagraph>
              <div v-else class="empty">选择缓存键查看内容</div>
            </Card>
          </Col>
        </Row>
      </Spin>
    </Card>
  </Page>
</template>
<style scoped>.toolbar {
  margin-bottom: 16px;
}

.list-item {
  padding: 8px 10px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.list-item:hover, .list-item.active {
  color: #1677ff;
  background: #e6f4ff;
}

.list-item-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.list-item-sub {
  overflow: hidden;
  margin-top: 2px;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #98a2b3;
  font-size: 12px;
}

.empty {
  padding: 28px 0;
  color: #98a2b3;
  text-align: center;
}

.content {
  max-height: 540px;
  overflow: auto;
}

.content pre {
  white-space: pre-wrap;
  word-break: break-all;
}</style>
