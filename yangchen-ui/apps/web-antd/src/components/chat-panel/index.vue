<script lang="ts" setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';

import {IconifyIcon} from '@vben/icons';

import ChatComposer from './composer.vue';
import ChatMessageList from './message-list.vue';
import {genId, nowTime} from './utils';

import type {
  AiUiPayload,
  ChatMessage,
  ChatRendererMap,
  ChatSendHandler,
  ChatStreamHandle,
  ChatUiAction,
} from './types';

defineOptions({name: 'ChatPanel'});

/**
 * 通用 AI 对话面板（可复用组件）
 *
 * 设计为"受控组件"：消息通过 v-model 由调用方持有，
 * 数据层（发送/流式/持久化）全部由调用方注入，本组件只负责界面与交互。
 *
 * 两种形态：
 * - variant="panel"：紧凑面板（悬浮助手小窗等），气泡式消息
 * - variant="page"：全页对话（类似 DeepSeek / ChatGPT），
 *   开场为居中欢迎页（Logo + 标题 + 大输入框 + 提问卡片），
 *   对话后为通栏流式消息 + 底部大输入区
 *
 * 扩展点：
 * - `send` 发送处理器：返回字符串则自动追加助手回复；调用 onChunk 推送增量（实时 Markdown 渲染）；
 *   第三个参数 signal 用于响应「停止生成」，见 types.ts
 * - `busy` 外部忙碌状态：控制"正在输入"指示与输入禁用（调用方自行驱动流式时传 true）
 * - 生成期间发送键自动变为停止键，点击触发 stop 事件，并向 send 传入的 signal 发出中止
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
    /** 欢迎语（page 形态开场展示，缺省用 subtitle） */
    description?: string;
    /** 助手头像图标（iconify 名称） */
    avatarIcon?: string;
    /** 输入框占位文案 */
    placeholder?: string;
    /** 底部说明文案 */
    hint?: string;
    /** 快捷提问（对话刚开始时展示） */
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
    /** 形态：panel（紧凑面板）/ page（全页对话） */
    variant?: 'panel' | 'page';
  }>(),
  {
    title: '小辰',
    badge: 'AI',
    subtitle: '在线 · 随时待命',
    description: '',
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
    variant: 'panel',
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
  /** 点击"停止生成" / 流式被中止 */
  stop: [];
  /** 业务 UI 组件触发的确认、提交等操作，由页面宿主接管 */
  'ui-action': [payload: ChatUiAction];
}>();

const messages = computed(() => props.modelValue);

/** 内部等待 send 返回的 Promise */
const pending = ref(false);
const listRef = ref<HTMLElement>();
const composerRef = ref<InstanceType<typeof ChatComposer>>();

/** 一次提交对应的流式会话状态（仅在提交期间非空） */
interface StreamSession {
  signal: ChatStreamHandle;
  baseMessages: ChatMessage[];
  assistantId: string;
  streamed: boolean;
  streamedContent: string;
  uiMessages: ChatMessage[];
  buffer: string;
  scheduled: boolean;
}

let streamSession: StreamSession | null = null;
/** 当前在途请求的中止句柄（stop / 卸载时使用，宿主也可主动 abort） */
const activeSignal = ref<ChatStreamHandle | null>(null);
/** 正在流式输出的助手消息 id（光标指示 + 隐藏打字点） */
const streamingId = ref<string | null>(null);
/** 用户是否停留在消息底部（流式期间仅在底部时自动滚动） */
const stickToBottom = ref(true);
/** 用户离最新消息较远时显示「回到底部」快捷按钮。 */
const showBackToBottom = ref(false);

const isBusy = computed(() => props.busy || pending.value);

const isPage = computed(() => props.variant === 'page');

/** page 形态：对话为空时展示欢迎页 */
const showHero = computed(
  () => isPage.value && messages.value.length === 0 && !isBusy.value,
);

/** panel 形态：对话刚开始（只有问候语）时展示快捷提问卡片 */
const showStarter = computed(
  () =>
    !isPage.value &&
    props.suggestions.length > 0 &&
    !isBusy.value &&
    messages.value.length <= 1,
);

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

