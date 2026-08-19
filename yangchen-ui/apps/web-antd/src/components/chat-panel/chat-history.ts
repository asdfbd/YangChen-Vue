import {computed, ref, watch} from 'vue';

import {genId} from './utils';

import type {ChatMessage} from './types';

/** 一条历史会话 */
export interface ChatHistoryConversation {
  id: string;
  /** 后端 AI 会话 ID，用于 x-conversation-id 请求头 */
  conversationId?: string;
  /** 会话标题（自动取首条用户消息，未开口时为"新对话"） */
  title: string;
  /** 创建时间戳 */
  createdAt: number;
  /** 最近活跃时间戳 */
  updatedAt: number;
  messages: ChatMessage[];
  /** 服务端历史消息是否已加载 */
  messagesLoaded?: boolean;
}

interface ChatHistoryState {
  activeId: string | null;
  conversations: ChatHistoryConversation[];
}

const DEFAULT_TITLE = '新对话';

/** 由首条用户消息生成会话标题（最长 24 字） */
export function titleFromMessages(messages: ChatMessage[]): string {
  const first = messages.find((m) => m.role === 'user');
  if (!first) return '';
  const text = first.content.replace(/\s+/g, ' ').trim();
  if (!text) return '';
  return text.length > 24 ? `${text.slice(0, 24)}…` : text;
}

/**
 * 多会话历史存储（localStorage 持久化）
 *
 * - 数据源：`localStorage[storageKey]`，结构为 `{activeId, conversations[]}`
 * - 职责：新建 / 切换 / 删除会话、保存当前会话消息、旧版单会话数据迁移
 * - 与 ChatPanel 解耦：宿主通过 `messages` 计算属性把 v-model 映射到
 *   `active.value.messages`，切换会话时自动切换数据源
 */
