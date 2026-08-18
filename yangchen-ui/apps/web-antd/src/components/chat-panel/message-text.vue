<script lang="ts" setup>
import {ref} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {renderMarkdown} from './render-markdown';

import type {ChatMessage} from './types';

defineOptions({name: 'ChatMessageText'});

const props = withDefaults(
  defineProps<{
    message: ChatMessage;
    /** 该消息正在流式输出：内容末尾显示闪烁光标 */
    streaming?: boolean;
  }>(),
  {streaming: false},
);

const messageCopied = ref(false);

async function copyText(text: string) {
  if (!text) return false;
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.append(textarea);
    textarea.select();
    const copied = document.execCommand('copy');
    textarea.remove();
    return copied;
  }
}

async function copyMessage() {
  if (!(await copyText(props.message.content))) return;
  messageCopied.value = true;
  window.setTimeout(() => {
    messageCopied.value = false;
  }, 1600);
}

async function onRenderedClick(event: MouseEvent) {
  const target = event.target as HTMLElement;
  const button = target.closest<HTMLButtonElement>('[data-copy-code]');
  if (!button) return;
  const code = button.closest('.cmt-code-block')?.querySelector('code')?.textContent;
  if (!(await copyText(code?.trim() ?? ''))) return;
  const original = button.textContent;
  button.textContent = '已复制';
  window.setTimeout(() => {
    button.textContent = original || '复制代码';
  }, 1600);
}
</script>

<template>
  <div :class="['cmt-text-wrap', {'cmt-text-wrap--streaming': streaming}]">
    <button
      :title="messageCopied ? '已复制' : '复制回答'"
      class="cmt-message-copy"
      type="button"
      @click="copyMessage"
    >
      <IconifyIcon :icon="messageCopied ? 'lucide:check' : 'lucide:copy'"/>
      <span>{{ messageCopied ? '已复制' : '复制' }}</span>
    </button>
    <div
      class="cmt-text"
      v-html="renderMarkdown(message.content)"
      @click="onRenderedClick"
    ></div>

  </div>
</template>

<style scoped>
.cmt-text-wrap {
  position: relative;
}

.cmt-message-copy {
  position: absolute;
  top: -3px;
  right: 0;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 8px;
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  border: 1px solid hsl(var(--border) / 0.78);
  border-radius: 7px;
  background: hsl(var(--card) / 0.9);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.cmt-text-wrap:hover .cmt-message-copy,
.cmt-message-copy:focus-visible {
  opacity: 1;
}

.cmt-message-copy:hover {
  color: hsl(var(--primary));
  border-color: hsl(var(--primary) / 0.45);
}

.cmt-message-copy :deep(svg) {
  width: 13px;
  height: 13px;
}

.cmt-text {
  font-size: inherit;
  line-height: inherit;
}

.cmt-text :deep(strong) {
  font-weight: 600;
  color: hsl(var(--primary));
}

.cmt-text :deep(p) {
  margin: 0 0 10px;
}

.cmt-text :deep(p:last-child) {
  margin-bottom: 0;
}

.cmt-text :deep(h1),
.cmt-text :deep(h2),
.cmt-text :deep(h3),
.cmt-text :deep(h4) {
  margin: 14px 0 8px;
  font-weight: 650;
  line-height: 1.35;
}

.cmt-text :deep(h1) { font-size: 1.35em; }
.cmt-text :deep(h2) { font-size: 1.2em; }
.cmt-text :deep(h3),
.cmt-text :deep(h4) { font-size: 1.08em; }

.cmt-text :deep(ul),
.cmt-text :deep(ol) {
  margin: 6px 0 10px;
  padding-left: 22px;
}

.cmt-text :deep(li) {
  margin: 4px 0;
}

.cmt-text :deep(a) {
  color: hsl(var(--primary));
  text-decoration: underline;
  text-underline-offset: 2px;
}

.cmt-text :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 12px;
  color: hsl(var(--muted-foreground));
  border-left: 3px solid hsl(var(--primary) / 0.45);
  background: hsl(var(--muted) / 0.55);
}

.cmt-text :deep(hr) {
  margin: 12px 0;
  border: 0;
  border-top: 1px solid hsl(var(--border));
}

.cmt-text :deep(.cmt-table-wrap) {
  width: 100%;
  margin: 12px 0 14px;
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: hsl(var(--muted-foreground) / 0.26) transparent;
}

.cmt-text :deep(table) {
  width: max-content;
  min-width: 0;
  overflow: hidden;
  border: 1px solid hsl(var(--border) / 0.9);
  border-radius: 10px;
  border-spacing: 0;
  background: hsl(var(--card) / 0.54);
}

.cmt-text :deep(th),
.cmt-text :deep(td) {
  min-width: 100px;
  padding: 9px 12px;
  text-align: left;
  white-space: nowrap;
  border-right: 1px solid hsl(var(--border) / 0.72);
  border-bottom: 1px solid hsl(var(--border) / 0.72);
}

.cmt-text :deep(th:last-child),
.cmt-text :deep(td:last-child) {
  border-right: 0;
}

.cmt-text :deep(th) {
  font-size: 0.92em;
  font-weight: 650;
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.09);
}

.cmt-text :deep(tr:nth-child(even) td) {
  background: hsl(var(--muted) / 0.28);
}

.cmt-text :deep(tr:last-child td) {
  border-bottom: 0;
}

.cmt-text :deep(.cmt-math) {
  color: inherit;
}

.cmt-text :deep(.cmt-math--block) {
  display: block;
  margin: 12px 0;
  overflow-x: auto;
  text-align: center;
}

.cmt-text :deep(.katex-display) {
  margin: 0.5em 0;
}

