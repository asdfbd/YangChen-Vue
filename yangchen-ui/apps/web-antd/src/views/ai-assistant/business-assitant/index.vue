<script lang="ts" setup>
import {
  computed,
  nextTick,
  onActivated,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue';
import {useRoute} from 'vue-router';

import {getTabKey, useTabbarStore, useUserStore} from '@vben/stores';

import ChatPanel from '#/components/chat-panel/index.vue';
import HistorySidebar from '#/components/chat-panel/history-sidebar.vue';
import {useChatHistory} from '#/components/chat-panel/chat-history';
import {
  extractDirectQueryPrefix,
  parseDirectQueryUi,
} from '#/components/chat-panel/direct-query-ui';
import {
  deleteChatTitleApi,
  generateConversationIdApi,
  generateChatTitleApi,
  listChatContentApi,
  listChatTitlesApi,
  streamChatApi,
} from '#/api/ai';
import type {
  AiUiPayload,
  ChatMessage,
  ChatStreamHandle,
  ChatUiAction,
} from '#/components/chat-panel/types';
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
const route = useRoute();
const tabbarStore = useTabbarStore();
const {
  conversations,
  activeId,
  active,
  newChat,
  open,
  remove,
  saveActive,
  mergeConversations,
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
const chatPanelRef = ref<InstanceType<typeof ChatPanel>>();
/** 当前待处理的确认卡片。存在时只允许点击卡片上的确认或取消。 */
const pendingToolApprovalId = ref<string | null>(null);
const titleGenerationInProgress = new Set<string>();
let historyRequestVersion = 0;
let contentRequestVersion = 0;
const loadingConversationId = ref<string | null>(null);
const conversationLoadError = ref<{
  conversationId: string;
  message: string;
} | null>(null);

function toTimestamp(value?: string) {
  const timestamp = value ? Date.parse(value) : NaN;
  return Number.isFinite(timestamp) ? timestamp : Date.now();
}

/** 会话仅保留在页面状态中，路由只负责定位业务助手页面。 */
async function keepSingleBusinessAssistantTab() {
  if (route.meta.fullPathKey !== false) {
    route.meta.fullPathKey = false;
  }

  // 只收敛旧版本遗留的重复 Tab；不能在会话路由变化时手动 addTab，
  // 否则一次切换会话会被识别成一次新开页面，触发组件重建和历史列表闪动。
  const currentKey = getTabKey(route);
  const samePageTabs = tabbarStore.tabs.filter(
    (tab) => tab.name === route.name || tab.path === route.path,
  );
  if (samePageTabs.length < 2) return;

  const tabToKeep =
    [...samePageTabs].reverse().find((tab) => getTabKey(tab) === currentKey) ||
    samePageTabs[0];
  tabbarStore.tabs = tabbarStore.tabs.filter(
    (tab) =>
      !samePageTabs.includes(tab) ||
      tab === tabToKeep,
  );
  await tabbarStore.updateCacheTabs();
}

/** 清理旧版本写进地址栏的会话参数，后续不再读写它。 */
function clearLegacyConversationQuery() {
  const url = new URL(window.location.href);
  if (!url.searchParams.has('conversationId')) return;
  url.searchParams.delete('conversationId');
  window.history.replaceState(
    window.history.state,
    '',
    `${url.pathname}${url.search}${url.hash}`,
  );
}

/** 旧版响应中已落库的思考块不再展示，只保留最终回答。 */
function removeLegacyThinking(content: string) {
  return content
    .replace(/:::thinking\s*\n[\s\S]*?(?:\n:::\s*|$)/gi, '')
    .replace(/^\s*\*\*回答\*\*\s*/i, '')
    .trim();
}

function toCompletedConfirmationMessage(
  id: string,
  time: string,
  action: 'cancel' | 'confirm',
): ChatMessage {
  return {
    id: `${id}:confirmation`,
    role: 'assistant',
    content: '',
    time,
    type: 'confirm',
    extra: {
      ui: {
        type: 'ui',
        component: 'confirm',
        data: {
          title: '请确认继续',
          description: action === 'confirm' ? '该操作已确认执行。' : '该操作已取消。',
          completedAction: action,
        },
      },
    },
  };
}

function toChatMessages(item: {
  id?: string | number;
  messageType: string;
  content: string;
  createTime?: string;
  type?: string;
  component?: string;
  data?: unknown;
  action?: AiUiPayload['action'];
  messageId?: string;
  ui?: Omit<AiUiPayload, 'type'> & {type?: 'ui'};
}): ChatMessage[] {
  const kind = item.messageType.toLowerCase();
  if (['reason', 'reasoning', 'thinking'].includes(kind)) return [];
  const role = kind === 'user' || kind === 'human' ? 'user' : 'assistant';
  const time = item.createTime?.slice(11, 16) || nowTime();
  const messageId = String(item.id ?? genId());
  // 兼容旧版本把 JSON 字符串两侧引号一并落库的记录。
  const actionMarker = item.content?.trim().replace(/^["“]|["”]$/g, '');
  if (role === 'user' && actionMarker === '【确认执行】') {
    return [toCompletedConfirmationMessage(messageId, time, 'confirm')];
  }
  if (role === 'user' && actionMarker === '【取消执行】') {
    return [toCompletedConfirmationMessage(messageId, time, 'cancel')];
  }

  let ui: AiUiPayload | null = item.ui?.component
    ? {
        type: 'ui' as const,
        component: item.ui.component,
        data: item.ui.data,
        action: item.ui.action,
        messageId: item.ui.messageId,
      }
    : item.component
      ? {
          type: 'ui' as const,
          component: item.component,
          data: item.data,
          action: item.action,
          messageId: item.messageId,
        }
      : null;

  // 兼容历史表只保存 messageType + content 的情况：ui 消息的 content 可以是协议 JSON。
  if (!ui && (kind === 'ui' || item.type === 'ui') && item.content?.trim()) {
    try {
      const parsed = JSON.parse(item.content) as Partial<AiUiPayload>;
      if (typeof parsed.component === 'string') {
        ui = {
          type: 'ui',
          component: parsed.component,
          data: parsed.data,
          action: parsed.action,
          messageId: parsed.messageId,
        };
      }
    } catch {
      // 历史内容不是协议 JSON 时按普通文本兜底。
    }
  }

  // returnDirect 的旧历史会以统一结果集 JSON 落库；读取时直接还原为业务组件。
  let directResultUi = false;
  if (!ui && role === 'assistant') {
    ui = parseDirectQueryUi(item.content);
    directResultUi = Boolean(ui);
  }

  if (ui) {
    const resultMessage: ChatMessage = {
      id: directResultUi
        ? `${messageId}:result`
        : String(item.id ?? ui.messageId ?? genId()),
      role: 'assistant',
      content: '',
      time,
      type: ui.component,
      extra: {ui},
    };

    // 旧记录把模型说明和 returnDirect JSON 存在同一字段。拆成两条前端消息，
    // 才能同时显示“我正在查询……”这类说明和下面的查询结果组件。
    const prefix = directResultUi
      ? removeLegacyThinking(extractDirectQueryPrefix(item.content))
      : '';
    return prefix
      ? [
          {
            id: messageId,
            role: 'assistant',
            content: prefix,
            time,
          },
          resultMessage,
        ]
      : [resultMessage];
  }

  const content = role === 'assistant'
    ? removeLegacyThinking(item.content || '')
    : item.content?.trim();
  if (!content) return [];
  return [{
    id: messageId,
    role,
    content,
    time,
  }];
}

const isConversationLoading = computed(
  () => loadingConversationId.value === active.value?.conversationId,
);
const activeConversationLoadError = computed(() => {
  const error = conversationLoadError.value;
  if (!error || error.conversationId !== active.value?.conversationId) {
    return '';
  }
  return error.message;
});

function cancelConversationLoading() {
  contentRequestVersion += 1;
  loadingConversationId.value = null;
  conversationLoadError.value = null;
}

async function loadConversationMessages(conversationId: string): Promise<boolean> {
  const conversation = conversations.value.find(
    (item) => item.conversationId === conversationId,
  );
  if (!conversation) return false;

  // 每次切换会话都重新拉取，保证内容与服务端落库状态一致。
  // requestVersion 仍用于阻止快速切换时的旧请求覆盖当前会话。
  const requestVersion = ++contentRequestVersion;
  loadingConversationId.value = conversationId;
  conversationLoadError.value = null;
  try {
    const items = await listChatContentApi(conversationId);
    // 用户可能已切换到另一段会话，过期请求不能覆盖新会话的数据或状态。
    if (requestVersion !== contentRequestVersion) return false;
    conversation.messages = items
      .slice()
      .sort((a, b) => toTimestamp(a.createTime) - toTimestamp(b.createTime))
      .flatMap(toChatMessages);
    conversation.messagesLoaded = true;
    return true;
  } catch {
    if (requestVersion === contentRequestVersion) {
      // 请求失败时不要把它误标记为“已加载的空会话”，下次点击仍可重试。
      conversation.messagesLoaded = false;
      conversationLoadError.value = {
        conversationId,
        message: '会话内容加载失败，请重试。',
      };
    }
    return false;
  } finally {
    if (requestVersion === contentRequestVersion) {
      loadingConversationId.value = null;
    }
  }
}

async function loadHistory() {
  const userId = userStore.userInfo?.userId;
  if (!userId) return;
  const requestVersion = ++historyRequestVersion;
  try {
    const titles = await listChatTitlesApi(userId);
    if (requestVersion !== historyRequestVersion) return;
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
      // mergeConversations 会自动把第一条设为 active；因此必须在合并前记录状态，
      // 否则首次进入会被误认为“已有会话”，只高亮标题却不加载消息。
      const hadActiveConversation = !!active.value;
      mergeConversations(mapped);
      // 标题刷新只更新列表；已有当前会话时绝不重新选中服务端第一条。
      if (hadActiveConversation) {
        const current = active.value;
        if (current?.conversationId && !current.messagesLoaded) {
          await loadConversationMessages(current.conversationId);
        }
        return;
      }
      const target = active.value ?? conversations.value[0];
      if (target?.conversationId) {
        open(target.id);
        await loadConversationMessages(target.conversationId);
      }
    } else if (!active.value) {
      await handleNewChat();
    }
  } catch {
    // 历史标题接口异常时也保留当前会话，避免页面刷新或重复创建会话。
    if (!active.value) await handleNewChat();
  }
}

/** 创建前端会话并预先申请后端会话 ID。 */
async function handleNewChat() {
  abortCurrentStream();
  cancelConversationLoading();
  pendingToolApprovalId.value = null;
  if (creatingConversation.value) return;
  creatingConversation.value = true;
  // 先切到本地新会话，再异步申请 ID；任何标题列表响应都不能把它替换掉。
  const conversation = newChat();
  try {
    const conversationId = await generateConversationIdApi();
    conversation.conversationId = conversationId;
  } catch {
    // 后端暂时不可用时仍保留可用的非空请求头，后续请求会给出明确错误。
    conversation.conversationId = genId();
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
  clearLegacyConversationQuery();
  void keepSingleBusinessAssistantTab();
  void loadHistory();
});
onActivated(() => {
  nextTick(updatePageHeight);
  void keepSingleBusinessAssistantTab();
});
onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize);
  abortCurrentStream();
  cancelConversationLoading();
});

