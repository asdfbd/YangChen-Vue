package com.yangchen.system.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户和岗位关联 sys_user_post
 *
 * @author yangchen
 */
@Data
@TableName("sys_user_post")
@Schema(description = "用户和岗位关联 sys_user_post")
public class SysUserPost {
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 岗位ID
     */
    @Schema(description = "岗位ID")
    private Long postId;
}
