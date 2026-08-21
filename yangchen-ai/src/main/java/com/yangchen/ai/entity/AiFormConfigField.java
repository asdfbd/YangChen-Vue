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
 * AI动态单-单项配置 entity for ai_form_config_field.
 *
 * @author yangchen
 * @date 2026-08-21
 */
@Schema(description = "AI动态单-单项配置对象 ai_form_config_field")
@Data
@TableName("ai_form_config_field")
public class AiFormConfigField implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联ai_form_config.form_id，所属表单
     */
    @Schema(description = "关联ai_form_config.form_id，所属表单")
    @Excel(name = "关联ai_form_config.form_id，所属表单")
    @TableField("form_id")
    private Long formId;

    /**
     * 字段名，对应前端fields[].name，提交回传的key
     */
    @Schema(description = "字段名，对应前端fields[].name，提交回传的key")
    @Excel(name = "字段名，对应前端fields[].name，提交回传的key")
    @TableField("field_name")
    private String fieldName;

    /**
     * 字段中文标题，对应前端fields[].label
     */
    @Schema(description = "字段中文标题，对应前端fields[].label")
    @Excel(name = "字段中文标题，对应前端fields[].label")
    @TableField("field_label")
    private String fieldLabel;

    /**
     * 控件类型：text/textarea/number/select
     */
    @Schema(description = "控件类型：text/textarea/number/select")
    @Excel(name = "控件类型：text/textarea/number/select")
    @TableField("field_type")
    private String fieldType;

    /**
     * 是否必填：0否 1是，对应fields[].required
     */
    @Schema(description = "是否必填：0否 1是，对应fields[].required")
    @Excel(name = "是否必填：0否 1是，对应fields[].required")
    @TableField("required_flag")
    private String requiredFlag;

    /**
     * 默认值，对应fields[].defaultValue
     */
    @Schema(description = "默认值，对应fields[].defaultValue")
    @Excel(name = "默认值，对应fields[].defaultValue")
    @TableField("default_value")
    private String defaultValue;

    /**
     * 选项来源：0无选项 1数据字典 2动态工具
     */
    @Schema(description = "选项来源：0无选项 1数据字典 2动态工具")
    @Excel(name = "选项来源：0无选项 1数据字典 2动态工具")
    @TableField("option_source")
    private String optionSource;

    /**
     * 选项来源为字典时的字典编码，如sys_user_sex
     */
    @Schema(description = "选项来源为字典时的字典编码，如sys_user_sex")
    @Excel(name = "选项来源为字典时的字典编码，如sys_user_sex")
    @TableField("dict_type")
    private String dictType;

    /**
     * 选项来源为动态工具时的Tool函数名，如selectRoleOptions
     */
    @Schema(description = "选项来源为动态工具时的Tool函数名，如selectRoleOptions")
    @Excel(name = "选项来源为动态工具时的Tool函数名，如selectRoleOptions")
    @TableField("option_tool")
    private String optionTool;

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
