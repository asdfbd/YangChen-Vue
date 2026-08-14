<script lang="ts" setup>
/**
 * 修改代码生成配置抽屉（三 Tab）：
 * - 基本信息：表描述 / 实体类 / 作者 / 上级菜单 / 备注
 * - 字段信息：字段级配置（是否插入/编辑/列表/查询、查询方式、显示类型、字典等）
 * - 生成信息：模板类型 / 前端类型 / 包路径 / 生成方式（树表与主子表额外字段）
 */
import type {TableColumnType} from 'ant-design-vue';

import {computed, reactive, ref, watch} from 'vue';

import {message} from 'ant-design-vue';
import {
  Button,
  Divider,
  Drawer,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Space,
  Spin,
  Switch,
  Tabs,
  TreeSelect,
} from 'ant-design-vue';

import ResizableTable from '#/components/resizable-table/index.vue';

import {
  getGenTableInfoApi,
  updateGenTableApi,
  type GenTable,
  type GenTableColumn,
} from '#/api/tool/gen';
import {getMenuTreeSelectApi} from '#/api/system/menu';
import type {TreeSelectNode} from '#/api/system/role';

import {
  genTypeOptions,
  htmlTypeOptions,
  javaTypeOptions,
  queryTypeOptions,
  tplCategoryOptions,
  tplWebTypeOptions,
} from '../options';

defineOptions({name: 'GenEditDrawer'});

const props = defineProps<{
  open: boolean;
  tableId?: number | string;
}>();

const emit = defineEmits<{
  'update:open': [value: boolean];
  success: [];
}>();

/**
 * 与后端 GenTableColumn.isSuperColumn / isUsableColumn 一致：
 * 基类字段（createBy 等）与树表基类字段不允许编辑，白名单字段（parentId/orderNum/remark）除外。
 */
const SUPER_COLUMNS = [
  'ancestors',
  'createBy',
  'createTime',
  'parentName',
  'remark',
  'updateBy',
  'updateTime',
];
const USABLE_COLUMNS = ['orderNum', 'parentId', 'remark'];

function isSuperColumn(javaField?: string) {
  if (!javaField) return false;
  return (
      SUPER_COLUMNS.includes(javaField) && !USABLE_COLUMNS.includes(javaField)
  );
}

/** 开关值转 1/0 字符串（antd Switch 的 change 参数类型为 CheckedType） */
function yesNoSetter(record: GenTableColumn, key: string) {
  return (checked: boolean | string | number) => {
    record[key] = checked ? '1' : '0';
  };
}
// ===== 状态 =====
const loading = ref(false);
const saving = ref(false);
const activeTab = ref('basic');

/** 基本信息表单 */
const form = reactive<Record<string, any>>({
  className: undefined,
  functionAuthor: undefined,
  parentMenuId: undefined,
  parentMenuName: undefined,
  remark: undefined,
  tableComment: undefined,
  tableId: undefined,
  tableName: undefined,
});

/** 生成信息表单 */
const genForm = reactive<Record<string, any>>({
  businessName: undefined,
  functionName: undefined,
  genPath: '/',
  genType: '0',
  genView: true,
  moduleName: undefined,
  packageName: undefined,
  subTableFkName: undefined,
  subTableName: undefined,
  tplCategory: 'crud',
  tplWebType: 'antd-vue-typescript',
});

/** 树表字段配置 */
const treeForm = reactive<Record<string, any>>({
  treeCode: undefined,
  treeName: undefined,
  treeParentCode: undefined,
});

const columns = ref<GenTableColumn[]>([]);
const allTables = ref<GenTable[]>([]);
const menuOptions = ref<TreeSelectNode[]>([]);

// ===== 派生数据 =====
/** 上级菜单下拉树 */
const menuTreeSelectData = computed(() => toTreeSelectData(menuOptions.value));

function toTreeSelectData(nodes: TreeSelectNode[]): any[] {
  return (nodes ?? []).map((node) => ({
    children: node.children?.length ? toTreeSelectData(node.children) : undefined,
    title: node.label,
    value: node.id,
  }));
}

