const BasicLayout = () => import('./basic.vue');
const AuthPageLayout = () => import('./auth.vue');

const IFrameView = () => import('@vben/layouts').then((m) => m.IFrameView);

// RuoYi ParentView：仅透传渲染子路由的中间容器（如 日志管理）
const ParentViewLayout = () => import('./blank.vue');

export {AuthPageLayout, BasicLayout, IFrameView, ParentViewLayout};
