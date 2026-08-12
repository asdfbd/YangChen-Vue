package com.yangchen.system.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色和部门关联 sys_role_dept
 *
 * @author yangchen
 */
@Data
@TableName("sys_role_dept")
@Schema(description = "角色和部门关联 sys_role_dept")
public class SysRoleDept {
    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private Long roleId;

    /**
     * 部门ID
     */
    @Schema(description = "部门ID")
    private Long deptId;
}
