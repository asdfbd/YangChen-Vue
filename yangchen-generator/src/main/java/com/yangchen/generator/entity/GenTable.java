package com.yangchen.generator.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yangchen.common.constant.GenConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务表 gen_table
 *
 * @author yangchen
 */
@Data
@TableName("gen_table")
@Schema(description = "业务表 gen_table")
public class GenTable {
    private static final long serialVersionUID = 1L;

    /**
     * 编号
     */
    @Schema(description = "编号")
    @TableId(value = "table_id", type = IdType.ASSIGN_ID)
    private Long tableId;

    /**
     * 表名称
     */
    @Schema(description = "表名称")
    @NotBlank(message = "表名称不能为空")
    private String tableName;

    /**
     * 表描述
     */
    @Schema(description = "表描述")
    @NotBlank(message = "表描述不能为空")
    private String tableComment;

    /**
     * 关联父表的表名
     */
    @Schema(description = "关联父表的表名")
    private String subTableName;

    /**
     * 本表关联父表的外键名
     */
    @Schema(description = "本表关联父表的外键名")
    private String subTableFkName;

    /**
     * 实体类名称(首字母大写)
     */
    @Schema(description = "实体类名称(首字母大写)")
    @NotBlank(message = "实体类名称不能为空")
    private String className;

    /**
     * 使用的模板（crud单表操作 tree树表操作 sub主子表操作）
     */
    @Schema(description = "使用的模板（crud单表操作 tree树表操作 sub主子表操作）")
    private String tplCategory;

    /**
     * 前端类型（antd-vue-typescript 模版）
     */
    @Schema(description = "前端类型（antd-vue-typescript 模版）")
    private String tplWebType;

    /**
     * 生成包路径
     */
    @Schema(description = "生成包路径")
    @NotBlank(message = "生成包路径不能为空")
    private String packageName;

    /**
     * 生成模块名
     */
    @Schema(description = "生成模块名")
    @NotBlank(message = "生成模块名不能为空")
    private String moduleName;

    /**
     * 生成业务名
     */
    @Schema(description = "生成业务名")
    @NotBlank(message = "生成业务名不能为空")
    private String businessName;

    /**
     * 生成功能名
     */
    @Schema(description = "生成功能名")
    @NotBlank(message = "生成功能名不能为空")
    private String functionName;

    /**
     * 生成作者
     */
    @Schema(description = "生成作者")
    @NotBlank(message = "作者不能为空")
    private String functionAuthor;

    /**
     * 表单布局（单列 双列 三列）
     */
    @Schema(description = "表单布局（单列 双列 三列）")
    private Integer formColNum;

    /**
     * 生成代码方式（0zip压缩包 1自定义路径）
     */
    @Schema(description = "生成代码方式（0zip压缩包 1自定义路径）")
    private String genType;

    /**
     * 生成路径（不填默认项目路径）
     */
    @Schema(description = "生成路径（不填默认项目路径）")
    private String genPath;

    /**
     * 主键信息
     */
    @Schema(description = "主键信息")
    @TableField(exist = false)
    private GenTableColumn pkColumn;

    /**
     * 子表信息
     */
    @Schema(description = "子表信息")
    @TableField(exist = false)
    private GenTable subTable;

    /**
     * 表列信息
     */
    @Schema(description = "表列信息")
    @Valid
    @TableField(exist = false)
    private List<GenTableColumn> columns;

    /**
     * 其它生成选项
     */
    @Schema(description = "其它生成选项")
    private String options;

    /**
     * 树编码字段
     */
    @Schema(description = "树编码字段")
    @TableField(exist = false)
    private String treeCode;

    /**
     * 树父编码字段
     */
    @Schema(description = "树父编码字段")
    @TableField(exist = false)
    private String treeParentCode;

    /**
     * 树名称字段
     */
    @Schema(description = "树名称字段")
    @TableField(exist = false)
    private String treeName;

    /**
     * 上级菜单ID字段
     */
    @Schema(description = "上级菜单ID字段")
    @TableField(exist = false)
    private Long parentMenuId;

    /**
     * 上级菜单名称字段
     */
    @Schema(description = "上级菜单名称字段")
    @TableField(exist = false)
    private String parentMenuName;

    /**
     * 是否生成详情页
     */
    @Schema(description = "是否生成详情页")
    @TableField(exist = false)
    private boolean isView;

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

    public static boolean isSub(String tplCategory) {
        return tplCategory != null && StrUtil.equals(GenConstants.TPL_SUB, tplCategory);
    }

    public static boolean isTree(String tplCategory) {
        return tplCategory != null && StrUtil.equals(GenConstants.TPL_TREE, tplCategory);
    }

    public static boolean isCrud(String tplCategory) {
        return tplCategory != null && StrUtil.equals(GenConstants.TPL_CRUD, tplCategory);
    }

    public static boolean isSuperColumn(String tplCategory, String javaField) {
        if (isTree(tplCategory)) {
            return StrUtil.equalsAnyIgnoreCase(javaField,
                    ArrayUtils.addAll(GenConstants.TREE_ENTITY, GenConstants.BASE_ENTITY));
        }
        return StrUtil.equalsAnyIgnoreCase(javaField, GenConstants.BASE_ENTITY);
    }

    public boolean isSub() {
        return isSub(this.tplCategory);
    }

    public boolean isTree() {
        return isTree(this.tplCategory);
    }

    public boolean isCrud() {
        return isCrud(this.tplCategory);
    }

    public boolean isSuperColumn(String javaField) {
        return isSuperColumn(this.tplCategory, javaField);
    }
}
