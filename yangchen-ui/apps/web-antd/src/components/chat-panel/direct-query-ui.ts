import {fieldLabel} from './field-labels';

import type {AiUiPayload} from './types';

interface QueryColumn {
  name?: unknown;
  typeName?: unknown;
  /** 后端可选下发的中文表头，优先使用 */
  displayName?: unknown;
}

interface ReadOnlyQueryResult {
  columns?: QueryColumn[];
  rows?: unknown[][];
  rowCount?: unknown;
}

interface ResultEnvelope {
  code?: unknown;
  data?: ReadOnlyQueryResult;
  msg?: unknown;
}

/**
 * 从 returnDirect 工具的统一结果集中提取展示卡片。
 *
 * Spring AI 会把 returnDirect 的返回值作为对话文本继续下发；这里仅识别
 * `R<ReadOnlyQueryResult>` 这一种明确结构，普通 JSON 或 Markdown 不会被误判。
 */
export function parseDirectQueryUi(content?: string): AiUiPayload | null {
  if (!content) return null;

  const envelope = findResultEnvelope(content);
  if (!envelope) return null;

  if (Number(envelope.code) !== 200) {
    return {
      type: 'ui',
      component: 'error',
      data: {
        title: '查询未完成',
        message: safeMessage(envelope.msg) || '查询暂时无法完成，请稍后重试。',
      },
      replaceText: true,
    };
  }

  const data = envelope.data;
  if (!data || !Array.isArray(data.columns) || !Array.isArray(data.rows)) {
    return null;
  }

  const columns = data.columns.map((column, index) => {
    const key = textOrFallback(column?.name, `column_${index + 1}`);
    // 优先使用后端下发的 displayName，其次前端字段中文映射，最后保留原名兜底。
    const displayName = textOrFallback(column?.displayName, '');
    return {key, title: displayName || fieldLabel(key)};
  });
  const rows = data.rows.map((values) => {
    const row: Record<string, unknown> = {};
    columns.forEach((column, index) => {
      row[column.key] = Array.isArray(values) ? values[index] : undefined;
    });
    return row;
  });

  if (rows.length === 0) {
    return {
      type: 'ui',
      component: 'result',
      data: {
        title: '查询结果',
        message: '暂无符合条件的数据',
      },
      replaceText: true,
    };
  }

  if (columns.length === 1 && rows.length === 1) {
    const column = columns[0];
    return {
      type: 'ui',
      component: 'stat',
      data: {
        title: '查询结果',
        label: statisticLabel(column.title),
        value: rows[0][column.key],
      },
      replaceText: true,
    };
  }

  return {
    type: 'ui',
    component: 'table',
    data: {
      title: '查询结果',
      columns,
      rows,
    },
    replaceText: true,
  };
}

/** 查找一段嵌在普通文本中的完整 JSON 对象。 */
function findResultEnvelope(content: string): ResultEnvelope | null {
  for (let index = 0; index < content.length; index += 1) {
    if (content[index] !== '{') continue;
    const candidate = readJsonObject(content, index);
    if (!candidate) continue;
    try {
      const parsed = JSON.parse(candidate) as ResultEnvelope;
      if (
        typeof parsed === 'object' &&
        parsed !== null &&
        ('code' in parsed || 'msg' in parsed) &&
        (Array.isArray(parsed.data?.columns) || Number(parsed.code) !== 200)
      ) {
        return parsed;
      }
    } catch {
      // 继续扫描下一个 JSON 对象。
    }
  }
  return null;
}

function readJsonObject(content: string, start: number): null | string {
  let depth = 0;
  let escaped = false;
  let quoted = false;
  for (let index = start; index < content.length; index += 1) {
    const char = content[index];
    if (quoted) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === '"') {
        quoted = false;
      }
      continue;
    }
    if (char === '"') {
      quoted = true;
    } else if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) return content.slice(start, index + 1);
    }
  }
  return null;
}

function safeMessage(value: unknown) {
  const message = typeof value === 'string' ? value.trim() : '';
  return message && !/操作成功|success/i.test(message) ? message : '';
}

function statisticLabel(value: string) {
  return /^(count|total|total_count|数量|总数)$/i.test(value.trim())
    ? '数量'
    : value;
}

function textOrFallback(value: unknown, fallback: string) {
  const text = typeof value === 'string' ? value.trim() : '';
  return text || fallback;
}