/** 用户滚动时记录是否停留在底部（距底 < 120px 视为吸底） */
function onThreadScroll() {
  const el = listRef.value;
  if (!el) return;
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
  stickToBottom.value = distanceToBottom < 120;
  showBackToBottom.value = distanceToBottom > 180;
}

function scrollToBottom() {
  if (!stickToBottom.value) return;
  nextTick(() => {
    const el = listRef.value;
    if (el) {
      el.scrollTop = el.scrollHeight;
      showBackToBottom.value = false;
    }
  });
}

/** 手动回到最新消息；流式输出随后恢复自动吸底。 */
function scrollToLatestMessage() {
  const el = listRef.value;
  if (!el) return;
  stickToBottom.value = true;
  showBackToBottom.value = false;
  el.scrollTo({top: el.scrollHeight, behavior: 'smooth'});
}

watch(
  [() => props.modelValue, isBusy],
  () => scrollToBottom(),
  {deep: true},
);

// 列表末尾消息变化（新增消息 / 切换会话）时恢复吸底，便于流式连续滚动
let lastTailId = '';
watch(
  () => props.modelValue[props.modelValue.length - 1]?.id,
  (id) => {
    if (id && id !== lastTailId) {
      lastTailId = id;
      stickToBottom.value = true;
    }
  },
);

/** 把缓冲中的增量一次性写入消息流（合并渲染，避免每块都触发整段 Markdown 重绘） */
function flushStreamChunks() {
  const session = streamSession;
  if (!session) return;
  session.scheduled = false;
  if (!session.buffer) return;
  session.streamed = true;
  session.streamedContent += session.buffer;
  session.buffer = '';
  streamingId.value = session.assistantId;
  emitStreamMessages(session);
  scrollToBottom();
}

function emitStreamMessages(session: StreamSession) {
  const nextMessages = [...session.baseMessages];
  if (session.streamedContent) {
    nextMessages.push({
      id: session.assistantId,
      role: 'assistant',
      content: session.streamedContent,
      time: nowTime(),
    });
  }
  nextMessages.push(...session.uiMessages);
  emit('update:modelValue', nextMessages);
}

function scheduleFlush() {
  const session = streamSession;
  if (!session || session.scheduled) return;
  session.scheduled = true;
  if (typeof requestAnimationFrame === 'function') {
    requestAnimationFrame(() => flushStreamChunks());
  } else {
    flushStreamChunks();
  }
}

function finishStream() {
  const session = streamSession;
  if (session) {
    if (!session.signal.aborted) {
      flushStreamChunks();
    }
    // 已中止时不落盘残余缓冲（≤1 帧），避免增量写入已切换的会话
    session.buffer = '';
  }
  streamSession = null;
  streamingId.value = null;
  pending.value = false;
}

/** 停止生成：先落盘未刷新增量，再向宿主 signal 发出中止 */
function stopGenerating() {
  if (!streamSession && !isBusy.value) return;
  const signal = activeSignal.value ?? streamSession?.signal;
  flushStreamChunks();
  if (signal) {
    signal.abort();
  }
  streamingId.value = null;
  pending.value = false;
  streamSession = null;
  activeSignal.value = null;
  emit('stop');
}

