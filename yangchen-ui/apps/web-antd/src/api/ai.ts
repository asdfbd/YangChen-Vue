import {useAppConfig} from '@vben/hooks';
import {useAccessStore} from '@vben/stores';

import {requestClient} from './request';

const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);

export interface ChatResp {
  text?: string;
  reasonText?: string;
  role?: string;
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
}

export const CONVERSATION_ID_HEADER = 'x-conversation-id';

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
  let reasoning = '';
  let hasReasoning = false;
  let hasAnswer = false;

  const consumeLine = (line: string) => {
    const payload = line.trim().replace(/^data:\s*/, '');
    if (!payload || payload === '[DONE]') return;

    try {
      const parsed = JSON.parse(payload) as ChatResp | ChatResp[];
      const chunks = Array.isArray(parsed) ? parsed : [parsed];
      chunks.forEach((chunk) => {
        if (chunk?.reasonText?.trim()) {
          if (!hasReasoning) {
            hasReasoning = true;
            onChunk?.(':::thinking\n');
          }
          reasoning += chunk.reasonText;
          onChunk?.(chunk.reasonText);
        }
        if (chunk?.text?.trim()) {
          if (hasReasoning && !hasAnswer) {
            onChunk?.('\n:::\n\n**回答**\n\n');
          }
          hasAnswer = true;
          result += chunk.text;
          onChunk?.(chunk.text);
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

  // 仅返回思考文本时也补齐结束标记，让前端在流结束后自动收起思考过程。
  if (hasReasoning && !hasAnswer) {
    onChunk?.('\n:::');
  }

  if (!result.trim() && !reasoning.trim()) {
    throw new Error('AI 接口未返回有效内容');
  }

  if (reasoning.trim() && result.trim()) {
    return `:::thinking\n${reasoning}\n:::\n\n**回答**\n\n${result}`;
  }

  return reasoning ? `:::thinking\n${reasoning}\n:::` : result;
}
