<script lang="ts" setup>
import {computed, reactive} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {getUiData, getUiPayload, isRecord} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatDynamicForm'});

interface FormField {
  name: string;
  label?: string;
  type?: string;
  placeholder?: string;
  required?: boolean;
  defaultValue?: unknown;
  options?: Array<{label: string; value: string | number}>;
}

const props = defineProps<{message: ChatMessage}>();
const emit = defineEmits<{
  'ui-action': [payload: {action: 'submit'; actionId?: string; values: Record<string, unknown>}];
}>();

const payload = computed(() => getUiPayload(props.message));
const data = computed(() => getUiData(props.message));
const fields = computed<FormField[]>(() =>
  (Array.isArray(data.value.fields) ? data.value.fields : []).filter(
    (field): field is FormField => isRecord(field) && typeof field.name === 'string',
  ),
);
const form = reactive<Record<string, unknown>>({});

function fieldValue(field: FormField) {
  return form[field.name] ?? field.defaultValue ?? '';
}

function setFieldValue(field: FormField, value: unknown) {
  form[field.name] = value;
}

function submit() {
  const values = Object.fromEntries(
    fields.value.map((field) => [field.name, fieldValue(field)]),
  );
  emit('ui-action', {
    action: 'submit',
    actionId: payload.value?.action?.actionId,
    values,
  });
}
</script>

<template>
  <section class="ui-card ui-form-card">
    <div class="ui-form-card__head">
      <div>
        <div class="ui-card__title">{{ data.title || '请补充信息' }}</div>
        <div v-if="data.description" class="ui-card__desc">{{ data.description }}</div>
      </div>
      <span class="ui-card__mark"><IconifyIcon icon="lucide:clipboard-pen-line"/></span>
    </div>
    <form class="ui-form" @submit.prevent="submit">
      <label v-for="field in fields" :key="field.name" class="ui-form__field">
        <span class="ui-form__label">
          {{ field.label || field.name }}<i v-if="field.required">*</i>
        </span>
        <select
          v-if="field.type === 'select'"
          :value="String(fieldValue(field))"
          class="ui-input"
          :required="field.required"
          @change="setFieldValue(field, ($event.target as HTMLSelectElement).value)"
        >
          <option value="" disabled>{{ field.placeholder || '请选择' }}</option>
          <option v-for="option in field.options || []" :key="String(option.value)" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <textarea
          v-else-if="field.type === 'textarea'"
          :value="String(fieldValue(field))"
          class="ui-input ui-input--textarea"
          :placeholder="field.placeholder"
          :required="field.required"
          rows="3"
          @input="setFieldValue(field, ($event.target as HTMLTextAreaElement).value)"
        />
        <input
          v-else
          :type="field.type === 'number' ? 'number' : 'text'"
          :value="String(fieldValue(field))"
          class="ui-input"
          :placeholder="field.placeholder"
          :required="field.required"
          @input="setFieldValue(field, ($event.target as HTMLInputElement).value)"
        />
      </label>
      <button class="ui-btn ui-btn--primary ui-form__submit" type="submit">
        <IconifyIcon icon="lucide:send"/>
        {{ payload?.action?.submitText || '提交' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.ui-card {
  width: 100%;
  overflow: hidden;
  border: 1px solid hsl(var(--border));
  border-radius: 16px;
  background: hsl(var(--card) / 0.9);
  box-shadow: 0 10px 28px -24px rgba(15, 23, 42, 0.55);
}

.ui-form-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px 13px;
  border-bottom: 1px solid hsl(var(--border) / 0.75);
}

.ui-card__title {
  font-size: 15px;
  font-weight: 650;
}

.ui-card__desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: hsl(var(--muted-foreground));
}

.ui-card__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  color: hsl(var(--primary));
  border-radius: 9px;
  background: hsl(var(--primary) / 0.1);
}

.ui-card__mark :deep(svg),
.ui-btn :deep(svg) {
  width: 16px;
  height: 16px;
}

.ui-form {
  display: grid;
  gap: 13px;
  padding: 16px 18px 18px;
}

.ui-form__field {
  display: grid;
  gap: 6px;
}

.ui-form__label {
  font-size: 12px;
  font-weight: 550;
}

.ui-form__label i {
  margin-left: 3px;
  color: hsl(var(--destructive));
  font-style: normal;
}

.ui-input {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  font: inherit;
  font-size: 13px;
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
  border-radius: 9px;
  outline: none;
  background: hsl(var(--background) / 0.65);
}

.ui-input:focus {
  border-color: hsl(var(--primary) / 0.65);
  box-shadow: 0 0 0 3px hsl(var(--primary) / 0.1);
}

.ui-input--textarea {
  resize: vertical;
}

.ui-btn {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 14px;
  font-size: 12px;
  font-weight: 550;
  color: hsl(var(--primary-foreground));
  border: 0;
  border-radius: 9px;
  background: hsl(var(--primary));
  cursor: pointer;
}

.ui-form__submit {
  justify-self: end;
  min-width: 96px;
  margin-top: 2px;
}
</style>
