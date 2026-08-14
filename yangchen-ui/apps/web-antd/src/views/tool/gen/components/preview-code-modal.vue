<script lang="ts" setup>
/**
 * 代码预览弹窗：按文件 Tab 展示生成代码内容。
 */
import {ref, watch} from 'vue';

import {message} from 'ant-design-vue';
import {Empty, Modal, Spin, Tabs} from 'ant-design-vue';

import {previewCodeApi} from '#/api/tool/gen';

defineOptions({name: 'GenPreviewCodeModal'});

const props = defineProps<{
  open: boolean;
  tableId?: number | string;
  tableName?: string;
}>();

const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const loading = ref(false);
const codeMap = ref<Record<string, string>>({});
const activeKey = ref('');

/** Tab 只显示文件名，name 用完整路径（后端返回的 key 是相对路径） */
function fileName(path: string) {
  const idx = path.lastIndexOf('/');
  return idx >= 0 ? path.slice(idx + 1) : path;
}

async function loadPreview() {
  if (!props.tableId) return;
  loading.value = true;
  try {
    codeMap.value = await previewCodeApi(props.tableId);
    const keys = Object.keys(codeMap.value);
    activeKey.value = keys[0] ?? '';
  } catch (error: any) {
    message.error(error?.message ?? '预览失败');
  } finally {
    loading.value = false;
  }
}

function close() {
  emit('update:open', false);
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      codeMap.value = {};
      loadPreview();
    }
  },
);
</script>

<template>
  <Modal
    :footer="null"
    :open="open"
    :title="`代码预览 - ${tableName ?? ''}`"
    width="920px"
    @cancel="close"
  >
    <Spin :spinning="loading">
      <Tabs v-if="Object.keys(codeMap).length" v-model:activeKey="activeKey">
        <Tabs.TabPane
          v-for="(content, path) in codeMap"
          :key="path"
          :name="path"
          :tab="fileName(path)"
        >
          <pre class="code-viewer">{{ content }}</pre>
        </Tabs.TabPane>
      </Tabs>
      <Empty v-else-if="!loading" description="暂无代码数据"/>
    </Spin>
  </Modal>
</template>

<style scoped>
.code-viewer {
  max-height: 480px;
  margin: 0;
  padding: 14px 16px;
  overflow: auto;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.7;
  color: #1f2329;
  background: #f8f9fb;
  border: 1px solid #eceff3;
  border-radius: 6px;
  white-space: pre;
}
</style>
