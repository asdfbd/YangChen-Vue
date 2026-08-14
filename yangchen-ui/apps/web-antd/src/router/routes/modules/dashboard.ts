import type {RouteRecordRaw} from 'vue-router';

import {$t} from '#/locales';

/**
 * 首页（登录后默认落地页）。
 * 前端静态路由，与后端菜单按名称合并，order 置为最小使其排在菜单最前。
 */
const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/dashboard/index.vue'),
    meta: {
      icon: 'lucide:layout-dashboard',
      order: -999,
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
  },
];

export default routes;