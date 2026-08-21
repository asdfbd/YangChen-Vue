<script lang="ts" setup>
import {computed, ref} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {displayValue, getUiData, getUiPayload} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatConfirmCard'});

const props = defineProps<{message: ChatMessage}>();
const emit = defineEmits<{
  'ui-action': [payload: {action: 'confirm' | 'cancel'; actionId?: string}];
}>();

const payload = computed(() => getUiPayload(props.message));
const data = computed(() => getUiData(props.message));
const fields = computed(() => (Array.isArray(data.value.fields) ? data.value.fields : []));
const completedAction = ref<null | 'cancel' | 'confirm'>(null);
const restoredAction = computed<null | 'cancel' | 'confirm'>(() => {
  const value = data.value.completedAction;
  return value === 'confirm' || value === 'cancel' ? value : null;
});
const currentAction = computed(() => completedAction.value ?? restoredAction.value);

function action(action: 'confirm' | 'cancel') {
  if (currentAction.value) return;
  completedAction.value = action;
  emit('ui-action', {action, actionId: payload.value?.action?.actionId});
}
</script>

<template>
  <section class="ui-card ui-confirm-card">
    <div class="ui-confirm-card__icon"><IconifyIcon icon="lucide:shield-check"/></div>
    <div class="ui-confirm-card__content">
      <div class="ui-card__title">{{ data.title || '请确认此操作' }}</div>
      <div v-if="data.description" class="ui-card__desc">{{ data.description }}</div>
      <div v-if="fields.length" class="ui-confirm-fields">
        <div v-for="(field, index) in fields" :key="String(field.key ?? index)" class="ui-confirm-field">
          <span>{{ field.label || field.key }}</span>
          <strong>{{ displayValue(field.value) }}</strong>
        </div>
      </div>
      <div class="ui-confirm-actions">
        <button class="ui-btn ui-btn--ghost" type="button" :disabled="Boolean(currentAction)" @click="action('cancel')">
          {{ currentAction === 'cancel' ? '已取消' : (payload?.action?.cancelText || '取消') }}
        </button>
        <button class="ui-btn ui-btn--primary" type="button" :disabled="Boolean(currentAction)" @click="action('confirm')">
          <IconifyIcon icon="lucide:check"/>
          {{ currentAction === 'confirm' ? '已确认' : (payload?.action?.confirmText || '确认操作') }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ui-card {
  display: flex;
  width: 100%;
  gap: 13px;
  padding: 16px;
  border: 1px solid hsl(var(--primary) / 0.22);
  border-radius: 16px;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.065), hsl(var(--card) / 0.94));
  box-shadow: 0 12px 30px -25px hsl(var(--primary) / 0.62);
}

.ui-confirm-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  color: hsl(var(--primary));
  border-radius: 10px;
  background: hsl(var(--primary) / 0.12);
}

.ui-confirm-card__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.ui-confirm-card__content {
  min-width: 0;
  flex: 1;
}

.ui-card__title {
  font-size: 15px;
  font-weight: 650;
  line-height: 1.45;
}

.ui-card__desc {
  margin-top: 5px;
  font-size: 13px;
  line-height: 1.65;
  color: hsl(var(--muted-foreground));
}

.ui-confirm-fields {
  display: grid;
  gap: 7px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: hsl(var(--background) / 0.52);
}

.ui-confirm-field {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.ui-confirm-field span {
  color: hsl(var(--muted-foreground));
}

.ui-confirm-field strong {
  overflow: hidden;
  font-weight: 550;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.ui-btn {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 13px;
  font-size: 12px;
  border: 1px solid transparent;
  border-radius: 9px;
  cursor: pointer;
}

.ui-btn :deep(svg) {
  width: 14px;
  height: 14px;
}

.ui-btn--ghost {
  color: hsl(var(--muted-foreground));
  border-color: hsl(var(--border));
  background: hsl(var(--card) / 0.75);
}

.ui-btn--primary {
  color: hsl(var(--primary-foreground));
  background: hsl(var(--primary));
  box-shadow: 0 6px 12px -8px hsl(var(--primary) / 0.75);
}

.ui-btn:hover {
  filter: brightness(0.98);
  transform: translateY(-1px);
}

.ui-btn:disabled {
  cursor: default;
  opacity: 0.58;
  transform: none;
}
</style>
