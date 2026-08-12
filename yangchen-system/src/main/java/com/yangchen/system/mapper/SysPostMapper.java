package com.yangchen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.system.entity.SysPost;

import java.util.List;

public interface SysPostMapper extends BaseMapper<SysPost> {
    List<Long> selectPostListByUserId(Long userId);

    List<SysPost> selectPostsByUserName(String userName);
}