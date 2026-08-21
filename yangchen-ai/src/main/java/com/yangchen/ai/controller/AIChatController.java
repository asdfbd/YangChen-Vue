package com.yangchen.ai.controller;


import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yangchen.ai.context.AIContext;
import com.yangchen.ai.context.ToolApprovalRegistry;
import com.yangchen.ai.context.ToolInvocationContext;
import com.yangchen.ai.context.ToolProgressRegistry;
import com.yangchen.ai.entity.AIChatContent;
import com.yangchen.ai.entity.vo.ChatResp;
import com.yangchen.ai.service.AIChatContentService;
import com.yangchen.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "AI对话管理")
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {
    private final ChatClient chatClient;
    private final AIChatContentService aiChatContentService;
    private final ToolCallbackResolver toolCallbackResolver;
    private final ToolProgressRegistry toolProgressRegistry;
    private final ToolApprovalRegistry toolApprovalRegistry;
    private final ObjectMapper objectMapper;

    @PostMapping("/")
    @Operation(summary = "AI常规对话")
    @Parameters({
            @Parameter(name = "userInput", description = "用户输入")
    })
    public Flux<ChatResp> chat(
            @RequestBody String userInput,
            @RequestHeader(value = AIContext.DEFAULT_HEADER_TOOL_APPROVAL_ID, required = false) String approvalId) {
        userInput = normalizeUserInput(userInput);
        String conversationId = AIContext.getConversationId();
        toolProgressRegistry.bind(conversationId);

        Flux<ChatResp> chatFlux = chatClient.prompt()
                .user(userInput)
                .stream()
                .chatResponse()
                .doOnError(ex -> {
                    if (ex instanceof WebClientResponseException wcre) {
                        log.error("DeepSeek 返回 HTTP {}，响应体：{}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    } else {
                        log.error("AI 对话流异常", ex);
                    }
                })
                .flatMap(resp -> {
                    if (resp == null) {
                        return Flux.empty();
                    }
                    Generation result = resp.getResult();
                    if (result == null || result.getOutput() == null) {
                        return Flux.empty();
                    }
                    AssistantMessage message = result.getOutput();
                    String text = message.getText();
                    // 不向前端透出模型思考过程，只流式返回最终回答。
                    if (StringUtils.isBlank(text)) {
                        return Flux.empty();
                    }

                    ChatResp chatResp = new ChatResp();
                    chatResp.setRole("assistant");
                    chatResp.setText(text);
                    return Flux.just(chatResp);
                })
                // ToolCallAdvisor 在 boundedElastic 上执行工具循环；确认令牌必须通过
                // Reactor Context 传递，不能依赖 Web 请求线程的 ThreadLocal。
                .contextWrite(context -> StringUtils.isBlank(approvalId)
                        ? context.put(ToolInvocationContext.CONVERSATION_ID_KEY, conversationId)
                        : context.put(ToolInvocationContext.CONVERSATION_ID_KEY, conversationId)
                        .put(ToolInvocationContext.APPROVAL_ID_KEY, approvalId));

        // 进度通道是手动完成的 Sink。聊天主流结束或浏览器取消时先解绑，避免 merge
        // 等待一个永不结束的事件流，导致前端一直停留在“停止生成”状态。
        return Flux.merge(
                chatFlux.doFinally(signal -> {
                    toolApprovalRegistry.clearActive(approvalId);
                    toolProgressRegistry.unbind(conversationId);
                }),
                toolProgressRegistry.fluxOf(conversationId));
    }

    /** fetch(JSON.stringify(text)) 传来的 JSON 字符串不能把两侧引号一并写入聊天历史。 */
    private String normalizeUserInput(String userInput) {
        if (userInput == null) {
            return "";
        }
        String value = userInput.trim();
        if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            return userInput;
        }
        try {
            return objectMapper.readValue(value, String.class);
        } catch (JsonProcessingException exception) {
            log.warn("无法解析 JSON 字符串形式的用户输入，按原文处理");
            return userInput;
        }
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
        return R.ok(aiChatContentService.listByConversationId(conversationId, false));
    }


}
