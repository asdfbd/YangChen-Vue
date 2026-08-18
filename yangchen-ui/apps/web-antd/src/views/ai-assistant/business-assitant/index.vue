<script lang="ts" setup>
import {
  computed,
  nextTick,
  onActivated,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue';

import {useUserStore} from '@vben/stores';

import ChatPanel from '#/components/chat-panel/index.vue';
import HistorySidebar from '#/components/chat-panel/history-sidebar.vue';
import {useChatHistory} from '#/components/chat-panel/chat-history';
import {
  deleteChatTitleApi,
  generateConversationIdApi,
  listChatContentApi,
  listChatTitlesApi,
  streamChatApi,
} from '#/api/ai';
import type {ChatMessage, ChatStreamHandle} from '#/components/chat-panel/types';
import {genId, nowTime} from '#/components/chat-panel/utils';

defineOptions({name: 'BusinessAssistant'});

/**
 * 业务助手（AI 助手目录下的业务问答页 · 全页对话形态）
 * - 复用通用 ChatPanel 的 variant="page"（类似 DeepSeek / GPT 的全页聊天）
 * - 左侧历史侧栏：多会话管理（新建 / 切换 / 删除），仅在当前页面内存中保留
 * - 回复来自后端 /ai/chat 流式接口，前端负责拼接 ChatResp.text
 */

const HISTORY_KEY = 'yangchen-business-assistant-history-v1';
/** 旧版单会话存储（v1），首次进入时迁移为一段历史后移除 */
const LEGACY_KEY = 'yangchen-business-assistant-v1';
const DEFAULT_TITLE = '新对话';

const userStore = useUserStore();
const {
  conversations,
  activeId,
  active,
  newChat,
  open,
  remove,
  saveActive,
  replaceConversations,
} = useChatHistory(HISTORY_KEY, false);

/* ===== 旧版存储迁移 ===== */
(function migrateLegacy() {
  try {
    const raw = localStorage.getItem(LEGACY_KEY);
    if (!raw) return;
    localStorage.removeItem(LEGACY_KEY);
  } catch {
    // 解析失败则忽略
  }
})();

const creatingConversation = ref(false);
const historyLoading = ref(false);

function toTimestamp(value?: string) {
  const timestamp = value ? Date.parse(value) : NaN;
  return Number.isFinite(timestamp) ? timestamp : Date.now();
}

function routeConversationId() {
  return new URL(window.location.href).searchParams.get('conversationId') ?? undefined;
}

function updateConversationRoute(conversationId?: string) {
  const url = new URL(window.location.href);
  const currentId = url.searchParams.get('conversationId');
  if (currentId === (conversationId || null)) return;
  if (conversationId) {
    url.searchParams.set('conversationId', conversationId);
  } else {
    url.searchParams.delete('conversationId');
  }
  // 只同步地址，不触发 Vue Router 导航，避免标签页容器重复创建页面。
  window.history.replaceState(
    window.history.state,
    '',
    `${url.pathname}${url.search}${url.hash}`,
  );
}

function toChatMessage(item: {
  id?: string | number;
  messageType: string;
  content: string;
  createTime?: string;
}): ChatMessage | null {
  const kind = item.messageType.toLowerCase();
  const role = kind === 'user' || kind === 'human' ? 'user' : 'assistant';
  if (!item.content?.trim()) return null;
  return {
    id: String(item.id ?? genId()),
    role,
    content: item.content,
    time: item.createTime?.slice(11, 16) || nowTime(),
  };
}

async function loadConversationMessages(conversationId: string) {
  const conversation = conversations.value.find(
    (item) => item.conversationId === conversationId,
  );
  if (!conversation || conversation.messagesLoaded) return;
  try {
    const items = await listChatContentApi(conversationId);
    conversation.messages = items
      .slice()
      .sort((a, b) => toTimestamp(a.createTime) - toTimestamp(b.createTime))
      .map(toChatMessage)
      .filter((item): item is ChatMessage => item !== null);
  } catch {
    conversation.messages = [];
  } finally {
    conversation.messagesLoaded = true;
  }
}

async function loadHistory() {
  const userId = userStore.userInfo?.userId;
  if (!userId || historyLoading.value) return;
  historyLoading.value = true;
  try {
    const titles = await listChatTitlesApi(userId);
    const mapped = titles.map((item) => {
      const conversationId = String(item.conversationId);
      const existing = conversations.value.find(
        (conversation) => conversation.conversationId === conversationId,
      );
      return {
      id: conversationId,
      conversationId,
      title: item.title || DEFAULT_TITLE,
      createdAt: toTimestamp(item.createTime),
      updatedAt: toTimestamp(item.updateTime || item.createTime),
      messages: existing?.messages ?? [],
      messagesLoaded: existing?.messagesLoaded ?? false,
      };
    });
    if (mapped.length > 0) {
      replaceConversations(mapped);
      const selectedId = routeConversationId();
      const targetId = selectedId && mapped.some((item) => item.id === selectedId)
        ? selectedId
        : active.value?.conversationId;
      if (targetId) {
        open(targetId);
        updateConversationRoute(targetId);
        await loadConversationMessages(targetId);
      }
    } else if (!active.value) {
      await handleNewChat();
    } else {
      // 标题生成存在异步延迟，列表暂时为空时必须保留当前会话，不能重新创建。
      updateConversationRoute(active.value.conversationId);
    }
  } catch {
    // 历史标题接口异常时也保留当前会话，避免页面刷新或重复创建会话。
    if (!active.value) await handleNewChat();
  } finally {
    historyLoading.value = false;
  }
}

/** 创建前端会话并预先申请后端会话 ID。 */
async function handleNewChat() {
  abortCurrentStream();
  if (creatingConversation.value) return;
  creatingConversation.value = true;
  try {
    const conversationId = await generateConversationIdApi();
    const conversation = newChat();
    conversation.conversationId = conversationId;
    updateConversationRoute(conversationId);
  } catch {
    // 后端暂时不可用时仍保留可用的非空请求头，后续请求会给出明确错误。
    const conversation = newChat();
    conversation.conversationId = genId();
    updateConversationRoute(conversation.conversationId);
  } finally {
    creatingConversation.value = false;
  }
}

/** 老会话或用户点击过快时的兜底：发送前保证一定拿到后端会话 ID。 */
async function ensureConversationId(): Promise<string> {
  if (!active.value) {
    await handleNewChat();
  }
  const conversation = active.value;
  if (!conversation) throw new Error('AI 会话未创建');
  if (!conversation.conversationId) {
    conversation.conversationId = await generateConversationIdApi();
  }
  return conversation.conversationId;
}

/**
 * 消息绑定：v-model 直连"当前会话"，切换会话时数据源自动切换。
 * 会话消息的读写由 ChatPanel 的 update:modelValue 驱动。
 */
const messages = computed<ChatMessage[]>({
  get: () => (active.value ? active.value.messages : []),
  set: (val) => saveActive(val),
});

/* ===== 侧栏折叠（窄屏自动收起，用户可手动展开） ===== */
const sidebarCollapsed = ref(window.innerWidth < 1100);
const pageRef = ref<HTMLElement>();
const pageHeight = ref('100%');

/** 根据组件在当前布局中的实际位置填满剩余视口，避免顶部栏高度变化造成底部留白。 */
function updatePageHeight() {
  const top = pageRef.value?.getBoundingClientRect().top;
  if (top === undefined) return;
  pageHeight.value = `${Math.max(420, Math.floor(window.innerHeight - top))}px`;
}

function onWindowResize() {
  if (window.innerWidth < 1100) {
    sidebarCollapsed.value = true;
  }
  updatePageHeight();
}

window.addEventListener('resize', onWindowResize);
onMounted(() => {
  nextTick(updatePageHeight);
  void loadHistory();
});
onActivated(() => nextTick(updatePageHeight));
onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize);
  abortCurrentStream();
});

