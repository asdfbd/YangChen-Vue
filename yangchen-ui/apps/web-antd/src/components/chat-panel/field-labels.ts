/**
 * 业务查询常用字段的展示名称。
 *
 * 新查询会要求模型使用中文别名；该映射用于兜底旧历史和未加别名的常见系统字段，
 * 保证数据表不会直接向业务人员展示数据库字段名。
 */
const FIELD_LABELS: Record<string, string> = {
  address: '地址',
  amount: '金额',
  ancestors: '祖级列表',
  avatar: '头像',
  begin_time: '开始时间',
  business_type: '业务类型',
  category: '分类',
  config_key: '参数键名',
  config_value: '参数键值',
  count: '数量',
  create_time: '创建时间',
  creator: '创建人',
  dept_name: '所属部门',
  dict_label: '字典标签',
  dict_value: '字典值',
  email: '邮箱',
  end_time: '结束时间',
  login_date: '最后登录时间',
  login_ip: '最后登录 IP',
  login_name: '登录账号',
  menu_name: '菜单名称',
  menu_type: '菜单类型',
  method: '请求方式',
  nick_name: '昵称',
  notice_content: '公告内容',
  notice_title: '公告标题',
  notice_type: '公告类型',
  oper_ip: '操作 IP',
  oper_location: '操作地点',
  oper_name: '操作人',
  oper_param: '请求参数',
  oper_time: '操作时间',
  oper_url: '请求地址',
  phonenumber: '手机号',
  post_code: '岗位编码',
  post_name: '岗位名称',
  remark: '备注',
  role_key: '权限字符',
  role_name: '角色名称',
  sex: '性别',
  sort: '排序',
  status: '状态',
  total: '数量',
  total_count: '数量',
  update_time: '更新时间',
  updater: '更新人',
  user_name: '登录账号',
};

const FIELD_WORDS: Record<string, string> = {
  code: '编码',
  content: '内容',
  date: '日期',
  dept: '部门',
  description: '说明',
  name: '名称',
  number: '编号',
  role: '角色',
  time: '时间',
  type: '类型',
  user: '用户',
  value: '值',
};

export function fieldLabel(value: string): string {
  const raw = value.trim();
  if (!raw || /[\u3400-\u9fff]/.test(raw)) return raw || '字段';

  const normalized = raw.toLowerCase().replace(/[\s-]+/g, '_');
  const exact = FIELD_LABELS[normalized];
  if (exact) return exact;

  const translated = normalized
    .split('_')
    .map((word) => FIELD_WORDS[word])
    .filter((word): word is string => !!word)
    .join('');
  return translated || '业务字段';
}