async function submit(text: string) {
  const content = text.trim();
  if (!content || isBusy.value) return;
  const userMessage: ChatMessage = {
    id: genId(),
    role: 'user',
    content,
    time: nowTime(),
  };
  const baseMessages = [...props.modelValue, userMessage];
  emit('update:modelValue', baseMessages);
  emit('send-start', content);

  const handler = props.send;
  if (!handler) return;

  // 本次提交的流式中止句柄：停止按钮 / 组件卸载 / 宿主主动取消共用
  let aborted = false;
  const abortListeners: Array<() => void> = [];
  const signal: ChatStreamHandle = {
    get aborted() {
      return aborted;
    },
    onAbort(callback: () => void) {
      if (aborted) {
        callback();
      } else {
        abortListeners.push(callback);
      }
    },
    abort() {
      if (aborted) return;
      aborted = true;
      abortListeners.splice(0).forEach((callback) => callback());
    },
  };

  const assistantId = genId();
  streamSession = {
    signal,
    baseMessages,
    assistantId,
    streamed: false,
    streamedContent: '',
    uiMessages: [],
    buffer: '',
    scheduled: false,
  };
  activeSignal.value = signal;
  stickToBottom.value = true;

  const onChunk = (chunk: string) => {
    if (!chunk || !streamSession || streamSession.assistantId !== assistantId) {
      return;
    }
    streamSession.streamed = true;
    streamSession.buffer += chunk;
    scheduleFlush();
  };

  const onUiMessage = (payload: AiUiPayload) => {
    if (!streamSession || streamSession.assistantId !== assistantId) return;
    if (payload.replaceText) {
      streamSession.buffer = '';
      streamSession.streamed = false;
      streamSession.streamedContent = '';
      streamingId.value = null;
    }
    streamSession.uiMessages.push({
      id: payload.messageId || genId(),
      role: 'assistant',
      content: '',
      time: nowTime(),
      type: payload.component,
      extra: {ui: payload},
    });
    emitStreamMessages(streamSession);
    scrollToBottom();
  };

  const result = handler(content, onChunk, signal, onUiMessage);
  if (result && typeof (result as PromiseLike<unknown>).then === 'function') {
    pending.value = true;
    try {
      const reply = await result;
      if (
        typeof reply === 'string' &&
        reply &&
        !aborted &&
        !streamSession?.streamed &&
        streamSession?.uiMessages.length === 0
      ) {
        pushMessage('assistant', reply);
      }
    } finally {
      finishStream();
      activeSignal.value = null;
    }
  } else {
    // 同步 handler：调用方自行管理 busy，仅收尾本次流式资源
    finishStream();
    activeSignal.value = null;
  }
}

/** 供宿主调用，用于选择器等业务 UI 触发的即时发送。 */
defineExpose({sendText: submit});

function focusComposer() {
  nextTick(() => composerRef.value?.focus());
}

onMounted(() => {
  if (props.autofocus) {
    focusComposer();
  }
});

onBeforeUnmount(() => {
  // 面板销毁时中止在途生成，保留已生成的部分
  stopGenerating();
});

// 新对话回到欢迎页时聚焦输入框
watch(showHero, (val) => {
  if (val) {
    focusComposer();
  }
});
</script>

