package com.yangchen.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.yangchen.system.entity.SysNotice;
import com.yangchen.system.entity.SysNoticeRead;
import com.yangchen.system.mapper.SysNoticeReadMapper;
import com.yangchen.system.service.SysNoticeReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 公告已读记录 服务层实现
 *
 * @author yangchen
 */
@Service
public class SysNoticeReadServiceImpl implements SysNoticeReadService {
    @Autowired
    private SysNoticeReadMapper noticeReadMapper;

    /**
     * 标记已读
     */
    @Override
    public void markRead(Long noticeId, Long userId) {
        SysNoticeRead record = new SysNoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(new Date());
        if (noticeReadMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>().eq(SysNoticeRead::getNoticeId, noticeId).eq(SysNoticeRead::getUserId, userId)) == 0) {
            noticeReadMapper.insert(record);
        }
    }

    /**
     * 查询某用户未读公告数量
     */
    @Override
    public int selectUnreadCount(Long userId) {
        return noticeReadMapper.selectUnreadCount(userId);
    }

    /**
     * 查询公告列表并标记当前用户已读状态
     */
    @Override
    public List<SysNotice> selectNoticeListWithReadStatus(Long userId, int limit) {
        return noticeReadMapper.selectNoticeListWithReadStatus(userId, limit);
    }

    /**
     * 批量标记已读
     */
    @Override
    public void markReadBatch(Long userId, Long[] noticeIds) {
        if (noticeIds == null || noticeIds.length == 0) {
            return;
        }
        for (Long noticeId : noticeIds) {
            markRead(noticeId, userId);
        }
    }

    /**
     * 查询已阅读某公告的用户列表
     */
    @Override
    public List<Map<String, Object>> selectReadUsersByNoticeId(Long noticeId, String searchValue) {
        return noticeReadMapper.selectReadUsersByNoticeId(noticeId, searchValue);
    }

    /**
     * 删除公告时清理对应已读记录
     */
    @Override
    public void deleteByNoticeIds(Long[] noticeIds) {
        noticeReadMapper.delete(new LambdaQueryWrapper<SysNoticeRead>().in(SysNoticeRead::getNoticeId, java.util.Arrays.asList(noticeIds)));
    }
}
