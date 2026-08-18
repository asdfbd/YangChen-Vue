import hljs from 'highlight.js/lib/common';
import katex from 'katex';
import MarkdownIt from 'markdown-it';
import 'katex/dist/katex.min.css';

/**
 * Markdown 渲染器。
 * html=false：不允许模型直接注入 HTML，避免 v-html 带来的 XSS 风险。
 * breaks=true：适合聊天场景，保留模型输出中的普通换行。
 */
const markdown = new MarkdownIt({
  breaks: true,
  highlight(code: string, language: string) {
    const aliases: Record<string, string> = {
      html: 'xml',
      js: 'javascript',
      jsx: 'javascript',
      sh: 'bash',
      shell: 'bash',
      ts: 'typescript',
      tsx: 'typescript',
      vue: 'xml',
      yml: 'yaml',
    };
    const normalizedLanguage = aliases[language.toLowerCase()] ?? language;

    if (!normalizedLanguage || !hljs.getLanguage(normalizedLanguage)) {
      return '';
    }

    return hljs.highlight(code, {language: normalizedLanguage}).value;
  },
  html: false,
  linkify: true,
  typographer: true,
});

/** fenced code block 增加语言标签和复制入口，复制行为由 message-text.vue 统一代理。 */
markdown.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index];
  if (!token) return '';
  const language = token.info.trim().split(/\s+/)[0]?.toLowerCase() ?? '';
  const aliases: Record<string, string> = {
    html: 'xml',
    js: 'javascript',
    jsx: 'javascript',
    md: 'markdown',
    sh: 'bash',
    shell: 'bash',
    ts: 'typescript',
    tsx: 'typescript',
    vue: 'xml',
    yml: 'yaml',
  };
  const normalizedLanguage = aliases[language] ?? language;
  const content = token.content;
  const highlighted =
    normalizedLanguage && hljs.getLanguage(normalizedLanguage)
      ? hljs.highlight(content, {language: normalizedLanguage}).value
      : markdown.utils.escapeHtml(content);
  const label = markdown.utils.escapeHtml(language || '代码');
  const className = normalizedLanguage
    ? ` class="language-${markdown.utils.escapeHtml(normalizedLanguage)}"`
    : '';
  return `<div class="cmt-code-block"><div class="cmt-code-block__bar"><span>${label}</span><button class="cmt-code-copy" type="button" data-copy-code>复制代码</button></div><pre><code${className}>${highlighted}</code></pre></div>`;
};

markdown.renderer.rules.table_open = () =>
  '<div class="cmt-table-wrap"><table>';
markdown.renderer.rules.table_close = () => '</table></div>';

interface MathItem {
  block: boolean;
  html: string;
}

function renderMath(source: string): {items: MathItem[]; source: string} {
  const items: MathItem[] = [];
  const addMath = (expression: string, block: boolean) => {
    try {
      const html = katex.renderToString(expression.trim(), {
        displayMode: block,
        throwOnError: false,
        strict: false,
      });
      const index = items.push({block, html}) - 1;
      return `CHAT_MATH_BLOCK_${index}`;
    } catch {
      return null;
    }
  };

  const normalized = source
    // 先处理块公式，避免被行内公式规则截断。
    .replace(/\$\$([\s\S]+?)\$\$|\\\[([\s\S]+?)\\\]/g, (_match, dollar, bracket) => {
      return addMath(dollar ?? bracket, true) ?? _match;
    })
    .replace(/\$([^$\n]+?)\$|\\\(([^)\n]+?)\\\)/g, (_match, dollar, paren) => {
      return addMath(dollar ?? paren, false) ?? _match;
    })
    // 兼容模型把独立公式输出成 [e^{i\\pi}+1=0] 的形式。
    .replace(/\[([^\]\n]+)\]/g, (_match, expression: string) => {
      if (!/[\\^_=]|\\[a-zA-Z]+/.test(expression)) return _match;
      return addMath(expression, true) ?? _match;
    })
    // 兼容单独成行但没有公式包裹符的 LaTeX，例如 y\ge x\Rightarrow ...。
    .replace(
      /(^|\n)([^\n]*\\[a-zA-Z]+[^\n]*)(?=\n|$)/g,
      (_match: string, prefix: string, expression: string) => {
        if (/[一-鿿]/.test(expression)) return _match;
        return `${prefix}${addMath(expression, true) ?? expression}`;
      },
    )
    // 兼容中文句子中的裸公式，例如“如果 f(x,y)\ge0”。
    .replace(
      /\b[A-Za-z]\w*\([^\)\n]*\)\s*\\(?:ge|le|neq|approx|gt|lt|in)\s*[A-Za-z0-9^_+\-*/.,]+/g,
      (expression: string) => addMath(expression, false) ?? expression,
    )
    // 兼容模型未加 $ 分隔符的函数方程，例如 s(t)=t^2、v(t)=s'(t)=2t。
    .replace(
      /\b[A-Za-z]\w*\([^\)\n]*\)(?:\s*=\s*(?:[A-Za-z]\w*'?\([^\)\n]*\)|[A-Za-z0-9^_+\-*/]+))+/g,
      (expression: string) => addMath(expression, false) ?? expression,
    );

  // 数学占位符不能被 Markdown 解释成普通段落内容，因此保留原样返回。
  return {items, source: normalized};
}

