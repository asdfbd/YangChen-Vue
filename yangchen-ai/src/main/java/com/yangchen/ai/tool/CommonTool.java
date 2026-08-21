package com.yangchen.ai.tool;

import com.yangchen.common.core.domain.R;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 通用只读数据库工具。
 *
 * <p>工具分成两步：模型先读取表目录/指定表结构，再根据结构生成只读 SQL 查询。
 * SQL 工具只允许单条 SELECT/WITH 语句，真正的生产安全边界仍然应由数据库只读账号保证。</p>
 */
@Service
public class CommonTool {

    private static final int MAX_TABLE_COUNT = 200;
    private static final int MAX_ROW_COUNT = 200;
    private static final int MAX_CHOICE_OPTION_COUNT = 6;
    private static final int MAX_CHOICE_DESCRIPTION_LENGTH = 120;
    private static final int MAX_CHOICE_LABEL_LENGTH = 40;
    private static final int MAX_CHOICE_QUESTION_LENGTH = 120;
    private static final int MAX_CHOICE_VALUE_LENGTH = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 10;
    private static final String DEFAULT_SCHEMA = "public";
    private static final Pattern TABLE_IDENTIFIER = Pattern.compile(
            "^(?:([A-Za-z_][A-Za-z0-9_]*)\\.)?([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern READ_ONLY_START = Pattern.compile(
            "^(select|with)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> FORBIDDEN_SQL_KEYWORDS = List.of(
            "insert", "update", "delete", "merge", "drop", "alter", "truncate",
            "create", "grant", "revoke", "comment", "call", "execute", "copy",
            "refresh", "vacuum", "set", "reset", "into", "returning", "lock",
            "pg_sleep", "dblink");
    private static final List<String> SENSITIVE_COLUMN_MARKERS = List.of(
            "password", "passwd", "pwd", "salt", "token", "secret", "credential",
            "private_key", "access_key", "authorization", "session", "cookie",
            "id_card", "idcard", "identity", "bank", "card_no", "card_number");
    private static final List<String> INTERNAL_COLUMNS = List.of(
            "id", "create_by", "create_time", "update_by", "update_time", "del_flag",
            "remark", "login_ip", "login_date", "password_update_time");

    private final JdbcTemplate jdbcTemplate;