.cmt-text :deep(.cp-thinking) {
  margin: 2px 0 14px;
  padding: 8px 11px;
  color: hsl(var(--muted-foreground));
  border-left: 2px solid hsl(var(--border));
  border-radius: 0 8px 8px 0;
  background: hsl(var(--muted) / 0.42);
  font-size: 0.9em;
  line-height: 1.65;
  opacity: 0.78;
}

.cmt-text :deep(.cp-thinking__title) {
  display: flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--muted-foreground));
  font-size: 0.86em;
  font-weight: 600;
  list-style: none;
  cursor: pointer;
  user-select: none;
}

.cmt-text :deep(.cp-thinking__title::-webkit-details-marker) {
  display: none;
}

.cmt-text :deep(.cp-thinking__title::before) {
  content: '›';
  font-size: 15px;
  line-height: 1;
  transform: rotate(0deg);
  transition: transform 0.18s ease;
}

.cmt-text :deep(.cp-thinking[open] > .cp-thinking__title::before) {
  transform: rotate(90deg);
}

.cmt-text :deep(.cp-thinking__body) {
  margin-top: 6px;
  white-space: pre-wrap;
}

.cmt-text :deep(.cp-thinking__body > :last-child) {
  margin-bottom: 0;
}

.cmt-text :deep(code:not(pre code)) {
  padding: 1px 6px;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  color: hsl(var(--primary));
  border-radius: 5px;
  background: hsl(var(--primary) / 0.12);
}

.cmt-text :deep(pre) {
  margin: 0;
  padding: 10px 12px;
  overflow-x: auto;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.65;
  color: #e2e8f0;
  border-radius: 10px;
  background: #111827;
}

.cmt-text :deep(.cmt-code-block) {
  margin: 10px 0 6px;
  overflow: hidden;
  border: 1px solid #263244;
  border-radius: 10px;
  background: #111827;
}

.cmt-text :deep(.cmt-code-block__bar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 30px;
  padding: 0 10px 0 12px;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #9fb0c3;
  border-bottom: 1px solid #263244;
  background: #161f2e;
}

.cmt-text :deep(.cmt-code-copy) {
  padding: 3px 7px;
  font: inherit;
  letter-spacing: 0;
  color: #c7d2df;
  border: 1px solid #334155;
  border-radius: 5px;
  background: #1f2937;
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.cmt-text :deep(.cmt-code-copy:hover) {
  color: #fff;
  border-color: #5ea9ff;
  background: #263b54;
}

.cmt-text :deep(pre code) {
  padding: 0;
  font: inherit;
  color: inherit;
  background: transparent;
}

/* highlight.js 语法着色（GitHub Dark 风格，兼顾当前深色代码块） */
.cmt-text :deep(.hljs-comment),
.cmt-text :deep(.hljs-quote) {
  color: #b7c7d8;
  font-style: normal;
  font-weight: 450;
}

.cmt-text :deep(.hljs-keyword),
.cmt-text :deep(.hljs-selector-tag),
.cmt-text :deep(.hljs-subst) {
  color: #ff7b72;
}

.cmt-text :deep(.hljs-string),
.cmt-text :deep(.hljs-doctag),
.cmt-text :deep(.hljs-regexp),
.cmt-text :deep(.hljs-template-tag),
.cmt-text :deep(.hljs-template-variable) {
  color: #a5d6ff;
}

.cmt-text :deep(.hljs-title),
.cmt-text :deep(.hljs-title.class_),
.cmt-text :deep(.hljs-title.function_),
.cmt-text :deep(.hljs-type),
.cmt-text :deep(.hljs-built_in) {
  color: #d2a8ff;
}

.cmt-text :deep(.hljs-number),
.cmt-text :deep(.hljs-literal),
.cmt-text :deep(.hljs-symbol),
.cmt-text :deep(.hljs-bullet) {
  color: #79c0ff;
}

.cmt-text :deep(.hljs-attr),
.cmt-text :deep(.hljs-attribute),
.cmt-text :deep(.hljs-variable),
.cmt-text :deep(.hljs-property) {
  color: #ffa657;
}

.cmt-text :deep(.hljs-meta),
.cmt-text :deep(.hljs-selector-id),
.cmt-text :deep(.hljs-selector-class) {
  color: #7ee787;
}

/* ===== 流式输出光标：挂在最后一块内容末尾，模拟打字机效果 ===== */
.cmt-text-wrap--streaming .cmt-text :deep(p:last-child)::after,
.cmt-text-wrap--streaming .cmt-text :deep(pre:last-child)::after,
.cmt-text-wrap--streaming .cmt-text :deep(blockquote:last-child)::after,
.cmt-text-wrap--streaming .cmt-text :deep(li:last-child)::after,
.cmt-text-wrap--streaming .cmt-text :deep(div:last-child)::after {
  display: inline-block;
  width: 7px;
  height: 1.05em;
  margin-left: 3px;
  content: '';
  vertical-align: -0.18em;
  border-radius: 2px;
  background: hsl(var(--primary) / 0.85);
  animation: cmt-cursor-blink 1.05s steps(2, start) infinite;
}

@keyframes cmt-cursor-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cmt-text-wrap--streaming .cmt-text :deep(p:last-child)::after,
  .cmt-text-wrap--streaming .cmt-text :deep(pre:last-child)::after,
  .cmt-text-wrap--streaming .cmt-text :deep(blockquote:last-child)::after,
  .cmt-text-wrap--streaming .cmt-text :deep(li:last-child)::after,
  .cmt-text-wrap--streaming .cmt-text :deep(div:last-child)::after {
    animation: none;
    opacity: 0.6;
  }
}</style>