/** 树字段选项：取当前表字段的 javaField */
const treeFieldOptions = computed(() =>
    columns.value
        .filter((column) => column.javaField)
        .map((column) => ({
          label: column.javaField as string,
          value: column.javaField as string,
        })),
);

/** 主子表模式：可选的子表（排除当前表自身） */
const subTableOptions = computed(() =>
    allTables.value
        .filter((table) => table.tableName !== form.tableName)
        .map((table) => ({
          label: `${table.tableName}(${table.tableComment ?? ''})`,
          value: table.tableName,
        })),
);

/** 子表关联外键选项：所选子表的字段 */
const subTableColumns = computed(() => {
  const table = allTables.value.find(
      (item) => item.tableName === genForm.subTableName,
  );
  return table?.columns ?? [];
});

/**
 * 统一取字段表列的 dataIndex 字符串。
 * antd 的 DataIndex 类型可能是字符串/数字/数组，本表列全部为字符串字段名。
 */
function fieldKey(dataIndex: TableColumnType['dataIndex']): string {
  return typeof dataIndex === 'string' ? dataIndex : '';
}

/** 字段表只读列（仅展示，不可编辑） */
function isReadonlyField(key: TableColumnType['dataIndex']) {
  return ['columnName', 'columnType', 'javaField'].includes(fieldKey(key));
}
// ===== 字段信息表列配置 =====
const fieldColumns = ref<TableColumnType[]>([
  {dataIndex: 'columnName', fixed: 'left' as const, title: '字段名称', width: 130},
  {dataIndex: 'columnComment', title: '字段描述', width: 150},
  {dataIndex: 'columnType', title: '物理类型', width: 110},
  {dataIndex: 'javaType', title: 'Java类型', width: 110},
  {dataIndex: 'javaField', title: 'Java属性', width: 130},
  {align: 'center' as const, dataIndex: 'isInsert', title: '插入', width: 70},
  {align: 'center' as const, dataIndex: 'isEdit', title: '编辑', width: 70},
  {align: 'center' as const, dataIndex: 'isList', title: '列表', width: 70},
  {align: 'center' as const, dataIndex: 'isQuery', title: '查询', width: 70},
  {align: 'center' as const, dataIndex: 'queryType', title: '查询方式', width: 110},
  {align: 'center' as const, dataIndex: 'isRequired', title: '必填', width: 70},
  {dataIndex: 'htmlType', title: '显示类型', width: 130},
  {dataIndex: 'dictType', title: '字典类型', width: 140},
  {align: 'center' as const, dataIndex: 'sort', title: '排序', width: 80},
]);

// ===== 加载 =====
async function loadInfo() {
  if (!props.tableId) return;
  loading.value = true;
  try {
    const {info, rows, tables} = await getGenTableInfoApi(props.tableId);
    // 接口异常/数据为空时直接提示，避免后续读取 info.xxx 产生连锁报错
    if (!info || !info.tableId) {
      message.error('加载配置失败：未获取到表信息');
      return;
    }
    Object.assign(form, {
      className: info.className,
      functionAuthor: info.functionAuthor,
      parentMenuId: info.parentMenuId,
      parentMenuName: info.parentMenuName,
      remark: info.remark,
      tableComment: info.tableComment,
      tableId: info.tableId,
      tableName: info.tableName,
    });
    Object.assign(genForm, {
      businessName: info.businessName,
      functionName: info.functionName,
      genPath: info.genPath ?? '/',
      genType: info.genType ?? '0',
      // 后端 Jackson 对 boolean isView 字段输出的属性名是 view
      genView: info.view ?? info.isView ?? true,
      moduleName: info.moduleName,
      packageName: info.packageName,
      subTableFkName: info.subTableFkName,
      subTableName: info.subTableName,
      tplCategory: info.tplCategory ?? 'crud',
      tplWebType: info.tplWebType ?? 'antd-vue-typescript',
    });
    Object.assign(treeForm, {
      treeCode: info.treeCode,
      treeName: info.treeName,
      treeParentCode: info.treeParentCode,
    });
    columns.value = rows ?? [];
    allTables.value = tables ?? [];
    if (!menuOptions.value.length) {
      menuOptions.value = await getMenuTreeSelectApi();
    }
  } catch (error: any) {
    message.error(error?.message ?? '加载配置失败');
  } finally {
    loading.value = false;
  }
}