    public CommonTool(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 获取数据库表目录或指定表结构。
     *
     * <p>tableName 为空时只返回当前 schema 下的表名、表注释和表类型；
     * 传入表名或 schema.table 时返回该表的字段、类型、可空性和字段注释。</p>
     */
    @Tool(
            name = "getDatabaseSchema",
            description = """
                    读取业务数据库的表目录或指定表结构。必须在生成业务查询 SQL 前调用。
                    1. tableName 为空：返回当前业务 schema 下可查询的表名、表注释和表类型，帮助你选择正确的表。
                    2. tableName 非空：返回指定表的完整字段结构、字段类型、是否可为空和字段注释。
                    3. 只能使用工具实际返回的表名和字段名，不要猜测数据库结构。
                    """
    )
    public R<DatabaseSchemaResult> getDatabaseSchema(
            @ToolParam(required = false, description = "表名，可为空；支持 table_name 或 schema.table") String tableName) {
        String requestedTable = tableName == null ? "" : tableName.trim();
        try {
            DatabaseSchemaResult result = jdbcTemplate.execute(
                    (ConnectionCallback<DatabaseSchemaResult>) connection -> readSchema(connection, requestedTable));
            return R.ok(result);
        } catch (DataAccessException | IllegalArgumentException exception) {
            return R.error("读取数据库表结构失败：" + safeMessage(exception));
        }
    }

    /** 执行可直接展示给用户的只读查询。 */
    @Tool(
            name = "executeReadOnlySqlDirect",
            returnDirect = true,
            description = """
                    执行可直接展示的只读查询。调用前先使用 getDatabaseSchema 获取表和字段；只能使用已读取的真实名称。
                    仅允许单条 SELECT/WITH 查询，最多返回 200 行；敏感字段和默认不展示的内部标识会由服务端自动过滤。
                    当结果可直接作为列表、表格、详情或统计展示时使用，例如当前用户基本信息、用户/部门列表、操作记录或总数。
                    仅在用户明确要直接查看数据时调用。若无法判断用户要“直接查看数据”还是要“关联分析、解释或结论”，不要调用工具，先追问用户希望的输出形式。
                    若用户要求某个用户、部门或对象的详情却没有唯一定位条件，先追问账号、名称、编号或其他筛选条件；聚合统计无需追问。
                    只选择用户需要的字段；每一个对外展示字段必须使用双引号中文别名，例如 user_name AS "账号"、dept_name AS "所属部门"，统计列使用清晰中文别名。
                    """
    )
    public R<ReadOnlyQueryResult> executeReadOnlySqlDirect(
            @ToolParam(description = "根据已读取的表结构生成的单条 SELECT/WITH 查询 SQL，不要带代码块标记") String sql) {
        return executeReadOnlyQuery(sql);
    }

    /** 执行供模型继续分析的只读查询。 */
    @Tool(
            name = "executeReadOnlySqlForAnalysis",
            description = """
                    执行供后续业务分析使用的只读查询。调用前先使用 getDatabaseSchema 获取表和字段；只能使用已读取的真实名称。
                    仅允许单条 SELECT/WITH 查询，最多返回 200 行；敏感字段和默认不展示的内部标识会由服务端自动过滤。
                    当回答需要关联多个对象、解释关系、判断权限、汇总多个结果或形成业务结论时使用，
                    例如查询某用户的角色，并说明这些角色拥有的权限。最终回复只输出业务结论，不展示查询过程。
                    仅在用户明确需要分析、解释、关联或结论时调用。若无法判断应直接展示数据还是进行分析，不要调用工具，先向用户确认期望的输出形式和关注字段。
                    若用户要求某个用户、部门或对象的详情却没有唯一定位条件，先追问账号、名称、编号或其他筛选条件；聚合统计无需追问。
                    """
    )
    public R<ReadOnlyQueryResult> executeReadOnlySqlForAnalysis(
            @ToolParam(description = "根据已读取的表结构生成的单条 SELECT/WITH 查询 SQL，不要带代码块标记") String sql) {
        return executeReadOnlyQuery(sql);
    }

    /** 在用户意图存在明确分支时，向前端下发可直接选择的澄清项。 */
    @Tool(
            name = "askUserChoice",
            returnDirect = true,
            description = """
                    向用户展示一个可直接选择的澄清问题。仅在无法确定查询对象、查询范围，或无法判断用户想“直接查看数据”还是“分析说明”时使用。
                    question 使用一句简短、面向业务用户的问题；options 必须提供 2 到 6 项，每项使用清晰中文 label，value 必须是用户选中后可直接作为下一轮消息发送的完整业务意图，description 可选。
                    调用后会由界面展示下拉选择器，用户选择会自动发送，因此不要在普通文本中重复列出选项、工具名称、表名、字段名或查询过程。
                    用户已提供唯一条件，或意图已经明确时不要调用；此工具不查询数据、不执行任何业务操作。
                    """
    )
    public R<UserChoiceResult> askUserChoice(
            @ToolParam(description = "需要用户确认的一句简短问题") String question,
            @ToolParam(description = "2 至 6 个可选项；每项包含 label、value 和可选 description") List<UserChoiceOption> options) {
        String normalizedQuestion = normalizeChoiceText(question, MAX_CHOICE_QUESTION_LENGTH);
        if (normalizedQuestion.isBlank()) {
            return R.error("请选择需要确认的业务范围");
        }

        List<UserChoiceOption> normalizedOptions = new ArrayList<>();
        Set<String> seenValues = new LinkedHashSet<>();
        if (options != null) {
            for (UserChoiceOption option : options) {
                if (option == null || normalizedOptions.size() >= MAX_CHOICE_OPTION_COUNT) {
                    continue;
                }
                String label = normalizeChoiceText(option.label(), MAX_CHOICE_LABEL_LENGTH);
                String value = normalizeChoiceText(option.value(), MAX_CHOICE_VALUE_LENGTH);
                if (value.isBlank()) {
                    value = label;
                }
                if (label.isBlank() || value.isBlank() || !seenValues.add(value)) {
                    continue;
                }
                normalizedOptions.add(new UserChoiceOption(
                        label,
                        value,
                        normalizeChoiceText(option.description(), MAX_CHOICE_DESCRIPTION_LENGTH)));
            }
        }

        if (normalizedOptions.size() < 2) {
            return R.error("请提供至少两项可选的业务范围");
        }
        return R.ok(new UserChoiceResult(
                "ui",
                "select",
                normalizedQuestion,
                "请选择一项",
                List.copyOf(normalizedOptions)));
    }

    private R<ReadOnlyQueryResult> executeReadOnlyQuery(String sql) {
        String normalizedSql = normalizeSql(sql);
        String validationMessage = validateReadOnlySql(normalizedSql);
        if (validationMessage != null) {
            return R.error(validationMessage);
        }

        String limitedSql = "SELECT * FROM (" + normalizedSql + ") AS ai_read_only_result LIMIT "
                + (MAX_ROW_COUNT + 1);
        try {
            ReadOnlyQueryResult result = jdbcTemplate.execute(
                    (ConnectionCallback<ReadOnlyQueryResult>) connection -> executeQuery(connection, limitedSql));
            return R.ok(result);
        } catch (DataAccessException | IllegalArgumentException exception) {
            // 直出工具的失败信息会显示在用户界面，不能泄漏 SQL 或数据库内部异常。
            return R.error("查询暂时无法完成，请检查筛选条件后重试");
        }
    }

    private DatabaseSchemaResult readSchema(Connection connection, String requestedTable) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String currentSchema = getCurrentSchema(connection);
        String catalog = connection.getCatalog();

        TableIdentifier identifier = parseTableIdentifier(requestedTable, currentSchema);
        List<TableSchema> tables = new ArrayList<>();
        try (ResultSet resultSet = metadata.getTables(
                catalog,
                identifier.schema(),
                identifier.tablePattern(),
                new String[]{"TABLE", "VIEW"})) {
            while (resultSet.next() && tables.size() < MAX_TABLE_COUNT) {
                String table = resultSet.getString("TABLE_NAME");
                String comment = resultSet.getString("REMARKS");
                String tableType = resultSet.getString("TABLE_TYPE");
                List<ColumnSchema> columns = requestedTable.isBlank()
                        ? List.of()
                        : readColumns(metadata, catalog, identifier.schema(), table);
                tables.add(new TableSchema(table, emptyToUnknown(comment), tableType, columns));
            }
        }

        if (!requestedTable.isBlank() && tables.isEmpty()) {
            throw new IllegalArgumentException(
                    "未找到表：" + requestedTable + "，请先调用 getDatabaseSchema 获取可用表名");
        }

        String hint = requestedTable.isBlank()
                ? "请从 tables 中选择表名，再调用 getDatabaseSchema(tableName) 获取字段结构。"
                : "请只使用返回的字段名生成只读查询，并按问题选择直出或分析工具。";
        return new DatabaseSchemaResult(
                requestedTable.isBlank() ? "table_catalog" : "table_detail",
                metadata.getDatabaseProductName(),
                identifier.schema(),
                tables,
                hint);
    }

