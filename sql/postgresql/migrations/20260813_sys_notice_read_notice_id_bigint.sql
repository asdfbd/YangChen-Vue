-- 修复：sys_notice_read.notice_id 升级为 bigint
--
-- 背景：SysNotice.notice_id 采用 MyBatis-Plus 雪花 ID（bigint，如 2087713924078448641），
-- 但历史库中 sys_notice_read.notice_id 为 integer。标记已读（/system/notice/markRead）
-- 插入已读记录时会抛出 PostgreSQL "ERROR: integer out of range"。
--
-- 主库 DDL（ry_20260417.postgresql.sql）中该列已是 bigint，本迁移仅用于修复已存在的旧库。

begin;

alter table sys_notice_read
    alter column notice_id type bigint;

commit;
