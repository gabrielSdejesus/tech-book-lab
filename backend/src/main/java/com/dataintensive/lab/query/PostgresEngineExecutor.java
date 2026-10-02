package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
public class PostgresEngineExecutor implements QueryEngineExecutor {

    private final String postgresUrl;
    private final String postgresUser;
    private final String postgresPassword;

    public PostgresEngineExecutor(
            @Value("${lab.postgres.url:jdbc:postgresql://localhost:5432/tbl_lab}") String postgresUrl,
            @Value("${lab.postgres.username:postgres}") String postgresUser,
            @Value("${lab.postgres.password:postgrespassword}") String postgresPassword) {
        this.postgresUrl = postgresUrl;
        this.postgresUser = postgresUser;
        this.postgresPassword = postgresPassword;
    }

    @Override
    public EngineType getEngineType() {
        return EngineType.POSTGRES;
    }

    @Override
    public QueryResult execute(String sql, long startTime) {
        List<String> statements = splitStatements(sql);
        if (statements.isEmpty()) {
            return QueryResult.update(0, System.currentTimeMillis() - startTime);
        }

        try (Connection conn = DriverManager.getConnection(postgresUrl, postgresUser, postgresPassword);
             Statement stmt = conn.createStatement()) {

            QueryResult lastResult = null;

            for (String singleSql : statements) {
                boolean hasResultSet = stmt.execute(singleSql);
                long duration = System.currentTimeMillis() - startTime;

                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        List<String> columns = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            columns.add(meta.getColumnLabel(i));
                        }

                        List<Map<String, Object>> rows = new ArrayList<>();
                        int maxRows = 200;
                        while (rs.next() && rows.size() < maxRows) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(columns.get(i - 1), rs.getObject(i));
                            }
                            rows.add(row);
                        }

                        lastResult = QueryResult.ok(columns, rows, duration);
                    }
                } else {
                    int updateCount = stmt.getUpdateCount();
                    lastResult = QueryResult.update(Math.max(updateCount, 0), duration);
                }
            }

            return lastResult != null ? lastResult : QueryResult.update(0, System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> splitStatements(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }

        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = sql.length();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        String dollarTag = null;

        for (int i = 0; i < length; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < length) ? sql.charAt(i + 1) : '\0';

            // Comentário de linha: --
            if (!inSingleQuote && !inDoubleQuote && dollarTag == null && !inBlockComment && !inLineComment && c == '-' && next == '-') {
                inLineComment = true;
                current.append(c).append(next);
                i++;
                continue;
            }
            if (inLineComment) {
                current.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }

            // Comentário de bloco: /* ... */
            if (!inSingleQuote && !inDoubleQuote && dollarTag == null && !inLineComment && !inBlockComment && c == '/' && next == '*') {
                inBlockComment = true;
                current.append(c).append(next);
                i++;
                continue;
            }
            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // Dollar quote PostgreSQL: $$ ou $tag$
            if (!inSingleQuote && !inDoubleQuote && !inLineComment && !inBlockComment) {
                if (dollarTag == null && c == '$') {
                    int endDollar = sql.indexOf('$', i + 1);
                    if (endDollar != -1) {
                        String potentialTag = sql.substring(i, endDollar + 1);
                        if (potentialTag.matches("^\\$[A-Za-z0-9_]*\\$$")) {
                            dollarTag = potentialTag;
                            current.append(potentialTag);
                            i = endDollar;
                            continue;
                        }
                    }
                } else if (dollarTag != null && sql.startsWith(dollarTag, i)) {
                    current.append(dollarTag);
                    i += dollarTag.length() - 1;
                    dollarTag = null;
                    continue;
                }
            }

            // Aspas simples '...' (com escape '')
            if (dollarTag == null && !inDoubleQuote && !inLineComment && !inBlockComment) {
                if (c == '\'') {
                    if (inSingleQuote && next == '\'') {
                        current.append("''");
                        i++;
                        continue;
                    }
                    inSingleQuote = !inSingleQuote;
                    current.append(c);
                    continue;
                }
            }

            // Aspas duplas "..."
            if (dollarTag == null && !inSingleQuote && !inLineComment && !inBlockComment) {
                if (c == '"') {
                    inDoubleQuote = !inDoubleQuote;
                    current.append(c);
                    continue;
                }
            }

            // Delimitador de statement ; fora de literais e comentários
            if (c == ';' && !inSingleQuote && !inDoubleQuote && !inLineComment && !inBlockComment && dollarTag == null) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }
}
