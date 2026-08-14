import type {Component} from 'vue';

/** 消息角色 */
export type ChatRole = 'user' | 'assistant';

/** 聊天消息 */
export interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  time: string;
  /** 消息类型，用于渲染器分发；缺省为 text（Markdown 文本） */
  type?: string;
  /** 业务扩展数据，由自定义渲染器消费 */
  extra?: Record<string, unknown>;
}

/**
 * 发送处理器（数据层协议）
 * - 返回 `string` 或 `Promise<string>`：ChatPanel 会先显示"正在输入"，待结果返回后自动追加为助手消息
 * - 返回 `void`：由调用方自行推送消息（流式场景，配合 busy 属性控制"正在输入"状态）
 */
export type ChatSendHandler = (
  text: string,
) => Promise<string | void> | string | void;

/**
 * 消息渲染器注册表：消息 type -> Vue 组件
 * 渲染器通过 props 接收 `{ message: ChatMessage }`，渲染在助手气泡内部
 */
export type ChatRendererMap = Record<string, Component>;
