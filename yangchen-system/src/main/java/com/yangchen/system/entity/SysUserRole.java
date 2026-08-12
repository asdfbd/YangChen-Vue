package com.yangchen.system.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author yangchen
 */
@Data
@TableName("sys_user_role")
@Schema(description = "用户和角色关联 sys_user_role")
public class SysUserRole {
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private Long roleId;
}
