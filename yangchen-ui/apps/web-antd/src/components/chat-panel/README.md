# ChatPanel — 通用 AI 对话面板

可复用的 AI 对话界面组件，供系统内多个"助手"场景（AI 智能助手、业务助手等）共用。
界面与交互由组件负责；**数据层全部由调用方注入**，因此可以对接任意后端（mock、HTTP、SSE 流式、Vercel AI SDK 等）。

## 文件结构

```
src/components/chat-panel/
├── index.vue            — ChatPanel 主组件（v-model 受控，panel / page 两种形态）
├── types.ts             — ChatMessage / ChatSendHandler / ChatStreamHandle / ChatRendererMap
├── utils.ts             — genId / nowTime
├── render-markdown.ts   — 轻量 Markdown 渲染（流式安全、防 XSS）
├── message-text.vue     — 内置文本/Markdown 消息渲染器
├── composer.vue         — 输入区（textarea + 发送键，尺寸由宿主用 --cc-* 定制）
├── message-list.vue     — 消息列表（panel 气泡 / page 流式两种排版）
├── chat-history.ts      — 多会话历史存储（useChatHistory，localStorage 持久化）
├── history-sidebar.vue  — 历史会话侧栏（DeepSeek 式，可折叠 / 复用为弹层）
└── README.md            — 本文件
```

## 两种形态

| variant | 适用场景 | 开场 | 消息排版 |
|---|---|---|---|
| `panel`（默认） | 悬浮小窗等紧凑容器 | 快捷提问卡片 | 气泡式（用户右侧药丸、助手左侧气泡） |
| `page` | 全页对话（类似 DeepSeek / ChatGPT） | 居中欢迎页（Logo + 大输入框 + 提问卡片） | 通栏流式（助手无框正文、用户右侧药丸） |

## 基本用法

```vue
<script lang="ts" setup>
import {ref} from 'vue';

import ChatPanel from '#/components/chat-panel/index.vue';
import type {ChatMessage} from '#/components/chat-panel/types';

const messages = ref<ChatMessage[]>([
  {id: '1', role: 'assistant', content: '你好，我是业务助手', time: '09:00'},
]);

// 简单问答：返回字符串 -> 组件自动追加助手消息并显示"正在输入"
async function handleSend(text: string): Promise<string> {
  const res = await fetch('/api/business/chat', {method: 'POST', body: JSON.stringify({message: text})});
  return (await res.json()).reply;
}
</script>

<template>
  <div style="height: 640px">
    <ChatPanel
      v-model="messages"
      title="业务助手"
      badge="业务"
      :suggestions="['查询今日订单', '生成周报']"
      :send="handleSend"
      :autofocus="true"
    />
  </div>
</template>
```

> 注意：ChatPanel 根节点撑满父容器（height: 100%），请在外层给定宽高。

## 发送协议（send）

`send(text, onChunk?, signal?)` 的三个参数：

| 参数 | 说明 |
|---|---|
| `text` | 用户输入 |
| `onChunk(chunk)` | 逐块推送增量文本，组件**实时渲染 Markdown**（增量合并到一帧内写入，避免逐块整段重绘） |
| `signal` | `ChatStreamHandle` 中止句柄：点击「停止生成」或组件卸载时会触发 `abort()`，宿主应据此取消底层请求并尽快结束返回的 Promise |

返回值约定：

| 返回值 | 行为 |
|---|---|
| `string` / `Promise<string>` | 显示"正在输入" -> 结果返回后自动追加为助手消息 |
| `void` / `Promise<void>` | 组件不追加消息，由调用方自行推送（配合 `busy` 控制状态） |

流式示例（SSE + 停止）：

```ts
async function handleSend(
  text: string,
  onChunk?: (chunk: string) => void,
  signal?: ChatStreamHandle,
) {
  const controller = new AbortController();
  signal?.onAbort(() => controller.abort()); // 停止按钮 -> 中止底层 fetch
  try {
    await streamApi(text, (chunk) => onChunk?.(chunk), controller.signal);
  } catch {
    if (signal?.aborted) return ''; // 用户主动停止：保留已生成内容，静默结束
    onChunk?.('抱歉，AI 暂时没有返回有效内容。');
    return '';
  }
}
```

生成期间输入框的发送键会自动变为**停止键**（方形图标 + 脉冲动画），点击触发 `stop` 事件；
已生成的增量原样保留，不会追加错误提示。流式消息末尾会显示闪烁光标，滚动条只在停留在底部时自动跟随。

