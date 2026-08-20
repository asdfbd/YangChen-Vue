package com.yangchen.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
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

    // ==================== 序列化 ====================

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
     * 对象转格式化（带缩进）JSON 字符串
     *
     * @param obj 对象
     * @return 格式化 JSON 字符串
     */
    public static String toJsonStringPretty(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
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

    // ==================== 反序列化 ====================

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
     * JSON 字符串转指定类型对象（兼容 Fastjson2 JSON.parseObject(String, Class) 语义）
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @return 目标类型对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        return toBean(json, clazz);
    }

    /**
     * JSON 字符串转指定类型对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标类型对象
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    /**
     * JSON 字符串转指定泛型对象（用于 List&lt;T&gt;、Map&lt;String,T&gt; 等复杂类型）
     *
     * @param json         JSON 字符串
     * @param typeReference 泛型类型
     * @param <T>          目标类型
     * @return 目标类型对象
     */
    public static <T> T toBean(String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    /**
     * JSON 数组字符串转 List
     *
     * @param json  JSON 字符串
     * @param clazz 元素类型
     * @param <T>   元素类型
     * @return List
     */
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    /**
     * JSON 数组字符串转 List&lt;Object&gt;
     *
     * @param json JSON 字符串
     * @return List
     */
    public static List<Object> parseArray(String json) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    // ==================== 对象互转 ====================

    /**
     * 对象转指定类型（Map 转 Bean、Bean 转 Bean 等）
     *
     * @param source 源对象
     * @param clazz  目标类型
     * @param <T>    目标类型
     * @return 目标类型对象
     */
    public static <T> T convert(Object source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(source, clazz);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }

    /**
     * 对象转指定泛型类型
     *
     * @param source        源对象
     * @param typeReference 泛型类型
     * @param <T>           目标类型
     * @return 目标类型对象
     */
    public static <T> T convert(Object source, TypeReference<T> typeReference) {
        if (source == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(source, typeReference);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }

    /**
     * Map 转指定类型对象
     *
     * @param map   Map
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标类型对象
     */
    public static <T> T toBean(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }

    // ==================== Map 取值 ====================

    /**
     * 从 Map 中取字符串值（兼容 Fastjson2 JSONObject.getString 语义）
     */
    public static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 从 Map 中取字符串值，为空时返回默认值
     */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value == null ? defaultValue : value;
    }

    /**
     * 从 Map 中取布尔值（兼容 Fastjson2 JSONObject.getBooleanValue 语义）
     */
    public static boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 从 Map 中取布尔值，为空时返回默认值
     */
    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return getBoolean(map, key);
    }

    /**
     * 从 Map 中取 int 值（兼容 Fastjson2 JSONObject.getIntValue 语义）
     */
    public static int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    /**
     * 从 Map 中取 Integer 值，不存在时返回 null（兼容 Fastjson2 JSONObject.getInteger 语义）
     */
    public static Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    /**
     * 从 Map 中取 int 值，为空时返回默认值
     */
    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return getInt(map, key);
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

    /**
     * 从 Map 中取 long 值，为空时返回默认值
     */
    public static long getLong(Map<String, Object> map, String key, long defaultValue) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return getLong(map, key);
    }

    /**
     * 从 Map 中取 double 值（兼容 Fastjson2 JSONObject.getDoubleValue 语义）
     */
    public static double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return value == null ? 0D : Double.parseDouble(String.valueOf(value));
    }

    /**
     * 从 Map 中取 double 值，为空时返回默认值
     */
    public static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return getDouble(map, key);
    }

    // ==================== 嵌套结构取值 ====================

    /**
     * 从 Map 中取嵌套对象（值为嵌套 Map 或 JSON 对象字符串时返回 Map，否则返回 null）
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value instanceof String) {
            return parseObject((String) value);
        }
        return null;
    }

    /**
     * 从 Map 中取 JSON 数组（值为 List 或 JSON 数组字符串时返回 List，否则返回 null）
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return (List<Object>) value;
        }
        if (value instanceof String) {
            return parseArray((String) value);
        }
        return null;
    }

    /**
     * 从 Map 中取指定元素类型的 JSON 数组
     */
    public static <T> List<T> getList(Map<String, Object> map, String key, Class<T> clazz) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List || value instanceof String) {
            return MAPPER.convertValue(value, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        }
        return null;
    }

    /**
     * 判断 Map 是否包含指定 key 且值非 null
     */
    public static boolean containsKey(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) && map.get(key) != null;
    }

    // ==================== JSON 校验 ====================

    /**
     * 判断字符串是否为合法 JSON
     *
     * @param json JSON 字符串
     * @return true 表示合法
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 JSON 对象（以 { 开头）
     */
    public static boolean isJsonObject(String json) {
        if (!isValidJson(json)) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return node != null && node.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 JSON 数组（以 [ 开头）
     */
    public static boolean isJsonArray(String json) {
        if (!isValidJson(json)) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return node != null && node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
