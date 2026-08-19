import type {AiUiPayload, ChatMessage} from '../types';

export interface UiRecord {
  [key: string]: unknown;
}

export function isRecord(value: unknown): value is UiRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function getUiPayload(message: ChatMessage): AiUiPayload | null {
  const payload = message.extra?.ui;
  return isRecord(payload) && typeof payload.component === 'string'
    ? (payload as unknown as AiUiPayload)
    : null;
}

export function getUiData(message: ChatMessage): UiRecord {
  const payload = getUiPayload(message);
  return isRecord(payload?.data) ? payload.data : {};
}

export function displayValue(value: unknown): string {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

export function statusClass(value: unknown): string {
  const text = String(value ?? '').toLowerCase();
  if (['正常', '成功', '启用', 'active', 'success', 'ok'].includes(text)) {
    return 'is-success';
  }
  if (['失败', '异常', '停用', '禁用', 'error', 'failed'].includes(text)) {
    return 'is-danger';
  }
  return 'is-neutral';
}
