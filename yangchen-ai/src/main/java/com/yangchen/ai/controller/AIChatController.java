package com.yangchen.ai.controller;


import cn.hutool.core.util.IdUtil;
import com.yangchen.ai.context.AIContext;
import com.yangchen.ai.entity.AIChatContent;
import com.yangchen.ai.entity.AiChatTitle;
import com.yangchen.ai.entity.vo.ChatResp;
import com.yangchen.ai.service.AIChatContentService;
import com.yangchen.ai.service.AiChatTitleService;
import com.yangchen.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@RestController
@Tag(name = "AI对话管理")
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {
    private final ChatClient chatClient;
    private final AIChatContentService aiChatContentService;
    private final AiChatTitleService aiChatTitleService;

    @PostMapping("/")
    @Operation(summary = "AI常规对话")
    @Parameters({
            @Parameter(name = "userInput", description = "用户输入")
    })
    public Flux<ChatResp> chat(@RequestBody String userInput) {
        AiChatTitle aiChatTitle = aiChatTitleService.listByConversationId(Long.valueOf(AIContext.getConversationId()));
        if (Objects.isNull(aiChatTitle)) {
            //生成标题
            aiChatTitleService.generateTitle(userInput,Long.valueOf(AIContext.getConversationId()));
        }
        return chatClient.prompt()
                .user(userInput)
                .stream()
                .chatResponse()
                .flatMap(resp -> {
                    if (resp == null) {
                        return Flux.empty();
                    } else {
                        resp.getResult();
                        resp.getResult();
                    }
                    ChatResp chatResp = new ChatResp();
                    chatResp.setRole("assistant");

                    Generation result = resp.getResult();
                    DeepSeekAssistantMessage message = (DeepSeekAssistantMessage) result.getOutput();
                    String reasoningContent = message.getReasoningContent();
                    String text = message.getText();
                    if (StringUtils.isBlank(text) && StringUtils.isBlank(reasoningContent)) {
                        return Flux.empty();
                    }

                    if (StringUtils.isNotBlank(text)) {
                        chatResp.setText(text);
                    }
                    if (StringUtils.isNotBlank(reasoningContent)) {
                        chatResp.setReasonText(reasoningContent);
                    }
                    return Flux.just(chatResp);
                });
    }

    @Operation(summary = "获取对话ID")
    @GetMapping("/generateConversationId")
    public R<Long> generateConversationId() {
        return R.ok(IdUtil.getSnowflakeNextId());
    }

    @GetMapping("/list/{conversationId}")
    @Operation(summary = "获取历史对话 ")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话id")
    })
    public R<List<AIChatContent>> list(@PathVariable Long conversationId) {
        return R.ok(aiChatContentService.listByConversationId(conversationId));
    }


}
