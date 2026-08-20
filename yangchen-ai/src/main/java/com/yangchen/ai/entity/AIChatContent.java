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
 * AI对话内容 entity for ai_chat_content.
 *
 * @author yangchen
 * @date 2026-08-18
 */
@Schema(description = "AI对话内容对象 ai_chat_content")
@Data
@TableName("ai_chat_content")
public class AIChatContent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对话id
     */
    @Schema(description = "对话id")
    @Excel(name = "对话id")
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 消息类型
     */
    @Schema(description = "消息类型")
    @Excel(name = "消息类型")
    @TableField("message_type")
    private String messageType;

    /**
     * 文本内容
     */
    @Schema(description = "文本内容")
    @Excel(name = "文本内容")
    @TableField("content")
    private String content;

    /**
     * 原数据信息
     */
    @Schema(description = "原数据信息")
    @Excel(name = "原数据信息")
    @TableField("mate_data")
    private String mateData;

    @Schema(description = "工具调用信息")
    @Excel(name = "工具调用消息")
    @TableField("tool_calls")
    private String toolCalls;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

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
