package com.yangchen.ai.service;

import com.yangchen.ai.entity.AIChatContent;

import java.util.List;

/**
 * AI对话内容Service接口
 *
 * @author yangchen
 * @date 2026-08-18
 */
public interface AIChatContentService {
    List<AIChatContent> listAll();

    List<AIChatContent> listByConversationId(Long conversationId, boolean toolFlag);

    /**
     * 批量新增或修改AI对话内容（主键存在则修改，不存在则新增）
     *
     * @param list AI对话内容列表
     * @return 结果
     */
    public int batchInsertOrUpdate(List<AIChatContent> list);

    int deleteByConversationId(Long conversationId);

}
