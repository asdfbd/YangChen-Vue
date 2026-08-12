package com.yangchen.common.utils;

import com.yangchen.common.constant.CacheConstants;
import com.yangchen.common.core.entity.SysDictData;
import com.yangchen.common.core.redis.RedisCache;
import com.yangchen.common.utils.spring.SpringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典工具类
 *
 * @author yangchen
 */
public class DictUtils {
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";

    /**
     * 设置字典缓存
     *
     * @param key       参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<SysDictData> dictDatas) {
        SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas);
    }

    /**
     * 获取字典缓存
     *
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    @SuppressWarnings("unchecked")
    public static List<SysDictData> getDictCache(String key) {
        Object arrayCache = SpringUtils.getBean(RedisCache.class).getCacheObject(getCacheKey(key));
        if (cn.hutool.core.util.ObjectUtil.isNotNull(arrayCache) && arrayCache instanceof List) {
            return (List<SysDictData>) arrayCache;
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue) {
        if (cn.hutool.core.util.StrUtil.isEmpty(dictValue)) {
            return cn.hutool.core.util.StrUtil.EMPTY;
        }
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel) {
        if (cn.hutool.core.util.StrUtil.isEmpty(dictLabel)) {
            return cn.hutool.core.util.StrUtil.EMPTY;
        }
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator) {
        List<SysDictData> datas = getDictCache(dictType);
        if (cn.hutool.core.util.ObjectUtil.isNull(datas) || cn.hutool.core.util.StrUtil.isEmpty(dictValue)) {
            return cn.hutool.core.util.StrUtil.EMPTY;
        }
        Map<String, String> dictMap = datas.stream().collect(HashMap::new, (map, dict) -> map.put(dict.getDictValue(), dict.getDictLabel()), Map::putAll);
        if (!cn.hutool.core.util.StrUtil.contains(dictValue, separator)) {
            return dictMap.getOrDefault(dictValue, cn.hutool.core.util.StrUtil.EMPTY);
        }
        StringBuilder labelBuilder = new StringBuilder();
        for (String seperatedValue : dictValue.split(separator)) {
            if (dictMap.containsKey(seperatedValue)) {
                labelBuilder.append(dictMap.get(seperatedValue)).append(separator);
            }
        }
        return org.apache.commons.lang3.StringUtils.removeEnd(labelBuilder.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator) {
        List<SysDictData> datas = getDictCache(dictType);
        if (cn.hutool.core.util.ObjectUtil.isNull(datas) || cn.hutool.core.util.StrUtil.isEmpty(dictLabel)) {
            return cn.hutool.core.util.StrUtil.EMPTY;
        }
        Map<String, String> dictMap = datas.stream().collect(HashMap::new, (map, dict) -> map.put(dict.getDictLabel(), dict.getDictValue()), Map::putAll);
        if (!cn.hutool.core.util.StrUtil.contains(dictLabel, separator)) {
            return dictMap.getOrDefault(dictLabel, cn.hutool.core.util.StrUtil.EMPTY);
        }
        StringBuilder valueBuilder = new StringBuilder();
        for (String seperatedValue : dictLabel.split(separator)) {
            if (dictMap.containsKey(seperatedValue)) {
                valueBuilder.append(dictMap.get(seperatedValue)).append(separator);
            }
        }
        return org.apache.commons.lang3.StringUtils.removeEnd(valueBuilder.toString(), separator);
    }

    /**
     * 根据字典类型获取字典所有值
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictValues(String dictType) {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (cn.hutool.core.util.ObjectUtil.isNull(datas)) {
            return cn.hutool.core.util.StrUtil.EMPTY;
        }
        for (SysDictData dict : datas) {
            propertyString.append(dict.getDictValue()).append(SEPARATOR);
        }
        return org.apache.commons.lang3.StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    /**
     * 根据字典类型获取字典所有标签
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictLabels(String dictType) {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (cn.hutool.core.util.ObjectUtil.isNull(datas)) {
            return cn.hutool.core.util.StrUtil.EMPTY;
        }
        for (SysDictData dict : datas) {
            propertyString.append(dict.getDictLabel()).append(SEPARATOR);
        }
        return org.apache.commons.lang3.StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    /**
     * 删除指定字典缓存
     *
     * @param key 字典键
     */
    public static void removeDictCache(String key) {
        SpringUtils.getBean(RedisCache.class).deleteObject(getCacheKey(key));
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache() {
        Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(CacheConstants.SYS_DICT_KEY + "*");
        SpringUtils.getBean(RedisCache.class).deleteObject(keys);
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey) {
        return CacheConstants.SYS_DICT_KEY + configKey;
    }
}
