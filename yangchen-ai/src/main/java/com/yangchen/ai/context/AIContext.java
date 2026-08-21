package com.yangchen.ai.context;

/**
 * AI上下文
 */
public class AIContext {
    public static final String DEFAULT_HEADER_CONVERSATION_ID = "x-conversation-id";
    public static final String DEFAULT_HEADER_TOOL_APPROVAL_ID = "x-tool-approval-id";
    private static final ThreadLocal<String> CONVERSATION_ID_HOLDER = new ThreadLocal<>();

    public static String getConversationId() {
        return CONVERSATION_ID_HOLDER.get();
    }

    public static void setConversationId(String conversationId) {
        CONVERSATION_ID_HOLDER.set(conversationId);
    }

    public static void clear() {
        CONVERSATION_ID_HOLDER.remove();
    }

}
