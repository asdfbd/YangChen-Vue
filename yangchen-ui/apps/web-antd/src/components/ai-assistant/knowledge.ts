/**
 * AI 智能助手 · 本地知识库（演示引擎）
 *
 * ⚠️ 当前为纯前端演示阶段：回复由关键词匹配的本地知识库生成，
 * 尚未接入后端大模型接口。后续接入时，将本模块替换为真实接口调用即可，
 * 组件内部只依赖 `resolveReply(text): Promise<string>` 这一个出口。
 */

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  time: string;
}

interface KnowledgeEntry {
  keywords: string[];
  reply: string;
}

/** 关键词规则：越靠前优先级越高 */
const KNOWLEDGE: KnowledgeEntry[] = [
  {
    keywords: ['你好', '您好', 'hello', 'hi', '嗨', '在吗', '在不在'],
    reply:
      '你好，我是**小辰**，扬辰管理系统的 AI 智能助手。\n\n' +
      '我可以帮你了解系统的各项功能：用户、角色、菜单、部门、字典、日志、定时任务、代码生成……\n\n' +
      '你可以直接问我，比如：\n' +
      '- 如何新增一个用户？\n' +
      '- 怎么给角色分配权限？\n' +
      '- 操作日志在哪里查看？',
  },
  {
    keywords: ['你是谁', '介绍你', '什么助手', '你叫什么', '功能'],
    reply:
      '我是**小辰**，扬辰管理系统的 AI 智能助手。\n\n' +
      '目前系统已支持的功能模块包括：\n' +
      '- **系统管理**：用户、角色、菜单、部门、岗位、字典、参数、通知公告\n' +
      '- **系统监控**：在线用户、定时任务、数据监控、服务监控、缓存监控\n' +
      '- **日志管理**：操作日志、登录日志\n' +
      '- **系统工具**：代码生成、接口文档（Swagger）\n\n' +
      '有任何使用上的问题，随时问我。',
  },
  {
    keywords: ['用户', 'user'],
    reply:
      '**用户管理** 位于「系统管理 → 用户管理」（/system/user）\n\n' +
      '常用操作：\n' +
      '- **新增用户**：点击右上角「新增」，填写用户名、昵称、密码，勾选角色即可\n' +
      '- **编辑**：行内「编辑」按钮，可修改昵称、手机号、邮箱、性别\n' +
      '- **重置密码**：行内「重置密码」，输入新密码后确认\n' +
      '- **分配角色**：在新增/编辑弹窗的「角色」多选框中勾选\n' +
      '- **删除用户**：勾选行后点击「删除」，或在行内操作\n' +
      '- **导出**：点击「导出」可将当前筛选结果导出为 Excel\n\n' +
      '提示：用户名一经创建不可修改；删除用户需谨慎，关联数据会一并处理。',
  },
  {
    keywords: ['角色', '权限', 'permi', '授权', '角色分配'],
    reply:
      '**角色与权限** 位于「系统管理 → 角色管理」（/system/role）\n\n' +
      '操作步骤：\n' +
      '- 点击「新增」创建角色，填写**角色名称**与**权限字符**（如 admin）\n' +
      '- 在「菜单权限」树中勾选该角色可访问的菜单与按钮权限\n' +
      '- 创建后，在「用户管理」中为用户分配该角色即可生效\n\n' +
      '提示：权限字符会生成形如 `system:user:list` 的权限码，用于接口与按钮级控制；超级管理员角色拥有 `*:*:*` 全部权限。',
  },
  {
    keywords: ['菜单', '路由', '页面', '新增页面', '新页面'],
    reply:
      '**菜单管理** 位于「系统管理 → 菜单管理」（/system/menu）\n\n' +
      '新增一个页面的大致流程：\n' +
      '1. 在 `yangchen-ui/apps/web-antd/src/views/` 下创建页面组件\n' +
      '2. 在「菜单管理」点击「新增」，填写菜单名称、**路由地址**、**组件路径**（如 system/user/index）\n' +
      '3. 选择菜单类型：目录 / 菜单 / 按钮\n' +
      '4. 勾选显示状态、菜单状态后保存\n\n' +
      '提示：若要在「代码生成」中生成整套前后端代码，可前往「系统工具 → 代码生成」操作。',
  },
  {
    keywords: ['部门', 'dept', '组织'],
    reply:
      '**部门管理** 位于「系统管理 → 部门管理」（/system/dept）\n\n' +
      '- 以**树形结构**维护组织架构，可无限层级\n' +
      '- 点击「新增」选择上级部门后创建子部门\n' +
      '- 部门负责人、联系电话可在编辑中维护\n' +
      '- 删除部门前需确保该部门下没有用户或子部门\n\n' +
      '提示：部门树会在用户管理、数据权限中作为数据范围使用。',
  },
  {
    keywords: ['岗位', 'post'],
    reply:
      '**岗位管理** 位于「系统管理 → 岗位管理」（/system/post）\n\n' +
      '- 用于维护职位字典（如董事长、经理、员工）\n' +
      '- 每个岗位有岗位编码、岗位名称与显示顺序\n' +
      '- 在用户管理中可为用户设置所属岗位',
  },
  {
    keywords: ['字典', 'dict', '数据字典'],
    reply:
      '**字典管理** 位于「系统管理 → 字典管理」（/system/dict）\n\n' +
      '- **字典类型**：定义一类枚举，如「用户性别」「通知类型」\n' +
      '- **字典数据**：维护该类型下的具体选项（标签 + 键值 + 样式）\n' +
      '- 页面上的下拉框、标签样式大多由字典驱动，修改后刷新即可生效\n\n' +
      '提示：页面中通过 `getDicts("sys_user_sex")` 这类方法拉取字典数据。',
  },
  {
    keywords: ['参数', 'config', '参数设置'],
    reply:
      '**参数设置** 位于「系统管理 → 参数设置」（/system/config）\n\n' +
      '- 维护系统全局配置项，如系统名称、上传大小限制等\n' +
      '- 支持「参数键名」与「参数键值」，可通过 `@Value` 或 `configService` 读取\n' +
      '- 修改后对全系统生效，请谨慎操作',
  },
  {
    keywords: ['公告', '通知', 'notice'],
    reply:
      '**通知公告** 位于「系统管理 → 通知公告」（/system/notice）\n\n' +
      '- 支持**通知**与**公告**两种类型\n' +
      '- 新增时填写标题与正文（支持富文本），可选择状态\n' +
      '- 顶部导航栏的铃铛会展示最新公告，未读会有角标提示',
  },
  {
    keywords: ['操作日志', 'operlog', '操作记录'],
    reply:
      '**操作日志** 位于「系统监控 → 操作日志」（/monitor/operlog）\n\n' +
      '- 记录所有带 `@Log` 注解接口的调用：操作人、模块、类型、耗时、状态、请求参数\n' +
      '- 支持按操作人员、操作模块、状态等条件筛选\n' +
      '- 可「清空」全部日志，也可勾选后批量删除\n' +
      '- 点击行内「详情」可查看完整的请求与响应信息',
  },
  {
    keywords: ['登录日志', '登录记录', 'logininfor'],
    reply:
      '**登录日志** 位于「系统监控 → 登录日志」（/monitor/logininfor）\n\n' +
      '- 记录每次登录：用户名、登录地点、IP、浏览器、操作系统、状态与提示信息\n' +
      '- 支持按状态（成功/失败）筛选，辅助排查安全问题\n' +
      '- 可「解锁」被锁定用户、可清空日志',
  },
  {
    keywords: ['在线用户', '在线', '强退', '踢出'],
    reply:
      '**在线用户** 位于「系统监控 → 在线用户」（/monitor/online）\n\n' +
      '- 展示当前登录系统的会话：用户、部门、登录 IP、登录时间\n' +
      '- 点击「强退」可强制下线指定用户（管理端专用）',
  },
  {
    keywords: ['缓存', 'redis', 'cache'],
    reply:
      '**缓存监控** 位于「系统监控 → 缓存监控」（/monitor/cache）\n\n' +
      '- **缓存列表**：查看 Redis 中各业务缓存 key 及其内容\n' +
      '- 支持按缓存名称筛选，查看/修改具体 key 的值\n' +
      '- 可单独清理某个缓存或一键**清空全部缓存**\n\n' +
      '提示：系统登录态、验证码、字典数据均存储在 Redis 中，清理需谨慎。',
  },
  {
    keywords: ['定时任务', '任务', 'job', 'quartz'],
    reply:
      '**定时任务** 位于「系统监控 → 定时任务」（/monitor/job）\n\n' +
      '- 基于 Quartz 的分布式定时任务管理\n' +
      '- 新增时填写任务名称、调用目标（如 `ryTask.ryParams`）与 **Cron 表达式**\n' +
      '- 支持「立即执行」「暂停」「恢复」操作\n' +
      '- 可查看任务执行日志，便于排查调度问题',
  },
  {
    keywords: ['服务监控', '服务器', 'server', 'cpu', '内存'],
    reply:
      '**服务监控** 位于「系统监控 → 服务监控」（/monitor/server）\n\n' +
      '- 实时展示服务器状态：CPU、内存、磁盘使用率\n' +
      '- 展示 JVM 运行信息与服务器硬件配置\n' +
      '- 有助于快速定位资源瓶颈',
  },
  {
    keywords: ['代码生成', '生成', 'gen', '自动生成'],
    reply:
      '**代码生成** 位于「系统工具 → 代码生成」（/tool/gen）\n\n' +
      '从数据库表一键生成全套前后端代码：\n' +
      '1. 「导入」选择数据库表\n' +
      '2. 「编辑」配置字段属性、查询条件、表单控件\n' +
      '3. 「生成代码」下载压缩包，包含 Controller / Service / Mapper / Vue 页面等\n' +
      '4. 按项目结构放入对应模块即可\n\n' +
      '提示：生成的代码基于 Velocity 模板，可在后端 `resources/vm` 中定制模板。',
  },
  {
    keywords: ['接口文档', 'swagger', 'api 文档', '接口'],
    reply:
      '**接口文档** 位于「系统工具 → 接口文档」\n\n' +
      '- 基于 Swagger/Knife4j 生成，可在线调试后端接口\n' +
      '- 访问路径通常为 `/swagger-ui/index.html` 或通过系统菜单进入\n\n' +
      '提示：前端所有请求统一走 `src/api/request.ts` 中的 `requestClient`，会自动携带 token。',
  },
  {
    keywords: ['个人中心', '个人资料', '修改密码', '头像', 'profile'],
    reply:
      '**个人中心** 点击右上角头像进入「个人中心」\n\n' +
      '- **基本资料**：修改昵称、手机号、邮箱、性别\n' +
      '- **修改密码**：验证旧密码后设置新密码\n' +
      '- **头像设置**：支持上传/裁剪头像',
  },
  {
    keywords: ['sql', '查询用户', 'select', '数据库', '表结构'],
    reply:
      '**数据库查询示例** 系统使用 **PostgreSQL** 数据库\n\n' +
      '查询所有用户：\n' +
      '```sql\n' +
      'SELECT user_id, user_name, nick_name, phonenumber, status\n' +
      'FROM sys_user;\n' +
      '```\n\n' +
      '查询某角色的用户：\n' +
      '```sql\n' +
      'SELECT u.user_id, u.user_name, u.nick_name\n' +
      'FROM sys_user u\n' +
      'JOIN sys_user_role ur ON u.user_id = ur.user_id\n' +
      'WHERE ur.role_id = 1;\n' +
      '```\n\n' +
      '主要业务表：`sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_dict_type`、`sys_dict_data`、`sys_notice`、`sys_oper_log`、`sys_logininfor`。',
  },
  {
    keywords: ['导出', 'excel', 'excel 导出', '下载'],
    reply:
      '**数据导出** 列表页基本都提供「导出」按钮\n\n' +
      '- 点击「导出」将按**当前筛选条件**导出 Excel\n' +
      '- 后端通过 `@Excel` 注解控制导出列与格式\n' +
      '- 导出文件在浏览器下载中心查看',
  },
  {
    keywords: ['帮助', 'help', '怎么用', '使用', '不会', '教程'],
    reply:
      '**使用小贴士**\n\n' +
      '- 页面右上角有**全局搜索**，可快速跳转菜单\n' +
      '- 表格右上角的「列设置」可自定义显示列\n' +
      '- 右上角「偏好设置」可切换主题、语言、布局等\n' +
      '- 更多细节可直接问我，例如：\n' +
      '  - 如何新增一个用户？\n' +
      '  - 怎么给角色分配权限？\n' +
      '  - 如何查看操作日志？',
  },
];

