-- Vben 动态路由协议迁移
-- 组件路径由后端原样返回，需与 apps/web-antd/src/views 下的页面路径一致。
-- 图标由最新前端主题自行处理，旧 RuoYi 图标字段不再用于 /getRouters。

begin;

update sys_menu
   set component = '/' || ltrim(component, '/')
 where menu_type = 'C'
   and component is not null
   and btrim(component) <> ''
   and component not like '/%';

-- 图标直接使用 Iconify 标识，/getRouters 原样返回，前端不再转换旧图标名称。
update sys_menu
   set icon = case menu_id
       when 1 then 'lucide:settings'
       when 2 then 'lucide:activity'
       when 3 then 'lucide:wrench'
       when 4 then 'lucide:compass'
       when 100 then 'lucide:user'
       when 101 then 'lucide:shield'
       when 102 then 'lucide:menu'
       when 103 then 'lucide:network'
       when 104 then 'lucide:briefcase-business'
       when 105 then 'lucide:book-open'
       when 106 then 'lucide:settings-2'
       when 107 then 'lucide:bell'
       when 108 then 'lucide:scroll-text'
       when 109 then 'lucide:users'
       when 110 then 'lucide:clock-3'
       when 111 then 'lucide:database-zap'
       when 112 then 'lucide:server'
       when 113 then 'lucide:database'
       when 114 then 'lucide:list-tree'
       when 115 then 'lucide:form-input'
       when 116 then 'lucide:code-2'
       when 117 then 'lucide:book-open-text'
       when 500 then 'lucide:clipboard-list'
       when 501 then 'lucide:log-in'
   end
 where menu_id in (1, 2, 3, 4, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 500, 501);

commit;
