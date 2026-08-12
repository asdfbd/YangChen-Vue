package com.yangchen.common.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yangchen.common.core.page.PageDomain;
import com.yangchen.common.core.page.TableSupport;
import com.yangchen.common.utils.sql.SqlUtil;

import java.util.List;

/**
 * 分页工具类（基于 MyBatis-Plus 分页）
 *
 * @author yangchen
 */
public class PageUtils {
    /**
     * 当前线程持有的分页对象
     */
    private static final ThreadLocal<Page<?>> PAGE_LOCAL = new ThreadLocal<>();

    /**
     * 设置请求分页数据，构建 MyBatis-Plus 分页对象并存入线程变量
     */
    public static void startPage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Page<Object> page = Page.of(pageNum, pageSize);
        applyOrderBy(page, orderBy);
        PAGE_LOCAL.set(page);
    }

    /**
     * 设置请求排序数据（追加到当前线程的分页对象，未分页时新建）
     */
    public static void startOrderBy() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Page<Object> page = (Page<Object>) PAGE_LOCAL.get();
        if (page == null) {
            page = Page.of(1, 10);
            PAGE_LOCAL.set(page);
        }
        applyOrderBy(page, orderBy);
    }

    /**
     * 获取当前线程的分页对象
     *
     * @param <T> 实体类型
     * @return 分页对象，未调用 startPage() 时为 null
     */
    @SuppressWarnings("unchecked")
    public static <T> Page<T> getPage() {
        return (Page<T>) PAGE_LOCAL.get();
    }

    /**
     * 执行单表分页查询：已启动分页时使用 selectPage，否则退化为普通 selectList
     *
     * @param mapper       MyBatis-Plus 单表 Mapper
     * @param queryWrapper 查询条件
     * @param <T>          实体类型
     * @return 记录列表
     */
    public static <T> List<T> selectPage(BaseMapper<T> mapper, Wrapper<T> queryWrapper) {
        Page<T> page = getPage();
        if (page == null) {
            return mapper.selectList(queryWrapper);
        }
        return mapper.selectPage(page, queryWrapper).getRecords();
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage() {
        PAGE_LOCAL.remove();
    }

    /**
     * 将 orderBy 字符串（形如 "create_time asc,user_id desc"，已通过 SqlUtil 过滤）应用到分页对象
     */
    private static void applyOrderBy(Page<Object> page, String orderBy) {
        if (StrUtil.isEmpty(orderBy)) {
            return;
        }
        for (String segment : orderBy.split(",")) {
            String trimmed = segment.trim();
            if (StrUtil.isEmpty(trimmed)) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            String column = parts[0];
            boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);
            page.addOrder(desc ? OrderItem.desc(column) : OrderItem.asc(column));
        }
    }
}
