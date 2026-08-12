package com.yangchen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yangchen.common.core.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {
    List<SysRole> selectRoleList(Page<SysRole> page, @Param("role") SysRole role);

    List<SysRole> selectRolePermissionByUserId(Long userId);

    List<Long> selectRoleListByUserId(Long userId);

    List<SysRole> selectRolesByUserName(String userName);
}
