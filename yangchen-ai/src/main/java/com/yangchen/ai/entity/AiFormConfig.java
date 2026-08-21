package com.yangchen.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yangchen.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ai单配置 entity for ai_form_config.
 *
 * @author yangchen
 * @date 2026-08-21
 */
@Schema(description = "ai单配置对象 ai_form_config")
@Data
@TableName("ai_form_config")
public class AiFormConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @Schema(description = "")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 表单标识，AI 命中用（如 user-create）
     */
    @Schema(description = "表单标识，AI 命中用（如 user-create）")
    @Excel(name = "表单标识，AI 命中用", readConverterExp = "如=,u=ser-create")
    @TableField("form_key")
    private String formKey;

    /**
     * 表单名称（界面展示）
     */
    @Schema(description = "表单名称（界面展示）")
    @Excel(name = "表单名称", readConverterExp = "界=面展示")
    @TableField("form_name")
    private String formName;

    /**
     * 绑定的 Tool 函数名
     */
    @Schema(description = "绑定的 Tool 函数名")
    @Excel(name = "绑定的 Tool 函数名")
    @TableField("tool_name")
    private String toolName;

    /**
     * 状态0启用，1停用
     */
    @Schema(description = "状态0启用，1停用")
    @Excel(name = "状态0启用，1停用")
    @TableField("status")
    private String status;

    /**
     * 创建者
     */
    @Schema(description = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者
     */
    @Schema(description = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 搜索值
     */
    @Schema(description = "搜索值")
    @TableField(exist = false)
    @JsonIgnore
    private String searchValue;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params = new HashMap<>();
}