const suggestions = [
  '如何新增一个用户？',
  '怎么给角色分配权限？',
  '当前最新的操作日志有哪些？',
  '代码生成怎么用？',
];

async function handleSelect(id: string) {
  const conversation = conversations.value.find((item) => item.id === id);
  if (!conversation) return;
  abortCurrentStream();
  pendingToolApprovalId.value = null;
  open(id);
  await loadConversationMessages(conversation.conversationId || id);
}

/** 首条消息才异步生成标题；只原地更新对应会话，绝不刷新或切换整个历史列表。 */
function generateConversationTitle(conversationId: string, userInput: string) {
  const conversation = conversations.value.find(
    (item) => item.conversationId === conversationId,
  );
  if (
    !conversation ||
    conversation.title !== DEFAULT_TITLE ||
    titleGenerationInProgress.has(conversationId)
  ) {
    return;
  }

  titleGenerationInProgress.add(conversationId);
  void generateChatTitleApi(userInput, conversationId)
    .then((title) => {
      const target = conversations.value.find(
        (item) => item.conversationId === conversationId,
      );
      const generatedTitle = title?.title?.trim();
      if (!target || !generatedTitle) return;
      target.title = generatedTitle;
      target.updatedAt = toTimestamp(title.updateTime || title.createTime);
    })
    .catch(() => {
      // 标题生成失败不影响主对话；下一轮发送仍会尝试生成。
    })
    .finally(() => titleGenerationInProgress.delete(conversationId));
}