const suggestions = [
  '如何新增一个用户？',
  '怎么给角色分配权限？',
  '操作日志在哪里查看？',
  '代码生成怎么用？',
];

async function handleSelect(id: string) {
  abortCurrentStream();
  open(id);
  const conversation = active.value;
  updateConversationRoute(conversation?.conversationId || id);
  await loadConversationMessages(id);
}

async function handleRemove(id: string) {
  abortCurrentStream();
  const conversation = conversations.value.find((item) => item.id === id);
  try {
    if (conversation?.conversationId) {
      await deleteChatTitleApi(conversation.conversationId);
    }
  } finally {
    remove(id);
    updateConversationRoute(active.value?.conversationId);
  }
}

/** 当前在途的流式中止句柄（切换会话 / 卸载时主动停止，防止增量写入错误的会话） */
let currentSignal: ChatStreamHandle | null = null;

/** 停止当前正在生成的回复（保留已生成的部分） */
function abortCurrentStream() {
  if (currentSignal && !currentSignal.aborted) {
    currentSignal.abort();
  }
  currentSignal = null;
}

/**
 * 流式发送处理器：ChatPanel 逐块收到 onChunk 并实时渲染 Markdown；
 * 第三个参数 signal 用于响应「停止生成」——中止底层 fetch，已生成内容原样保留。
 */
