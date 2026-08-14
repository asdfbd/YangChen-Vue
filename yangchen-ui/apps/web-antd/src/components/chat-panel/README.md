# ChatPanel — 通用 AI 对话面板

可复用的 AI 对话界面组件，供系统内多个"助手"场景（AI 智能助手、业务助手等）共用。
界面与交互由组件负责；**数据层全部由调用方注入**，因此可以对接任意后端（mock、HTTP、SSE 流式、Vercel AI SDK 等）。

## 文件结构

```
src/components/chat-panel/
├── index.vue            — ChatPanel 主组件（v-model 受控）
├── types.ts             — ChatMessage / ChatSendHandler / ChatRendererMap
├── utils.ts             — genId / nowTime
├── render-markdown.ts   — 轻量 Markdown 渲染（流式安全、防 XSS）
├── message-text.vue     — 内置文本/Markdown 消息渲染器
└── README.md            — 本文件
```

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

| 返回值 | 行为 |
|---|---|
| `string` / `Promise<string>` | 显示"正在输入" -> 结果返回后自动追加为助手消息 |
| `void` / `Promise<void>` | 组件不追加消息，由调用方自行推送（**流式场景**） |

流式示例（配合 `busy`）：

```vue
<ChatPanel
  v-model="messages"
  :busy="streaming"
  @send-start="(text) => startStream(text)"
/>
```

```ts
// 调用方自行维护消息流：
async function startStream(text: string) {
  streaming.value = true;
  // 1. 先入列用户消息（send-start 已由组件触发，此处只推送助手占位）
  messages.value.push({id: 'assistant', role: 'assistant', content: '', time: '...'});
  // 2. 从 SSE / useChat 流中逐块更新 messages 最后一条的 content
  // 3. 结束后 streaming.value = false
}
```

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

## 事件

| 事件 | 说明 |
|---|---|
| `update:modelValue` | 消息列表变化（v-model 绑定） |
| `send-start(text)` | 用户提交消息 |
| `new-chat` | 点击"新对话"（由调用方重置消息） |
| `close` | 点击"收起/关闭" |

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