<template>
  <div :class="['chat-panel', `chat-panel--${variant}`]">
    <!-- ============ 全页对话形态（DeepSeek / GPT 式） ============ -->
    <template v-if="isPage">
      <header v-if="showHeader" class="cp-head cp-head--page">
        <div class="cp-head__inner">
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
        </div>
      </header>

      <div class="cp-stage">
        <!-- 欢迎页 -->
        <div v-if="showHero" class="cp-hero">
          <div class="cp-hero__logo">
            <IconifyIcon :icon="avatarIcon"/>
          </div>
          <h1 class="cp-hero__title">
            {{ title }}<em v-if="badge">{{ badge }}</em>
          </h1>
          <p class="cp-hero__desc">{{ description || subtitle }}</p>
          <div class="cp-hero__composer">
            <ChatComposer
              ref="composerRef"
              :placeholder="placeholder"
              :max-length="maxLength"
              :disabled="isBusy"
              :busy="isBusy"
              @stop="stopGenerating"
              @submit="submit"
            />
          </div>
          <div v-if="suggestions.length" class="cp-hero__suggestions">
            <button
              v-for="q in suggestions"
              :key="q"
              class="cp-sug"
              @click="submit(q)"
            >
              <IconifyIcon icon="lucide:sparkle" class="cp-sug__icon"/>
              <span class="cp-sug__text">{{ q }}</span>
              <IconifyIcon icon="lucide:arrow-up-right" class="cp-sug__arrow"/>
            </button>
          </div>
          <div v-if="hint" class="cp-hint">
            <IconifyIcon icon="lucide:info" class="cp-hint__icon"/>
            {{ hint }}
          </div>
        </div>

        <!-- 对话区 -->
        <div v-else class="cp-page-thread">
          <div ref="listRef" class="cp-thread" @scroll.passive="onThreadScroll">
            <ChatMessageList
              :messages="messages"
              :busy="isBusy && !streamingId"
              :streaming-id="streamingId"
              :avatar-icon="avatarIcon"
              :renderers="renderers"
              variant="page"
              @ui-action="emit('ui-action', $event)"
            />
          </div>
          <Transition name="cp-back-bottom">
            <button
              v-if="showBackToBottom"
              class="cp-back-bottom"
              type="button"
              title="回到最新消息"
              aria-label="回到最新消息"
              @click="scrollToLatestMessage"
            >
              <IconifyIcon icon="lucide:arrow-down-to-line"/>
              <span>回到底部</span>
            </button>
          </Transition>
          <div class="cp-dock">
            <div class="cp-dock__inner">
              <ChatComposer
                ref="composerRef"
                :placeholder="placeholder"
                :max-length="maxLength"
                :disabled="isBusy"
                :busy="isBusy"
                @stop="stopGenerating"
                @submit="submit"
              />
              <div v-if="hint" class="cp-hint">
                <IconifyIcon icon="lucide:info" class="cp-hint__icon"/>
                {{ hint }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 紧凑面板形态（悬浮小窗等） ============ -->
    <template v-else>
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

      <div ref="listRef" class="cp-body" @scroll.passive="onThreadScroll">
        <ChatMessageList
          :messages="messages"
          :busy="isBusy && !streamingId"
          :streaming-id="streamingId"
          :avatar-icon="avatarIcon"
          :renderers="renderers"
          variant="panel"
          @ui-action="emit('ui-action', $event)"
        />

        <!-- 开场引导：对话刚开始时展示快捷提问卡片 -->
        <Transition name="cp-starter">
          <div v-if="showStarter" class="cp-starter">
            <div class="cp-starter__hint">你可以这样问我：</div>
            <div class="cp-starter__grid">
              <button
                v-for="q in suggestions"
                :key="q"
                class="cp-starter__item"
                @click="submit(q)"
              >
                <IconifyIcon icon="lucide:sparkle" class="cp-starter__icon"/>
                <span class="cp-starter__text">{{ q }}</span>
                <IconifyIcon
                  icon="lucide:arrow-up-right"
                  class="cp-starter__arrow"
                />
              </button>
            </div>
          </div>
        </Transition>
      </div>

      <footer class="cp-foot">
        <ChatComposer
          ref="composerRef"
          :placeholder="placeholder"
          :max-length="maxLength"
          :disabled="isBusy"
          :busy="isBusy"
          @stop="stopGenerating"
          @submit="submit"
        />
        <div v-if="hint" class="cp-hint">
          <IconifyIcon icon="lucide:info" class="cp-hint__icon"/>
          {{ hint }}
        </div>
      </footer>
    </template>
  </div>
</template>

<style scoped>
/* ============================================================
   主题 token：全部消费项目全局 CSS 变量，随系统主题自动适配
   组件根节点透明，背景由宿主（悬浮壳 / 页面容器）提供
   ============================================================ */
.chat-panel {
  --cp-text: hsl(var(--foreground));
  --cp-sub: hsl(var(--muted-foreground));
  --cp-line: hsl(var(--border));
  --cp-card: hsl(var(--card));
  --cp-primary: hsl(var(--primary));
  --cp-primary-soft: hsl(var(--primary) / 0.08);
  --cp-primary-border: hsl(var(--primary) / 0.32);
  --cp-success: hsl(var(--success));
  --cp-gradient: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18),
      transparent 42%,
      rgba(0, 0, 0, 0.16)
    ),
    hsl(var(--primary));

  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

/* ============================================================
   头部（两种形态共用）
   ============================================================ */
.cp-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px 13px;
}

.cp-head--page {
  position: sticky;
  top: 0;
  z-index: 10;
  justify-content: center;
  padding: 18px 24px 10px;
  border-bottom: 1px solid hsl(var(--border) / 0.55);
  background: hsl(var(--card) / 0.36);
}

.cp-head__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 960px;
}

.cp-head__brand {
  display: flex;
  align-items: center;
  gap: 11px;
}

.cp-orb {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: hsl(var(--primary-foreground));
  border: 1px solid hsl(var(--primary) / 0.18);
  border-radius: 11px;
  background: hsl(var(--primary) / 0.1);
  box-shadow: none;
}

.cp-orb :deep(svg) {
  width: 17px;
  height: 17px;
}

