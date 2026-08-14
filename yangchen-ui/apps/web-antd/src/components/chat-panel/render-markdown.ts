/**
 * 轻量 Markdown 渲染（流式安全）
 * 先转义 HTML 再格式化，杜绝 XSS；流式输出未完成的 Markdown 也不会报错，
 * 只会渲染当前已有的内容。
 *
 * 支持：**加粗**、`行内代码`、```代码块```、换行
 */

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function renderMarkdown(text: string): string {
  const blocks: string[] = [];
  let html = text.replace(/```([\s\S]*?)```/g, (_match, code: string) => {
    blocks.push(code.trim());
    return `\u0000CHAT_CODE_${blocks.length - 1}\u0000`;
  });
  html = escapeHtml(html);
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/`([^`\n]+)`/g, '<code class="cp-code-inline">$1</code>');
  html = html.replace(/\n/g, '<br/>');
  html = html.replace(/\u0000CHAT_CODE_(\d+)\u0000/g, (_match, i: string) => {
    return `<pre class="cp-code"><code>${escapeHtml(blocks[Number(i)] ?? '')}</code></pre>`;
  });
  return html;
}