/** 兜底回复 */
const FALLBACK_REPLY =
  '抱歉，这个问题我暂时还没有相关知识。\n\n' +
  '当前是**界面演示阶段**，我的"大脑"（后端大模型接口）还未接入，上面的回复来自内置的演示知识库。\n\n' +
  '你可以先试试问我这些：\n' +
  '- 如何新增一个用户？\n' +
  '- 怎么给角色分配权限？\n' +
  '- 代码生成怎么用？\n' +
  '- 如何查询用户 SQL？';

/** 获取当前时间 HH:mm */
export function nowTime(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** 生成消息 ID */
export function genId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

/** 关键词匹配回复（演示引擎） */
export function resolveReply(input: string): Promise<string> {
  const text = input.trim().toLowerCase();
  for (const entry of KNOWLEDGE) {
    if (entry.keywords.some((kw) => text.includes(kw.toLowerCase()))) {
      return Promise.resolve(entry.reply);
    }
  }
  return Promise.resolve(FALLBACK_REPLY);
}

/** 开场白（按用户昵称个性化） */
export function buildGreeting(displayName: string): string {
  const name = displayName || '朋友';
  return (
    `你好，**${name}**！我是**小辰**，扬辰管理系统的 AI 智能助手。\n\n` +
    '我可以帮你快速了解系统功能、解答使用疑问。下面这些是你可能想知道的：'
  );
}

/** 推荐提问（快捷指令） */
export const QUICK_QUESTIONS: string[] = [
  '如何新增一个用户？',
  '怎么给角色分配权限？',
  '代码生成怎么用？',
  '操作日志在哪里查看？',
];
