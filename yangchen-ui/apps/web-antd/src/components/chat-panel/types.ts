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
 * 流式中止句柄：ChatPanel 每次提交会创建一个，并通过 send 的第三个参数传给宿主。
 * - 用户点击「停止生成」按钮 / 组件卸载时，ChatPanel 调用 `abort()`
 * - 宿主应在 `onAbort` 中取消自己的底层请求（如 abort fetch），并尽快结束返回的 Promise
 * - 已中止的回复保留已生成的部分，不会追加错误提示
 * - 宿主也可以在合适的时机主动调用 `abort()`（例如切换会话时取消当前生成）
 */
export interface ChatStreamHandle {
  /** 是否已中止 */
  readonly aborted: boolean;
  /** 注册中止回调（若已中止会立即触发） */
  onAbort(callback: () => void): void;
  /** 请求中止当前流 */
  abort(): void;
}

/**
 * 发送处理器（数据层协议）
 * - 返回 `string` / `Promise<string>`：ChatPanel 先显示"正在输入"，结果返回后自动追加为助手消息
 * - 调用 `onChunk(chunk)` 推送增量文本：ChatPanel 实时更新助手消息（Markdown 逐块渲染），
 *   此时返回的完整字符串不会重复追加
 * - 第三个参数 `signal` 用于响应「停止生成」：宿主应据此取消底层请求并结束返回的 Promise
 */
export type ChatSendHandler = (
  text: string,
  onChunk?: (chunk: string) => void,
  signal?: ChatStreamHandle,
) => Promise<string | void> | string | void;

/**
 * 消息渲染器注册表：消息 type -> Vue 组件
 * 渲染器通过 props 接收 `{ message: ChatMessage }`，渲染在助手气泡内部
 */
export type ChatRendererMap = Record<string, Component>;