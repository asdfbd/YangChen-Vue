package com.yangchen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.system.entity.SysNotice;
import com.yangchen.system.entity.SysNoticeRead;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SysNoticeReadMapper extends BaseMapper<SysNoticeRead> {
    int selectUnreadCount(@Param("userId") Long userId);

    List<SysNotice> selectNoticeListWithReadStatus(@Param("userId") Long userId, @Param("limit") int limit);

    List<Map<String, Object>> selectReadUsersByNoticeId(@Param("noticeId") Long noticeId, @Param("searchValue") String searchValue);
}