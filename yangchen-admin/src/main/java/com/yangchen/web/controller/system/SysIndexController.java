package com.yangchen.web.controller.system;

import cn.hutool.core.util.StrUtil;
import com.yangchen.common.config.YangChenConfig;
import com.yangchen.common.core.domain.R;
import com.yangchen.common.core.entity.SysUser;
import com.yangchen.common.utils.SecurityUtils;
import com.yangchen.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页
 *
 * @author yangchen
 */
@RestController
@Tag(name = "首页")
public class SysIndexController {
    /**
     * 系统基础配置
     */
    @Autowired
    private YangChenConfig yangchenConfig;

    @Autowired
    private SysUserService userService;

    /**
     * 访问首页，提示语
     */
    @Operation(summary = "访问首页，提示语")
    @RequestMapping("/")
    public String index() {
        return StrUtil.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", yangchenConfig.getName(), yangchenConfig.getVersion());
    }

    /**
     * 解锁屏幕
     */
    @Operation(summary = "解锁屏幕")
    @PostMapping("/unlockscreen")
    public R unlockScreen(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (StrUtil.isEmpty(password)) {
            return R.error("密码不能为空");
        }
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) {
            return R.error("服务器超时，请重新登录");
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword())) {
            return R.error("密码错误，请重新输入");
        }

        return R.ok("解锁成功");
    }
}
