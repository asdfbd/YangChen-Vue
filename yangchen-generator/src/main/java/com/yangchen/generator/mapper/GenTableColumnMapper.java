package com.yangchen.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.generator.entity.GenTableColumn;

import java.util.List;

/**
 * 业务字段 数据层
 *
 * @author yangchen
 */
public interface GenTableColumnMapper extends BaseMapper<GenTableColumn> {
    /**
     * 根据表名称查询列信息
     *
     * @param tableName 表名称
     * @return 列信息
     */
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName);

    /**
     * 查询业务字段列表
     *
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId);

    /**
     * 新增业务字段
     *
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    default int insertGenTableColumn(GenTableColumn genTableColumn) {
        return insert(genTableColumn);
    }

    /**
     * 修改业务字段
     *
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    default int updateGenTableColumn(GenTableColumn genTableColumn) {
        return updateById(genTableColumn);
    }

    /**
     * 删除业务字段
     *
     * @param genTableColumns 列数据
     * @return 结果
     */
    public int deleteGenTableColumns(List<GenTableColumn> genTableColumns);

    /**
     * 批量删除业务字段
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    default int deleteGenTableColumnByIds(Long[] ids) {
        return deleteByIds(java.util.Arrays.asList(ids));
    }
}
