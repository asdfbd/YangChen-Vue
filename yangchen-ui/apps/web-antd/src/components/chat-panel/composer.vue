<script lang="ts" setup>
import {computed, nextTick, ref} from 'vue';

import {IconifyIcon} from '@vben/icons';

defineOptions({name: 'ChatComposer'});

/**
 * 输入区组件（textarea + 发送键）
 * 尺寸通过宿主传入的 --cc-* CSS 变量定制（panel 紧凑 / page 大输入框）
 * busy 时发送键切换为停止键，点击触发 stop 事件
 */

const props = withDefaults(
  defineProps<{
    placeholder?: string;
    maxLength?: number;
    disabled?: boolean;
    /** 生成中：发送键切换为停止键 */
    busy?: boolean;
  }>(),
  {
    placeholder: '输入你的问题，Enter 发送，Shift + Enter 换行',
    maxLength: 2000,
    disabled: false,
    busy: false,
  },
);

const emit = defineEmits<{
  submit: [text: string];
  stop: [];
}>();

const draft = ref('');
const textareaRef = ref<HTMLElement>();

const canSend = computed(() => draft.value.trim().length > 0 && !props.disabled);

function submit() {
  const content = draft.value.trim();
  if (!content || props.disabled) return;
  draft.value = '';
  resetHeight();
  emit('submit', content);
}

function stop() {
  emit('stop');
}

function handleKeydown(e: KeyboardEvent) {
  // 中文输入法组词中不触发发送
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault();
    submit();
  }
}

function autosize(e: Event) {
  const el = e.target as HTMLTextAreaElement;
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight, 130)}px`;
}

function resetHeight() {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto';
    }
  });
}

function focus() {
  nextTick(() => textareaRef.value?.focus());
}

defineExpose({focus});
</script>

<template>
  <div :class="['cc-composer', {'cc-composer--busy': busy}]">
    <textarea
      ref="textareaRef"
      v-model="draft"
      class="cc-textarea"
      rows="1"
      :maxlength="maxLength"
      :placeholder="placeholder"
      :disabled="disabled"
      aria-label="输入问题"
      @input="autosize"
      @keydown="handleKeydown"
    ></textarea>
    <button
      v-if="!busy"
      class="cc-send"
      :disabled="!canSend"
      aria-label="发送"
      title="发送"
      @click="submit"
    >
      <IconifyIcon icon="lucide:send"/>
    </button>
    <button
      v-else
      class="cc-send cc-send--stop"
      type="button"
      aria-label="停止生成"
      title="停止生成"
      @click="stop"
    >
      <IconifyIcon icon="lucide:square"/>
    </button>
  </div>
</template>

<style scoped>
/* 尺寸由宿主通过 --cc-* 变量定制（panel / page 两种形态） */
.cc-composer {
  --cc-pad: 10px 10px 10px 16px;
  --cc-radius: 18px;
  --cc-font: 14px;
  --cc-send: 38px;

  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: var(--cc-pad);
  border: 1px solid hsl(var(--border));
  border-radius: var(--cc-radius);
  background: hsl(var(--card) / 0.94);
  box-shadow:
    0 10px 28px -14px rgba(15, 23, 42, 0.22),
    0 2px 6px -2px rgba(15, 23, 42, 0.08);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.cc-composer:focus-within {
  border-color: hsl(var(--primary) / 0.5);
  box-shadow:
    0 0 0 3px hsl(var(--primary) / 0.12),
    0 10px 28px -14px rgba(15, 23, 42, 0.22);
}

/* 生成中：输入框整体高亮 + 主色描边 */
.cc-composer--busy {
  border-color: hsl(var(--primary) / 0.45);
  box-shadow:
    0 0 0 3px hsl(var(--primary) / 0.12),
    0 10px 28px -14px rgba(15, 23, 42, 0.22);
}

.cc-textarea {
  flex: 1;
  max-height: 130px;
  padding: 6px 0;
  font-family: inherit;
  font-size: var(--cc-font);
  line-height: 1.7;
  color: hsl(var(--foreground));
  resize: none;
  border: none;
  outline: none;
  background: transparent;
}

.cc-textarea::placeholder {
  color: hsl(var(--muted-foreground));
}

.cc-send {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: var(--cc-send);
  height: var(--cc-send);
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: 12px;
  background: hsl(var(--primary));
  box-shadow: 0 6px 14px -4px hsl(var(--primary) / 0.5);
  cursor: pointer;
  transition:
    transform 0.16s,
    opacity 0.16s,
    box-shadow 0.16s;
}

.cc-send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px -4px hsl(var(--primary) / 0.6);
}

.cc-send:active:not(:disabled) {
  transform: scale(0.94);
}

.cc-send:disabled {
  opacity: 0.38;
  cursor: not-allowed;
  box-shadow: none;
}

.cc-send :deep(svg) {
  width: 17px;
  height: 17px;
}

/* 停止键：方形图标 + 柔和扩散脉冲，提示"正在生成，可随时停止" */
.cc-send--stop::after {
  position: absolute;
  inset: -4px;
  content: '';
  pointer-events: none;
  border: 1px solid hsl(var(--primary) / 0.55);
  border-radius: 16px;
  animation: cc-stop-pulse 1.7s ease-out infinite;
}

.cc-send--stop:hover {
  box-shadow: 0 0 0 4px hsl(var(--primary) / 0.16);
}

.cc-send--stop :deep(svg) {
  width: 15px;
  height: 15px;
}

@keyframes cc-stop-pulse {
  0% {
    opacity: 0.9;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(1.4);
  }
}

@media (prefers-reduced-motion: reduce) {
  .cc-send--stop::after {
    animation: none;
    opacity: 0.4;
  }
}
</style>