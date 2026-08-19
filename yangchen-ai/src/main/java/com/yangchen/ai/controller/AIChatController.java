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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
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
    private final ToolCallbackResolver toolCallbackResolver;

    @PostMapping("/")
    @Operation(summary = "AI常规对话")
    @Parameters({
            @Parameter(name = "userInput", description = "用户输入")
    })
    public Flux<ChatResp> chat(@RequestBody String userInput) {
        AiChatTitle aiChatTitle = aiChatTitleService.listByConversationId(Long.valueOf(AIContext.getConversationId()));
        if (Objects.isNull(aiChatTitle)) {
            //生成标题
            aiChatTitleService.generateTitle(userInput, Long.valueOf(AIContext.getConversationId()));
        }
        return chatClient.prompt()
                .user(userInput)
                .stream()
                .chatResponse()
                .flatMap(resp -> {
                    if (resp == null) {
                        return Flux.empty();
                    }
                    ChatResp chatResp = new ChatResp();
                    chatResp.setRole("assistant");

                    Generation result = resp.getResult();
                    AssistantMessage message = result.getOutput();
                    String text = message.getText();
                    // 不向前端透出模型思考过程，只流式返回最终回答。
                    if (StringUtils.isBlank(text)) {
                        return Flux.empty();
                    }

                    chatResp.setText(text);
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
