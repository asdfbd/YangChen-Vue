package com.yangchen.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yangchen.common.core.entity.SysDictData;
import com.yangchen.common.utils.DictUtils;
import com.yangchen.common.utils.PageUtils;
import com.yangchen.system.mapper.SysDictDataMapper;
import com.yangchen.system.service.SysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author yangchen
 */
@Service
public class SysDictDataServiceImpl implements SysDictDataService {
    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        return PageUtils.selectPage(dictDataMapper, new LambdaQueryWrapper<SysDictData>()
                .eq(StrUtil.isNotBlank(dictData.getDictType()), SysDictData::getDictType, dictData.getDictType())
                .like(StrUtil.isNotBlank(dictData.getDictLabel()), SysDictData::getDictLabel, dictData.getDictLabel())
                .eq(StrUtil.isNotBlank(dictData.getStatus()), SysDictData::getStatus, dictData.getStatus())
                .orderByAsc(SysDictData::getDictSort));
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        SysDictData data = dictDataMapper.selectOne(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, dictType).eq(SysDictData::getDictValue, dictValue));
        return data == null ? null : data.getDictLabel();
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return dictDataMapper.selectById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes) {
        for (Long dictCode : dictCodes) {
            SysDictData data = selectDictDataById(dictCode);
            dictDataMapper.deleteById(dictCode);
            List<SysDictData> dictDatas = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getStatus, "0").eq(SysDictData::getDictType, data.getDictType()).orderByAsc(SysDictData::getDictSort));
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int insertDictData(SysDictData data) {
        int row = dictDataMapper.insert(data);
        if (row > 0) {
            List<SysDictData> dictDatas = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getStatus, "0").eq(SysDictData::getDictType, data.getDictType()).orderByAsc(SysDictData::getDictSort));
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 修改保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int updateDictData(SysDictData data) {
        int row = dictDataMapper.updateById(data);
        if (row > 0) {
            List<SysDictData> dictDatas = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getStatus, "0").eq(SysDictData::getDictType, data.getDictType()).orderByAsc(SysDictData::getDictSort));
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }
}
