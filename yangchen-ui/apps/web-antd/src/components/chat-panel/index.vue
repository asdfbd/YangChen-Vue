<script lang="ts" setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue';

import {IconifyIcon} from '@vben/icons';

import ChatMessageText from './message-text.vue';
import {genId, nowTime} from './utils';

import type {ChatMessage, ChatRendererMap, ChatSendHandler} from './types';

defineOptions({name: 'ChatPanel'});

/**
 * 通用 AI 对话面板（可复用组件）
 *
 * 设计为"受控组件"：消息通过 v-model 由调用方持有，
 * 数据层（发送/流式/持久化）全部由调用方注入，本组件只负责界面与交互。
 *
 * 扩展点：
 * - `send` 发送处理器：返回字符串则自动追加助手回复；返回 void 则由调用方自行推送（流式）
 * - `busy` 外部忙碌状态：控制"正在输入"指示与输入禁用（流式进行中传 true）
 * - `renderers` 消息渲染器注册表：按 message.type 分发到自定义组件（props: {message}）
 * - `#header-actions` 插槽：自定义头部右侧操作区
 * - `closable` / `newChatable` / `showHeader`：按需裁剪部件
 */
const props = withDefaults(
  defineProps<{
    /** 消息列表（v-model） */
    modelValue: ChatMessage[];
    /** 面板标题 */
    title?: string;
    /** 标题徽标文字（如 AI / 业务），空字符串隐藏 */
    badge?: string;
    /** 副标题（状态行） */
    subtitle?: string;
    /** 助手头像图标（iconify 名称） */
    avatarIcon?: string;
    /** 输入框占位文案 */
    placeholder?: string;
    /** 底部说明文案 */
    hint?: string;
    /** 快捷提问（消息数 <= 1 时展示） */
    suggestions?: string[];
    /** 发送处理器（见 types.ts） */
    send?: ChatSendHandler;
    /** 外部忙碌状态（流式进行中） */
    busy?: boolean;
    /** 消息渲染器注册表（type -> 组件） */
    renderers?: ChatRendererMap;
    /** 输入最大长度 */
    maxLength?: number;
    /** 是否显示头部 */
    showHeader?: boolean;
    /** 是否显示"新对话"按钮 */
    newChatable?: boolean;
    /** 是否显示"收起/关闭"按钮 */
    closable?: boolean;
    /** 挂载后自动聚焦输入框 */
    autofocus?: boolean;
  }>(),
  {
    title: '小辰',
    badge: 'AI',
    subtitle: '在线 · 随时待命',
    avatarIcon: 'lucide:sparkles',
    placeholder: '输入你的问题，Enter 发送，Shift + Enter 换行',
    hint: '',
    suggestions: () => [],
    busy: false,
    renderers: () => ({}),
    maxLength: 2000,
    showHeader: true,
    newChatable: true,
    closable: true,
    autofocus: false,
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: ChatMessage[]];
  /** 用户提交消息（流式场景可监听后自行触发发送） */
  'send-start': [text: string];
  /** 点击"新对话" */
  'new-chat': [];
  /** 点击"收起/关闭" */
  close: [];
}>();

const messages = computed(() => props.modelValue);

const draft = ref('');
/** 内部等待 send 返回的 Promise */
const pending = ref(false);
const listRef = ref<HTMLElement>();
const textareaRef = ref<HTMLElement>();

const isBusy = computed(() => props.busy || pending.value);

const canSend = computed(() => draft.value.trim().length > 0 && !isBusy.value);

/** 仅当对话刚开始（只有问候语）时展示快捷提问 */
const showSuggestions = computed(
  () =>
    props.suggestions.length > 0 &&
    !isBusy.value &&
    messages.value.length <= 1,
);

/** 自定义渲染器是否命中该消息类型 */
function hasRenderer(msg: ChatMessage): boolean {
  return Boolean(msg.type && props.renderers[msg.type]);
}

function rendererFor(msg: ChatMessage) {
  return (msg.type && props.renderers[msg.type]) || ChatMessageText;
}

