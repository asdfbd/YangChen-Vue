<script lang="ts" setup>
import {ref} from 'vue';

import {VCropper} from '@vben/common-ui';
import {useAppConfig} from '@vben/hooks';
import {preferences} from '@vben/preferences';
import {useUserStore} from '@vben/stores';

import {Button, message, Modal, Upload} from 'ant-design-vue';

import {uploadAvatarApi} from '#/api/system/user';

defineOptions({name: 'AvatarUpload'});

const emit = defineEmits<{
  changed: [avatar: string];
}>();

const userStore = useUserStore();
const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);

const open = ref(false);
const saving = ref(false);
const imgSrc = ref('');
const cropperRef = ref<InstanceType<typeof VCropper>>();

/** 打开裁剪弹窗：以当前头像作为裁剪原图 */
function openModal() {
  imgSrc.value = userStore.userInfo?.avatar ?? preferences.app.defaultAvatar;
  open.value = true;
}

function handleClose() {
  if (saving.value) return;
  open.value = false;
}

/** 选择图片：读入本地文件作为裁剪源，阻止默认上传 */
function beforeUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error('文件格式错误，请上传图片类型文件（如 JPG、PNG）。');
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB。');
    return false;
  }
  const reader = new FileReader();
  reader.readAsDataURL(file);
  reader.onload = () => {
    imgSrc.value = reader.result as string;
  };
  return false;
}

async function handleSave() {
  if (saving.value) return;
  saving.value = true;
  try {
    const blob = await cropperRef.value?.getCropImage(
      'image/png',
      0.92,
      'blob',
      200,
      200,
    );
    if (!blob) {
      message.error('裁剪失败，请重试');
      return;
    }
    const formData = new FormData();
    formData.append(
      'avatarfile',
      new File([blob], 'avatar.png', {type: 'image/png'}),
    );
    const res = await uploadAvatarApi(formData);
    const avatar = `${apiURL}${res.imgUrl}`;
    const info = userStore.userInfo;
    if (info) {
      userStore.setUserInfo({...info, avatar});
    }
    emit('changed', avatar);
    message.success('修改成功');
    open.value = false;
  } finally {
    saving.value = false;
  }
}

defineExpose({open: openModal});
</script>

<template>
  <Modal
    :footer="null"
    :open="open"
    :width="460"
    title="更换头像"
    @cancel="handleClose"
  >
    <div class="avatar-crop">
      <VCropper
        ref="cropperRef"
        :height="360"
        :img="imgSrc"
        :width="360"
        aspect-ratio="1:1"
      />
    </div>
    <div class="avatar-crop__actions">
      <Upload
        :before-upload="beforeUpload"
        :show-upload-list="false"
        accept="image/*"
      >
        <Button>选择图片</Button>
      </Upload>
      <Button :loading="saving" type="primary" @click="handleSave">保存</Button>
    </div>
    <p class="avatar-crop__tip">支持 JPG / PNG，大小不超过 5MB；保存后将生成 200×200 头像。</p>
  </Modal>
</template>

<style scoped>
.avatar-crop {
  display: flex;
  justify-content: center;
  padding: 12px 0 8px;
}

.avatar-crop__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 4px;
}

.avatar-crop__tip {
  margin-top: 12px;
  margin-bottom: 0;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}
</style>
