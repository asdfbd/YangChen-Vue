package com.yangchen.ai.config;

import com.yangchen.ai.advisor.AutoConversationAdvisor;
import com.yangchen.ai.memory.CustomMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI客户端配置类
 */
@Configuration
@RequiredArgsConstructor
public class ClientConfig {
    private final AutoConversationAdvisor autoConversationAdvisor;
    private final CustomMemoryRepository customMemoryRepository;

    @Bean
    @Primary
    public ChatClient defaultClient(ChatClient.Builder builder, MessageWindowChatMemory chatMemory) {
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
        return builder
                .defaultSystem("""
                        你是一个专业、友好、可靠的 AI 助手。请使用中文回答问题，表达清晰、简洁、准确。
                        根据用户的问题提供可执行的建议或解决方案。
                        不确定的信息不要编造，应明确说明。
                        遇到复杂问题时，请分步骤进行说明。
                        涉及代码时，请提供规范、易懂且可直接参考的示例。
                        涉及风险操作时，请提前提醒用户注意事项。
                        """)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(memoryAdvisor)
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
}
