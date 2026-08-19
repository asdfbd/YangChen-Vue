package com.yangchen.ai.service;

import com.yangchen.ai.entity.AiChatTitle;

import java.util.List;

/**
 * ai对话聊天标题Service接口
 *
 * @author yangchen
 * @date 2026-08-18
 */
public interface AiChatTitleService {

    List<AiChatTitle> listByUserId(Long userId);

    void updateById(AiChatTitle aiChatTitle);

    void save(AiChatTitle aiChatTitle);

    void deleteByConversationId(Long conversationId);

    AiChatTitle listByConversationId(Long conversationId);

    AiChatTitle generateTitleIfAbsent(String userInput, Long conversationId);
}