function pushMessage(
  role: ChatMessage['role'],
  content: string,
  extra?: Record<string, unknown>,
) {
  const msg: ChatMessage = {id: genId(), role, content, time: nowTime()};
  if (extra) {
    msg.extra = extra;
  }
  emit('update:modelValue', [...props.modelValue, msg]);
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

watch(
  [() => props.modelValue, isBusy],
  () => scrollToBottom(),
  {deep: true},
);

async function submit(text?: string) {
  const content = (text ?? draft.value).trim();
  if (!content || isBusy.value) return;
  draft.value = '';
  resetTextarea();
  pushMessage('user', content);
  emit('send-start', content);

  const handler = props.send;
  if (!handler) return;
  const result = handler(content);
  if (result && typeof (result as PromiseLike<unknown>).then === 'function') {
    pending.value = true;
    try {
      const reply = await result;
      if (typeof reply === 'string' && reply) {
        pushMessage('assistant', reply);
      }
    } finally {
      pending.value = false;
    }
  }
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
  el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
}

function resetTextarea() {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto';
    }
  });
}

onMounted(() => {
  if (props.autofocus) {
    nextTick(() => textareaRef.value?.focus());
  }
});
</script>

<template>
  <div class="chat-panel">
    <!-- 头部 -->
    <header v-if="showHeader" class="cp-head">
      <div class="cp-head__brand">
        <span class="cp-orb">
          <IconifyIcon :icon="avatarIcon"/>
        </span>
        <div class="cp-head__text">
          <div class="cp-head__title">
            {{ title }}<em v-if="badge">{{ badge }}</em>
          </div>
          <div class="cp-head__status">
            <i class="cp-status-dot"></i>
            {{ subtitle }}
          </div>
        </div>
      </div>
      <div class="cp-head__actions">
        <slot name="header-actions">
          <button
            v-if="newChatable"
            class="cp-icon-btn"
            title="新对话"
            aria-label="新对话"
            @click="emit('new-chat')"
          >
            <IconifyIcon icon="lucide:rotate-ccw"/>
          </button>
          <button
            v-if="closable"
            class="cp-icon-btn"
            title="收起"
            aria-label="收起面板"
            @click="emit('close')"
          >
            <IconifyIcon icon="lucide:chevron-down"/>
          </button>
        </slot>
      </div>
    </header>

    <!-- 消息区 -->
    <div ref="listRef" class="cp-body">
      <TransitionGroup name="cp-msg">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['cp-msg', `cp-msg--${msg.role}`, {'is-card': hasRenderer(msg)}]"
        >
          <template v-if="msg.role === 'assistant'">
            <span class="cp-avatar">
              <IconifyIcon :icon="avatarIcon"/>
            </span>
            <div :class="['cp-bubble', {'cp-bubble--wide': hasRenderer(msg)}]">
              <component :is="rendererFor(msg)" :message="msg"/>
              <span class="cp-bubble__time">{{ msg.time }}</span>
            </div>
          </template>
          <template v-else>
            <div class="cp-bubble">
              <div class="cp-bubble__text">{{ msg.content }}</div>
              <span class="cp-bubble__time">{{ msg.time }}</span>
            </div>
            <span class="cp-avatar cp-avatar--user">
              <IconifyIcon icon="lucide:user"/>
            </span>
          </template>
        </div>
      </TransitionGroup>

      <!-- 正在输入 -->
      <Transition name="cp-msg">
        <div v-if="isBusy" class="cp-msg cp-msg--assistant">
          <span class="cp-avatar">
            <IconifyIcon :icon="avatarIcon"/>
          </span>
          <div class="cp-bubble cp-bubble--typing">
            <span class="cp-typing-dot"></span>
            <span class="cp-typing-dot"></span>
            <span class="cp-typing-dot"></span>
          </div>
        </div>
      </Transition>
    </div>

    <!-- 底部输入 -->
    <footer class="cp-foot">
      <Transition name="cp-chips">
        <div v-if="showSuggestions" class="cp-chips">
          <button
            v-for="q in suggestions"
            :key="q"
            class="cp-chip"
            @click="submit(q)"
          >
            <IconifyIcon icon="lucide:sparkle" class="cp-chip__icon"/>
            {{ q }}
          </button>
        </div>
      </Transition>

      <div class="cp-inputbar">
        <textarea
          ref="textareaRef"
          v-model="draft"
          class="cp-textarea"
          rows="1"
          :maxlength="maxLength"
          :placeholder="placeholder"
          aria-label="输入问题"
          @input="autosize"
          @keydown="handleKeydown"
        ></textarea>
        <button
          class="cp-send"
          :disabled="!canSend"
          aria-label="发送"
          @click="submit()"
        >
          <IconifyIcon icon="lucide:send"/>
        </button>
      </div>

      <div v-if="hint" class="cp-hint">
        <IconifyIcon icon="lucide:info" class="cp-hint__icon"/>
        {{ hint }}
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ============================================================
   主题 token：全部消费项目全局 CSS 变量，随系统主题自动适配
   ============================================================ */