.cp-head__title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--cp-text);
  letter-spacing: 0.5px;
}

.cp-head__title em {
  margin-left: 6px;
  padding: 0 6px;
  font-size: 10px;
  font-style: normal;
  font-weight: 600;
  vertical-align: 2px;
  color: var(--cp-primary);
  border-radius: 6px;
  background: var(--cp-primary-soft);
}

.cp-head__status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  font-size: 11px;
  color: var(--cp-sub);
}

.cp-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cp-success);
  box-shadow: 0 0 0 3px hsl(var(--success) / 0.18);
}

.cp-head__actions {
  display: flex;
  gap: 2px;
}

.cp-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--cp-sub);
  border: none;
  border-radius: 9px;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}

.cp-icon-btn:hover {
  color: var(--cp-text);
  background: hsl(var(--muted));
}

.cp-icon-btn :deep(svg) {
  width: 15px;
  height: 15px;
}

/* ============================================================
   page 形态：舞台 / 欢迎页 / 对话区
   ============================================================ */
.cp-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 欢迎页 */
.cp-hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px 56px;
}

.cp-hero__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  color: hsl(var(--primary));
  border: 1px solid hsl(var(--primary) / 0.18);
  border-radius: 22px;
  background: linear-gradient(145deg, hsl(var(--card)), hsl(var(--primary) / 0.1));
  box-shadow: 0 18px 40px -24px hsl(var(--primary) / 0.5);
}

.cp-hero__logo :deep(svg) {
  width: 30px;
  height: 30px;
}

.cp-hero__title {
  margin-top: 20px;
  font-size: 25px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--cp-text);
  letter-spacing: 0.2px;
}

.cp-hero__title em {
  margin-left: 8px;
  padding: 2px 8px;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  vertical-align: 4px;
  color: var(--cp-primary);
  border-radius: 7px;
  background: var(--cp-primary-soft);
}

.cp-hero__desc {
  max-width: 560px;
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  text-align: center;
  color: var(--cp-sub);
}

.cp-hero__composer {
  width: 100%;
  max-width: 760px;
  margin-top: 28px;
}

.cp-hero__suggestions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  max-width: 760px;
  margin-top: 14px;
}

.cp-sug {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 58px;
  padding: 13px 16px;
  font-size: 14px;
  text-align: left;
  color: var(--cp-text);
  border: 1px solid var(--cp-line);
  border-radius: 12px;
  background: hsl(var(--card) / 0.82);
  cursor: pointer;
  transition:
    border-color 0.18s,
    transform 0.18s,
    box-shadow 0.18s,
    background 0.18s;
}

.cp-sug:hover {
  border-color: var(--cp-primary-border);
  background: var(--cp-card);
  transform: translateY(-1px);
  box-shadow: 0 12px 26px -12px hsl(var(--primary) / 0.35);
}

.cp-sug:focus-visible,
.cp-icon-btn:focus-visible {
  outline: 2px solid hsl(var(--primary) / 0.65);
  outline-offset: 2px;
}

.cp-sug__icon {
  flex-shrink: 0;
  color: var(--cp-primary);
}

.cp-sug__icon :deep(svg) {
  width: 16px;
  height: 16px;
}

.cp-sug__text {
  flex: 1;
  min-width: 0;
}

.cp-sug__arrow {
  flex-shrink: 0;
  opacity: 0.35;
}

.cp-sug__arrow :deep(svg) {
  width: 15px;
  height: 15px;
}

