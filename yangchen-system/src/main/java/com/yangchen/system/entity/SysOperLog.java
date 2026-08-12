package com.yangchen.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yangchen.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志记录表 sys_oper_log
 *
 * @author yangchen
 */
@Data
@TableName("sys_oper_log")
@Schema(description = "操作日志记录表 sys_oper_log")
public class SysOperLog {
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @Schema(description = "日志主键")
    @Excel(name = "操作序号", cellType = Excel.ColumnType.NUMERIC)
    @TableId(value = "oper_id", type = IdType.ASSIGN_ID)
    private Long operId;

    /**
     * 操作模块
     */
    @Schema(description = "操作模块")
    @Excel(name = "操作模块")
    private String title;

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     */
    @Schema(description = "业务类型（0其它 1新增 2修改 3删除）")
    @Excel(name = "业务类型", readConverterExp = "0=其它,1=新增,2=修改,3=删除,4=授权,5=导出,6=导入,7=强退,8=生成代码,9=清空数据")
    private Integer businessType;

    /**
     * 业务类型数组
     */
    @Schema(description = "业务类型数组")
    @TableField(exist = false)
    private Integer[] businessTypes;

    /**
     * 请求方法
     */
    @Schema(description = "请求方法")
    @Excel(name = "请求方法")
    private String method;

    /**
     * 请求方式
     */
    @Schema(description = "请求方式")
    @Excel(name = "请求方式")
    private String requestMethod;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     */
    @Schema(description = "操作类别（0其它 1后台用户 2手机端用户）")
    @Excel(name = "操作类别", readConverterExp = "0=其它,1=后台用户,2=手机端用户")
    private Integer operatorType;

    /**
     * 操作人员
     */
    @Schema(description = "操作人员")
    @Excel(name = "操作人员")
    private String operName;

    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    @Excel(name = "部门名称")
    private String deptName;

    /**
     * 请求url
     */
    @Schema(description = "请求url")
    @Excel(name = "请求地址")
    private String operUrl;

    /**
     * 操作地址
     */
    @Schema(description = "操作地址")
    @Excel(name = "操作地址")
    private String operIp;

    /**
     * 操作地点
     */
    @Schema(description = "操作地点")
    @Excel(name = "操作地点")
    private String operLocation;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    @Excel(name = "请求参数")
    private String operParam;

    /**
     * 返回参数
     */
    @Schema(description = "返回参数")
    @Excel(name = "返回参数")
    private String jsonResult;

    /**
     * 操作状态（0正常 1异常）
     */
    @Schema(description = "操作状态（0正常 1异常）")
    @Excel(name = "状态", readConverterExp = "0=正常,1=异常")
    private Integer status;

    /**
     * 错误消息
     */
    @Schema(description = "错误消息")
    @Excel(name = "错误消息")
    private String errorMsg;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date operTime;

    /**
     * 消耗时间
     */
    @Schema(description = "消耗时间")
    @Excel(name = "消耗时间", suffix = "毫秒")
    private Long costTime;

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
