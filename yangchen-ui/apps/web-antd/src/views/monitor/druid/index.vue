<script lang="ts" setup>
import {computed} from 'vue';
import {Page} from '@vben/common-ui';
import {useAppConfig} from '@vben/hooks';

defineOptions({name: 'MonitorDruid'});
const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);
const source = computed(() => `${apiURL.replace(/\/$/, '')}/druid/login.html`);
</script>
<template>
  <Page auto-content-height>
    <div class="druid-frame">
      <iframe :src="source" frameborder="0" title="Druid 数据源监控"/>
    </div>
  </Page>
</template>
<style scoped>.druid-frame {
  height: calc(100vh - 190px);
  min-height: 620px;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
}

.druid-frame iframe {
  width: 100%;
  height: 100%;
}</style>
