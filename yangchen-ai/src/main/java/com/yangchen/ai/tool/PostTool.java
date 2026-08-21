package com.yangchen.ai.tool;

import com.yangchen.common.annotation.ToolConfirm;
import com.yangchen.common.annotation.ToolDescription;
import com.yangchen.system.entity.SysPost;
import com.yangchen.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 岗位tool
 */
@Service
@RequiredArgsConstructor
@ToolDescription("专门用于岗位管理的tool")
public class PostTool {

    private final SysPostService sysPostService;

    /**
     * 新增岗位
     */
    @ToolConfirm(description = "新增岗位")
    @Tool(description = """
            新增一个岗位。当用户明确要求创建/新增岗位时调用。
            post 需包含：岗位编码 postCode、岗位名称 postName；
            可选：岗位排序 postSort、岗位状态 status（0正常 1停用）。
            此操作会写入数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void insertPost(SysPost post) {
        sysPostService.insertPost(post);
    }

    /**
     * 修改岗位
     */
    @ToolConfirm(description = "修改岗位")
    @Tool(description = """
            修改一个已存在的岗位。当用户明确要求修改/更新岗位信息时调用。
            post 需包含：岗位ID postId；
            可选要修改的字段：岗位编码 postCode、岗位名称 postName、
            岗位排序 postSort、岗位状态 status。
            此操作会修改数据库，执行前会弹出确认卡片供用户确认。
            """)
    public void updatePost(SysPost post) {
        sysPostService.updatePost(post);
    }

    /**
     * 删除岗位
     */
    @ToolConfirm(description = "删除岗位")
    @Tool(description = """
            删除一个岗位。仅当用户明确要求删除某个岗位且已给出岗位ID时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deletePostById(@ToolParam(description = "要删除的岗位ID") Long postId) {
        sysPostService.deletePostById(postId);
    }

    /**
     * 批量删除岗位
     */
    @ToolConfirm(description = "批量删除岗位")
    @Tool(description = """
            批量删除多个岗位。当用户明确要求删除多个岗位并给出岗位ID数组时调用。
            此操作不可恢复，执行前会弹出确认卡片供用户确认。
            """)
    public void deletePostByIds(@ToolParam(description = "要删除的岗位ID数组") Long[] postIds) {
        sysPostService.deletePostByIds(postIds);
    }
}
