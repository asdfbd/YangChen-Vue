package com.yangchen.common.utils.html;

/**
 * HTML 工具类
 *
 * @author yangchen
 */
public class EscapeUtil {
    /**
     * 清除所有HTML标签，但是不删除标签内的内容
     *
     * @param content 文本
     * @return 清除标签后的文本
     */
    public static String clean(String content) {
        return new HTMLFilter().filter(content);
    }
}
