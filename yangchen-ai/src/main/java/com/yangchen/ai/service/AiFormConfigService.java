package com.yangchen.ai.service;

import com.yangchen.ai.entity.AiFormConfig;

import java.util.List;

/**
 * ai单配置Service接口
 *
 * @author yangchen
 * @date 2026-08-21
 */
public interface AiFormConfigService {
    /**
     * 查询ai单配置
     *
     * @param id ai单配置主键
     * @return ai单配置
     */
    public AiFormConfig selectAiFormConfigById(Long id);

    /**
     * 查询ai单配置列表
     *
     * @param aiFormConfig ai单配置
     * @return ai单配置集合
     */
    public List<AiFormConfig> selectAiFormConfigList(AiFormConfig aiFormConfig);

    /**
     * 新增ai单配置
     *
     * @param aiFormConfig ai单配置
     * @return 结果
     */
    public int insertAiFormConfig(AiFormConfig aiFormConfig);

    /**
     * 修改ai单配置
     *
     * @param aiFormConfig ai单配置
     * @return 结果
     */
    public int updateAiFormConfig(AiFormConfig aiFormConfig);

    /**
     * 批量删除ai单配置
     *
     * @param ids 需要删除的ai单配置主键集合
     * @return 结果
     */
    public int deleteAiFormConfigByIds(Long[] ids);

    /**
     * 删除ai单配置信息
     *
     * @param id ai单配置主键
     * @return 结果
     */
    public int deleteAiFormConfigById(Long id);

    /**
     * 批量新增或修改ai单配置（主键存在则修改，不存在则新增）
     *
     * @param list ai单配置列表
     * @return 结果
     */
    public int batchInsertOrUpdate(List<AiFormConfig> list);
}
