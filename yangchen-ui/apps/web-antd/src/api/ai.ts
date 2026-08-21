import {useAppConfig} from '@vben/hooks';
import {useAccessStore} from '@vben/stores';

import {requestClient} from './request';

import {parseDirectQueryUi} from '#/components/chat-panel/direct-query-ui';

import type {AiUiPayload, AiUiComponent, AiUiActionSpec} from '#/components/chat-panel/types';

const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);

export interface ChatResp {
  text?: string;
  role?: string;
  type?: 'text' | 'ui' | 'event' | string;
  component?: AiUiComponent;
  data?: unknown;
  action?: AiUiActionSpec;
  messageId?: string;
  ui?: Omit<AiUiPayload, 'type'> & {type?: 'ui'};
}

export interface ChatTitle {
  id?: number | string;
  userId?: number | string;
  conversationId: number | string;
  title: string;
  createTime?: string;
  updateTime?: string;
}

export interface ChatContent {
  id?: number | string;
  conversationId: number | string;
  messageType: string;
  content: string;
  createTime?: string;
  type?: 'text' | 'ui' | 'event' | string;
  component?: AiUiComponent;
  data?: unknown;
  action?: AiUiActionSpec;
  messageId?: string;
  ui?: Omit<AiUiPayload, 'type'> & {type?: 'ui'};
}

export const CONVERSATION_ID_HEADER = 'x-conversation-id';
export const TOOL_APPROVAL_ID_HEADER = 'x-tool-approval-id';

/** 创建后端 AI 会话，普通对话接口会校验该会话 ID。 */
export async function generateConversationIdApi(): Promise<string> {
  const conversationId = await requestClient.get<number | string>(
    '/ai/chat/generateConversationId',
  );
  if (conversationId === undefined || conversationId === null) {
    throw new Error('AI 会话 ID为空');
  }
  return String(conversationId);
}

export function listChatTitlesApi(userId: string | number) {
  return requestClient.get<ChatTitle[]>(`/ai/chat/title/list/${userId}`, {
    headers: {[CONVERSATION_ID_HEADER]: 'history'},
  });
}

export function listChatContentApi(conversationId: string | number) {
  return requestClient.get<ChatContent[]>(`/ai/chat/list/${conversationId}`, {
    headers: {[CONVERSATION_ID_HEADER]: String(conversationId)},
  });
}

/** 根据首条用户消息生成会话标题；重复调用同一会话会直接返回既有标题。 */
export function generateChatTitleApi(
  userInput: string,
  conversationId: string | number,
) {
  return requestClient.post<ChatTitle>('/ai/chat/title/generate', userInput, {
    headers: {[CONVERSATION_ID_HEADER]: String(conversationId)},
  });
}

export function updateChatTitleApi(
  title: Pick<ChatTitle, 'id' | 'title'> & {conversationId?: string | number},
) {
  return requestClient.put('/ai/chat/title/', title, {
    headers: {
      [CONVERSATION_ID_HEADER]: String(title.conversationId ?? title.id ?? 'title'),
    },
  });
}

export function deleteChatTitleApi(conversationId: string | number) {
  return requestClient.delete(`/ai/chat/title/${conversationId}`, {
    headers: {[CONVERSATION_ID_HEADER]: String(conversationId)},
  });
}

/**
 * 调用 AI 流式对话接口。
 * Spring WebFlux 可能返回 NDJSON 或 SSE，这里统一按行解析。
 * @param signal 可选的 AbortSignal：前端「停止生成」时中止底层请求，
 *   已收到的增量文本由上游保留，不会追加错误提示。
 */
