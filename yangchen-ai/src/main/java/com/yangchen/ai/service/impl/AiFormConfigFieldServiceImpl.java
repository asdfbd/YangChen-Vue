package com.yangchen.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.yangchen.ai.entity.AiFormConfigField;
import com.yangchen.ai.mapper.AiFormConfigFieldMapper;
import com.yangchen.ai.service.AiFormConfigFieldService;
import com.yangchen.common.utils.DateUtils;
import com.yangchen.common.utils.SecurityUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * AI动态单-单项配置Service业务层处理
 *
 * @author yangchen
 * @date 2026-08-21
 */
@Service
public class AiFormConfigFieldServiceImpl implements AiFormConfigFieldService {
    @Autowired
    private AiFormConfigFieldMapper aiFormConfigFieldMapper;

    /**
     * 查询AI动态单-单项配置
     *
     * @param id AI动态单-单项配置主键
     * @return AI动态单-单项配置
     */
    @Override
    public AiFormConfigField selectAiFormConfigFieldById(Long id) {
        AiFormConfigField aiFormConfigField = aiFormConfigFieldMapper.selectById(id);
        return aiFormConfigField;
    }

    /**
     * 查询AI动态单-单项配置列表
     *
     * @param aiFormConfigField AI动态单-单项配置
     * @return AI动态单-单项配置集合
     */
    @Override
    public List<AiFormConfigField> selectAiFormConfigFieldList(AiFormConfigField aiFormConfigField) {
        LambdaQueryWrapper<AiFormConfigField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(aiFormConfigField.getFormId() != null, AiFormConfigField::
                getFormId, aiFormConfigField.getFormId());
        queryWrapper.like(aiFormConfigField.getFieldName() != null && StrUtil.isNotBlank(aiFormConfigField.getFieldName()), AiFormConfigField::
                getFieldName, aiFormConfigField.getFieldName());
        queryWrapper.eq(aiFormConfigField.getFieldLabel() != null && StrUtil.isNotBlank(aiFormConfigField.getFieldLabel()), AiFormConfigField::
                getFieldLabel, aiFormConfigField.getFieldLabel());
        queryWrapper.eq(aiFormConfigField.getFieldType() != null && StrUtil.isNotBlank(aiFormConfigField.getFieldType()), AiFormConfigField::
                getFieldType, aiFormConfigField.getFieldType());
        queryWrapper.eq(aiFormConfigField.getRequiredFlag() != null && StrUtil.isNotBlank(aiFormConfigField.getRequiredFlag()), AiFormConfigField::
                getRequiredFlag, aiFormConfigField.getRequiredFlag());
        queryWrapper.eq(aiFormConfigField.getDefaultValue() != null && StrUtil.isNotBlank(aiFormConfigField.getDefaultValue()), AiFormConfigField::
                getDefaultValue, aiFormConfigField.getDefaultValue());
        queryWrapper.eq(aiFormConfigField.getOptionSource() != null && StrUtil.isNotBlank(aiFormConfigField.getOptionSource()), AiFormConfigField::
                getOptionSource, aiFormConfigField.getOptionSource());
        queryWrapper.eq(aiFormConfigField.getDictType() != null && StrUtil.isNotBlank(aiFormConfigField.getDictType()), AiFormConfigField::
                getDictType, aiFormConfigField.getDictType());
        queryWrapper.eq(aiFormConfigField.getOptionTool() != null && StrUtil.isNotBlank(aiFormConfigField.getOptionTool()), AiFormConfigField::
                getOptionTool, aiFormConfigField.getOptionTool());
        return aiFormConfigFieldMapper.selectList(queryWrapper);
    }

    /**
     * 新增AI动态单-单项配置
     *
     * @param aiFormConfigField AI动态单-单项配置
     * @return 结果
     */
    @Override
    public int insertAiFormConfigField(AiFormConfigField aiFormConfigField) {
        aiFormConfigField.setCreateTime(DateUtils.getNowDate());
        if (aiFormConfigField.getCreateBy() == null || aiFormConfigField.getCreateBy().isBlank()) {
            aiFormConfigField.setCreateBy(SecurityUtils.getUsername());
            aiFormConfigField.setUpdateBy(SecurityUtils.getUsername());
        }
        return aiFormConfigFieldMapper.insert(aiFormConfigField);
    }

    /**
     * 修改AI动态单-单项配置
     *
     * @param aiFormConfigField AI动态单-单项配置
     * @return 结果
     */
    @Override
    public int updateAiFormConfigField(AiFormConfigField aiFormConfigField) {
        aiFormConfigField.setUpdateTime(DateUtils.getNowDate());
        if (aiFormConfigField.getUpdateBy() == null || aiFormConfigField.getUpdateBy().isBlank()) {
            aiFormConfigField.setUpdateBy(SecurityUtils.getUsername());
        }
        return aiFormConfigFieldMapper.updateById(aiFormConfigField);
    }

    /**
     * 批量删除AI动态单-单项配置
     *
     * @param ids 需要删除的AI动态单-单项配置主键
     * @return 结果
     */
    @Override
    public int deleteAiFormConfigFieldByIds(Long[] ids) {
        return aiFormConfigFieldMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除AI动态单-单项配置信息
     *
     * @param id AI动态单-单项配置主键
     * @return 结果
     */
    @Override
    public int deleteAiFormConfigFieldById(Long id) {
        return aiFormConfigFieldMapper.deleteById(id);
    }

    /**
     * 批量新增或修改AI动态单-单项配置（主键存在则修改，不存在则新增；ExecutorType.BATCH 分批执行）
     *
     * @param list AI动态单-单项配置列表
     * @return 结果
     */
    @Override
    public int batchInsertOrUpdate(List<AiFormConfigField> list) {
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        SqlSessionFactory sqlSessionFactory = SqlHelper.sqlSessionFactory(AiFormConfigField.class);
        int batchSize = 1000;
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            AiFormConfigFieldMapper batchMapper = session.getMapper(AiFormConfigFieldMapper.class);
            for (int i = 0; i < list.size(); i++) {
                AiFormConfigField item = list.get(i);
                if (item.getId() != null && batchMapper.selectById(item.getId()) != null) {
                    item.setUpdateTime(now);
                    if (item.getUpdateBy() == null || item.getUpdateBy().isBlank()) {
                        item.setUpdateBy(SecurityUtils.getUsername());
                    }
                    batchMapper.updateById(item);
                } else {
                    item.setCreateTime(now);
                    item.setUpdateTime(now);
                    if (item.getCreateBy() == null || item.getCreateBy().isBlank()) {
                        item.setCreateBy(SecurityUtils.getUsername());
                        item.setUpdateBy(SecurityUtils.getUsername());
                    }
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
}
