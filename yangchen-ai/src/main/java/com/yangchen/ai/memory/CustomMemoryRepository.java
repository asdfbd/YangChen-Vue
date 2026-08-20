package com.yangchen.ai.memory;

import com.yangchen.ai.entity.AIChatContent;
import com.yangchen.ai.service.AIChatContentService;
import com.yangchen.common.exception.ServiceException;
import com.yangchen.common.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<AIChatContent> aiChatContentList = aiChatContentService.listByConversationId(Long.valueOf(conversationId), true);
        return convertMessageList(aiChatContentList);
    }

    private List<Message> convertMessageList(List<AIChatContent> aiChatContentList) {
        return aiChatContentList.stream()
                .map(e -> {
                    String content = e.getContent();
                    String messageType = e.getMessageType();
                    Map<String, Object> mateMap = JsonUtils.parseObject(e.getMateData());
                    if (Objects.isNull(mateMap)) {
                        mateMap = new HashMap<>();
                    }
                    mateMap.put(RECORD_ID, e.getId());
                    if (Objects.equals(messageType, MessageType.USER.getValue())) {
                        return UserMessage.builder()
                                .text(content)
                                .metadata(mateMap)
                                .build();
                    } else if (Objects.equals(messageType, MessageType.SYSTEM.getValue())) {
                        return SystemMessage.builder()
                                .text(content)
                                .metadata(mateMap)
                                .build();
                    } else if (Objects.equals(messageType, MessageType.ASSISTANT.getValue())) {
                        AssistantMessage.Builder builder = AssistantMessage.builder()
                                .properties(mateMap)
                                .content(content);
                        String toolCalls = e.getToolCalls();
                        if (StringUtils.isNotBlank(toolCalls)) {
                            builder.toolCalls(JsonUtils.parseArray(toolCalls, AssistantMessage.ToolCall.class));
                        }
                        return builder.build();
                    }
                    throw new ServiceException("未知消息类型！");
                })
                .map(e -> (Message) e)
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<AIChatContent> list = messages.stream()
                .filter(message -> !message.getMetadata().containsKey(RECORD_ID))
                .filter(message -> !(message instanceof ToolResponseMessage))
                .filter(message -> !(message instanceof AssistantMessage assistant
                        && !assistant.getToolCalls().isEmpty()))
                .map(e -> {
                    AIChatContent aiChatContent = new AIChatContent();
                    aiChatContent.setContent(e.getText());
                    aiChatContent.setMessageType(e.getMessageType().getValue());
                    aiChatContent.setConversationId(Long.valueOf(conversationId));
                    return aiChatContent;
                }).toList();

        aiChatContentService.batchInsertOrUpdate(list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        aiChatContentService.deleteByConversationId(Long.valueOf(conversationId));
    }
}
