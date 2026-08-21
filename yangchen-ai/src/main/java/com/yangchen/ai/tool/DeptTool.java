package com.yangchen.ai.tool;

import com.yangchen.common.annotation.ToolConfirm;
import com.yangchen.common.annotation.ToolDescription;
import com.yangchen.common.core.entity.SysDept;
import com.yangchen.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 部门tool
 */
@Service
@RequiredArgsConstructor
@ToolDescription("专门用于部门管理的tool")
public class DeptTool {

    private final SysDeptService sysDeptService;

    /**
     * 新增部门
     */
    @ToolConfirm(description = "新增部门")
    @Tool(description = """
            新增一个部门。当用户明确要求创建/新增部门时调用。
            dept 需包含：部门名称 deptName；
            可选：父部门 parentId、显示顺序 orderNum、负责人 leader、联系电话 phone、
            邮箱 email、部门状态 status（0正常 1停用）。
            此操作会写入数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void insertDept(SysDept dept) {
        sysDeptService.insertDept(dept);
    }

    /**
     * 修改部门
     */
    @ToolConfirm(description = "修改部门")
    @Tool(description = """
            修改一个已存在的部门。当用户明确要求修改/更新部门信息时调用。
            dept 需包含：部门ID deptId；
            可选要修改的字段：部门名称 deptName、父部门 parentId、显示顺序 orderNum、
            负责人 leader、联系电话 phone、邮箱 email、部门状态 status。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void updateDept(SysDept dept) {
        sysDeptService.updateDept(dept);
    }

    /**
     * 删除部门
     */
    @ToolConfirm(description = "删除部门")
    @Tool(description = """
            删除一个部门。仅当用户明确要求删除某个部门且已给出部门ID时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deleteDeptById(@ToolParam(description = "要删除的部门ID") Long deptId) {
        sysDeptService.deleteDeptById(deptId);
    }
}
