package com.yangchen.ai.service;

import com.yangchen.ai.entity.AiFormConfigField;

import java.util.List;

/**
 * AI动态单-单项配置Service接口
 *
 * @author yangchen
 * @date 2026-08-21
 */
public interface AiFormConfigFieldService {
    /**
     * 查询AI动态单-单项配置
     *
     * @param id AI动态单-单项配置主键
     * @return AI动态单-单项配置
     */
    public AiFormConfigField selectAiFormConfigFieldById(Long id);

    /**
     * 查询AI动态单-单项配置列表
     *
     * @param aiFormConfigField AI动态单-单项配置
     * @return AI动态单-单项配置集合
     */
    public List<AiFormConfigField> selectAiFormConfigFieldList(AiFormConfigField aiFormConfigField);

    /**
     * 新增AI动态单-单项配置
     *
     * @param aiFormConfigField AI动态单-单项配置
     * @return 结果
     */
    public int insertAiFormConfigField(AiFormConfigField aiFormConfigField);

    /**
     * 修改AI动态单-单项配置
     *
     * @param aiFormConfigField AI动态单-单项配置
     * @return 结果
     */
    public int updateAiFormConfigField(AiFormConfigField aiFormConfigField);

    /**
     * 批量删除AI动态单-单项配置
     *
     * @param ids 需要删除的AI动态单-单项配置主键集合
     * @return 结果
     */
    public int deleteAiFormConfigFieldByIds(Long[] ids);

    /**
     * 删除AI动态单-单项配置信息
     *
     * @param id AI动态单-单项配置主键
     * @return 结果
     */
    public int deleteAiFormConfigFieldById(Long id);

    /**
     * 批量新增或修改AI动态单-单项配置（主键存在则修改，不存在则新增）
     *
     * @param list AI动态单-单项配置列表
     * @return 结果
     */
    public int batchInsertOrUpdate(List<AiFormConfigField> list);
}
