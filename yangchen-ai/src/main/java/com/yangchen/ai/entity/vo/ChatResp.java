package com.yangchen.ai.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天对话信息返回
 */
@Data
@Schema(description = "聊天对话信息返回")
public class ChatResp {
    @Schema(description = "文本")
    private String text;
    @Schema(description = "角色")
    private String role;
    @Schema(description = "消息类型：text、event 或 ui")
    private String type;
    @Schema(description = "UI 组件名称")
    private String component;
    @Schema(description = "UI 组件数据")
    private Object data;
    @Schema(description = "UI 组件动作")
    private Object action;
    @Schema(description = "UI 消息 ID")
    private String messageId;
}
