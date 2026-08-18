package com.yangchen.ai.controller;


import com.yangchen.ai.entity.AiChatTitle;
import com.yangchen.ai.service.AiChatTitleService;
import com.yangchen.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "AI标题管理")
@RequestMapping("/ai/chat/title")
@RequiredArgsConstructor
public class AIChatTitleController {
    private final AiChatTitleService aiChatTitleService;

    @GetMapping("/list/{userId}")
    @Operation(summary = "查询对话列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户id")
    })
    public R<List<AiChatTitle>> list(@PathVariable Long userId) {
        return R.ok(aiChatTitleService.listByUserId(userId));
    }

    @PutMapping("/")
    @Operation(summary = "标题修改")
    public R<String> update(@RequestBody AiChatTitle aiChatTitle) {
        aiChatTitleService.updateById(aiChatTitle);
        return R.ok();
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "标题删除")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话id")
    })
    public R<String> delete(@PathVariable Long conversationId) {
        aiChatTitleService.deleteByConversationId(conversationId);
        return R.ok();
    }
}
