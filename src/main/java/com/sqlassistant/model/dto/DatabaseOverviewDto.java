package com.sqlassistant.model.dto;

public class DatabaseOverviewDto {

    private long totalTables;
    private long totalSensors;
    private long totalLogs;
    private long totalDepartments;
    private long totalUsers;

    public DatabaseOverviewDto() {}

    public DatabaseOverviewDto(long totalTables, long totalSensors, long totalLogs, long totalDepartments, long totalUsers) {
        this.totalTables = totalTables;
        this.totalSensors = totalSensors;
        this.totalLogs = totalLogs;
        this.totalDepartments = totalDepartments;
        this.totalUsers = totalUsers;
    }

    public long getTotalTables() { return totalTables; }
    public void setTotalTables(long totalTables) { this.totalTables = totalTables; }
    public long getTotalSensors() { return totalSensors; }
    public void setTotalSensors(long totalSensors) { this.totalSensors = totalSensors; }
    public long getTotalLogs() { return totalLogs; }
    public void setTotalLogs(long totalLogs) { this.totalLogs = totalLogs; }
    public long getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
}