export async function streamChatApi(
  userInput: string,
  conversationId: string,
  onChunk?: (chunk: string) => void,
  signal?: AbortSignal,
  onUiMessage?: (payload: AiUiPayload) => void,
  approvalId?: string,
): Promise<string> {
  const accessStore = useAccessStore();
  const response = await fetch(`${apiURL}/ai/chat/`, {
    method: 'POST',
    headers: {
      Accept: 'application/x-ndjson, text/event-stream, application/json',
      Authorization: accessStore.accessToken
        ? `Bearer ${accessStore.accessToken}`
        : '',
      'Content-Type': 'application/json',
      [CONVERSATION_ID_HEADER]: conversationId,
      ...(approvalId ? {[TOOL_APPROVAL_ID_HEADER]: approvalId} : {}),
    },
    body: JSON.stringify(userInput),
    signal,
  });

  if (!response.ok) {
    throw new Error(`AI 请求失败（HTTP ${response.status}）`);
  }

  if (!response.body) {
    throw new Error('AI 接口未返回可读取的响应流');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let result = '';
  let hasUiMessage = false;
  let directPayloadBuffer = '';
  let directResultHandled = false;

  const emitUi = (payload: AiUiPayload) => {
    hasUiMessage = true;
    onUiMessage?.(payload);
  };

  const flushDirectPayload = (force = false) => {
    if (!directPayloadBuffer) return;
    const ui = parseDirectQueryUi(directPayloadBuffer);
    if (ui) {
      directResultHandled = true;
      directPayloadBuffer = '';
      emitUi(ui);
      return;
    }
    if (force) {
      onChunk?.(directPayloadBuffer);
      directPayloadBuffer = '';
    }
  };

  const consumeText = (text: string) => {
    result += text;
    if (directResultHandled) return;

    if (directPayloadBuffer) {
      directPayloadBuffer += text;
      flushDirectPayload();
      return;
    }

    // `returnDirect` 的统一结果集可能被模型转义为 {\"msg\"...}，
    // 并且开头的 {\ 还可能被单独拆成一个流式分片。
    const match = /\{\s*\\?"(?:msg|code)"\s*:/.exec(text);
    if (!match || match.index === undefined) {
      const possibleStart = /\{\s*(?:\\?"?)?$/.exec(text);
      if (possibleStart?.index !== undefined) {
        const beforePayload = text.slice(0, possibleStart.index);
        if (beforePayload) onChunk?.(beforePayload);
        directPayloadBuffer = text.slice(possibleStart.index);
        return;
      }
      onChunk?.(text);
      return;
    }

    const beforePayload = text.slice(0, match.index);
    if (beforePayload) onChunk?.(beforePayload);
    directPayloadBuffer = text.slice(match.index);
    flushDirectPayload();
  };

  const consumeLine = (line: string) => {
    const payload = line.trim().replace(/^data:\s*/, '');
    if (!payload || payload === '[DONE]') return;

    try {
      const parsed = JSON.parse(payload) as ChatResp | ChatResp[];
      const chunks = Array.isArray(parsed) ? parsed : [parsed];
      chunks.forEach((chunk) => {
        const ui = chunk.ui?.component
          ? {
              type: 'ui' as const,
              component: chunk.ui.component,
              data: chunk.ui.data,
              action: chunk.ui.action,
              messageId: chunk.ui.messageId,
            }
          : chunk.type === 'ui' && chunk.component
            ? {
                type: 'ui' as const,
                component: chunk.component,
                data: chunk.data,
                action: chunk.action,
                messageId: chunk.messageId,
              }
            : null;
        if (ui) {
          emitUi(ui);
        }
        if (chunk?.text?.trim()) {
          consumeText(chunk.text);
        }
      });
    } catch {
      // 非完整 JSON 留给下一次读取继续拼接。
      buffer = `${payload}${buffer}`;
    }
  };

  try {
    while (true) {
      const {done, value} = await reader.read();
      buffer += decoder.decode(value, {stream: !done});

      if (done) {
        if (buffer.trim()) consumeLine(buffer);
        break;
      }

      const lines = buffer.split(/\r?\n/);
      buffer = lines.pop() ?? '';
      lines.forEach(consumeLine);
    }
  } finally {
    reader.releaseLock();
  }

  flushDirectPayload(true);

  if (!result.trim() && !hasUiMessage) {
    throw new Error('AI 接口未返回有效内容');
  }

  // 仅消费最终回答或结构化 UI 消息，避免旧服务端意外返回 reasonText 时重新展示思考过程。
  return result;
}
