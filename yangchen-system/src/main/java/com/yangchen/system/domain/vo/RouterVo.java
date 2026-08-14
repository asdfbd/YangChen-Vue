package com.yangchen.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Vben 动态路由配置。
 *
 * component 直接使用前端页面路径或布局组件名：BasicLayout、ParentView、IFrameView。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterVo {
    private List<RouterVo> children;

    private String component;

    private MetaVo meta;

    private String name;

    private String path;

    public List<RouterVo> getChildren() {
        return children;
    }

    public void setChildren(List<RouterVo> children) {
        this.children = children;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public MetaVo getMeta() {
        return meta;
    }

    public void setMeta(MetaVo meta) {
        this.meta = meta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
