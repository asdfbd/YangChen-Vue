package com.yangchen.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yangchen.ai.entity.AIChatContent;
import com.yangchen.ai.service.AIChatContentService;
import com.yangchen.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 自定义持久化内存消息
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomMemoryRepository implements ChatMemoryRepository {
    private final AIChatContentService aiChatContentService;
    private final ObjectMapper objectMapper;
    private final String RECORD_ID = "recordId";

    @Override
    public List<String> findConversationIds() {
        List<AIChatContent> list = aiChatContentService.listAll();
        return list.stream().map(AIChatContent::getConversationId)
                .map(String::valueOf)
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<AIChatContent> aiChatContentList = aiChatContentService.listByConversationId(Long.valueOf(conversationId));
        return convertMessageList(aiChatContentList);
    }

    private static @NonNull List<Message> convertMessageList(List<AIChatContent> aiChatContentList) {
        return aiChatContentList.stream().map(e -> {
                    String content = e.getContent();
                    String messageType = e.getMessageType();
                    if (Objects.equals(messageType, MessageType.TOOL.getValue())) {
                        //return new ToolResponseMessage()
                    } else if (Objects.equals(messageType, MessageType.USER.getValue())) {
                        UserMessage userMessage = new UserMessage(content);
                        userMessage.getMetadata().put("recordId", e.getId());
                        return userMessage;
                    } else if (Objects.equals(messageType, MessageType.SYSTEM.getValue())) {
                        SystemMessage systemMessage = new SystemMessage(content);
                        systemMessage.getMetadata().put("recordId", e.getId());
                        return systemMessage;
                    } else if (Objects.equals(messageType, MessageType.ASSISTANT.getValue())) {
                        AssistantMessage message = new AssistantMessage(content);
                        message.getMetadata().put("recordId", e.getId());
                        return message;
                    }
                    throw new ServiceException("未知消息类型！");
                }).map(e -> (Message) e)
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<AIChatContent> list = messages.stream()
                .filter(f -> !f.getMetadata().containsKey(RECORD_ID))
                .map(e -> {
                    AIChatContent aiChatContent = new AIChatContent();
                    aiChatContent.setContent(e.getText());
                    aiChatContent.setMessageType(e.getMessageType().getValue());
                    aiChatContent.setConversationId(Long.valueOf(conversationId));
                    try {
                        aiChatContent.setMateData(objectMapper.writeValueAsString(e.getMetadata()));
                    } catch (JsonProcessingException ex) {
                        log.error("获取对话参数信息失败", ex);
                        throw new RuntimeException(ex);
                    }
                    return aiChatContent;
                }).toList();

        aiChatContentService.batchInsertOrUpdate(list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        aiChatContentService.deleteByConversationId(Long.valueOf(conversationId));
    }
}