.chat-panel {
  --cp-bg: hsl(var(--card));
  --cp-text: hsl(var(--foreground));
  --cp-sub: hsl(var(--muted-foreground));
  --cp-line: hsl(var(--border));
  --cp-surface: hsl(var(--muted));
  --cp-primary: hsl(var(--primary));
  --cp-primary-soft: hsl(var(--primary) / 0.08);
  --cp-chip-bg: hsl(var(--primary) / 0.06);
  --cp-chip-border: hsl(var(--primary) / 0.3);
  --cp-code-inline-bg: hsl(var(--primary) / 0.12);
  --cp-code-inline-text: hsl(var(--primary));
  --cp-success: hsl(var(--success));
  --cp-gradient: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.16),
      transparent 40%,
      rgba(0, 0, 0, 0.18)
    ),
    hsl(var(--primary));

  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--cp-bg);
}

/* ============================================================
   头部
   ============================================================ */
.cp-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 12px 16px;
  border-bottom: 1px solid var(--cp-line);
}

.cp-head__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cp-orb {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: hsl(var(--primary-foreground));
  border-radius: 50%;
  background: var(--cp-gradient);
  box-shadow: 0 4px 10px -2px hsl(var(--primary) / 0.5);
}

.cp-orb :deep(svg) {
  width: 16px;
  height: 16px;
}

.cp-head__title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--cp-text);
  letter-spacing: 0.5px;
}

.cp-head__title em {
  margin-left: 5px;
  padding: 0 5px;
  font-size: 10px;
  font-style: normal;
  font-weight: 600;
  vertical-align: 2px;
  color: var(--cp-primary);
  border-radius: 5px;
  background: var(--cp-primary-soft);
}

.cp-head__status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
  font-size: 11px;
  color: var(--cp-sub);
}

.cp-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cp-success);
  box-shadow: 0 0 0 3px hsl(var(--success) / 0.2);
}

.cp-head__actions {
  display: flex;
  gap: 2px;
}

.cp-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: var(--cp-sub);
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}

.cp-icon-btn:hover {
  color: var(--cp-text);
  background: var(--cp-surface);
}

.cp-icon-btn :deep(svg) {
  width: 15px;
  height: 15px;
}

/* ============================================================
   消息区
   ============================================================ */
.cp-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 16px 8px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(107, 114, 128, 0.3) transparent;
}

.cp-body::-webkit-scrollbar {
  width: 5px;
}

.cp-body::-webkit-scrollbar-thumb {
  border-radius: 99px;
  background: rgba(107, 114, 128, 0.3);
}