    private List<ColumnSchema> readColumns(
            DatabaseMetaData metadata, String catalog, String schema, String tableName) throws SQLException {
        List<ColumnSchema> columns = new ArrayList<>();
        try (ResultSet resultSet = metadata.getColumns(catalog, schema, tableName, null)) {
            while (resultSet.next()) {
                int columnSize = resultSet.getInt("COLUMN_SIZE");
                int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
                int ordinalPosition = resultSet.getInt("ORDINAL_POSITION");
                columns.add(new ColumnSchema(
                        resultSet.getString("COLUMN_NAME"),
                        resultSet.getString("TYPE_NAME"),
                        columnSize == 0 ? null : columnSize,
                        decimalDigits == 0 ? null : decimalDigits,
                        nullableText(resultSet.getInt("NULLABLE")),
                        emptyToUnknown(resultSet.getString("REMARKS")),
                        ordinalPosition));
            }
        }
        return List.copyOf(columns);
    }

    private ReadOnlyQueryResult executeQuery(Connection connection, String limitedSql) throws SQLException {
        boolean originalReadOnly = connection.isReadOnly();
        try {
            try {
                connection.setReadOnly(true);
            } catch (SQLException ignored) {
                // 某些连接池不允许在借出后切换只读状态，SQL 校验和数据库账号仍会生效。
            }

            try (PreparedStatement statement = connection.prepareStatement(limitedSql)) {
                statement.setMaxRows(MAX_ROW_COUNT + 1);
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return readQueryResult(resultSet);
                }
            }
        } finally {
            try {
                connection.setReadOnly(originalReadOnly);
            } catch (SQLException ignored) {
                // 连接归还连接池时由连接池负责重置状态。
            }
        }
    }

    private ReadOnlyQueryResult readQueryResult(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<QueryColumn> columns = new ArrayList<>();
        List<Integer> visibleIndexes = new ArrayList<>();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            String label = metadata.getColumnLabel(index);
            String columnName = label == null || label.isBlank() ? metadata.getColumnName(index) : label;
            if (shouldExposeColumn(columnName)) {
                columns.add(new QueryColumn(columnName, metadata.getColumnTypeName(index)));
                visibleIndexes.add(index);
            }
        }

        List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;
        while (resultSet.next()) {
            if (rows.size() >= MAX_ROW_COUNT) {
                truncated = true;
                break;
            }
            List<Object> row = new ArrayList<>(columns.size());
            for (Integer index : visibleIndexes) {
                row.add(normalizeJdbcValue(resultSet.getObject(index)));
            }
            rows.add(row);
        }

        return new ReadOnlyQueryResult(
                List.copyOf(columns),
                Collections.unmodifiableList(rows),
                rows.size(),
                truncated,
                "最多返回 " + MAX_ROW_COUNT + " 行");
    }

    /** 对直出与分析结果统一过滤，不允许依赖模型自觉隐藏敏感列。 */
    private boolean shouldExposeColumn(String columnName) {
        String normalized = columnName == null ? "" : columnName
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.isBlank() || INTERNAL_COLUMNS.contains(normalized)
                || normalized.endsWith("_id")) {
            return false;
        }
        return SENSITIVE_COLUMN_MARKERS.stream().noneMatch(normalized::contains);
    }

    private String validateReadOnlySql(String sql) {
        if (sql.isBlank()) {
            return "SQL 不能为空";
        }
        if (!READ_ONLY_START.matcher(sql).find()) {
            return "只允许执行 SELECT 或 WITH 查询";
        }
        if (sql.contains(";") || sql.contains("--") || sql.contains("/*") || sql.contains("*/")) {
            return "只允许执行单条不带注释的查询 SQL";
        }

        String maskedSql = maskQuotedText(sql).toLowerCase(Locale.ROOT);
        for (String keyword : FORBIDDEN_SQL_KEYWORDS) {
            if (containsSqlToken(maskedSql, keyword)) {
                return "SQL 包含禁止执行的关键字：" + keyword;
            }
        }
        return null;
    }

    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        String normalized = sql.trim();
        if (normalized.startsWith("```") && normalized.endsWith("```")) {
            int firstLineEnd = normalized.indexOf('\n');
            if (firstLineEnd >= 0) {
                normalized = normalized.substring(firstLineEnd + 1, normalized.length() - 3).trim();
            }
        }
        return normalized;
    }

    private String normalizeChoiceText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String maskQuotedText(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        char quote = 0;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (quote == 0 && (current == '\'' || current == '"' || current == '`')) {
                quote = current;
                result.append(' ');
            } else if (quote != 0) {
                result.append(' ');
                if (current == quote) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                        result.append(' ');
                        index++;
                    } else {
                        quote = 0;
                    }
                }
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private boolean containsSqlToken(String sql, String token) {
        return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(token)
                + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE).matcher(sql).find();
    }

    private TableIdentifier parseTableIdentifier(String tableName, String currentSchema) {
        if (tableName == null || tableName.isBlank()) {
            return new TableIdentifier(currentSchema, null);
        }
        Matcher matcher = TABLE_IDENTIFIER.matcher(tableName.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("表名格式不合法，只支持 table_name 或 schema.table");
        }
        return new TableIdentifier(
                matcher.group(1) == null ? currentSchema : matcher.group(1),
                matcher.group(2));
    }

    private String getCurrentSchema(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        return schema == null || schema.isBlank() ? DEFAULT_SCHEMA : schema;
    }

    private Object normalizeJdbcValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof UUID
                || value instanceof java.sql.Date
                || value instanceof java.sql.Time
                || value instanceof java.sql.Timestamp
                || value instanceof java.time.temporal.TemporalAccessor) {
            return value.toString();
        }
        if (value.getClass().getName().equals("org.postgresql.util.PGobject")) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private String nullableText(int nullable) {
        return switch (nullable) {
            case DatabaseMetaData.columnNoNulls -> "NO";
            case DatabaseMetaData.columnNullable -> "YES";
            default -> "UNKNOWN";
        };
    }

    private String emptyToUnknown(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "数据库未返回具体错误信息";
        }
        return message.replaceAll("[\\r\\n]+", " ").trim();
    }

    private record TableIdentifier(String schema, String tablePattern) {
    }

    public record DatabaseSchemaResult(
            String mode,
            String databaseProduct,
            String schema,
            List<TableSchema> tables,
            String hint) {
    }

    public record TableSchema(
            String tableName,
            String tableComment,
            String tableType,
            List<ColumnSchema> columns) {
    }

    public record ColumnSchema(
            String columnName,
            String typeName,
            Integer columnSize,
            Integer decimalDigits,
            String nullable,
            String columnComment,
            Integer ordinalPosition) {
    }

    public record ReadOnlyQueryResult(
            List<QueryColumn> columns,
            List<List<Object>> rows,
            int rowCount,
            boolean truncated,
            String limitHint) {
    }

    public record QueryColumn(String name, String typeName) {
    }

    public record UserChoiceResult(
            String type,
            String component,
            String question,
            String placeholder,
            List<UserChoiceOption> options) {
    }

    public record UserChoiceOption(String label, String value, String description) {
    }

}
