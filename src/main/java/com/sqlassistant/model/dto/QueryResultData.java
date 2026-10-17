package com.sqlassistant.model.dto;

import java.util.List;

public class QueryResultData {

    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
    private long executionTimeMs;
    private Integer rowsAffected;

    public QueryResultData() {}

    public QueryResultData(List<String> columns, List<List<Object>> rows, int rowCount, long executionTimeMs) {
        this.columns = columns;
        this.rows = rows;
        this.rowCount = rowCount;
        this.executionTimeMs = executionTimeMs;
    }

    public QueryResultData(int rowsAffected, long executionTimeMs) {
        this.rowsAffected = rowsAffected;
        this.rowCount = rowsAffected;
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Integer getRowsAffected() {
        return rowsAffected;
    }

    public void setRowsAffected(Integer rowsAffected) {
        this.rowsAffected = rowsAffected;
    }
}