function handleParentMenuChange(value: number) {
  const node = menuOptions.value.find(
      (item) => String(item.id) === String(value),
  );
  form.parentMenuName = node?.label ?? '';
}

// ===== 保存 =====
function validateForm(): boolean {
  if (!form.tableComment?.trim()) {
    message.warning('表描述不能为空');
    return false;
  }
  if (!form.className?.trim()) {
    message.warning('实体类名称不能为空');
    return false;
  }
  if (!form.functionAuthor?.trim()) {
    message.warning('作者不能为空');
    return false;
  }
  if (!genForm.packageName?.trim()) {
    message.warning('生成包路径不能为空');
    return false;
  }
  if (!genForm.moduleName?.trim()) {
    message.warning('生成模块名不能为空');
    return false;
  }
  if (!genForm.businessName?.trim()) {
    message.warning('生成业务名不能为空');
    return false;
  }
  if (!genForm.functionName?.trim()) {
    message.warning('生成功能名不能为空');
    return false;
  }
  if (genForm.tplCategory === 'tree') {
    if (!treeForm.treeCode) {
      message.warning('树编码字段不能为空');
      return false;
    }
    if (!treeForm.treeParentCode) {
      message.warning('树父编码字段不能为空');
      return false;
    }
    if (!treeForm.treeName) {
      message.warning('树名称字段不能为空');
      return false;
    }
  }
  if (genForm.tplCategory === 'sub') {
    if (!genForm.subTableName) {
      message.warning('关联子表的表名不能为空');
      return false;
    }
    if (!genForm.subTableFkName) {
      message.warning('子表关联的外键名不能为空');
      return false;
    }
  }
  return true;
}

async function handleSave() {
  if (!validateForm()) return;
  const params: Record<string, any> = {
    genView: Boolean(genForm.genView),
    parentMenuId: form.parentMenuId,
    parentMenuName: form.parentMenuName,
  };
  if (genForm.tplCategory === 'tree') {
    params.treeCode = treeForm.treeCode;
    params.treeParentCode = treeForm.treeParentCode;
    params.treeName = treeForm.treeName;
  }
  const payload: GenTable = {
    ...form,
    ...genForm,
    columns: columns.value,
    params,
    tableId: form.tableId ?? props.tableId,
  };
  saving.value = true;
  try {
    await updateGenTableApi(payload);
    message.success('修改成功');
    emit('success');
    close();
  } finally {
    saving.value = false;
  }
}

function close() {
  emit('update:open', false);
}

watch(
    () => props.open,
    (open) => {
      if (open) {
        activeTab.value = 'basic';
        loadInfo();
      }
    },
);
</script>