/* 对话区 */
.cp-page-thread {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cp-thread {
  flex: 1;
  padding: 20px 24px 8px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(107, 114, 128, 0.28) transparent;
}

.cp-thread::-webkit-scrollbar {
  width: 5px;
}

.cp-thread::-webkit-scrollbar-thumb {
  border-radius: 99px;
  background: rgba(107, 114, 128, 0.28);
}

.cp-thread :deep(.cml-list) {
  max-width: 960px;
  margin: 0 auto;
}

/* 居中放在输入框上方，便于在长内容中快速回到最新消息。 */
.cp-back-bottom {
  position: absolute;
  z-index: 4;
  left: 50%;
  bottom: 84px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 17px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  color: hsl(var(--primary-foreground));
  border: 1px solid hsl(var(--primary) / 0.55);
  border-radius: 20px;
  background: var(--cp-gradient);
  box-shadow: 0 12px 28px -13px hsl(var(--primary) / 0.8);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transform: translateX(-50%);
  transition:
    border-color 0.16s,
    box-shadow 0.16s,
    transform 0.16s;
}

.cp-back-bottom:hover {
  border-color: hsl(var(--primary));
  box-shadow: 0 16px 30px -13px hsl(var(--primary) / 0.88);
  transform: translate(-50%, -2px);
}

.cp-back-bottom:focus-visible {
  outline: 2px solid hsl(var(--primary) / 0.65);
  outline-offset: 2px;
}

.cp-back-bottom :deep(svg) {
  width: 17px;
  height: 17px;
}

.cp-dock {
  display: flex;
  flex-shrink: 0;
  justify-content: center;
  padding: 4px 24px 2px;
}

.cp-dock__inner {
  width: 100%;
  max-width: 960px;
}

/* page 形态：大输入框（通过 --cc-* 变量放大 Composer） */
.chat-panel--page :deep(.cc-composer) {
  --cc-pad: 16px 16px 16px 20px;
  --cc-radius: 16px;
  --cc-font: 16px;
  --cc-send: 44px;
  box-shadow:
    0 18px 42px -26px hsl(var(--primary) / 0.42),
    0 4px 10px -4px rgba(15, 23, 42, 0.08);
}

/* ============================================================
   panel 形态：消息区 + 底部
   ============================================================ */
.cp-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 16px 18px 10px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(107, 114, 128, 0.28) transparent;
}

.cp-body::-webkit-scrollbar {
  width: 5px;
}

.cp-body::-webkit-scrollbar-thumb {
  border-radius: 99px;
  background: rgba(107, 114, 128, 0.28);
}

.cp-foot {
  flex-shrink: 0;
  padding: 10px 18px 18px;
}

/* panel 形态开场引导 */
.cp-starter {
  margin-top: 2px;
}

.cp-starter__hint {
  margin-bottom: 10px;
  padding-left: 2px;
  font-size: 12px;
  color: var(--cp-sub);
}

.cp-starter__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.cp-starter__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  font-size: 13px;
  text-align: left;
  color: var(--cp-text);
  border: 1px solid var(--cp-line);
  border-radius: 14px;
  background: var(--cp-card);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  cursor: pointer;
  transition:
    border-color 0.18s,
    transform 0.18s,
    box-shadow 0.18s;
}

.cp-starter__item:hover {
  border-color: var(--cp-primary-border);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px -10px hsl(var(--primary) / 0.3);
}

.cp-starter__icon {
  flex-shrink: 0;
  color: var(--cp-primary);
}

.cp-starter__icon :deep(svg) {
  width: 15px;
  height: 15px;
}

.cp-starter__text {
  flex: 1;
  min-width: 0;
}

.cp-starter__arrow {
  flex-shrink: 0;
  opacity: 0.35;
}

.cp-starter__arrow :deep(svg) {
  width: 14px;
  height: 14px;
}

/* ============================================================
   底部说明
   ============================================================ */
.cp-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--cp-sub);
}

.cp-hint__icon {
  width: 11px;
  height: 11px;
}

/* ============================================================
   过渡动画（进入 ease-out；尊重 reduced-motion）
   ============================================================ */
.cp-starter-enter-active,
.cp-starter-leave-active {
  transition: all 0.26s cubic-bezier(0.2, 0.8, 0.3, 1);
}

.cp-starter-enter-from,
.cp-starter-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.cp-back-bottom-enter-active,
.cp-back-bottom-leave-active {
  transition: all 0.18s ease;
}

.cp-back-bottom-enter-from,
.cp-back-bottom-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}

@media (prefers-reduced-motion: reduce) {
  .cp-starter {
    transition: none !important;
  }

  .cp-back-bottom {
    transition: none !important;
  }
}

/* ============================================================
   小屏适配
   ============================================================ */
@media (max-width: 640px) {
  .cp-hero__suggestions {
    grid-template-columns: 1fr;
  }

  .cp-thread,
  .cp-dock {
    padding-right: 14px;
    padding-left: 14px;
  }
}
</style>
