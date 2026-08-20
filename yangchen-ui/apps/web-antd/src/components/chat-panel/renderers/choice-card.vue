<script lang="ts" setup>
import {computed, ref} from 'vue';

import {Button as AButton, Input as AInput, Select as ASelect} from 'ant-design-vue';
import {IconifyIcon} from '@vben/icons';

import {getUiData, isRecord} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatChoiceCard'});

interface ChoiceOption {
  description: string;
  label: string;
  value: string;
}

const props = withDefaults(
  defineProps<{
    message: ChatMessage;
    disabled?: boolean;
    /** 卡片后紧邻的 user 原文；用于历史恢复，不另存 UI 状态。 */
    submittedText?: string;
  }>(),
  {disabled: false, submittedText: ''},
);

const emit = defineEmits<{
  'ui-action': [
    payload: {
      action: 'submit';
      values: {choiceLabel?: string; choiceValue?: string; inputValue?: string};
    },
  ];
}>();

const data = computed(() => getUiData(props.message));
const question = computed(() => text(data.value.question, '请选择需要继续处理的内容'));
const placeholder = computed(() => text(data.value.placeholder, '请选择一项'));
const options = computed<ChoiceOption[]>(() => {
  const source = data.value.options;
  if (!Array.isArray(source)) return [];
  return source
    .map((option) => {
      if (!isRecord(option)) return null;
      const label = text(option.label);
      const value = text(option.value);
      if (!label || !value) return null;
      return {label, value, description: text(option.description)};
    })
    .filter((option): option is ChoiceOption => option !== null);
});
const selectedValue = ref('');
const customInput = ref('');
const localSubmittedText = ref('');
const submittedText = computed(
  () => props.submittedText.trim() || localSubmittedText.value.trim(),
);
const submitted = computed(() => Boolean(submittedText.value));
const selectedOption = computed(() =>
  options.value.find((option) => option.value === submittedText.value),
);
const customSubmittedText = computed(() =>
  submitted.value && !selectedOption.value ? submittedText.value : '',
);
const selectOptions = computed(() =>
  options.value.map((option) => ({label: option.label, value: option.value})),
);

function selectOption(value: unknown) {
  if (props.disabled || submitted.value) return;
  const selected = typeof value === 'string' ? value : '';
  const option = options.value.find((item) => item.value === selected);
  if (!option) return;
  selectedValue.value = option.value;
  localSubmittedText.value = option.value;
  emit('ui-action', {
    action: 'submit',
    values: {choiceLabel: option.label, choiceValue: option.value},
  });
}

function submitCustomInput() {
  const value = customInput.value.trim();
  if (props.disabled || submitted.value || !value) return;
  localSubmittedText.value = value;
  emit('ui-action', {
    action: 'submit',
    values: {inputValue: value},
  });
}

function text(value: unknown, fallback = '') {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback;
}
</script>

<template>
  <section class="choice-card">
    <div class="choice-card__icon"><IconifyIcon icon="lucide:circle-help"/></div>
    <div class="choice-card__content">
      <p class="choice-card__question">{{ question }}</p>
      <label v-if="options.length" class="choice-card__label">
        <span>快捷选项（可选）</span>
        <ASelect
          :value="selectedOption?.value || selectedValue || undefined"
          :options="selectOptions"
          :placeholder="placeholder"
          :disabled="disabled || submitted"
          @change="selectOption"
        />
      </label>
      <p v-if="selectedOption?.description" class="choice-card__description">
        {{ selectedOption.description }}
      </p>
      <div v-if="!submitted" class="choice-card__custom">
        <span class="choice-card__custom-label">或自行补充</span>
        <div class="choice-card__custom-row">
          <AInput
            v-model:value="customInput"
            :disabled="disabled"
            :placeholder="text(data.placeholder, '请输入补充条件')"
            @press-enter="submitCustomInput"
          />
          <AButton
            type="primary"
            :disabled="disabled || !customInput.trim()"
            @click="submitCustomInput"
          >
            提交
          </AButton>
        </div>
      </div>
      <p v-if="selectedOption" class="choice-card__selected">
        <IconifyIcon icon="lucide:check"/>已选择「{{ selectedOption?.label }}」，正在继续处理
      </p>
      <p v-else-if="customSubmittedText" class="choice-card__selected">
        <IconifyIcon icon="lucide:check"/>已补充「{{ customSubmittedText }}」，正在继续处理
      </p>
      <p v-else class="choice-card__hint">选择后将立即发送；也可以直接输入补充条件。</p>
    </div>
  </section>
</template>

<style scoped>
.choice-card {
  display: flex;
  gap: 12px;
  width: min(100%, 620px);
  padding: 15px 17px;
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 14px;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.08), hsl(var(--card) / 0.94) 58%);
}

.choice-card__icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: hsl(var(--primary));
  border-radius: 9px;
  background: hsl(var(--primary) / 0.12);
}

.choice-card__icon :deep(svg) {
  width: 16px;
  height: 16px;
}

.choice-card__content {
  flex: 1;
  min-width: 0;
}

.choice-card__question {
  margin: 1px 0 11px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.55;
  color: hsl(var(--foreground));
}

.choice-card__label {
  display: grid;
  gap: 5px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.choice-card :deep(.ant-select) {
  width: 100%;
}

.choice-card :deep(.ant-select-selector) {
  min-height: 38px !important;
  padding: 3px 11px !important;
  border-color: hsl(var(--border)) !important;
  border-radius: 9px !important;
  background: hsl(var(--background)) !important;
}

.choice-card :deep(.ant-select-selection-item),
.choice-card :deep(.ant-select-selection-placeholder) {
  font-size: 14px;
  line-height: 30px !important;
}

.choice-card :deep(.ant-select-focused .ant-select-selector) {
  border-color: hsl(var(--primary) / 0.65);
  box-shadow: 0 0 0 3px hsl(var(--primary) / 0.12);
}

.choice-card :deep(.ant-select-disabled .ant-select-selector) {
  opacity: 0.7;
}

.choice-card__description,
.choice-card__hint,
.choice-card__selected {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.55;
}

.choice-card__description,
.choice-card__hint {
  color: hsl(var(--muted-foreground));
}

.choice-card__selected {
  display: flex;
  align-items: center;
  gap: 4px;
  color: hsl(var(--primary));
}

.choice-card__selected :deep(svg) {
  width: 13px;
  height: 13px;
}

.choice-card__custom {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.choice-card__custom-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.choice-card__custom-label::after,
.choice-card__custom-label::before {
  flex: 1;
  height: 1px;
  content: '';
  background: hsl(var(--border) / 0.72);
}

.choice-card__custom-row {
  display: flex;
  gap: 8px;
}

.choice-card__custom-row :deep(.ant-input) {
  min-width: 0;
  min-height: 38px;
  border-radius: 9px;
}

.choice-card__custom-row :deep(.ant-btn) {
  flex: 0 0 auto;
  height: 38px;
  border-radius: 9px;
}
</style>
