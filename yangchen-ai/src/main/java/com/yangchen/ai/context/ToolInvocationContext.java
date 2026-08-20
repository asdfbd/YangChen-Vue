package com.yangchen.ai.context;

import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
import reactor.util.context.ContextView;

/** 从 Spring AI 工具递归的 Reactor Context 读取当前请求控制信息。 */
public final class ToolInvocationContext {

    public static final String APPROVAL_ID_KEY = ToolInvocationContext.class.getName() + ".approvalId";
    public static final String CONVERSATION_ID_KEY = ToolInvocationContext.class.getName() + ".conversationId";

    private ToolInvocationContext() {
    }

    public static String approvalId() {
        return value(APPROVAL_ID_KEY);
    }

    public static String conversationId() {
        return value(CONVERSATION_ID_KEY);
    }

    private static String value(String key) {
        ContextView context = ToolCallReactiveContextHolder.getContext();
        if (context == null || !context.hasKey(key)) {
            return null;
        }
        Object value = context.get(key);
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
