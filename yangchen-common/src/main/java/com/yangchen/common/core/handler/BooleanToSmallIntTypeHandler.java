package com.yangchen.common.core.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 将 Java boolean 与 PostgreSQL smallint(0/1) 互转的全局 TypeHandler。
 * 用于 sys_role.menu_check_strictly / dept_check_strictly 这类 smallint 列。
 * 已在 mybatis-config.xml 全局注册，所有 boolean 类型字段自动使用。
 *
 * @author yangchen
 */
@MappedTypes({boolean.class, Boolean.class})
public class BooleanToSmallIntTypeHandler extends BaseTypeHandler<Boolean> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setShort(i, (short) (parameter ? 1 : 0));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toBoolean(rs.getObject(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toBoolean(rs.getObject(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toBoolean(cs.getObject(columnIndex));
    }

    /**
     * 兼容多种底层列类型：
     * - smallint/int (0/1) -> Number
     * - boolean ('t'/'f') -> Boolean
     * - varchar ('1'/'0'/'true'/'false') -> String
     */
    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String s = value.toString().trim();
        return "1".equals(s) || "t".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s);
    }
}
