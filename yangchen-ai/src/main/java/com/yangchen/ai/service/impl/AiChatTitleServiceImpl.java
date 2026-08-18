package com.yangchen.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yangchen.ai.entity.AiChatTitle;
import com.yangchen.ai.mapper.AiChatTitleMapper;
import com.yangchen.ai.service.AiChatTitleService;
import com.yangchen.common.exception.ServiceException;
import com.yangchen.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * ai对话聊天标题Service业务层处理
 *
 * @author yangchen
 * @date 2026-08-18
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AiChatTitleServiceImpl implements AiChatTitleService {
    private final AiChatTitleMapper aiChatTitleMapper;
    private final MessageWindowChatMemory messageWindowChatMemory;
    @Qualifier("titleClient")
    @Autowired
    private  ChatClient chatClient;

    @Override
    public List<AiChatTitle> listByUserId(Long userId) {
        return aiChatTitleMapper.selectList(Wrappers.lambdaQuery(AiChatTitle.class).eq(AiChatTitle::getUserId, userId));
    }

    @Override
    public void updateById(AiChatTitle aiChatTitle) {
        Long id = aiChatTitle.getId();
        if (Objects.isNull(id)) {
            throw new ServiceException("id为空，无法修改！");
        }
        aiChatTitleMapper.updateById(aiChatTitle);
    }

    @Override
    public void save(AiChatTitle aiChatTitle) {
        aiChatTitleMapper.insert(aiChatTitle);
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        if (Objects.isNull(conversationId)) {
            throw new ServiceException("会话Id为空！");
        }
        LambdaUpdateWrapper<AiChatTitle> wrapper = Wrappers.lambdaUpdate(AiChatTitle.class)
                .eq(AiChatTitle::getConversationId, conversationId);
        aiChatTitleMapper.delete(wrapper);
        messageWindowChatMemory.clear(conversationId.toString());
    }

    @Override
    public AiChatTitle listByConversationId(Long conversationId) {
        if (Objects.isNull(conversationId)) {
            throw new ServiceException("对话id为空！");
        }
        return aiChatTitleMapper.selectOne(Wrappers.lambdaQuery(AiChatTitle.class)
                .eq(AiChatTitle::getConversationId, conversationId));
    }

    @Override
    public void generateTitle(String userInput, Long conversationId) {
        String content = chatClient.prompt().user(userInput).call().content();
        AiChatTitle aiChatTitle = new AiChatTitle();
        aiChatTitle.setTitle(content);
        aiChatTitle.setConversationId(conversationId);
        aiChatTitle.setUserId(SecurityUtils.getUserId());
        save(aiChatTitle);
    }
}