.cp-msg {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.cp-msg--user {
  flex-direction: row-reverse;
}

.cp-avatar {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.cp-msg--assistant > .cp-avatar {
  color: hsl(var(--primary-foreground));
  background: var(--cp-gradient);
  box-shadow: 0 3px 8px -2px hsl(var(--primary) / 0.45);
}

.cp-msg--user > .cp-avatar {
  color: var(--cp-sub);
  border: 1px solid var(--cp-line);
  background: var(--cp-surface);
}

.cp-avatar :deep(svg) {
  width: 13px;
  height: 13px;
}

.cp-bubble {
  position: relative;
  max-width: 78%;
  padding: 9px 12px;
  font-size: 13.5px;
  line-height: 1.75;
  word-break: break-word;
}

.cp-msg--assistant .cp-bubble {
  color: var(--cp-text);
  border: 1px solid var(--cp-line);
  border-radius: 4px 14px 14px 14px;
  background: var(--cp-surface);
}

.cp-msg--user .cp-bubble {
  color: hsl(var(--primary-foreground));
  border-radius: 14px 4px 14px 14px;
  background: var(--cp-gradient);
  box-shadow: 0 5px 14px -5px hsl(var(--primary) / 0.5);
}

/* 自定义渲染器消息：放宽宽度，便于业务卡片展示 */
.cp-msg--assistant .cp-bubble--wide {
  max-width: 96%;
}

.cp-bubble__text {
  font-size: inherit;
  line-height: inherit;
}

.cp-msg--user .cp-bubble__text :deep(strong) {
  color: hsl(var(--primary-foreground));
}

.cp-bubble__time {
  display: block;
  margin-top: 3px;
  font-size: 10px;
  opacity: 0.5;
}

/* 正在输入 */
.cp-bubble--typing {
  display: flex;
  gap: 5px;
  padding: 13px 15px;
}

.cp-typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cp-primary);
  animation: cp-blink 1.1s infinite ease-in-out;
}

.cp-typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.cp-typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes cp-blink {
  0%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

/* ============================================================
   底部输入
   ============================================================ */
.cp-foot {
  flex-shrink: 0;
  padding: 10px 14px 12px;
  border-top: 1px solid var(--cp-line);
}

.cp-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.cp-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  font-size: 12px;
  color: var(--cp-primary);
  border: 1px solid var(--cp-chip-border);
  border-radius: 999px;
  background: var(--cp-chip-bg);
  cursor: pointer;
  transition:
    background 0.18s,
    transform 0.18s,
    box-shadow 0.18s;
}

.cp-chip:hover {
  background: hsl(var(--primary) / 0.14);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px -4px hsl(var(--primary) / 0.4);
}

.cp-chip__icon {
  width: 12px;
  height: 12px;
}

.cp-inputbar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 6px 6px 6px 12px;
  border: 1px solid var(--cp-line);
  border-radius: 12px;
  background: var(--cp-surface);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.cp-inputbar:focus-within {
  border-color: hsl(var(--primary) / 0.55);
  box-shadow: 0 0 0 3px hsl(var(--primary) / 0.12);
}

.cp-textarea {
  flex: 1;
  max-height: 120px;
  padding: 5px 0;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--cp-text);
  resize: none;
  border: none;
  outline: none;
  background: transparent;
}

.cp-textarea::placeholder {
  color: var(--cp-sub);
}

.cp-send {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: 10px;
  background: var(--cp-gradient);
  box-shadow: 0 4px 10px -2px hsl(var(--primary) / 0.5);
  cursor: pointer;
  transition:
    transform 0.15s,
    opacity 0.15s;
}

.cp-send:hover:not(:disabled) {
  transform: translateY(-1px);
}

.cp-send:active:not(:disabled) {
  transform: scale(0.95);
}

.cp-send:disabled {
  opacity: 0.38;
  cursor: not-allowed;
  box-shadow: none;
}

.cp-send :deep(svg) {
  width: 15px;
  height: 15px;
}

.cp-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 9px;
  font-size: 10.5px;
  color: var(--cp-sub);
}

.cp-hint__icon {
  width: 11px;
  height: 11px;
}

/* ============================================================
   过渡动画（进入 ease-out，退出更快；尊重 reduced-motion）
   ============================================================ */
.cp-msg-enter-active {
  transition: all 0.26s cubic-bezier(0.2, 0.8, 0.3, 1);
}

.cp-msg-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.cp-chips-enter-active,
.cp-chips-leave-active {
  transition: all 0.2s ease;
}

.cp-chips-enter-from,
.cp-chips-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (prefers-reduced-motion: reduce) {
  .cp-msg,
  .cp-chips {
    transition: none !important;
  }

  .cp-typing-dot {
    animation: none !important;
  }
}
</style>