async function handleSend(
  text: string,
  onChunk?: (chunk: string) => void,
  signal?: ChatStreamHandle,
): Promise<string> {
  currentSignal = signal ?? null;
  let controller: AbortController | null = null;
  if (signal) {
    controller = new AbortController();
    signal.onAbort(() => controller?.abort());
  }
  try {
    const conversationId = await ensureConversationId();
    const reply = await streamChatApi(text, conversationId, onChunk, controller?.signal);
    // 首轮回答完成后，后端会异步生成标题；重新拉取让历史列表立即显示新标题。
    await loadHistory();
    if (currentSignal === signal) currentSignal = null;
    return reply;
  } catch {
    // 用户主动停止：保留已生成内容，静默结束
    if (signal?.aborted) {
      if (currentSignal === signal) currentSignal = null;
      return '';
    }
    const message = '抱歉，AI 暂时没有返回有效内容，请稍后重试。';
    onChunk?.(message);
    return '';
  }
}
</script>

<template>
  <div
    ref="pageRef"
    class="ba-page"
    :style="{height: pageHeight, maxHeight: pageHeight}"
  >
    <HistorySidebar
      v-model:collapsed="sidebarCollapsed"
      :conversations="conversations"
      :active-id="activeId"
      @select="handleSelect"
      @remove="handleRemove"
      @new-chat="handleNewChat"
    />

    <main class="ba-main">
      <ChatPanel
        v-model="messages"
        variant="page"
        title="业务助手"
        badge="业务"
        subtitle="智能问答 · 随时待命"
        description="我可以帮你解答系统使用与业务查询问题。试试下面的问题，或直接输入你想问的。"
        avatar-icon="lucide:briefcase-business"
        :suggestions="suggestions"
        :send="handleSend"
        hint="AI 对话由后端流式接口提供"
        :autofocus="true"
        :closable="false"
        :new-chatable="false"
        @new-chat="handleNewChat"
      />
    </main>
  </div>
</template>

<style scoped>
/* 左右布局：历史侧栏 + 全页对话；主区保留柔和的主色光晕氛围 */
.ba-page {
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: hsl(var(--foreground));
  background: hsl(var(--background-deep));
}

.ba-main {
  flex: 1;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background:
    radial-gradient(80% 65% at 50% 0%, hsl(var(--primary) / 0.055), transparent 72%),
    linear-gradient(180deg, hsl(var(--background-deep)), hsl(var(--background-deep) / 0.92));
}

</style>
