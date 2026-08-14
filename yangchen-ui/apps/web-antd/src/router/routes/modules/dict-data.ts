import type {RouteRecordRaw} from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {hideInMenu: true, title: '字典数据'},
    name: 'DictData',
    path: '/system/dict-data',
    children: [
      {
        component: () => import('#/views/system/dict/data.vue'),
        meta: {activePath: '/system/dict', hideInMenu: true, title: '字典数据'},
        name: 'DictDataIndex',
        path: 'index/:dictId(\\d+)',
      },
    ],
  },
];

export default routes;
