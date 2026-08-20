package com.yangchen.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.yangchen.ai.entity.AIChatContent;
import com.yangchen.ai.mapper.AIChatContentMapper;
import com.yangchen.ai.service.AIChatContentService;
import com.yangchen.common.exception.ServiceException;
import com.yangchen.common.utils.DateUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * AI对话内容Service业务层处理
 *
 * @author yangchen
 * @date 2026-08-18
 */
@Service
@Transactional
public class AIChatContentServiceImpl implements AIChatContentService {
    @Autowired
    private AIChatContentMapper aIChatContentMapper;


    @Override
    public List<AIChatContent> listAll() {
        return aIChatContentMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public List<AIChatContent> listByConversationId(Long conversationId, boolean toolFlag) {
        if (Objects.isNull(conversationId)) {
            throw new ServiceException("对话Id为空！");
        }
        LambdaQueryWrapper<AIChatContent> wrapper = Wrappers.lambdaQuery(AIChatContent.class)
                .eq(AIChatContent::getConversationId, conversationId)
                .orderByAsc(AIChatContent::getId);
        if (!toolFlag) {
            wrapper.isNotNull(AIChatContent::getContent);
        }
        return aIChatContentMapper.selectList(wrapper);
    }

    /**
     * 批量新增或修改AI对话内容（主键存在则修改，不存在则新增；ExecutorType.BATCH 分批执行）
     *
     * @param list AI对话内容列表
     * @return 结果
     */
    @Override
    public int batchInsertOrUpdate(List<AIChatContent> list) {
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        SqlSessionFactory sqlSessionFactory = SqlHelper.sqlSessionFactory(AIChatContent.class);
        int batchSize = 1000;
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            AIChatContentMapper batchMapper = session.getMapper(AIChatContentMapper.class);
            for (int i = 0; i < list.size(); i++) {
                AIChatContent item = list.get(i);
                if (item.getId() != null && batchMapper.selectById(item.getId()) != null) {
                    batchMapper.updateById(item);
                } else {
                    item.setCreateTime(now);
                    batchMapper.insert(item);
                }
                if ((i + 1) % batchSize == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
        }
        return list.size();
    }

    @Override
    public int deleteByConversationId(Long conversationId) {
        LambdaQueryWrapper<AIChatContent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIChatContent::getConversationId, conversationId);
        aIChatContentMapper.delete(queryWrapper);
        return 0;
    }
}
