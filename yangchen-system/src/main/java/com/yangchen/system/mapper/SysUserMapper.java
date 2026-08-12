package com.yangchen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yangchen.common.core.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {
    List<SysUser> selectUserList(Page<SysUser> page, @Param("user") SysUser user);

    List<SysUser> selectAllocatedList(Page<SysUser> page, @Param("user") SysUser user);

    List<SysUser> selectUnallocatedList(Page<SysUser> page, @Param("user") SysUser user);
}