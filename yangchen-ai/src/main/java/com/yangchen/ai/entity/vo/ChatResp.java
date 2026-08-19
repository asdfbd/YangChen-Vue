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
}
