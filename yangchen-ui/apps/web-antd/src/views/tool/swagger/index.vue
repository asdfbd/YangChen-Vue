<script lang="ts" setup>
import {computed} from 'vue';
import {useAppConfig} from '@vben/hooks';

defineOptions({name: 'ToolSwagger'});
const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);
const source = computed(() => `${apiURL.replace(/\/$/, '')}/doc.html`);
</script>
<template>
  <!--<Page auto-content-height>-->
    <div class="swagger-frame">
      <iframe :src="source" frameborder="0" title="系统接口文档"/>
    </div>
  <!--</Page>-->
</template>
<style scoped>.swagger-frame {
  height: 100%;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
}

.swagger-frame iframe {
  width: 100%;
  height: 100%;
}</style>