<template>
  <Drawer
      class="gen-edit-drawer"
      :open="open"
      :title="`修改配置 - ${form.tableName ?? ''}`"
      :width="'86%'"
      @close="close"
  >
    <Spin :spinning="loading">
      <Tabs v-model:activeKey="activeTab">
        <!-- 基本信息（含生成信息） -->
        <Tabs.TabPane key="basic" tab="基本信息">
          <Form :label-col="{flex: '120px'}">
            <div class="gen-section-title">基本信息</div>
            <div class="form-grid">
              <Form.Item label="表名称">
                <Input v-model:value="form.tableName" disabled/>
              </Form.Item>
              <Form.Item label="表描述">
                <Input v-model:value="form.tableComment" placeholder="请输入表描述"/>
              </Form.Item>
              <Form.Item label="实体类名称">
                <Input v-model:value="form.className" placeholder="请输入实体类名称"/>
              </Form.Item>
              <Form.Item label="作者">
                <Input v-model:value="form.functionAuthor" placeholder="请输入作者"/>
              </Form.Item>
              <Form.Item label="上级菜单">
                <TreeSelect
                    v-model:value="form.parentMenuId"
                    :tree-data="menuTreeSelectData"
                    allow-clear
                    placeholder="请选择上级菜单"
                    @change="handleParentMenuChange"
                />
              </Form.Item>
              <Form.Item class="form-grid__full" label="备注">
                <Input.TextArea
                    v-model:value="form.remark"
                    :rows="3"
                    placeholder="请输入备注"
                />
              </Form.Item>
            </div>

            <Divider class="gen-divider"/>

            <div class="gen-section-title">生成信息</div>
            <div class="form-grid">
              <Form.Item label="生成模板">
                <Select v-model:value="genForm.tplCategory" :options="tplCategoryOptions"/>
              </Form.Item>
              <Form.Item label="前端类型">
                <Select v-model:value="genForm.tplWebType" :options="tplWebTypeOptions"/>
              </Form.Item>
              <Form.Item label="生成包路径">
                <Input
                    v-model:value="genForm.packageName"
                    placeholder="如 com.yangchen.system"
                />
              </Form.Item>
              <Form.Item label="生成模块名">
                <Input v-model:value="genForm.moduleName" placeholder="如 system"/>
              </Form.Item>
              <Form.Item label="生成业务名">
                <Input v-model:value="genForm.businessName" placeholder="如 user"/>
              </Form.Item>
              <Form.Item label="生成功能名">
                <Input v-model:value="genForm.functionName" placeholder="如 用户信息"/>
              </Form.Item>
              <Form.Item label="生成代码方式">
                <Radio.Group v-model:value="genForm.genType" :options="genTypeOptions"/>
              </Form.Item>
              <Form.Item v-if="genForm.genType === '1'" label="自定义路径">
                <Input v-model:value="genForm.genPath" placeholder="不填默认项目路径"/>
              </Form.Item>
              <Form.Item label="生成详情页">
                <Switch v-model:checked="genForm.genView"/>
              </Form.Item>

              <!-- 树表 -->
              <template v-if="genForm.tplCategory === 'tree'">
                <Form.Item label="树编码字段">
                  <Select
                      v-model:value="treeForm.treeCode"
                      :options="treeFieldOptions"
                      placeholder="请选择树编码字段"
                  />
                </Form.Item>
                <Form.Item label="树父编码字段">
                  <Select
                      v-model:value="treeForm.treeParentCode"
                      :options="treeFieldOptions"
                      placeholder="请选择树父编码字段"
                  />
                </Form.Item>
                <Form.Item class="form-grid__full" label="树名称字段">
                  <Select
                      v-model:value="treeForm.treeName"
                      :options="treeFieldOptions"
                      placeholder="请选择树名称字段"
                  />
                </Form.Item>
              </template>

              <!-- 主子表 -->
              <template v-if="genForm.tplCategory === 'sub'">
                <Form.Item label="关联子表的表名">
                  <Select
                      v-model:value="genForm.subTableName"
                      :options="subTableOptions"
                      placeholder="请选择关联子表"
                  />
                </Form.Item>
                <Form.Item label="子表关联的外键名">
                  <Select
                      v-model:value="genForm.subTableFkName"
                      :options="subTableColumns.map((column) => ({
                      label: column.columnName,
                      value: column.columnName,
                    }))"
                      placeholder="请选择子表外键字段"
                  />
                </Form.Item>
              </template>
            </div>
          </Form>
        </Tabs.TabPane>

        <!-- 字段信息 -->
        <Tabs.TabPane key="columns" tab="字段信息">
          <ResizableTable
              :columns="fieldColumns"
              :data-source="columns"
              :pagination="false"
              :scroll="{x: 1600}"
              row-key="columnId"
              size="small"
          >
            <template #bodyCell="{column, record}">
              <!-- 只读列 -->
              <template v-if="isReadonlyField(column.dataIndex)">
                {{ record[fieldKey(column.dataIndex)] }}
              </template>
              <template v-else-if="column.dataIndex === 'columnComment'">
                <Input v-model:value="record.columnComment" class="cell-control" placeholder="请输入字段描述"/>
              </template>
              <template v-else-if="column.dataIndex === 'javaType'">
                <Select
                    v-model:value="record.javaType"
                    :options="javaTypeOptions"
                    class="cell-control"
                    size="small"
                />
              </template>
              <template v-else-if="column.dataIndex === 'isInsert'">
                <Switch
                    :checked="record.isInsert === '1'"
                    size="small"
                    @change="yesNoSetter(record, 'isInsert')"
                />
              </template>
              <template v-else-if="column.dataIndex === 'isEdit'">
                <Switch
                    :checked="record.isEdit === '1'"
                    :disabled="record.isPk === '1' || isSuperColumn(record.javaField)"
                    size="small"
                    @change="yesNoSetter(record, 'isEdit')"
                />
              </template>
              <template v-else-if="['isList', 'isQuery'].includes(fieldKey(column.dataIndex))">
                <Switch
                    :checked="record[fieldKey(column.dataIndex)] === '1'"
                    :disabled="isSuperColumn(record.javaField)"
                    size="small"
                    @change="yesNoSetter(record, fieldKey(column.dataIndex))"
                />
              </template>
              <template v-else-if="column.dataIndex === 'queryType'">
                <Select
                    v-model:value="record.queryType"
                    :disabled="record.isQuery !== '1'"
                    :options="queryTypeOptions"
                    class="cell-control"
                    size="small"
                />
              </template>
              <template v-else-if="column.dataIndex === 'isRequired'">
                <Switch
                    :checked="record.isRequired === '1'"
                    :disabled="record.isPk === '1'"
                    size="small"
                    @change="yesNoSetter(record, 'isRequired')"
                />
              </template>
              <template v-else-if="column.dataIndex === 'htmlType'">
                <Select
                    v-model:value="record.htmlType"
                    :options="htmlTypeOptions"
                    class="cell-control"
                    size="small"
                />
              </template>
              <template v-else-if="column.dataIndex === 'dictType'">
                <Input v-model:value="record.dictType" class="cell-control" placeholder="请输入字典类型"/>
              </template>
              <template v-else-if="column.dataIndex === 'sort'">
                <InputNumber v-model:value="record.sort" class="cell-control" :min="0" size="small"/>
              </template>
            </template>
          </ResizableTable>
        </Tabs.TabPane>
      </Tabs>
    </Spin>

    <template #footer>
      <Space>
        <Button @click="close">取消</Button>
        <Button :loading="saving" type="primary" @click="handleSave">保存</Button>
      </Space>
    </template>
  </Drawer>
</template>

<style scoped>
/* 字段信息表内的控件占满单元格宽度，避免下拉/输入框过窄看不到值 */
.cell-control {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.form-grid__full {
  grid-column: 1 / -1;
}

/* 基本信息 / 生成信息分区标题 */
.gen-section-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

.gen-divider {
  margin: 8px 0 16px;
}
</style>

<style>
/* 抽屉全高布局：antd Drawer 内容渲染在 body 级 portal，scoped 选择器无法到达，故用全局样式。
   高度链路：drawer-body(flex) > spin-nested-loading > spin-container > tabs > content-holder > tabpane，
   字段信息表（ResizableTable 内部 flex:1 + 自动 scroll.y）即可占满抽屉高度并在表内滚动。 */
.gen-edit-drawer .ant-drawer-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.gen-edit-drawer .ant-spin-nested-loading,
.gen-edit-drawer .ant-spin-container {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.gen-edit-drawer .ant-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.gen-edit-drawer .ant-tabs-content-holder {
  flex: 1;
  min-height: 0;
}

.gen-edit-drawer .ant-tabs-content {
  height: 100%;
}

.gen-edit-drawer .ant-tabs-tabpane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
}
</style>
