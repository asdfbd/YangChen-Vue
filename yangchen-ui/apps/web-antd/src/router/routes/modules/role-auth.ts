import type {RouteRecordRaw} from 'vue-router';

/**
 * 分配用户（隐藏路由，不显示在菜单中）。
 * 由角色管理页「更多 → 分配用户」进入，对应 Element UI 的 /system/role-auth/user/:roleId。
 */
const routes: RouteRecordRaw[] = [
  {
    meta: {
      hideInMenu: true,
      title: '分配用户',
    },
    name: 'RoleAuth',
    path: '/system/role-auth',
    children: [
      {
        name: 'RoleAuthUser',
        path: 'user/:roleId(\\d+)',
        component: () => import('#/views/system/role/auth-user.vue'),
        meta: {
          // 高亮左侧「角色管理」菜单
          activePath: '/system/role',
          hideInMenu: true,
          title: '分配用户',
        },
      },
    ],
  },
];

export default routes;
