package com.yangchen.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Vben 路由元信息。
 *
 * /getRouters 直接输出前端路由协议，前端不再承担 RuoYi 字段转换。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaVo {
    private String icon;

    private Boolean hideInMenu;

    private Boolean keepAlive;

    private String link;

    private Integer order;

    private Map<String, Object> query;

    private String title;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getHideInMenu() {
        return hideInMenu;
    }

    public void setHideInMenu(Boolean hideInMenu) {
        this.hideInMenu = hideInMenu;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Map<String, Object> getQuery() {
        return query;
    }

    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
