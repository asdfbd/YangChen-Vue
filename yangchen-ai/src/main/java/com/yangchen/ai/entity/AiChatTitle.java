package com.yangchen.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yangchen.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

/**
 * ai对话聊天标题 entity for ai_chat_title.
 *
 * @author yangchen
 * @date 2026-08-18
 */
@Schema(description = "ai对话聊天标题对象 ai_chat_title")
@Data
@TableName("ai_chat_title")
public class AiChatTitle {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @Schema(description = "")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户Id
     */
    @Schema(description = "用户Id")
    @Excel(name = "用户Id")
    @TableField("user_id")
    private Long userId;

    /**
     * 对话id
     */
    @Schema(description = "对话id")
    @Excel(name = "对话id")
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 对话标题
     */
    @Schema(description = "对话标题")
    @Excel(name = "对话标题")
    @TableField("title")
    @NotBlank(message = "对话标题不能为空！")
    @Length(max = 20, message = "字符最多只能有20个！")
    private String title;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date updateTime;
}
