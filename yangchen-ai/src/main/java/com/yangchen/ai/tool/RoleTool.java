package com.yangchen.ai.tool;

import com.yangchen.common.annotation.ToolConfirm;
import com.yangchen.common.annotation.ToolDescription;
import com.yangchen.common.core.entity.SysRole;
import com.yangchen.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 角色tool
 */
@Service
@RequiredArgsConstructor
@ToolDescription("专门用于角色管理的tool")
public class RoleTool {

    private final SysRoleService sysRoleService;

    /**
     * 新增角色
     */
    @ToolConfirm(description = "新增角色")
    @Tool(description = """
            新增一个角色。当用户明确要求创建/新增角色时调用。
            role 需包含：角色名称 roleName、权限字符 roleKey；
            可选：角色排序 roleSort、数据范围 dataScope、角色状态 status（0正常 1停用）。
            此操作会写入数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void insertRole(SysRole role) {
        sysRoleService.insertRole(role);
    }

    /**
     * 修改角色
     */
    @ToolConfirm(description = "修改角色")
    @Tool(description = """
            修改一个已存在的角色。当用户明确要求修改/更新角色信息时调用。
            role 需包含：角色ID roleId；
            可选要修改的字段：角色名称 roleName、权限字符 roleKey、角色排序 roleSort、
            数据范围 dataScope、角色状态 status。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void updateRole(SysRole role) {
        sysRoleService.updateRole(role);
    }

    /**
     * 删除角色
     */
    @ToolConfirm(description = "删除角色")
    @Tool(description = """
            删除一个角色。仅当用户明确要求删除某个角色且已给出角色ID时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deleteRoleById(@ToolParam(description = "要删除的角色ID") Long roleId) {
        sysRoleService.deleteRoleById(roleId);
    }

    /**
     * 批量删除角色
     */
    @ToolConfirm(description = "批量删除角色")
    @Tool(description = """
            批量删除多个角色。当用户明确要求删除多个角色并给出角色ID数组时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deleteRoleByIds(@ToolParam(description = "要删除的角色ID数组") Long[] roleIds) {
        sysRoleService.deleteRoleByIds(roleIds);
    }

    /**
     * 修改角色状态（启用/停用）
     */
    @ToolConfirm(description = "修改角色状态")
    @Tool(description = """
            修改角色状态（启用/停用）。当用户明确要求启用、停用或禁用某角色时调用。
            role 需包含：角色ID roleId、目标状态 status（0正常 1停用）。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void updateRoleStatus(SysRole role) {
        sysRoleService.updateRoleStatus(role);
    }
}
