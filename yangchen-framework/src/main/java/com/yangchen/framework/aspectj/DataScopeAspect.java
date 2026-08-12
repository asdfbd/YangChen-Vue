package com.yangchen.framework.aspectj;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yangchen.common.annotation.DataScope;
import com.yangchen.common.constant.Constants;
import com.yangchen.common.constant.UserConstants;
import com.yangchen.common.core.domain.DataScopeParam;
import com.yangchen.common.core.domain.model.LoginUser;
import com.yangchen.common.core.entity.SysRole;
import com.yangchen.common.core.entity.SysUser;
import com.yangchen.common.core.text.Convert;
import com.yangchen.common.utils.SecurityUtils;
import com.yangchen.framework.security.context.PermissionContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据过滤处理
 *
 * @author yangchen
 */
@Aspect
@Component
public class DataScopeAspect {
    /**
     * 数据权限过滤关键字
     */
    public static final String DATA_SCOPE = "dataScope";

    /**
     * 数据范围过滤
     *
     * @param joinPoint  切点
     * @param user       用户
     * @param deptAlias  部门别名
     * @param userAlias  用户别名
     * @param permission 权限字符
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String userAlias, String deptAlias, String userField, String deptField, String permission) {
        StringBuilder sqlString = new StringBuilder();
        List<String> conditions = new ArrayList<String>();
        List<String> scopeCustomIds = new ArrayList<String>();
        user.getRoles().forEach(role -> {
            if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(role.getDataScope()) && StrUtil.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && (StrUtil.isEmpty(permission) || role.getPermissions().stream().anyMatch(value -> cn.hutool.core.util.ArrayUtil.contains(Convert.toStrArray(permission), value)))) {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });

        for (SysRole role : user.getRoles()) {
            String dataScope = role.getDataScope();
            if (conditions.contains(dataScope) || StrUtil.equals(role.getStatus(), UserConstants.ROLE_DISABLE)) {
                continue;
            }
            if (StrUtil.isNotEmpty(permission) && !role.getPermissions().stream().anyMatch(value -> cn.hutool.core.util.ArrayUtil.contains(Convert.toStrArray(permission), value))) {
                continue;
            }
            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope)) {
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            } else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope)) {
                if (scopeCustomIds.size() > 1) {
                    // 多个自定数据权限使用in查询，避免多次拼接。
                    sqlString.append(StrUtil.format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id in ({}) ) ", deptAlias, deptField, String.join(",", scopeCustomIds)));
                } else {
                    sqlString.append(StrUtil.format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", deptAlias, deptField, role.getRoleId()));
                }
            } else if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope)) {
                sqlString.append(StrUtil.format(" OR {}.{} = {} ", deptAlias, deptField, user.getDeptId()));
            } else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                sqlString.append(StrUtil.format(" OR {}.{} IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or position(',' || {} || ',' in ',' || ancestors || ',') > 0 )", deptAlias, deptField, user.getDeptId(), user.getDeptId()));
            } else if (Constants.Dept.DATA_SCOPE_SELF.equals(dataScope)) {
                if (StrUtil.isNotBlank(userAlias)) {
                    sqlString.append(StrUtil.format(" OR {}.{} = {} ", userAlias, userField, user.getUserId()));
                } else {
                    // 数据权限为仅本人且没有userAlias别名不查询任何数据
                    sqlString.append(StrUtil.format(" OR {}.{} = 0 ", deptAlias, deptField));
                }
            }
            conditions.add(dataScope);
        }

        // 角色都不包含传递过来的权限字符，这个时候sqlString也会为空，所以要限制一下,不查询任何数据
        if (cn.hutool.core.collection.CollUtil.isEmpty(conditions)) {
            sqlString.append(StrUtil.format(" OR {}.{} = 0 ", deptAlias, deptField));
        }

        if (StrUtil.isNotBlank(sqlString.toString())) {
            Object params = joinPoint.getArgs()[0];
            if (ObjectUtil.isNotNull(params) && params instanceof DataScopeParam) {
                ((DataScopeParam) params).getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable {
        clearDataScope(point);
        handleDataScope(point, controllerDataScope);
    }

    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope) {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (ObjectUtil.isNotNull(loginUser)) {
            SysUser currentUser = loginUser.getUser();
            // 如果是超级管理员，则不过滤数据
            if (ObjectUtil.isNotNull(currentUser) && !currentUser.isAdmin()) {
                String permission = org.apache.commons.lang3.StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.userAlias(), controllerDataScope.deptAlias(), controllerDataScope.userField(), controllerDataScope.deptField(), permission);
            }
        }
    }

    /**
     * 拼接权限sql前先清空params.dataScope参数防止注入
     */
    private void clearDataScope(final JoinPoint joinPoint) {
        Object params = joinPoint.getArgs()[0];
        if (ObjectUtil.isNotNull(params) && params instanceof DataScopeParam) {
            ((DataScopeParam) params).getParams().put(DATA_SCOPE, "");
        }
    }
}
