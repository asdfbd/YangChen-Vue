<script lang="ts" setup>
import {renderMarkdown} from './render-markdown';

import type {ChatMessage} from './types';

defineOptions({name: 'ChatMessageText'});

defineProps<{message: ChatMessage}>();
</script>

<template>
  <div class="cmt-text" v-html="renderMarkdown(message.content)"></div>
</template>

<style scoped>
.cmt-text {
  font-size: inherit;
  line-height: inherit;
}

.cmt-text :deep(strong) {
  font-weight: 600;
  color: hsl(var(--primary));
}

.cp-code-inline {
  padding: 1px 6px;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  color: hsl(var(--primary));
  border-radius: 5px;
  background: hsl(var(--primary) / 0.12);
}

.cp-code {
  margin: 8px 0 4px;
  padding: 10px 12px;
  overflow-x: auto;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.65;
  color: #e2e8f0;
  border-radius: 10px;
  background: #111827;
}
</style>
