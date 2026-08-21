package com.yangchen.ai.config;

import com.yangchen.ai.advisor.AutoConversationAdvisor;
import com.yangchen.ai.advisor.CustomToolCallingManager;
import com.yangchen.ai.memory.CustomMemoryRepository;
import com.yangchen.ai.tool.CommonQueryTool;
import com.yangchen.ai.tool.DeptTool;
import com.yangchen.ai.tool.PostTool;
import com.yangchen.ai.tool.RoleTool;
import com.yangchen.ai.tool.UserTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;

/**
 * AI客户端配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClientConfig {
    private final AutoConversationAdvisor autoConversationAdvisor;
    private final CustomMemoryRepository customMemoryRepository;
    private final CommonQueryTool commonQueryTool;
    private final CustomToolCallingManager customToolCallingManager;

    @Bean
    @Primary
    public ChatClient defaultClient(ChatClient.Builder builder, MessageWindowChatMemory chatMemory, UserTool userTool, DeptTool deptTool, PostTool postTool, RoleTool roleTool) {
        // 系统提示词从 resources/prompts/ai-system-prompt.md 读取
        String systemPrompt = loadSystemPrompt();
        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(ToolCallAdvisor.builder()
                        .toolCallingManager(customToolCallingManager)
                        .advisorOrder(Ordered.HIGHEST_PRECEDENCE + 300)
                        .build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .order(Ordered.HIGHEST_PRECEDENCE + 200)
                        .build())
                .defaultTools(commonQueryTool, userTool, deptTool, postTool, roleTool)
                .defaultAdvisors(autoConversationAdvisor)
                .build();
    }

    @Bean
    public ChatClient titleClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(promptSystemSpec -> {
                    promptSystemSpec.text("你是会话标题生成助手。根据用户的第一条消息，生成一个简洁的中文会话标题，不超过20个字，不要加引号、标点或多余解释，直接输出标题。");
                })
                .build();
    }

    @Bean
    public MessageWindowChatMemory messageWindowChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(customMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(false);
    }

    /**
     * 加载系统提示词（resources/prompts/ai-system-prompt.md）
     */
    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/ai-system-prompt.md");
            try (var inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("读取 AI 系统提示词失败，使用默认提示", e);
            return "你是一个专业、友好、可靠的中文 AI 助手。";
        }
    }
}