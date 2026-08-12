package com.yangchen.framework.security.handle;

import cn.hutool.core.util.ObjectUtil;
import com.yangchen.common.constant.Constants;
import com.yangchen.common.core.domain.AjaxResult;
import com.yangchen.common.core.domain.model.LoginUser;
import com.yangchen.common.utils.JsonUtils;
import com.yangchen.common.utils.MessageUtils;
import com.yangchen.common.utils.ServletUtils;
import com.yangchen.framework.manager.AsyncManager;
import com.yangchen.framework.manager.factory.AsyncFactory;
import com.yangchen.framework.web.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

/**
 * 自定义退出处理类 返回成功
 *
 * @author yangchen
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {
    @Autowired
    private TokenService tokenService;

    /**
     * 退出处理
     *
     * @return
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (ObjectUtil.isNotNull(loginUser)) {
            String userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
        }
        ServletUtils.renderString(response, JsonUtils.toJsonString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
