package com.yangchen.ai.advisor;

import com.yangchen.ai.context.ToolApprovalRegistry;
import com.yangchen.ai.context.ToolInvocationContext;
import com.yangchen.ai.context.ToolProgressRegistry;
import com.yangchen.ai.entity.vo.ChatResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class CustomToolCallingManager implements ToolCallingManager {

    /**
     * 这些工具只读取表目录/字段，或只展示澄清组件；它们是查询的内部准备步骤，
     * 不应打断用户去反复确认。真正读取业务数据的 executeReadOnlySql* 才需要确认。
     */
    private static final Set<String> INTERNAL_PREPARATION_TOOLS = Set.of(
            "getDatabaseSchema", "askUserChoice");

    private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();
    private final ToolApprovalRegistry approvalRegistry;
    private final ToolProgressRegistry progressRegistry;

    public CustomToolCallingManager(ToolApprovalRegistry approvalRegistry,
                                    ToolProgressRegistry progressRegistry) {
        this.approvalRegistry = approvalRegistry;
        this.progressRegistry = progressRegistry;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        AssistantMessage assistantMessage = chatResponse.getResults().stream()
                .map(Generation::getOutput)
                .filter(AssistantMessage::hasToolCalls)
                .findFirst()
                .orElse(null);

        // 理论上 ToolCallAdvisor 只会在存在 tool_calls 时进入这里；防御性兜底。
        if (assistantMessage == null) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        List<AssistantMessage.ToolCall> calls = assistantMessage.getToolCalls();
        String conversationId = ToolInvocationContext.conversationId();
        if (!requiresUserApproval(calls)) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }
        String approvalId = ToolInvocationContext.approvalId();
        boolean alreadyActive = approvalRegistry.isActive(approvalId, conversationId);
        boolean justActivated = !alreadyActive
                && approvalRegistry.activateForCurrentConversation(approvalId, conversationId);
        if (alreadyActive || justActivated) {
            log.info("工具确认通过，开始执行：{}", calls.stream()
                    .map(AssistantMessage.ToolCall::name)
                    .toList());
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        ToolApprovalRegistry.PendingApproval approval = approvalRegistry.create(conversationId);
        emitConfirmationCard(conversationId, approval, calls);

        List<ToolResponseMessage.ToolResponse> responses = calls
                .stream()
                .map(call -> new ToolResponseMessage.ToolResponse(
                        call.id(),
                        call.name(),
                        """
                        {
                          "status": "CONFIRMATION_REQUIRED",
                           "message": "该操作尚未获得用户确认，禁止执行。请向用户说明影响并请求确认。不要输出任何文字、工具名、参数、批准编号或内部执行过程。"
                        }
                        """
                ))
                .toList();

        ToolResponseMessage toolMessage = ToolResponseMessage.builder()
                .responses(responses)
                .build();

        // 关键：必须保留原 Prompt + 含 tool_calls 的 assistant + tool response。
        List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());
        conversationHistory.add(assistantMessage);
        conversationHistory.add(toolMessage);

        return DefaultToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .returnDirect(false)
                .build();
    }

    private boolean requiresUserApproval(List<AssistantMessage.ToolCall> calls) {
        return calls.stream().anyMatch(call -> !INTERNAL_PREPARATION_TOOLS.contains(call.name()));
    }

    /** 确认卡片走独立 UI 事件通道，绝不插入 assistant/tool 协议消息。 */
    private void emitConfirmationCard(String conversationId,
                                      ToolApprovalRegistry.PendingApproval approval,
                                      List<AssistantMessage.ToolCall> calls) {
        if (conversationId == null || calls.isEmpty()) {
            return;
        }
        ChatResp response = new ChatResp();
        response.setType("ui");
        response.setRole("assistant");
        response.setComponent("confirm");
        response.setMessageId("tool-approval-" + approval.getApprovalId());
        response.setData(Map.of(
                "title", "请确认继续",
                "description", "该操作将在确认后执行。"));
        response.setAction(Map.of(
                "actionId", approval.getApprovalId(),
                "confirmText", "确认执行",
                "cancelText", "取消"));
        progressRegistry.emit(conversationId, response);
    }

}
