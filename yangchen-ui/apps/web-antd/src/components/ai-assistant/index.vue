<script lang="ts" setup>
import {computed, nextTick, ref, watch} from 'vue';

import {IconifyIcon} from '@vben/icons';
import {usePreferences} from '@vben/preferences';
import {useUserStore} from '@vben/stores';

import {
  buildGreeting,
  genId,
  nowTime,
  QUICK_QUESTIONS,
  resolveReply,
  type ChatMessage,
} from './knowledge';

defineOptions({name: 'AiAssistant'});

/**
 * 扬辰 AI 智能助手（全局悬浮组件）
 * - 挂载于 basic.vue 布局，全站页面可见
 * - 当前为界面演示模式：回复由本地知识库（knowledge.ts）生成，
 *   接入后端大模型接口时只需替换 resolveReply 的实现
 */

const STORAGE_KEY = 'yangchen-ai-assistant-v2';

/** 悬浮球尺寸与间距（与 CSS 保持一致） */
const FAB_SIZE = 52;
const FAB_MARGIN = 24;
const PANEL_WIDTH = 400;
const FAB_GAP = 16;

const {isDark} = usePreferences();
const userStore = useUserStore();

const open = ref(false);
const typing = ref(false);
const draft = ref('');
const listRef = ref<HTMLElement>();
const textareaRef = ref<HTMLElement>();
const dragging = ref(false);

/** 悬浮球被拖拽后的位置（left/top，相对视口）；null 表示未拖拽，使用默认右下角 */
const fabPos = ref<{left: number; top: number} | null>(null);

const fabStyle = computed(() => {
  const pos = fabPos.value;
  return pos ? {left: `${pos.left}px`, top: `${pos.top}px`} : {};
});

/** 面板展开位置：跟随悬浮球，优先在悬浮球上方，上方空间不足时改为下方 */
const panelStyle = computed(() => {
  const pos = fabPos.value;
  if (!pos) return {};
  const vw = window.innerWidth;
  const vh = window.innerHeight;
  const left = Math.max(
    12,
    Math.min(pos.left + FAB_SIZE - PANEL_WIDTH, vw - PANEL_WIDTH - 12),
  );
  const panelHeight = Math.min(640, vh - 160);
  if (pos.top < panelHeight + FAB_GAP + 60) {
    // 悬浮球位于上半屏：面板在其下方展开
    return {left: `${left}px`, top: `${pos.top + FAB_SIZE + FAB_GAP}px`};
  }
  // 默认在悬浮球上方展开
  return {left: `${left}px`, bottom: `${vh - pos.top + FAB_GAP}px`};
});

const messages = ref<ChatMessage[]>([]);
const quickQuestions = QUICK_QUESTIONS;

const displayName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '',
);

const canSend = computed(() => draft.value.trim().length > 0 && !typing.value);

/** 仅当对话刚开始（只有问候语）时展示快捷提问 */
const showSuggestions = computed(
  () => !typing.value && messages.value.length <= 1,
);

function createMessage(
  role: ChatMessage['role'],
  content: string,
): ChatMessage {
  return {id: genId(), role, content, time: nowTime()};
}

function pushMessage(role: ChatMessage['role'], content: string) {
  messages.value.push(createMessage(role, content));
  scrollToBottom();
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

async function send(text?: string) {
  const content = (text ?? draft.value).trim();
  if (!content || typing.value) return;
  draft.value = '';
  resetTextarea();
  pushMessage('user', content);
  typing.value = true;
  scrollToBottom();

  // 模拟思考耗时；接入真实接口后，此处替换为 await sendAiChatApi(content)
  const delay = 600 + Math.random() * 900;
  window.setTimeout(async () => {
    const reply = await resolveReply(content);
    typing.value = false;
    pushMessage('assistant', reply);
  }, delay);
}

function handleKeydown(e: KeyboardEvent) {
  // 中文输入法组词中不触发发送
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault();
    send();
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

function handleNewChat() {
  messages.value = [createMessage('assistant', buildGreeting(displayName.value))];
  scrollToBottom();
}

/* ===== 会话持久化（localStorage） ===== */
interface StoredState {
  fab?: {left: number; top: number} | null;
  messages?: ChatMessage[];
  open?: boolean;
}

function loadFromStorage(): StoredState | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const data = JSON.parse(raw) as StoredState;
    return data && typeof data === 'object' ? data : null;
  } catch {
    // 解析失败则忽略，回到默认问候
  }
  return null;
}

