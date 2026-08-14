import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';
import {downloadBlob} from '#/utils/download';

/** 代码生成字段表 gen_table_column */
export interface GenTableColumn {
  columnComment?: string;
  columnId?: number;
  columnName?: string;
  columnType?: string;
  createTime?: string;
  dictType?: string;
  htmlType?: string;
  isEdit?: string;
  isIncrement?: string;
  isInsert?: string;
  isList?: string;
  isPk?: string;
  isQuery?: string;
  isRequired?: string;
  javaField?: string;
  javaType?: string;
  queryType?: string;
  sort?: number;
  tableId?: number;

  [key: string]: any;
}

/** 业务表 gen_table */
export interface GenTable {
  businessName?: string;
  className?: string;
  columns?: GenTableColumn[];
  createBy?: string;
  createTime?: string;
  functionAuthor?: string;
  functionName?: string;
  genPath?: string;
  genType?: string;
  isView?: boolean;
  moduleName?: string;
  options?: string;
  packageName?: string;
  params?: Recordable<any>;
  parentMenuId?: number;
  parentMenuName?: string;
  pkColumn?: GenTableColumn;
  remark?: string;
  subTable?: GenTable;
  subTableFkName?: string;
  subTableName?: string;
  tableComment?: string;
  tableId?: number;
  tableName?: string;
  tplCategory?: string;
  tplWebType?: string;
  treeCode?: string;
  treeName?: string;
  treeParentCode?: string;
  updateTime?: string;

  [key: string]: any;
}

/** 查询代码生成列表 */
export async function getGenTableListApi(params: Recordable<any>) {
  return requestClient.get<{rows: GenTable[]; total: number}>(
    '/tool/gen/list',
    {params},
  );
}

/**
 * 获取代码生成信息（详情）。
 * 后端返回 AjaxResult 信封，data 字段即 { info: 业务表, rows: 字段列表, tables: 所有已导入表 }，
 * 默认 data 解包后直接得到该结构，无需 responseReturn:'body'。
 */
export async function getGenTableInfoApi(tableId: number | string) {
  return requestClient.get<{info: GenTable; rows: GenTableColumn[]; tables: GenTable[]}>(
    `/tool/gen/${tableId}`,
  );
}

/** 查询数据库表列表（导入弹窗用，可分页） */
export async function getDbTableListApi(params: Recordable<any>) {
  return requestClient.get<{rows: GenTable[]; total: number}>(
    '/tool/gen/db/list',
    {params},
  );
}

/** 导入表结构（tables 为逗号分隔的表名） */
export async function importTableApi(tables: string, tplWebType: string) {
  return requestClient.post('/tool/gen/importTable', undefined, {
    params: {tables, tplWebType},
  });
}

/** 创建表结构（sql 为建表语句，创建成功后自动导入） */
export async function createTableApi(sql: string, tplWebType: string) {
  return requestClient.post('/tool/gen/createTable', undefined, {
    params: {sql, tplWebType},
  });
}

/** 修改保存代码生成业务 */
export async function updateGenTableApi(data: GenTable) {
  return requestClient.put('/tool/gen', data);
}

/** 删除代码生成（支持批量） */
export async function deleteGenTableApi(tableIds: (number | string)[]) {
  return requestClient.delete(`/tool/gen/${tableIds.join(',')}`);
}

/** 预览代码（返回 { 文件路径: 内容 } 映射） */
export async function previewCodeApi(tableId: number | string) {
  return requestClient.get<Record<string, string>>(`/tool/gen/preview/${tableId}`);
}

/** 同步数据库 */
export async function synchDbApi(tableName: string) {
  return requestClient.get(`/tool/gen/synchDb/${tableName}`);
}

/** 生成代码（写入自定义路径） */
export async function genCodeToPathApi(tableName: string) {
  return requestClient.get(`/tool/gen/genCode/${tableName}`);
}

/** 下载单个表生成代码（zip） */
export async function downloadCodeApi(tableName: string) {
  await downloadBlob({
    filename: `${tableName}.zip`,
    method: 'GET',
    path: `/tool/gen/download/${tableName}`,
  });
}

/** 批量下载生成代码（zip） */
export async function batchGenCodeApi(tableNames: string[]) {
  await downloadBlob({
    filename: 'yangchen.zip',
    method: 'GET',
    path: `/tool/gen/batchGenCode?tables=${encodeURIComponent(tableNames.join(','))}`,
  });
}