async function retryLoadActiveConversation() {
  const conversationId = active.value?.conversationId;
  if (!conversationId) return;
  // 错误会话的 messagesLoaded 保持 false，因此这里会真正重新请求。
  await loadConversationMessages(conversationId);
}

async function handleRemove(id: string) {
  abortCurrentStream();
  cancelConversationLoading();
  const conversation = conversations.value.find((item) => item.id === id);
  try {
    if (conversation?.conversationId) {
      await deleteChatTitleApi(conversation.conversationId);
    }
  } finally {
    remove(id);
  }
}

const CONFIRM_MESSAGE = '【确认执行】';
const CANCEL_MESSAGE = '【取消执行】';

/** 下一次请求使用的确认令牌；只经请求头发送，不拼入用户可见文本。 */
let nextToolApprovalId: null | string = null;

/** 选择器、确认卡片均复用 ChatPanel 的标准发送链路。 */
function handleUiAction(payload: ChatUiAction) {
  if (payload.message.type === 'confirm') {
    if (payload.action === 'confirm' && payload.actionId) {
      pendingToolApprovalId.value = null;
      nextToolApprovalId = payload.actionId;
      void chatPanelRef.value?.sendActionText(CONFIRM_MESSAGE);
    } else if (payload.action === 'cancel') {
      pendingToolApprovalId.value = null;
      // 取消也按用户消息走正常链路，后端聊天记录可完整保留本次操作轨迹。
      void chatPanelRef.value?.sendActionText(CANCEL_MESSAGE);
    }
    return;
  }

  const submittedValue =
    payload.values?.choiceValue ?? payload.values?.inputValue;
  if (
    payload.message.type !== 'select' ||
    payload.action !== 'submit' ||
    typeof submittedValue !== 'string' ||
    !submittedValue.trim()
  ) {
    return;
  }
  void chatPanelRef.value?.sendText(submittedValue);
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
  onUiMessage?: (payload: AiUiPayload) => void,
): Promise<string> {
  currentSignal = signal ?? null;
  // sendText('已确认执行') 同步进入本方法；令牌仅消费给这一轮请求。
  const approvalId = nextToolApprovalId;
  nextToolApprovalId = null;
  let controller: AbortController | null = null;
  if (signal) {
    controller = new AbortController();
    signal.onAbort(() => controller?.abort());
  }
  try {
    const conversationId = await ensureConversationId();
    generateConversationTitle(conversationId, text);
    const reply = await streamChatApi(
      text,
      conversationId,
      onChunk,
      controller?.signal,
      (payload) => {
        if (payload.component === 'confirm' && payload.action?.actionId) {
          pendingToolApprovalId.value = payload.action.actionId;
        }
        onUiMessage?.(payload);
      },
      approvalId ?? undefined,
    );
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
      <div class="ba-chat-shell">
        <ChatPanel
          ref="chatPanelRef"
          v-model="messages"
          variant="page"
          title="业务助手"
          badge="业务"
          subtitle="智能问答 · 随时待命"
          description="我可以帮你解答系统使用与业务查询问题。试试下面的问题，或直接输入你想问的。"
          avatar-icon="lucide:briefcase-business"
          :suggestions="suggestions"
          :send="handleSend"
          :input-disabled="Boolean(pendingToolApprovalId)"
          hint="AI 对话由后端流式接口提供"
          :autofocus="true"
          :closable="false"
          :new-chatable="false"
          @new-chat="handleNewChat"
          @ui-action="handleUiAction"
        />

        <div v-if="isConversationLoading" class="ba-conversation-state" role="status">
          <span class="ba-conversation-state__spinner" />
          <span>正在加载会话内容…</span>
        </div>
        <div v-else-if="activeConversationLoadError" class="ba-conversation-state">
          <span>{{ activeConversationLoadError }}</span>
          <button
            class="ba-conversation-state__retry"
            type="button"
            @click="retryLoadActiveConversation"
          >
            重新加载
          </button>
        </div>
      </div>
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

.ba-chat-shell {
  position: relative;
  height: 100%;
  min-height: 0;
}

.ba-conversation-state {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: hsl(var(--foreground) / 0.7);
  font-size: 14px;
  background: hsl(var(--background-deep) / 0.56);
  backdrop-filter: blur(2px);
}

.ba-conversation-state__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid hsl(var(--primary) / 0.2);
  border-top-color: hsl(var(--primary));
  border-radius: 50%;
  animation: ba-spin 0.8s linear infinite;
}

.ba-conversation-state__retry {
  padding: 5px 10px;
  color: hsl(var(--primary));
  cursor: pointer;
  background: hsl(var(--background));
  border: 1px solid hsl(var(--primary) / 0.3);
  border-radius: 6px;
}

.ba-conversation-state__retry:hover {
  background: hsl(var(--primary) / 0.08);
}

@keyframes ba-spin {
  to {
    transform: rotate(360deg);
  }
}

</style>
