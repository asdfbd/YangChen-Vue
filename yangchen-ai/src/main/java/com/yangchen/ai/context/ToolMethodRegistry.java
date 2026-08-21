package com.yangchen.ai.context;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yangchen.common.annotation.ToolConfirm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ToolMethodRegistry implements ApplicationContextAware, SmartInitializingSingleton {
    private final Map<String, Method> toolMethods = new ConcurrentHashMap<>();
    private final Map<String, Method> toolConfirmMethods = new ConcurrentHashMap<>();
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = null;
            try {
                bean = context.getBean(beanName);
            } catch (BeansException e) {
                log.error("bean实例化异常", e);
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Method[] publicMethods = ReflectUtil.getPublicMethods(targetClass);
            for (Method method : publicMethods) {
                Tool toolAnnotation = AnnotationUtil.getAnnotation(method, Tool.class);
                if (Objects.isNull(toolAnnotation)) {
                    continue;
                }
                String name = StringUtils.isNoneBlank(toolAnnotation.name()) ? toolAnnotation.name() : method.getName();
                if (!AnnotationUtil.hasAnnotation(method, ToolConfirm.class)) {
                    toolMethods.put(name, method);
                    continue;
                }
                toolConfirmMethods.put(name, method);
            }
        }
    }

    public Set<String> getAllToolName() {
        Set<String> sets = new HashSet<>();
        sets.addAll(toolConfirmMethods.keySet());
        sets.addAll(toolMethods.keySet());
        return sets;
    }

    public Set<String> getToolConfirmNames() {
        return toolConfirmMethods.keySet();
    }

    public Set<String> getToolNames() {
        return toolMethods.keySet();
    }

    public Map<String, Method> getToolMethods() {
        return toolMethods;
    }

    public Map<String, Method> getToolConfirmMethods() {
        return toolConfirmMethods;
    }
}
