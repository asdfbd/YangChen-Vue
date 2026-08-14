<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue';
import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {Button, Card, Col, Descriptions, Row} from 'ant-design-vue';
import {type CacheInfo, getCacheApi} from '#/api/monitor/cache';

defineOptions({name: 'MonitorCache'});
const loading = ref(false);
const cache = ref<CacheInfo>({});
const info = computed(() => cache.value.info ?? {});

/** 命令统计命令过多，改为图标指标卡展示关键运行指标 */
const iconStats = computed(() => {
  const hits = Number(info.value.keyspace_hits ?? 0);
  const misses = Number(info.value.keyspace_misses ?? 0);
  const total = hits + misses;
  const hitRate = total > 0 ? `${((hits / total) * 100).toFixed(1)}%` : '-';
  return [
    {
      title: '内存消耗',
      value: info.value.used_memory_human ?? '-',
      icon: 'lucide:hard-drive',
      color: '#f97316',
      bg: '#fff7ed'
    },
    {
      title: 'Key 数量',
      value: cache.value.dbSize ?? '-',
      icon: 'lucide:key',
      color: '#1677ff',
      bg: '#e6f4ff'
    },
    {
      title: '客户端数',
      value: info.value.connected_clients ?? '-',
      icon: 'lucide:users',
      color: '#10b981',
      bg: '#ecfdf5'
    },
    {
      title: '每秒命令',
      value: info.value.instantaneous_ops_per_sec ?? '-',
      suffix: ' ops/s',
      icon: 'lucide:zap',
      color: '#8b5cf6',
      bg: '#f5f3ff'
    },
    {title: '命中率', value: hitRate, icon: 'lucide:target', color: '#ef4444', bg: '#fef2f2'},
    {
      title: '运行时长',
      value: info.value.uptime_in_days ?? '-',
      suffix: ' 天',
      icon: 'lucide:clock',
      color: '#f59e0b',
      bg: '#fffbeb'
    },
  ];
});

async function loadData() {
  loading.value = true;
  try {
    cache.value = await getCacheApi();
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
<template>
  <Page auto-content-height>
    <div class="page-head"><span>Redis 缓存运行状态</span>
      <Button size="small" @click="loadData">
        <IconifyIcon icon="lucide:refresh-cw"/>
        刷新
      </Button>
    </div>
    <Card :loading="loading" title="基本信息" class="card">
      <Descriptions :column="4" bordered size="small">
        <Descriptions.Item label="Redis 版本">{{ info.redis_version ?? '-' }}</Descriptions.Item>
        <Descriptions.Item label="运行模式">
          {{ info.redis_mode === 'standalone' ? '单机' : (info.redis_mode ?? '-') }}
        </Descriptions.Item>
        <Descriptions.Item label="端口">{{ info.tcp_port ?? '-' }}</Descriptions.Item>
        <Descriptions.Item label="客户端数">{{ info.connected_clients ?? '-' }}</Descriptions.Item>
        <Descriptions.Item label="运行时长">{{ info.uptime_in_days ?? '-' }} 天</Descriptions.Item>
        <Descriptions.Item label="已用内存">{{ info.used_memory_human ?? '-' }}</Descriptions.Item>
        <Descriptions.Item label="内存上限">{{ info.maxmemory_human ?? '-' }}</Descriptions.Item>
        <Descriptions.Item label="Key 数量">{{ cache.dbSize ?? '-' }}</Descriptions.Item>
      </Descriptions>
    </Card>
    <Row :gutter="16">
      <Col v-for="item in iconStats" :key="item.title" :lg="8" :sm="12" :xs="24">
        <Card class="icon-card" :bordered="false">
          <div class="icon-card-body">
            <div class="icon-tile" :style="{backgroundColor: item.bg, color: item.color}">
              <IconifyIcon :icon="item.icon" class="icon-svg"/>
            </div>
            <div class="icon-card-text">
              <div class="icon-card-title">{{ item.title }}</div>
              <div class="icon-card-value">{{ item.value }}<span v-if="item.suffix"
                                                                 class="icon-card-suffix">{{
                  item.suffix
                }}</span></div>
            </div>
          </div>
        </Card>
      </Col>
    </Row>
  </Page>
</template>
<style scoped>.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #667085;
}

.card {
  margin-bottom: 16px;
}

.icon-card {
  margin-bottom: 16px;
}

.icon-card-body {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 10px;
}

.icon-svg {
  font-size: 22px;
}

.icon-card-title {
  color: #98a2b3;
  font-size: 13px;
}

.icon-card-value {
  margin-top: 4px;
  color: #1d2939;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
}

.icon-card-suffix {
  margin-left: 4px;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 400;
}</style>