## 扩展点

### 1. 自定义消息渲染器（业务卡片）

消息对象带 `type` 字段，通过 `renderers` 注册表分发渲染。渲染器为 Vue 组件，接收
props `{ message: ChatMessage }`，`message.extra` 可携带任意业务数据。

```vue
<ChatPanel v-model="messages" :renderers="{order: OrderCard}"/>
```

```vue
<!-- OrderCard.vue -->
<script lang="ts" setup>
import type {ChatMessage} from '#/components/chat-panel/types';
defineProps<{message: ChatMessage}>();
</script>
<template>
  <div class="order-card">
    <div>订单号：{{ message.extra?.orderNo }}</div>
    <div>状态：{{ message.extra?.status }}</div>
  </div>
</template>
```

```ts
messages.value.push({
  id: 'x', role: 'assistant', type: 'order',
  content: '', time: '09:01',
  extra: {orderNo: 'SO-20260814-001', status: '已发货'},
});
```

自定义类型消息气泡会自动放宽宽度（`cp-bubble--wide`），适合卡片布局。

### 2. 插槽

- `#header-actions` — 替换头部右侧操作区（默认：新对话 + 收起）

### 3. 裁剪

- `showHeader={false}` 隐藏头部（页面内嵌场景）
- `newChatable={false}` / `closable={false}` 隐藏对应按钮

## 历史会话（chat-history.ts + history-sidebar.vue）

ChatPanel 本身不负责历史（数据层由调用方注入），但配套提供了开箱即用的**多会话历史**：

- `useChatHistory(storageKey)`：新建 / 切换 / 删除会话、保存当前会话消息、自动生成标题（取首条用户消息）、旧版单会话数据迁移，全部 localStorage 持久化。
- `history-sidebar.vue`：DeepSeek 式历史侧栏（标题 + 相对时间 + 悬停删除二次确认 + 折叠窄条）。`collapsible={false}` 时始终展开，可复用为弹层。

把消息 v-model 映射到"当前会话"即可自动获得历史能力：

```vue
<script lang="ts" setup>
import {computed} from 'vue';

import ChatPanel from '#/components/chat-panel/index.vue';
import HistorySidebar from '#/components/chat-panel/history-sidebar.vue';
import {useChatHistory} from '#/components/chat-panel/chat-history';
import type {ChatMessage} from '#/components/chat-panel/types';

const {conversations, activeId, active, newChat, open, remove, saveActive, importLegacy} =
  useChatHistory('my-assistant-history-v1');

// 首次进入：无历史时开始一段新对话（也可先 importLegacy 迁移旧数据）
if (conversations.value.length === 0) newChat();

const messages = computed<ChatMessage[]>({
  get: () => (active.value ? active.value.messages : []),
  set: (val) => saveActive(val),
});
</script>

<template>
  <div style="display: flex; height: 100%">
    <HistorySidebar
      v-model:collapsed="collapsed"
      :conversations="conversations"
      :active-id="activeId"
      @select="open"
      @remove="remove"
      @new-chat="newChat"
    />
    <ChatPanel v-model="messages" variant="page" :send="handleSend"/>
  </div>
</template>
```

> 切换会话时 `messages` 的数据源自动跟随 `active` 切换；每条消息的增删都会经 setter 写回对应会话。

## 事件

| 事件 | 说明 |
|---|---|
| `update:modelValue` | 消息列表变化（v-model 绑定） |
| `send-start(text)` | 用户提交消息 |
| `new-chat` | 点击"新对话"（由调用方重置消息） |
| `close` | 点击"收起/关闭" |
| `stop` | 点击"停止生成" / 流式被中止（已保留部分回复） |

## 接入 @ai-sdk/vue（可选）

```ts
import {useChat} from '@ai-sdk/vue';

const {messages: aiMessages, sendMessage, isLoading} = useChat({
  api: '/api/ai/chat', // 需后端提供 AI SDK 兼容流式接口
});

// 将 aiMessages 映射为 ChatMessage[] 后 v-model 绑定，isLoading 映射为 busy
```

## 注意事项

- 本组件**不负责**消息持久化，由调用方通过 v-model 自行保存（参见 ai-assistant 的 localStorage 示例）。
- 组件不构建问候语，初始消息由调用方传入 `modelValue`。
- 代码块、加粗等 Markdown 渲染为轻量实现，流式安全；需要更丰富的渲染（Mermaid/KaTeX 等）可替换 `renderers` 中对应 type 的实现，或引入 markstream-vue。
