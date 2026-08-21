package com.yangchen.ai.interceptor;

import cn.hutool.json.JSONUtil;
import com.yangchen.ai.context.AIContext;
import com.yangchen.common.core.domain.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * AI拦截器
 */
@Component
public class AIInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader(AIContext.DEFAULT_HEADER_CONVERSATION_ID);
        if (StringUtils.isBlank(header)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(R.error("请设置会话ID")));
            return false;
        }
        AIContext.setConversationId(header);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // 必须清理，防止线程池复用内存泄漏
        AIContext.clear();
    }

    @Bean
    public MappedInterceptor aiMappedInterceptor() {
        return new MappedInterceptor(new String[]{"/ai/chat/**"},
                new String[]{"/ai/chat/generateTitle", "/ai/chat/completeContent", "/ai/chat/generateConversationId"},
                this);
    }
}