/** 修复模型流式输出中常见但不符合标准 Markdown 的紧连写法。 */
function normalizeTableLayout(text: string): string {
  const lines = text.split('\n');
  const isDelimiter = (line: string) =>
    /^\s*\|?\s*:?-{3,}:?\s*(?:\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line);

  for (let index = 0; index < lines.length - 1; index += 1) {
    const line = lines[index] ?? '';
    const nextLine = lines[index + 1] ?? '';
    const headingMatch = line.match(/^(#{1,6}\s*[^|\n]+?)\s*(\|.+)$/);

    // 模型偶尔会输出 "###3.标题|表头"，将标题与表格拆成两个块。
    if (headingMatch && isDelimiter(nextLine)) {
      const heading = headingMatch[1] ?? '';
      const tableHeader = headingMatch[2] ?? '';
      lines.splice(index, 1, heading.trimEnd(), '', tableHeader.trim());
      index += 2;
      continue;
    }

    // 普通段落紧接表格时补一个空行，避免表格被前面的文本吞掉。
    if (line.includes('|') && isDelimiter(nextLine) && index > 0) {
      const previousLine = lines[index - 1] ?? '';
      if (previousLine.trim()) {
        lines.splice(index, 0, '');
        index += 1;
      }
    }
  }

  return lines.join('\n');
}

function normalizeStreamingMarkdown(text: string): string {
  return normalizeTableLayout(
    text
      .replace(/\r\n?/g, '\n')
      // 流式阶段可能暂时只有一个结束星号，避免显示为“回答*”。
      .replace(/\*\*([^*\n]+)\*(?!\*)/g, '**$1**')
      // "###标题" -> "### 标题"
      .replace(/^(#{1,6})(?=\S)/gm, '$1 ')
      // "###如何区分1. xxx" -> 标题与有序列表拆开
      .replace(/^(#{1,6}\s*[^\n]*?)(\d+\.\s+)/gm, '$1\n\n$2')
      // "*15.**求极限" -> "15. 求极限"，兼容题号与加粗标记粘连。
      .replace(/^(\s*)\*{1,2}(\d+)\.\*{1,2}\s*/gm, '$1$2. ')
      // "-单层循环" -> "- 单层循环"
      .replace(/^(\s*[-*+])(?=\S)/gm, '$1 ')
      // 模型偶尔会转义标题符号，导致 "# 标题" 以原文展示。
      .replace(/^(\s*)\\(#{1,6})(?=\s)/gm, '$1$2')
    // "```javaimport" / "```bashsudo" -> 语言标识后补换行
    .replace(
      /```(java|kotlin|groovy|javascript|typescript|jsx|tsx|vue|html|css|scss|less|json|xml|yaml|yml|md|markdown|sql|bash|shell|sh|powershell|python|go|rust|csharp|cpp|c)(?=\S)/gi,
      '```$1\n',
    )
    // "```\nmd\n内容" -> "```md\n内容"，避免语言标识被当成代码首行。
    .replace(
      /```[ \t]*\n[ \t]*(java|kotlin|groovy|javascript|typescript|jsx|tsx|vue|html|css|scss|less|json|xml|yaml|yml|md|markdown|sql|bash|shell|sh|powershell|python|go|rust|csharp|cpp|c)[ \t]*\n/gi,
      '```$1\n',
    )
    // "command```" -> 结束围栏单独占一行
    .replace(/([^\n])```/g, '$1\n```'),
  );
}

export function renderMarkdown(text: string): string {
  const thinkingBlocks: Array<{complete: boolean; content: string}> = [];
  const math = renderMath(normalizeStreamingMarkdown(text));
  let source = math.source;

  // 未收到结束标记时保持展开；收到 ::: 后视为思考完成并自动折叠。
  source = source.replace(
    /:::thinking\s*\n([\s\S]*?)(\n:::\s*|$)/g,
    (_match, content: string, terminator: string) => {
      const index =
        thinkingBlocks.push({
          complete: terminator.trimStart().startsWith(':::'),
          content: content.trim(),
        }) - 1;
      return `\n\nCHAT_THINKING_BLOCK_${index}\n\n`;
    },
  );

  let html = markdown.render(source);
  thinkingBlocks.forEach(({complete, content}, index) => {
    const token = `CHAT_THINKING_BLOCK_${index}`;
    const openAttribute = complete ? '' : ' open';
    const title = complete ? '思考完成' : '正在思考';
    const renderedThinking = `<details class="cp-thinking"${openAttribute}><summary class="cp-thinking__title">${title}</summary><div class="cp-thinking__body">${markdown.render(content)}</div></details>`;
    html = html.replace(`<p>${token}</p>`, renderedThinking);
  });

  math.items.forEach(({block, html: mathHtml}, index) => {
    const token = `CHAT_MATH_BLOCK_${index}`;
    const rendered = `<span class="cmt-math${block ? ' cmt-math--block' : ''}">${mathHtml}</span>`;
    // 块公式可能和正文处于同一个段落，必须有全局兜底替换，不能泄漏占位符。
    html = html.replace(`<p>${token}</p>`, rendered).replaceAll(token, rendered);
  });

  return html;
}
