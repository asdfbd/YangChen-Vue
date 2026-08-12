package com.yangchen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.common.core.entity.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {
    List<SysMenu> selectMenuListByUserId(SysMenu menu);

    List<String> selectMenuPermsByUserId(Long userId);

    List<String> selectMenuPermsByRoleId(Long roleId);

    List<SysMenu> selectMenuTreeByUserId(Long userId);

    List<Long> selectMenuListByRoleId(@Param("roleId") Long roleId,
                                      @Param("menuCheckStrictly") boolean menuCheckStrictly);
}
