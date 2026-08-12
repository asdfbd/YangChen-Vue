package com.yangchen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.common.core.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysDeptMapper extends BaseMapper<SysDept> {
    List<Long> selectDeptListByRoleId(@Param("roleId") Long roleId,
                                      @Param("deptCheckStrictly") boolean deptCheckStrictly);
}
