package com.yangchen.ai.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * 大模型调用tool的方法描述
 */
@Data
public class ToolConfirmDesc {

    private String name;

    private String desc;

    @JsonIgnore
    private Method method;
}
