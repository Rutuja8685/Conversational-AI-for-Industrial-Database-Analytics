package com.sqlassistant.model.dto;

import java.util.List;

public class SchemaSummary {

    public static class ColumnDetail {
        private String name;
        private String dataType;
        private boolean isNullable;
        private boolean isPrimaryKey;

        public ColumnDetail() {}

        public ColumnDetail(String name, String dataType, boolean isNullable, boolean isPrimaryKey) {
            this.name = name;
            this.dataType = dataType;
            this.isNullable = isNullable;
            this.isPrimaryKey = isPrimaryKey;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public boolean isNullable() { return isNullable; }
        public void setNullable(boolean nullable) { isNullable = nullable; }
        public boolean isPrimaryKey() { return isPrimaryKey; }
        public void setPrimaryKey(boolean primaryKey) { isPrimaryKey = primaryKey; }
    }

    public static class TableDetail {
        private String tableName;
        private List<ColumnDetail> columns;

        public TableDetail() {}

        public TableDetail(String tableName, List<ColumnDetail> columns) {
            this.tableName = tableName;
            this.columns = columns;
        }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<ColumnDetail> getColumns() { return columns; }
        public void setColumns(List<ColumnDetail> columns) { this.columns = columns; }
    }

    public static class ForeignKeyDetail {
        private String pkTable;
        private String pkColumn;
        private String fkTable;
        private String fkColumn;

        public ForeignKeyDetail() {}

        public ForeignKeyDetail(String pkTable, String pkColumn, String fkTable, String fkColumn) {
            this.pkTable = pkTable;
            this.pkColumn = pkColumn;
            this.fkTable = fkTable;
            this.fkColumn = fkColumn;
        }

        public String getPkTable() { return pkTable; }
        public void setPkTable(String pkTable) { this.pkTable = pkTable; }
        public String getPkColumn() { return pkColumn; }
        public void setPkColumn(String pkColumn) { this.pkColumn = pkColumn; }
        public String getFkTable() { return fkTable; }
        public void setFkTable(String fkTable) { this.fkTable = fkTable; }
        public String getFkColumn() { return fkColumn; }
        public void setFkColumn(String fkColumn) { this.fkColumn = fkColumn; }
    }

    private List<TableDetail> tables;
    private List<ForeignKeyDetail> foreignKeys;
    private String summaryPromptText;

    public SchemaSummary() {}

    public SchemaSummary(List<TableDetail> tables, List<ForeignKeyDetail> foreignKeys, String summaryPromptText) {
        this.tables = tables;
        this.foreignKeys = foreignKeys;
        this.summaryPromptText = summaryPromptText;
    }

    public List<TableDetail> getTables() { return tables; }
    public void setTables(List<TableDetail> tables) { this.tables = tables; }
    public List<ForeignKeyDetail> getForeignKeys() { return foreignKeys; }
    public void setForeignKeys(List<ForeignKeyDetail> foreignKeys) { this.foreignKeys = foreignKeys; }
    public String getSummaryPromptText() { return summaryPromptText; }
    public void setSummaryPromptText(String summaryPromptText) { this.summaryPromptText = summaryPromptText; }
}
