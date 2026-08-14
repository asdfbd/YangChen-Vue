/**
 * 代码生成 - 共享选项常量。
 *
 * 值与后端 yangchen-generator 模块的 GenConstants / VelocityUtils 保持一致：
 * - 查询方式 queryType：EQ/NE/GT/LT/LIKE/BETWEEN
 * - 显示类型 htmlType：input/textarea/select/radio/checkbox/datetime/imageUpload/upload/editor
 * - 前端类型 tplWebType：当前统一为 antd-vue-typescript。
 *   注意：后端 VelocityUtils 目前仅识别 element-plus / element-plus-typescript，
 *   传入其他值会回落到 vm/vue（Vue2）模板 —— 若要让 antd-vue-typescript 生效，
 *   需在后端新增对应模板目录或把该值映射到 vm/vue/v3ts。
 * - 生成模板 tplCategory：crud 单表 / tree 树表 / sub 主子表
 */

/** 生成模板 */
export const tplCategoryOptions = [
  {label: '单表（增删改查）', value: 'crud'},
  {label: '树表（增删改查）', value: 'tree'},
  {label: '主子表（增删改查）', value: 'sub'},
];

/** 前端类型（当前统一生成 antd-vue-typescript 模板） */
export const tplWebTypeOptions = [
  {label: 'antd-vue-typescript', value: 'antd-vue-typescript'},
];

/** 生成代码方式 */
export const genTypeOptions = [
  {label: 'zip压缩包', value: '0'},
  {label: '自定义路径', value: '1'},
];

/** 查询方式 */
export const queryTypeOptions = [
  {label: '=', value: 'EQ'},
  {label: '!=', value: 'NE'},
  {label: '>', value: 'GT'},
  {label: '<', value: 'LT'},
  {label: 'LIKE', value: 'LIKE'},
  {label: 'BETWEEN', value: 'BETWEEN'},
];

/** 显示类型 */
export const htmlTypeOptions = [
  {label: '文本框', value: 'input'},
  {label: '文本域', value: 'textarea'},
  {label: '下拉框', value: 'select'},
  {label: '单选框', value: 'radio'},
  {label: '复选框', value: 'checkbox'},
  {label: '日期控件', value: 'datetime'},
  {label: '图片上传', value: 'imageUpload'},
  {label: '文件上传', value: 'upload'},
  {label: '富文本控件', value: 'editor'},
];

/** Java 类型 */
export const javaTypeOptions = [
  {label: 'Long', value: 'Long'},
  {label: 'String', value: 'String'},
  {label: 'Integer', value: 'Integer'},
  {label: 'Double', value: 'Double'},
  {label: 'BigDecimal', value: 'BigDecimal'},
  {label: 'Date', value: 'Date'},
];

/** 是否选项（1 是 / 0 否），用于字段信息表的开关列 */
export const yesNoOptions = [
  {label: '是', value: '1'},
  {label: '否', value: '0'},
];
