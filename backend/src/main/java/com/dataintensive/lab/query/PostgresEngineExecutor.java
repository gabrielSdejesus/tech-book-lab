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
        try (Connection conn = DriverManager.getConnection(postgresUrl, postgresUser, postgresPassword);
             Statement stmt = conn.createStatement()) {

            boolean hasResultSet = stmt.execute(sql);
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

                    return QueryResult.ok(columns, rows, duration);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                return QueryResult.update(Math.max(updateCount, 0), duration);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
