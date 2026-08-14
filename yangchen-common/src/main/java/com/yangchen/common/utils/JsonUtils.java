package com.yangchen.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Jackson JSON 工具类（统一使用 Jackson 序列化，替代 Fastjson2）
 *
 * @author yangchen
 */
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 对象转 JSON 字符串，并排除指定顶层属性
     *
     * @param obj               对象
     * @param excludeProperties 需要排除的属性名
     * @return JSON 字符串
     */
    public static String toJsonStringExclude(Object obj, String... excludeProperties) {
        if (obj == null) {
            return null;
        }
        Map<String, Object> map;
        try {
            map = MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException e) {
            // 标量值（Number/String/Boolean 等）无法转换为 Map，直接序列化。
            // 例如操作日志记录 DELETE /menu/{menuId} 时，入参是 Long/Integer。
            return toJsonString(obj);
        }
        if (excludeProperties != null) {
            for (String name : excludeProperties) {
                map.remove(name);
            }
        }
        return toJsonString(map);
    }

    /**
     * JSON 字符串转 Map
     *
     * @param json JSON 字符串
     * @return Map
     */
    public static Map<String, Object> parseObject(String json) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    /**
     * 从 Map 中取字符串值（兼容 Fastjson2 JSONObject.getString 语义）
     */
    public static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 从 Map 中取布尔值（兼容 Fastjson2 JSONObject.getBoolean/getBooleanValue 语义）
     */
    public static boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 从 Map 中取 long 值（兼容 Fastjson2 JSONObject.getLongValue 语义）
     */
    public static long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }
}
