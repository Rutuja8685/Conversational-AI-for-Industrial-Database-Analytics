package com.sqlassistant.service;

import com.sqlassistant.model.dto.DatabaseOverviewDto;
import com.sqlassistant.model.dto.SchemaSummary;
import com.sqlassistant.model.dto.SchemaSummary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class SchemaService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SchemaService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseOverviewDto getDatabaseOverview() {
        long tablesCount = getDynamicSchema().getTables().size();
        long sensorsCount = queryCount("SELECT COUNT(*) FROM sensors");
        long logsCount = queryCount("SELECT COUNT(*) FROM maintenance_logs");
        long deptsCount = queryCount("SELECT COUNT(*) FROM departments");
        long usersCount = queryCount("SELECT COUNT(*) FROM users");
        return new DatabaseOverviewDto(tablesCount, sensorsCount, logsCount, deptsCount, usersCount);
    }

    private long queryCount(String sql) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class);
            return val != null ? val : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Inspects active database metadata dynamically using java.sql.DatabaseMetaData
     * Works on both MySQL and H2 databases.
     */
    public SchemaSummary getDynamicSchema() {
        List<TableDetail> tables = new ArrayList<>();
        List<ForeignKeyDetail> foreignKeys = new ArrayList<>();
        StringBuilder promptBuilder = new StringBuilder();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog(); // Database name e.g. industrial_db
            String schema = conn.getSchema();

            logger.info("Inspecting Database Metadata for Catalog: {}, Schema: {}, DB Product: {}", 
                    catalog, schema, metaData.getDatabaseProductName());

            // 1. Fetch user tables
            try (ResultSet tableRs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    String tableName = tableRs.getString("TABLE_NAME");
                    
                    // Filter out system / H2 internal / MySQL system tables
                    if (tableName.startsWith("SYSTEM_") || 
                        tableName.startsWith("INFORMATION_SCHEMA") || 
                        tableName.startsWith("HTR_") ||
                        tableName.equalsIgnoreCase("sys_config")) {
                        continue;
                    }

                    // 2. Fetch Primary Keys for table
                    Set<String> pkColumns = new HashSet<>();
                    try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName)) {
                        while (pkRs.next()) {
                            pkColumns.add(pkRs.getString("COLUMN_NAME"));
                        }
                    }

                    // 3. Fetch Columns for table
                    List<ColumnDetail> columns = new ArrayList<>();
                    try (ResultSet colRs = metaData.getColumns(catalog, null, tableName, "%")) {
                        while (colRs.next()) {
                            String colName = colRs.getString("COLUMN_NAME");
                            String dataType = colRs.getString("TYPE_NAME");
                            boolean isNullable = "YES".equalsIgnoreCase(colRs.getString("IS_NULLABLE"));
                            boolean isPk = pkColumns.contains(colName);

                            columns.add(new ColumnDetail(colName, dataType, isNullable, isPk));
                        }
                    }

                    tables.add(new TableDetail(tableName, columns));

                    // 4. Fetch Foreign Keys (imported keys)
                    try (ResultSet fkRs = metaData.getImportedKeys(catalog, null, tableName)) {
                        while (fkRs.next()) {
                            String pkTable = fkRs.getString("PKTABLE_NAME");
                            String pkCol = fkRs.getString("PKCOLUMN_NAME");
                            String fkTable = fkRs.getString("FKTABLE_NAME");
                            String fkCol = fkRs.getString("FKCOLUMN_NAME");

                            foreignKeys.add(new ForeignKeyDetail(pkTable, pkCol, fkTable, fkCol));
                        }
                    }
                }
            }

            // 5. Construct LLM Context String
            promptBuilder.append("DATABASE SCHEMA METADATA:\n");
            promptBuilder.append("========================\n\n");

            for (TableDetail table : tables) {
                promptBuilder.append("TABLE: ").append(table.getTableName().toLowerCase()).append("\n");
                promptBuilder.append("COLUMNS:\n");
                for (ColumnDetail col : table.getColumns()) {
                    promptBuilder.append("  - ").append(col.getName().toLowerCase())
                            .append(" (").append(col.getDataType()).append(")")
                            .append(col.isPrimaryKey() ? " [PRIMARY KEY]" : "")
                            .append(col.isNullable() ? "" : " [NOT NULL]")
                            .append("\n");
                }
                promptBuilder.append("\n");
            }

            if (!foreignKeys.isEmpty()) {
                promptBuilder.append("FOREIGN KEY RELATIONSHIPS:\n");
                for (ForeignKeyDetail fk : foreignKeys) {
                    promptBuilder.append("  - ")
                            .append(fk.getFkTable().toLowerCase()).append(".").append(fk.getFkColumn().toLowerCase())
                            .append(" REFERENCES ")
                            .append(fk.getPkTable().toLowerCase()).append(".").append(fk.getPkColumn().toLowerCase())
                            .append("\n");
                }
            }

        } catch (SQLException e) {
            logger.error("Error inspecting database metadata", e);
            throw new RuntimeException("Failed to inspect database schema", e);
        }

        return new SchemaSummary(tables, foreignKeys, promptBuilder.toString());
    }
}