const storedState = loadFromStorage();
const stored = storedState?.messages;
messages.value =
  stored && stored.length
    ? stored
    : [createMessage('assistant', buildGreeting(displayName.value))];

if (storedState?.open) {
  open.value = true;
}
if (storedState?.fab) {
  fabPos.value = storedState.fab;
}

watch(
  () => ({fab: fabPos.value, messages: messages.value, open: open.value}),
  (state) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // 存储不可用时静默降级
    }
  },
  {deep: true},
);

/* ===== 悬浮球拖拽（位移阈值 5px，避免误触） ===== */
function clampFab(left: number, top: number) {
  const margin = 12;
  const maxLeft = Math.max(margin, window.innerWidth - FAB_SIZE - margin);
  const maxTop = Math.max(margin, window.innerHeight - FAB_SIZE - margin);
  return {
    left: Math.min(Math.max(margin, left), maxLeft),
    top: Math.min(Math.max(margin, top), maxTop),
  };
}

function onPointerDown(e: PointerEvent) {
  if (e.button !== 0) return;
  const startX = e.clientX;
  const startY = e.clientY;
  const current = fabPos.value;
  const originLeft = current?.left ?? window.innerWidth - FAB_SIZE - FAB_MARGIN;
  const originTop = current?.top ?? window.innerHeight - FAB_SIZE - FAB_MARGIN;
  let moved = false;

  const onMove = (ev: PointerEvent) => {
    const dx = ev.clientX - startX;
    const dy = ev.clientY - startY;
    if (!moved && Math.hypot(dx, dy) < 5) return;
    moved = true;
    ev.preventDefault();
    dragging.value = true;
    fabPos.value = clampFab(originLeft + dx, originTop + dy);
  };

  const onUp = () => {
    window.removeEventListener('pointermove', onMove);
    window.removeEventListener('pointerup', onUp);
    window.removeEventListener('pointercancel', onUp);
    dragging.value = false;
    // 未发生拖动（位移 < 5px）时视为点击，打开面板
    if (!moved) {
      open.value = true;
    }
  };

  window.addEventListener('pointermove', onMove, {passive: false});
  window.addEventListener('pointerup', onUp);
  window.addEventListener('pointercancel', onUp);
}

/* ===== 面板交互：自动聚焦 + Esc 关闭 ===== */
function onGlobalKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    open.value = false;
  }
}

watch(open, (val) => {
  if (val) {
    nextTick(() => textareaRef.value?.focus());
    window.addEventListener('keydown', onGlobalKeydown);
  } else {
    window.removeEventListener('keydown', onGlobalKeydown);
  }
});

