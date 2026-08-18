package com.yangchen.ai.advisor;

import com.yangchen.ai.context.AIContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class AutoConversationAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        chatClientRequest.context().put(ChatMemory.CONVERSATION_ID, AIContext.getConversationId());
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        chatClientRequest.context().put(ChatMemory.CONVERSATION_ID, AIContext.getConversationId());
        Flux<ChatClientResponse> chatClientResponse = streamAdvisorChain.nextStream(chatClientRequest);
        return chatClientResponse;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
