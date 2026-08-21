package com.yangchen.ai.tool;

import com.yangchen.common.annotation.ToolConfirm;
import com.yangchen.common.annotation.ToolDescription;
import com.yangchen.common.core.entity.SysUser;
import com.yangchen.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 用户tool
 */
@Service
@RequiredArgsConstructor
@ToolDescription("专门用于用户管理的tool")
public class UserTool {

    private final SysUserService sysUserService;

    /**
     * 新增用户
     */
    @ToolConfirm(description = "新增用户")
    @Tool(description = """
            新增一个系统用户。当用户明确要求创建/新增用户时调用。
            user 需包含：账号 userName、昵称 nickName、密码 password；
            可选：部门 deptId、手机号 phonenumber、邮箱 email、性别 sex、
            账号状态 status（0正常 1停用）、角色 roleIds、岗位 postIds。
            此操作会写入数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void insertUser(SysUser user) {
        sysUserService.insertUser(user);
    }

    /**
     * 修改用户
     */
    @ToolConfirm(description = "修改用户")
    @Tool(description = """
            修改一个已存在的系统用户。当用户明确要求修改/更新用户信息时调用。
            user 需包含：用户ID userId；
            可选要修改的字段：昵称 nickName、部门 deptId、手机号 phonenumber、邮箱 email、
            性别 sex、账号状态 status、角色 roleIds、岗位 postIds、备注 remark。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void updateUser(SysUser user) {
        sysUserService.updateUser(user);
    }

    /**
     * 通过用户ID删除用户
     */
    @ToolConfirm(description = "删除用户")
    @Tool(description = """
            删除一个系统用户，会一并删除其角色、岗位关联。
            仅当用户明确要求删除某个用户且已给出用户ID时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deleteUserById(@ToolParam(description = "要删除的用户ID") Long userId) {
        sysUserService.deleteUserById(userId);
    }

    /**
     * 批量删除用户
     */
    @ToolConfirm(description = "批量删除用户")
    @Tool(description = """
            批量删除多个系统用户，会一并删除其角色、岗位关联。
            当用户明确要求删除多个用户并给出用户ID数组时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deleteUserByIds(@ToolParam(description = "要删除的用户ID数组") Long[] userIds) {
        sysUserService.deleteUserByIds(userIds);
    }

    /**
     * 重置用户密码
     */
    @ToolConfirm(description = "重置用户密码")
    @Tool(description = """
            重置用户的登录密码。仅当用户明确要求重置密码/忘记密码时调用。
            user 需包含：用户ID userId、新密码 password。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void resetPwd(SysUser user) {
        sysUserService.resetPwd(user);
    }

    /**
     * 修改用户状态（启用/停用）
     */
    @ToolConfirm(description = "修改用户状态")
    @Tool(description = """
            修改用户账号状态（启用/停用）。
            当用户明确要求启用、停用或禁用某用户账号时调用。
            user 需包含：用户ID userId、目标状态 status（0正常 1停用）。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void updateUserStatus(SysUser user) {
        sysUserService.updateUserStatus(user);
    }
}
