<script lang="ts" setup>
import {computed, ref, watch} from 'vue';

import {IconifyIcon} from '@vben/icons';
import {usePreferences} from '@vben/preferences';
import {useUserStore} from '@vben/stores';

import ChatPanel from '#/components/chat-panel/index.vue';
import type {ChatMessage} from '#/components/chat-panel/types';
import {genId, nowTime} from '#/components/chat-panel/utils';

import {buildGreeting, QUICK_QUESTIONS, resolveReply} from './knowledge';

defineOptions({name: 'AiAssistant'});

/**
 * 扬辰 AI 智能助手（全局悬浮组件）
 * - 挂载于 basic.vue 布局，全站页面可见
 * - 界面复用通用 ChatPanel；本组件负责：悬浮球拖拽、面板定位、会话持久化、演示问答
 * - 接入真实后端时，替换 handleSend 的实现即可（流式场景改用 busy + 自行推送）
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

/* ===== 消息数据（由 ChatPanel 通过 v-model 驱动） ===== */
const messages = ref<ChatMessage[]>([]);

const displayName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '',
);

function createGreeting(): ChatMessage {
  return {
    id: genId(),
    role: 'assistant',
    content: buildGreeting(displayName.value),
    time: nowTime(),
  };
}

function handleNewChat() {
  messages.value = [createGreeting()];
}

/**
 * 演示问答：返回字符串 -> ChatPanel 自动显示"正在输入"并追加回复。
 * 接入真实后端时替换为：async (text) => (await sendAiChatApi(text)).reply
 * 流式场景：返回 void，在 @send-start 中自行推送并控制 busy。
 */
function handleSend(text: string): Promise<string> {
  const delay = 600 + Math.random() * 900;
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(resolveReply(text)), delay);
  });
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
messages.value = stored && stored.length ? stored : [createGreeting()];

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

/* ===== Esc 关闭 ===== */
function onGlobalKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    open.value = false;
  }
}

watch(open, (val) => {
  if (val) {
    window.addEventListener('keydown', onGlobalKeydown);
  } else {
    window.removeEventListener('keydown', onGlobalKeydown);
  }
});
</script>

<template>
  <Teleport to="body">
    <!-- ===== 聊天面板（通用 ChatPanel + 悬浮定位壳） ===== -->
    <Transition name="ai-panel">
      <section
        v-if="open"
        :class="['ai-panel', {'is-dark': isDark}]"
        :style="panelStyle"
        role="dialog"
        aria-label="AI 智能助手"
      >
        <ChatPanel
          v-model="messages"
          title="小辰"
          badge="AI"
          subtitle="在线 · 随时待命"
          avatar-icon="lucide:sparkles"
          :suggestions="QUICK_QUESTIONS"
          :send="handleSend"
          hint="演示模式 · 回复来自本地知识库，后端接口接入后自动升级"
          :autofocus="true"
          @close="open = false"
          @new-chat="handleNewChat"
        />
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
   悬浮定位壳：固定定位 + 尺寸 + 圆角 + 阴影 + 顶部高光线；
   内部由通用 ChatPanel 填充（界面样式见 chat-panel/index.vue）
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
  border: 1px solid hsl(var(--border));
  border-radius: 18px;
  background: hsl(var(--card));
  box-shadow: 0 8px 20px -6px rgba(15, 23, 42, 0.12),
    0 24px 48px -16px rgba(15, 23, 42, 0.22);
  transform-origin: right bottom;
}

.ai-panel.is-dark {
  box-shadow: 0 8px 20px -6px rgba(0, 0, 0, 0.5),
    0 24px 48px -16px rgba(0, 0, 0, 0.6);
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
   悬浮入口（可拖拽）
   ============================================================ */
.ai-fab {
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
  background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18),
      transparent 42%,
      rgba(0, 0, 0, 0.2)
    ),
    hsl(var(--primary));
  box-shadow: 0 12px 28px -8px hsl(var(--primary) / 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
  cursor: grab;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
  user-select: none;
  -webkit-user-drag: none;
  touch-action: none;
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
  border: 2px solid hsl(var(--card));
  border-radius: 50%;
  background: hsl(var(--success));
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

@media (prefers-reduced-motion: reduce) {
  .ai-panel,
  .ai-fab {
    transition: none !important;
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
