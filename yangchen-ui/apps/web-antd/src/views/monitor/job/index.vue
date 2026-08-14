<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {IconifyIcon} from '@vben/icons';
import {
  Button,
  Card,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Switch,
  Table
} from 'ant-design-vue';

import {
  addJobApi,
  changeJobStatusApi,
  deleteJobApi,
  getJobApi,
  listJobApi,
  runJobApi,
  type SysJob,
  updateJobApi
} from '#/api/monitor/job';

defineOptions({name: 'MonitorJob'});

const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const isEdit = ref(false);
const jobs = ref<SysJob[]>([]);
const total = ref(0);
const formRef = ref();
const query = reactive({
  jobGroup: undefined as string | undefined,
  jobName: '',
  pageNum: 1,
  pageSize: 10,
  status: undefined as string | undefined
});
const form = reactive<SysJob>({
  concurrent: '1',
  cronExpression: '',
  invokeTarget: '',
  jobGroup: 'DEFAULT',
  jobName: '',
  misfirePolicy: '3',
  remark: '',
  status: '0'
});
const rules = {
  cronExpression: [{required: true, message: '请输入 Cron 表达式'}],
  invokeTarget: [{required: true, message: '请输入调用目标'}],
  jobName: [{required: true, message: '请输入任务名称'}]
};
const statusOptions = [{label: '正常', value: '0'}, {label: '暂停', value: '1'}];
const columns = [
  {dataIndex: 'jobName', title: '任务名称', width: 150}, {
    dataIndex: 'jobGroup',
    title: '任务组名',
    width: 120
  },
  {
    dataIndex: 'invokeTarget',
    ellipsis: true,
    title: '调用目标字符串',
    width: 260
  }, {dataIndex: 'cronExpression', title: 'Cron 表达式', width: 180},
  {key: 'status', title: '状态', width: 100}, {
    dataIndex: 'createTime',
    title: '创建时间',
    width: 180
  }, {key: 'operation', title: '操作', width: 180},
];

function resetForm() {
  Object.assign(form, {
    concurrent: '1',
    cronExpression: '',
    invokeTarget: '',
    jobGroup: 'DEFAULT',
    jobId: undefined,
    jobName: '',
    misfirePolicy: '3',
    remark: '',
    status: '0'
  });
  formRef.value?.clearValidate?.();
}

async function loadData() {
  loading.value = true;
  try {
    const result = await listJobApi(query);
    jobs.value = result.rows ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  loadData();
}

function reset() {
  query.jobGroup = undefined;
  query.jobName = '';
  query.status = undefined;
  search();
}

function changePage(page: { current?: number; pageSize?: number }) {
  query.pageNum = page.current ?? 1;
  query.pageSize = page.pageSize ?? query.pageSize;
  loadData();
}

function openAdd() {
  isEdit.value = false;
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: SysJob) {
  isEdit.value = true;
  resetForm();
  Object.assign(form, await getJobApi(row.jobId ?? ''));
  modalOpen.value = true;
}

async function save() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateJobApi(form);
      message.success('修改成功');
    } else {
      await addJobApi(form);
      message.success('新增成功');
    }
    modalOpen.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

function remove(row: SysJob) {
  Modal.confirm({
    content: `确定删除任务“${row.jobName}”吗？`,
    okText: '删除',
    okType: 'danger',
    title: '系统提示',
    async onOk() {
      await deleteJobApi([row.jobId ?? '']);
      message.success('删除成功');
      await loadData();
    }
  });
}

async function run(row: SysJob) {
  await runJobApi(row.jobId ?? '', row.jobGroup);
  message.success('任务已触发执行');
}

async function switchStatus(row: SysJob, checked: boolean | number | string) {
  const enabled = checked === true || checked === 1 || checked === '1';
  await changeJobStatusApi(row.jobId ?? '', enabled ? '0' : '1');
  message.success('状态已更新');
  row.status = enabled ? '0' : '1';
}

onMounted(loadData);
</script>

<template>
  <Page auto-content-height>
    <Card :bordered="false">
      <div class="search-panel">
        <Form layout="inline" @submit.prevent="search">
          <Form.Item label="任务名称"><Input v-model:value="query.jobName" allow-clear
                                             placeholder="请输入任务名称" @press-enter="search"/>
          </Form.Item>
          <Form.Item label="任务组名"><Input v-model:value="query.jobGroup" allow-clear
                                             placeholder="请输入任务组名"/></Form.Item>
          <Form.Item label="状态"><Select v-model:value="query.status" :options="statusOptions"
                                          allow-clear placeholder="任务状态" style="width: 160px"/>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" @click="search">
                <IconifyIcon icon="lucide:search"/>
                搜索
              </Button>
              <Button @click="reset">重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </div>
      <div class="toolbar">
        <Button type="primary" @click="openAdd">
          <IconifyIcon icon="lucide:plus"/>
          新增任务
        </Button>
        <Button size="small" @click="loadData">
          <IconifyIcon icon="lucide:refresh-cw"/>
          刷新
        </Button>
      </div>
      <Table :columns="columns" :data-source="jobs" :loading="loading"
             :pagination="{current: query.pageNum, pageSize: query.pageSize, showSizeChanger: true, total}"
             :scroll="{x: 1100}" row-key="jobId" @change="changePage">
        <template #bodyCell="{column, record}">
          <template v-if="column.key === 'status'">
            <Switch :checked="record.status === '0'" checked-children="正常"
                    un-checked-children="暂停" @change="(value) => switchStatus(record, value)"/>
          </template>
          <template v-else-if="column.key === 'operation'">
            <Space :size="2">
              <Button size="small" type="link" @click="openEdit(record)">修改</Button>
              <Button size="small" type="link" @click="run(record)">执行一次</Button>
              <Button danger size="small" type="link" @click="remove(record)">删除</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
    <Modal v-model:open="modalOpen" :confirm-loading="saving"
           :title="isEdit ? '修改任务' : '新增任务'" width="680px" @ok="save">
      <Form ref="formRef" :label-col="{span: 5}" :model="form" :rules="rules"
            :wrapper-col="{span: 17}">
        <Form.Item label="任务名称" name="jobName"><Input v-model:value="form.jobName"/></Form.Item>
        <Form.Item label="任务分组"><Input v-model:value="form.jobGroup"/></Form.Item>
        <Form.Item label="调用目标" name="invokeTarget"><Input v-model:value="form.invokeTarget"
                                                               placeholder="例如 ryTask.ryParams('ry')"/>
        </Form.Item>
        <Form.Item label="Cron 表达式" name="cronExpression"><Input
          v-model:value="form.cronExpression" placeholder="例如 0/10 * * * * ?"/></Form.Item>
        <Form.Item label="错失策略"><Select v-model:value="form.misfirePolicy"
                                            :options="[{label: '默认策略', value: '3'}, {label: '立即执行', value: '1'}, {label: '执行一次', value: '2'}]"/>
        </Form.Item>
        <Form.Item label="并发执行"><Select v-model:value="form.concurrent"
                                            :options="[{label: '允许', value: '0'}, {label: '禁止', value: '1'}]"/>
        </Form.Item>
        <Form.Item label="状态"><Select v-model:value="form.status" :options="statusOptions"/>
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea v-model:value="form.remark"/>
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>.search-panel {
  margin-bottom: 16px;
  padding: 16px 16px 2px;
  background: #f6f8fa;
  border: 1px solid #eceff3;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}</style>
