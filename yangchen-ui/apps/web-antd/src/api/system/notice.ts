import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

/** RuoYi SysNotice（通知公告） */
export interface SysNotice {
  createBy?: string;
  createTime?: string;
  isRead?: boolean;
  noticeContent?: string;
  noticeId?: number;
  noticeTitle?: string;
  noticeType?: string;
  remark?: string;
  status?: string;

  [key: string]: any;
}

/** 已读用户（Map 行） */
export interface NoticeReadUser {
  deptName?: string;
  nickName?: string;
  phonenumber?: string;
  readTime?: string;
  userId?: number;
  userName?: string;

  [key: string]: any;
}

/** 通知公告列表 */
export async function listNoticeApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysNotice[]; total: number }>(
    '/system/notice/list',
    {params},
  );
}

/** 通知公告详情 */
export async function getNoticeApi(noticeId: number | string) {
  return requestClient.get<SysNotice>(`/system/notice/${noticeId}`);
}

/** 新增通知公告 */
export async function addNoticeApi(data: SysNotice) {
  return requestClient.post('/system/notice', data);
}

/** 修改通知公告 */
export async function updateNoticeApi(data: SysNotice) {
  return requestClient.put('/system/notice', data);
}

/** 删除通知公告（支持逗号分隔批量） */
export async function deleteNoticeApi(noticeIds: (number | string)[]) {
  return requestClient.delete(`/system/notice/${noticeIds.join(',')}`);
}

/** 已读用户列表 */
export async function readUsersApi(params: Recordable<any>) {
  return requestClient.get<{ rows: NoticeReadUser[]; total: number }>(
    '/system/notice/readUsers/list',
    {params},
  );
}

/**
 * 首页顶部公告列表（带当前用户已读标记，最多 5 条）。
 * 响应为 AjaxResult 信封（data + unreadCount 平级），需用 body 完整解包。
 */
export async function listNoticeTopApi() {
  return requestClient.get<{ data: SysNotice[]; unreadCount: number }>(
    '/system/notice/listTop',
    {responseReturn: 'body'},
  );
}

/** 标记公告已读 */
export async function markNoticeReadApi(noticeId: number | string) {
  return requestClient.post('/system/notice/markRead', undefined, {
    params: {noticeId},
  });
}

/** 批量标记已读（ids 逗号分隔） */
export async function markNoticeReadAllApi(ids: string) {
  return requestClient.post('/system/notice/markReadAll', undefined, {
    params: {ids},
  });
}