/* ===== 轻量 Markdown 渲染（先转义再格式化，杜绝 XSS） ===== */
function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderContent(text: string): string {
  const blocks: string[] = [];
  let html = text.replace(/```([\s\S]*?)```/g, (_match, code: string) => {
    blocks.push(code.trim());
    return `\u0000AI_CODE_${blocks.length - 1}\u0000`;
  });
  html = escapeHtml(html);
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/`([^`\n]+)`/g, '<code class="ai-code-inline">$1</code>');
  html = html.replace(/\n/g, '<br/>');
  html = html.replace(/\u0000AI_CODE_(\d+)\u0000/g, (_match, i: string) => {
    return `<pre class="ai-code"><code>${escapeHtml(blocks[Number(i)] ?? '')}</code></pre>`;
  });
  return html;
}
</script>

<template>
  <Teleport to="body">
    <!-- ===== 聊天面板 ===== -->
    <Transition name="ai-panel">
      <section
        v-if="open"
        :class="['ai-panel', {'is-dark': isDark}]"
        :style="panelStyle"
        role="dialog"
        aria-label="AI 智能助手"
      >
        <!-- 头部 -->
        <header class="ai-head">
          <div class="ai-head__brand">
            <span class="ai-orb">
              <IconifyIcon icon="lucide:sparkles"/>
            </span>
            <div class="ai-head__text">
              <div class="ai-head__title">
                小辰<em>AI</em>
              </div>
              <div class="ai-head__status">
                <i class="ai-status-dot"></i>
                在线 · 随时待命
              </div>
            </div>
          </div>
          <div class="ai-head__actions">
            <button
              class="ai-icon-btn"
              title="新对话"
              aria-label="新对话"
              @click="handleNewChat"
            >
              <IconifyIcon icon="lucide:rotate-ccw"/>
            </button>
            <button
              class="ai-icon-btn"
              title="收起"
              aria-label="收起面板"
              @click="open = false"
            >
              <IconifyIcon icon="lucide:chevron-down"/>
            </button>
          </div>
        </header>

        <!-- 消息区 -->
        <div ref="listRef" class="ai-body">
          <TransitionGroup name="ai-msg">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['ai-msg', `ai-msg--${msg.role}`]"
            >
              <template v-if="msg.role === 'assistant'">
                <span class="ai-avatar">
                  <IconifyIcon icon="lucide:sparkles"/>
                </span>
                <div class="ai-bubble">
                  <div
                    class="ai-bubble__text"
                    v-html="renderContent(msg.content)"
                  ></div>
                  <span class="ai-bubble__time">{{ msg.time }}</span>
                </div>
              </template>
              <template v-else>
                <div class="ai-bubble">
                  <div class="ai-bubble__text">{{ msg.content }}</div>
                  <span class="ai-bubble__time">{{ msg.time }}</span>
                </div>
                <span class="ai-avatar ai-avatar--user">
                  <IconifyIcon icon="lucide:user"/>
                </span>
              </template>
            </div>
          </TransitionGroup>

          <!-- 正在输入 -->
          <Transition name="ai-msg">
            <div v-if="typing" class="ai-msg ai-msg--assistant">
              <span class="ai-avatar">
                <IconifyIcon icon="lucide:sparkles"/>
              </span>
              <div class="ai-bubble ai-bubble--typing">
                <span class="ai-typing-dot"></span>
                <span class="ai-typing-dot"></span>
                <span class="ai-typing-dot"></span>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 底部输入 -->
        <footer class="ai-foot">
          <Transition name="ai-chips">
            <div v-if="showSuggestions" class="ai-chips">
              <button
                v-for="q in quickQuestions"
                :key="q"
                class="ai-chip"
                @click="send(q)"
              >
                <IconifyIcon icon="lucide:sparkle" class="ai-chip__icon"/>
                {{ q }}
              </button>
            </div>
          </Transition>

          <div class="ai-inputbar">
            <textarea
              ref="textareaRef"
              v-model="draft"
              class="ai-textarea"
              rows="1"
              :maxlength="2000"
              placeholder="输入你的问题，Enter 发送，Shift + Enter 换行"
              aria-label="输入问题"
              @input="autosize"
              @keydown="handleKeydown"
            ></textarea>
            <button
              class="ai-send"
              :disabled="!canSend"
              aria-label="发送"
              @click="send()"
            >
              <IconifyIcon icon="lucide:send"/>
            </button>
          </div>

          <div class="ai-hint">
            <IconifyIcon icon="lucide:info" class="ai-hint__icon"/>
            演示模式 · 回复来自本地知识库，后端接口接入后自动升级
          </div>
        </footer>
      </section>
    </Transition>

    <!-- ===== 悬浮入口（可拖拽） ===== -->
    <Transition name="ai-fab">
      <button
        v-if="!open"
        :class="['ai-fab', {'is-dark': isDark, 'is-dragging': dragging}]"
        :style="fabStyle"
        aria-label="打开 AI 智能助手"
        @pointerdown="onPointerDown"
      >
        <IconifyIcon icon="lucide:sparkles" class="ai-fab__icon"/>
        <span class="ai-fab__dot"></span>
        <span class="ai-fab__tip">小辰 · AI 助手</span>
      </button>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ============================================================
   主题 token：全部消费项目全局 CSS 变量（--primary / --card /
   --foreground / --muted / --border 等），随系统主题与主色自动适配
   ============================================================ */
.ai-panel {
  --ai-bg: hsl(var(--card));
  --ai-text: hsl(var(--foreground));
  --ai-sub: hsl(var(--muted-foreground));
  --ai-line: hsl(var(--border));
  --ai-surface: hsl(var(--muted));
  --ai-primary: hsl(var(--primary));
  --ai-primary-soft: hsl(var(--primary) / 0.08);
  --ai-chip-bg: hsl(var(--primary) / 0.06);
  --ai-chip-border: hsl(var(--primary) / 0.3);
  --ai-code-bg: #111827;
  --ai-code-text: #e2e8f0;
  --ai-code-inline-bg: hsl(var(--primary) / 0.12);
  --ai-code-inline-text: hsl(var(--primary));
  --ai-success: hsl(var(--success));
  /* 主色渐变（用黑白叠层模拟深浅，兼容所有浏览器） */
  --ai-gradient: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.16),
      transparent 40%,
      rgba(0, 0, 0, 0.18)
    ),
    hsl(var(--primary));
  --ai-shadow: 0 8px 20px -6px rgba(15, 23, 42, 0.12),
    0 24px 48px -16px rgba(15, 23, 42, 0.22);
}

.ai-panel.is-dark {
  --ai-shadow: 0 8px 20px -6px rgba(0, 0, 0, 0.5),
    0 24px 48px -16px rgba(0, 0, 0, 0.6);
}

/* ============================================================
   面板
   ============================================================ */
.ai-panel {
  position: fixed;
  right: 24px;
  bottom: 92px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  width: min(400px, calc(100vw - 32px));
  height: min(640px, calc(100vh - 160px));
  overflow: hidden;
  border: 1px solid var(--ai-line);
  border-radius: 18px;
  background: var(--ai-bg);
  box-shadow: var(--ai-shadow);
  transform-origin: right bottom;
}

/* 顶部细渐变高光线（品牌点睛） */
.ai-panel::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  z-index: 1;
  height: 2px;
  pointer-events: none;
  content: '';
  background: linear-gradient(
    90deg,
    hsl(var(--primary)),
    hsl(var(--primary) / 0.55) 50%,
    hsl(var(--primary))
  );
  opacity: 0.9;
}

/* ============================================================
   头部
   ============================================================ */
.ai-head {
  position: relative;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 12px 16px;
  border-bottom: 1px solid var(--ai-line);
}

.ai-head__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-orb {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: hsl(var(--primary-foreground));
  border-radius: 50%;
  background: var(--ai-gradient);
  box-shadow: 0 4px 10px -2px hsl(var(--primary) / 0.5);
}

.ai-orb :deep(svg) {
  width: 16px;
  height: 16px;
}

.ai-head__title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--ai-text);
  letter-spacing: 0.5px;
}

.ai-head__title em {
  margin-left: 5px;
  padding: 0 5px;
  font-size: 10px;
  font-style: normal;
  font-weight: 600;
  vertical-align: 2px;
  color: var(--ai-primary);
  border-radius: 5px;
  background: var(--ai-primary-soft);
}

.ai-head__status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
  font-size: 11px;
  color: var(--ai-sub);
}

.ai-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ai-success);
  box-shadow: 0 0 0 3px hsl(var(--success) / 0.2);
}

.ai-head__actions {
  display: flex;
  gap: 2px;
}

.ai-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: var(--ai-sub);
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}

.ai-icon-btn:hover {
  color: var(--ai-text);
  background: var(--ai-surface);
}

.ai-icon-btn :deep(svg) {
  width: 15px;
  height: 15px;
}

/* ============================================================
   消息区
   ============================================================ */
.ai-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 16px 8px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(107, 114, 128, 0.3) transparent;
}

.ai-body::-webkit-scrollbar {
  width: 5px;
}

.ai-body::-webkit-scrollbar-thumb {
  border-radius: 99px;
  background: rgba(107, 114, 128, 0.3);
}

.ai-msg {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.ai-msg--user {
  flex-direction: row-reverse;
}

.ai-avatar {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.ai-msg--assistant > .ai-avatar {
  color: hsl(var(--primary-foreground));
  background: var(--ai-gradient);
  box-shadow: 0 3px 8px -2px hsl(var(--primary) / 0.45);
}

.ai-msg--user > .ai-avatar {
  color: var(--ai-sub);
  border: 1px solid var(--ai-line);
  background: var(--ai-surface);
}

.ai-avatar :deep(svg) {
  width: 13px;
  height: 13px;
}

.ai-bubble {
  position: relative;
  max-width: 78%;
  padding: 9px 12px;
  font-size: 13.5px;
  line-height: 1.75;
  word-break: break-word;
}

.ai-msg--assistant .ai-bubble {
  color: var(--ai-text);
  border: 1px solid var(--ai-line);
  border-radius: 4px 14px 14px 14px;
  background: var(--ai-surface);
}

.ai-msg--user .ai-bubble {
  color: hsl(var(--primary-foreground));
  border-radius: 14px 4px 14px 14px;
  background: var(--ai-gradient);
  box-shadow: 0 5px 14px -5px hsl(var(--primary) / 0.5);
}

.ai-bubble__text {
  font-size: inherit;
  line-height: inherit;
}

.ai-bubble__text :deep(strong) {
  font-weight: 600;
  color: var(--ai-primary);
}

.ai-msg--user .ai-bubble__text :deep(strong) {
  color: hsl(var(--primary-foreground));
}

.ai-bubble__time {
  display: block;
  margin-top: 3px;
  font-size: 10px;
  opacity: 0.5;
}

.ai-code-inline {
  padding: 1px 6px;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  color: var(--ai-code-inline-text);
  border-radius: 5px;
  background: var(--ai-code-inline-bg);
}

.ai-code {
  margin: 8px 0 4px;
  padding: 10px 12px;
  overflow-x: auto;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.65;
  color: var(--ai-code-text);
  border-radius: 10px;
  background: var(--ai-code-bg);
}

/* 正在输入 */
.ai-bubble--typing {
  display: flex;
  gap: 5px;
  padding: 13px 15px;
}

.ai-typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ai-primary);
  animation: ai-blink 1.1s infinite ease-in-out;
}

.ai-typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.ai-typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes ai-blink {
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
.ai-foot {
  flex-shrink: 0;
  padding: 10px 14px 12px;
  border-top: 1px solid var(--ai-line);
}

.ai-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.ai-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  font-size: 12px;
  color: var(--ai-primary);
  border: 1px solid var(--ai-chip-border);
  border-radius: 999px;
  background: var(--ai-chip-bg);
  cursor: pointer;
  transition:
    background 0.18s,
    transform 0.18s,
    box-shadow 0.18s;
}

.ai-chip:hover {
  background: hsl(var(--primary) / 0.14);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px -4px hsl(var(--primary) / 0.4);
}

.ai-chip__icon {
  width: 12px;
  height: 12px;
}

.ai-inputbar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 6px 6px 6px 12px;
  border: 1px solid var(--ai-line);
  border-radius: 12px;
  background: var(--ai-surface);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.ai-inputbar:focus-within {
  border-color: hsl(var(--primary) / 0.55);
  box-shadow: 0 0 0 3px hsl(var(--primary) / 0.12);
}

.ai-textarea {
  flex: 1;
  max-height: 120px;
  padding: 5px 0;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ai-text);
  resize: none;
  border: none;
  outline: none;
  background: transparent;
}

.ai-textarea::placeholder {
  color: var(--ai-sub);
}

.ai-send {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: 10px;
  background: var(--ai-gradient);
  box-shadow: 0 4px 10px -2px hsl(var(--primary) / 0.5);
  cursor: pointer;
  transition:
    transform 0.15s,
    opacity 0.15s;
}

.ai-send:hover:not(:disabled) {
  transform: translateY(-1px);
}

.ai-send:active:not(:disabled) {
  transform: scale(0.95);
}

.ai-send:disabled {
  opacity: 0.38;
  cursor: not-allowed;
  box-shadow: none;
}

.ai-send :deep(svg) {
  width: 15px;
  height: 15px;
}

.ai-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 9px;
  font-size: 10.5px;
  color: var(--ai-sub);
}

.ai-hint__icon {
  width: 11px;
  height: 11px;
}

/* ============================================================
   悬浮入口（可拖拽）
   ============================================================ */
.ai-fab {
  /* 悬浮按钮独立于 .ai-panel 作用域，自带所需变量 */
  --ai-primary: hsl(var(--primary));
  --ai-gradient: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18),
      transparent 42%,
      rgba(0, 0, 0, 0.2)
    ),
    hsl(var(--primary));
  --ai-success: hsl(var(--success));
  --ai-fab-border: hsl(var(--card));
  --ai-fab-shadow: 0 12px 28px -8px hsl(var(--primary) / 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);

  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: 50%;
  background: var(--ai-gradient);
  box-shadow: var(--ai-fab-shadow);
  cursor: grab;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
  user-select: none;
  -webkit-user-drag: none;
  touch-action: none;
}

.ai-fab.is-dark {
  --ai-fab-shadow: 0 12px 28px -8px hsl(var(--primary) / 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
}

.ai-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px -8px hsl(var(--primary) / 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
}

.ai-fab:active {
  transform: scale(0.96);
}

.ai-fab.is-dragging {
  cursor: grabbing;
  transform: none;
  transition: none;
  box-shadow: 0 16px 32px -8px hsl(var(--primary) / 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
}

.ai-fab__icon {
  width: 21px;
  height: 21px;
}

.ai-fab__dot {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 10px;
  height: 10px;
  border: 2px solid var(--ai-fab-border);
  border-radius: 50%;
  background: var(--ai-success);
}

.ai-fab__tip {
  position: absolute;
  top: 50%;
  right: 60px;
  padding: 5px 12px;
  font-size: 12px;
  white-space: nowrap;
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  background: hsl(var(--popover));
  box-shadow: 0 6px 16px -6px rgba(15, 23, 42, 0.28);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-50%) translateX(6px);
  transition:
    opacity 0.18s,
    transform 0.18s;
}

.ai-fab:hover .ai-fab__tip {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

/* ============================================================
   过渡动画（进入 ease-out，退出更快；尊重 reduced-motion）
   ============================================================ */
.ai-panel-enter-active {
  transition:
    opacity 0.22s ease,
    transform 0.24s cubic-bezier(0.2, 0.8, 0.3, 1);
}

.ai-panel-leave-active {
  transition:
    opacity 0.16s ease,
    transform 0.18s ease;
}

.ai-panel-enter-from,
.ai-panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}

.ai-fab-enter-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.ai-fab-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.ai-fab-enter-from,
.ai-fab-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.ai-msg-enter-active {
  transition: all 0.26s cubic-bezier(0.2, 0.8, 0.3, 1);
}

.ai-msg-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.ai-chips-enter-active,
.ai-chips-leave-active {
  transition: all 0.2s ease;
}

.ai-chips-enter-from,
.ai-chips-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (prefers-reduced-motion: reduce) {
  .ai-panel,
  .ai-fab,
  .ai-msg,
  .ai-chips {
    transition: none !important;
  }

  .ai-typing-dot {
    animation: none !important;
  }
}

/* ============================================================
   小屏适配
   ============================================================ */
@media (max-width: 480px) {
  .ai-panel {
    right: 12px;
    bottom: 76px;
    width: calc(100vw - 24px);
    height: calc(100vh - 130px);
  }

  .ai-fab {
    right: 14px;
    bottom: 14px;
  }

  .ai-fab__tip {
    display: none;
  }
}
</style>

