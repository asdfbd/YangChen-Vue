<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {Page} from '@vben/common-ui';
import {Button, Card, Col, Descriptions, Row, Table, Tag} from 'ant-design-vue';
import {getServerApi, type ServerInfo} from '#/api/monitor/server';

defineOptions({name: 'MonitorServer'});
const loading = ref(false);
const server = ref<ServerInfo>({});

async function loadData() {
  loading.value = true;
  try {
    server.value = await getServerApi();
  } finally {
    loading.value = false;
  }
}

const diskColumns = [{dataIndex: 'dirName', title: '盘符路径'}, {
  dataIndex: 'sysTypeName',
  title: '文件系统'
}, {dataIndex: 'typeName', title: '盘符类型'}, {
  dataIndex: 'total',
  title: '总大小'
}, {dataIndex: 'free', title: '可用大小'}, {dataIndex: 'used', title: '已用大小'}, {
  key: 'usage',
  title: '已用百分比'
}];
onMounted(loadData);
</script>
<template>
  <Page auto-content-height>
    <div class="page-head"><span>实时展示当前服务运行状态</span>
      <Button size="small" @click="loadData">刷新</Button>
    </div>
    <Row :gutter="16">
      <Col :lg="12" :xs="24">
        <Card :loading="loading" title="CPU" class="card">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="核心数">{{ server.cpu?.cpuNum ?? '-' }}</Descriptions.Item>
            <Descriptions.Item label="系统使用率">{{ server.cpu?.sys ?? '-' }}%</Descriptions.Item>
            <Descriptions.Item label="用户使用率">{{ server.cpu?.used ?? '-' }}%</Descriptions.Item>
            <Descriptions.Item label="当前空闲率">{{ server.cpu?.free ?? '-' }}%</Descriptions.Item>
          </Descriptions>
        </Card>
      </Col>
      <Col :lg="12" :xs="24">
        <Card :loading="loading" title="内存" class="card">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="总内存">{{ server.mem?.total ?? '-' }}G</Descriptions.Item>
            <Descriptions.Item label="已用内存">{{ server.mem?.used ?? '-' }}G</Descriptions.Item>
            <Descriptions.Item label="剩余内存">{{ server.mem?.free ?? '-' }}G</Descriptions.Item>
            <Descriptions.Item label="使用率">
              <Tag :color="Number(server.mem?.usage) > 80 ? 'error' : 'success'">
                {{ server.mem?.usage ?? '-' }}%
              </Tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </Col>
      <Col :span="24">
        <Card :loading="loading" title="服务器信息" class="card">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="服务器名称">{{
                server.sys?.computerName ?? '-'
              }}
            </Descriptions.Item>
            <Descriptions.Item label="操作系统">{{ server.sys?.osName ?? '-' }}</Descriptions.Item>
            <Descriptions.Item label="服务器 IP">{{
                server.sys?.computerIp ?? '-'
              }}
            </Descriptions.Item>
            <Descriptions.Item label="系统架构">{{ server.sys?.osArch ?? '-' }}</Descriptions.Item>
          </Descriptions>
        </Card>
      </Col>
      <Col :span="24">
        <Card :loading="loading" title="Java 虚拟机" class="card">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="Java 名称">{{ server.jvm?.name ?? '-' }}</Descriptions.Item>
            <Descriptions.Item label="Java 版本">{{
                server.jvm?.version ?? '-'
              }}
            </Descriptions.Item>
            <Descriptions.Item label="启动时间">{{
                server.jvm?.startTime ?? '-'
              }}
            </Descriptions.Item>
            <Descriptions.Item label="运行时长">{{ server.jvm?.runTime ?? '-' }}</Descriptions.Item>
            <Descriptions.Item :span="2" label="安装路径">{{
                server.jvm?.home ?? '-'
              }}
            </Descriptions.Item>
            <Descriptions.Item :span="2" label="项目路径">{{
                server.sys?.userDir ?? '-'
              }}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </Col>
      <Col :span="24">
        <Card :loading="loading" title="磁盘状态" class="card">
          <Table :columns="diskColumns" :data-source="server.sysFiles" :pagination="false"
                 row-key="dirName">
            <template #bodyCell="{column, record}">
              <template v-if="column.key === 'usage'">
                <Tag :color="Number(record.usage) > 80 ? 'error' : 'success'">{{
                    record.usage
                  }}%
                </Tag>
              </template>
            </template>
          </Table>
        </Card>
      </Col>
    </Row>
  </Page>
</template>
<style scoped>.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #667085;
}

.card {
  margin-bottom: 16px;
}</style>