export function useChatHistory(storageKey: string, persistMessages = true) {
  const emptyState = (): ChatHistoryState => ({
    activeId: null,
    conversations: [],
  });
  const state = ref<ChatHistoryState>(
    persistMessages ? loadState() : emptyState(),
  );

  /** 全部会话（按最近活跃排序，最新在前） */
  const conversations = computed(() => state.value.conversations);
  /** 当前会话 id */
  const activeId = computed(() => state.value.activeId);
  /** 当前会话对象（无会话时为 null） */
  const active = computed(
    () =>
      state.value.conversations.find((c) => c.id === state.value.activeId) ??
      null,
  );

  function loadState(): ChatHistoryState {
    const empty = emptyState();
    try {
      const raw = localStorage.getItem(storageKey);
      if (!raw) return empty;
      const data = JSON.parse(raw) as ChatHistoryState;
      if (!data || !Array.isArray(data.conversations)) return empty;
      const list = data.conversations.filter(
        (c): c is ChatHistoryConversation =>
          !!c && typeof c.id === 'string' && Array.isArray(c.messages),
      );
      list.sort((a, b) => b.updatedAt - a.updatedAt);
      return {
        activeId:
          typeof data.activeId === 'string' &&
          list.some((c) => c.id === data.activeId)
            ? data.activeId
            : list[0]?.id ?? null,
        conversations: list,
      };
    } catch {
      return empty;
    }
  }

  function persist() {
    if (!persistMessages) return;
    try {
      localStorage.setItem(storageKey, JSON.stringify(state.value));
    } catch {
      // 存储不可用时静默降级
    }
  }

  /** 新建一段空会话并切换过去 */
  function newChat(): ChatHistoryConversation {
    const conv: ChatHistoryConversation = {
      id: genId(),
      title: DEFAULT_TITLE,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      messages: [],
    };
    state.value.conversations.unshift(conv);
    state.value.activeId = conv.id;
    persist();
    return conv;
  }

  /** 切换到指定会话 */
  function open(id: string) {
    if (!state.value.conversations.some((c) => c.id === id)) return;
    state.value.activeId = id;
    persist();
  }

  /**
   * 删除会话；若删除的是当前会话：
   * - 列表还有剩余 -> 自动切到相邻会话
   * - 列表已空 -> 自动新建一段空会话（保持界面始终有可用的当前会话）
   */
  function remove(id: string) {
    const list = state.value.conversations;
    const idx = list.findIndex((c) => c.id === id);
    if (idx < 0) return;
    list.splice(idx, 1);
    if (state.value.activeId === id) {
      const next =
        list.length > 0 ? list[Math.min(idx, list.length - 1)] : undefined;
      state.value.activeId = next ? next.id : null;
      if (!state.value.activeId) {
        newChat();
      }
    }
    persist();
  }

  /** 保存当前会话的消息（自动生成标题、刷新活跃时间） */
  function saveActive(messages: ChatMessage[]) {
    const conv = active.value;
    if (!conv) return;
    conv.messages = messages;
    conv.messagesLoaded = true;
    if (!conv.title || conv.title === DEFAULT_TITLE) {
      conv.title = titleFromMessages(messages) || DEFAULT_TITLE;
    }
    conv.updatedAt = Date.now();
    persist();
  }

  /** 用服务端标题列表替换当前历史，不落浏览器存储。 */
  function replaceConversations(next: ChatHistoryConversation[]) {
    const currentId = state.value.activeId;
    state.value.conversations = [...next].sort(
      (a, b) => b.updatedAt - a.updatedAt,
    );
    state.value.activeId =
      (currentId && state.value.conversations.some((c) => c.id === currentId)
        ? currentId
        : state.value.conversations[0]?.id) ?? null;
  }

  /**
   * 将服务端标题列表合并到当前会话。
   *
   * 同一个会话保留已有消息和加载状态；默认保留本地尚未出现在标题接口中的会话，
   * 用于首轮消息已发送、异步标题仍未生成的时间窗口，避免侧栏闪烁和 active 会话跳转。
   */
  function mergeConversations(
    next: ChatHistoryConversation[],
    options: {retainMissing?: boolean} = {},
  ) {
    const retainMissing = options.retainMissing ?? true;
    const current = state.value.conversations;
    const currentByConversationId = new Map(
      current
        .filter((conversation) => conversation.conversationId)
        .map((conversation) => [conversation.conversationId!, conversation]),
    );
    const remoteIds = new Set(
      next
        .map((conversation) => conversation.conversationId)
        .filter((conversationId): conversationId is string => !!conversationId),
    );

    const merged = next.map((incoming) => {
      const existing = incoming.conversationId
        ? currentByConversationId.get(incoming.conversationId)
        : undefined;
      if (!existing) return {...incoming};

      existing.conversationId = incoming.conversationId;
      existing.title = incoming.title || existing.title || DEFAULT_TITLE;
      existing.createdAt = incoming.createdAt;
      existing.updatedAt = incoming.updatedAt;
      return existing;
    });

    if (retainMissing) {
      current.forEach((conversation) => {
        if (
          !conversation.conversationId ||
          !remoteIds.has(conversation.conversationId)
        ) {
          merged.push(conversation);
        }
      });
    }

    merged.sort((a, b) => b.updatedAt - a.updatedAt);
    // 原地替换，渲染层不会经历“先清空、再插入”的瞬间状态。
    current.splice(0, current.length, ...merged);

    if (!state.value.activeId && current[0]) {
      state.value.activeId = current[0].id;
    }
  }

  /**
   * 导入一段既有消息（旧版单会话存储迁移用）；
   * 仅当历史为空时生效，导入后自动置为当前会话。
   */
  function importLegacy(messages: ChatMessage[] | null | undefined) {
    if (!messages || messages.length === 0) return;
    if (state.value.conversations.length > 0) return;
    const conv: ChatHistoryConversation = {
      id: genId(),
      title: titleFromMessages(messages) || DEFAULT_TITLE,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      messages,
    };
    state.value.conversations.push(conv);
    state.value.activeId = conv.id;
    persist();
  }

  if (persistMessages) {
    // 兜底持久化：任何深度变更都会写回 localStorage
    watch(state, persist, {deep: true});
  } else {
    // 清理旧版本已经保存的消息，避免关闭持久化后仍残留历史数据。
    try {
      localStorage.removeItem(storageKey);
    } catch {
      // 存储不可用时静默降级
    }
  }

  return {
    conversations,
    activeId,
    active,
    newChat,
    open,
    remove,
    saveActive,
    replaceConversations,
    mergeConversations,
    importLegacy,
  };
}
