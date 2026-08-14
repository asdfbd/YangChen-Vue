<script lang="ts" setup>
import {reactive, ref} from 'vue';

import {Button, Form, Input, message} from 'ant-design-vue';

import {updateUserPwdApi} from '#/api/system/user';

defineOptions({name: 'ProfilePasswordSetting'});

const formRef = ref();
const saving = ref(false);

const form = reactive({
  confirmPassword: '',
  newPassword: '',
  oldPassword: '',
});

const rules: Record<string, any> = {
  oldPassword: [
    {required: true, message: '旧密码不能为空', trigger: 'blur'},
  ],
  newPassword: [
    {required: true, message: '新密码不能为空', trigger: 'blur'},
    {
      min: 6,
      max: 20,
      message: '新密码长度必须介于 6 和 20 之间',
      trigger: 'blur',
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: '密码不能包含非法字符：< > " \' \\ |',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {required: true, message: '确认密码不能为空', trigger: 'blur'},
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    await updateUserPwdApi(form.oldPassword, form.newPassword);
    message.success('修改成功');
    form.oldPassword = '';
    form.newPassword = '';
    form.confirmPassword = '';
    formRef.value?.clearValidate?.();
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Form
    ref="formRef"
    :model="form"
    :rules="rules"
    class="profile-form"
    layout="vertical"
  >
    <Form.Item label="旧密码" name="oldPassword">
      <Input.Password
        v-model:value="form.oldPassword"
        placeholder="请输入旧密码"
      />
    </Form.Item>
    <Form.Item label="新密码" name="newPassword">
      <Input.Password
        v-model:value="form.newPassword"
        placeholder="请输入新密码（6-20 位）"
      />
    </Form.Item>
    <Form.Item label="确认密码" name="confirmPassword">
      <Input.Password
        v-model:value="form.confirmPassword"
        placeholder="请再次输入新密码"
      />
    </Form.Item>
    <Form.Item>
      <Button :loading="saving" type="primary" @click="handleSubmit">
        保存
      </Button>
    </Form.Item>
  </Form>
</template>

<style scoped>
.profile-form {
  max-width: 460px;
}
</style>
