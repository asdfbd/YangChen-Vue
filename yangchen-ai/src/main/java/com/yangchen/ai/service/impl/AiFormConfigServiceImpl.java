package com.yangchen.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.yangchen.ai.entity.AiFormConfig;
import com.yangchen.ai.mapper.AiFormConfigMapper;
import com.yangchen.ai.service.AiFormConfigService;
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
 * ai单配置Service业务层处理
 *
 * @author yangchen
 * @date 2026-08-21
 */
@Service
public class AiFormConfigServiceImpl implements AiFormConfigService {
    @Autowired
    private AiFormConfigMapper aiFormConfigMapper;

    /**
     * 查询ai单配置
     *
     * @param id ai单配置主键
     * @return ai单配置
     */
    @Override
    public AiFormConfig selectAiFormConfigById(Long id) {
        AiFormConfig aiFormConfig = aiFormConfigMapper.selectById(id);
        return aiFormConfig;
    }

    /**
     * 查询ai单配置列表
     *
     * @param aiFormConfig ai单配置
     * @return ai单配置集合
     */
    @Override
    public List<AiFormConfig> selectAiFormConfigList(AiFormConfig aiFormConfig) {
        LambdaQueryWrapper<AiFormConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(aiFormConfig.getFormKey() != null && StrUtil.isNotBlank(aiFormConfig.getFormKey()), AiFormConfig::
                getFormKey, aiFormConfig.getFormKey());
        queryWrapper.like(aiFormConfig.getFormName() != null && StrUtil.isNotBlank(aiFormConfig.getFormName()), AiFormConfig::
                getFormName, aiFormConfig.getFormName());
        queryWrapper.like(aiFormConfig.getToolName() != null && StrUtil.isNotBlank(aiFormConfig.getToolName()), AiFormConfig::
                getToolName, aiFormConfig.getToolName());
        queryWrapper.eq(aiFormConfig.getStatus() != null && StrUtil.isNotBlank(aiFormConfig.getStatus()), AiFormConfig::
                getStatus, aiFormConfig.getStatus());
        return aiFormConfigMapper.selectList(queryWrapper);
    }

    /**
     * 新增ai单配置
     *
     * @param aiFormConfig ai单配置
     * @return 结果
     */
    @Override
    public int insertAiFormConfig(AiFormConfig aiFormConfig) {
        aiFormConfig.setCreateTime(DateUtils.getNowDate());
        if (aiFormConfig.getCreateBy() == null || aiFormConfig.getCreateBy().isBlank()) {
            aiFormConfig.setCreateBy(SecurityUtils.getUsername());
            aiFormConfig.setUpdateBy(SecurityUtils.getUsername());
        }
        return aiFormConfigMapper.insert(aiFormConfig);
    }

    /**
     * 修改ai单配置
     *
     * @param aiFormConfig ai单配置
     * @return 结果
     */
    @Override
    public int updateAiFormConfig(AiFormConfig aiFormConfig) {
        aiFormConfig.setUpdateTime(DateUtils.getNowDate());
        if (aiFormConfig.getUpdateBy() == null || aiFormConfig.getUpdateBy().isBlank()) {
            aiFormConfig.setUpdateBy(SecurityUtils.getUsername());
        }
        return aiFormConfigMapper.updateById(aiFormConfig);
    }

    /**
     * 批量删除ai单配置
     *
     * @param ids 需要删除的ai单配置主键
     * @return 结果
     */
    @Override
    public int deleteAiFormConfigByIds(Long[] ids) {
        return aiFormConfigMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除ai单配置信息
     *
     * @param id ai单配置主键
     * @return 结果
     */
    @Override
    public int deleteAiFormConfigById(Long id) {
        return aiFormConfigMapper.deleteById(id);
    }

    /**
     * 批量新增或修改ai单配置（主键存在则修改，不存在则新增；ExecutorType.BATCH 分批执行）
     *
     * @param list ai单配置列表
     * @return 结果
     */
    @Override
    public int batchInsertOrUpdate(List<AiFormConfig> list) {
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        SqlSessionFactory sqlSessionFactory = SqlHelper.sqlSessionFactory(AiFormConfig.class);
        int batchSize = 1000;
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            AiFormConfigMapper batchMapper = session.getMapper(AiFormConfigMapper.class);
            for (int i = 0; i < list.size(); i++) {
                AiFormConfig item = list.get(i);
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
