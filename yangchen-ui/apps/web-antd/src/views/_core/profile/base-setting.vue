<script lang="ts" setup>
import {reactive, ref, watch} from 'vue';

import {Button, Form, Input, message, Radio} from 'ant-design-vue';

import {type SysUser, updateUserProfileApi} from '#/api/system/user';

defineOptions({name: 'ProfileBaseSetting'});

const props = defineProps<{
  user: SysUser;
}>();

const emit = defineEmits<{
  saved: [values: SysUser];
}>();

const formRef = ref();
const saving = ref(false);

const form = reactive({
  email: '',
  nickName: '',
  phonenumber: '',
  sex: '0',
});

watch(
  () => props.user,
  (user) => {
    if (!user || user.userId == null) return;
    form.nickName = user.nickName ?? '';
    form.phonenumber = user.phonenumber ?? '';
    form.email = user.email ?? '';
    form.sex = user.sex ?? '0';
  },
  {immediate: true, deep: true},
);

const rules: Record<string, any> = {
  nickName: [{required: true, message: '用户昵称不能为空', trigger: 'blur'}],
  phonenumber: [
    {required: true, message: '手机号码不能为空', trigger: 'blur'},
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur',
    },
  ],
  email: [
    {required: true, message: '邮箱地址不能为空', trigger: 'blur'},
    {
      type: 'email',
      message: '请输入正确的邮箱地址',
      trigger: ['blur', 'change'],
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
    await updateUserProfileApi({...form});
    message.success('修改成功');
    emit('saved', {...form} as SysUser);
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
    <Form.Item label="用户昵称" name="nickName">
      <Input v-model:value="form.nickName" :maxlength="30" placeholder="请输入昵称"/>
    </Form.Item>
    <Form.Item label="手机号码" name="phonenumber">
      <Input
        v-model:value="form.phonenumber"
        :maxlength="11"
        placeholder="请输入手机号码"
      />
    </Form.Item>
    <Form.Item label="邮箱" name="email">
      <Input v-model:value="form.email" :maxlength="50" placeholder="请输入邮箱地址"/>
    </Form.Item>
    <Form.Item label="性别" name="sex">
      <Radio.Group v-model:value="form.sex">
        <Radio value="0">男</Radio>
        <Radio value="1">女</Radio>
      </Radio.Group>
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
