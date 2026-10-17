package Conversational_AI.Database_Integration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DatabaseExecutorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String cleanAndSanitizeQuery(String sqlQuery) {
        if (sqlQuery == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        
        // 1. Strip markdown tags, code blocks, and semicolons
        String cleanQuery = sqlQuery.replaceAll("(?i)```sql", "")
                                    .replaceAll("```", "")
                                    .replace(";", "")
                                    .trim();
        
        cleanQuery = cleanQuery.replaceAll("^[^a-zA-Z]+", "");

        // 2. Fix table name mismatches (Convert plural generated tables to singular schema tables)
        cleanQuery = cleanQuery.replaceAll("(?i)\\bsensors\\b", "sensor")
                               .replaceAll("(?i)\\bplants\\b", "plant")
                               .replaceAll("(?i)\\bdepartments\\b", "department")
                               .replaceAll("(?i)\\bproductionlines\\b", "productionline");

        return cleanQuery;
    }

    public List<Map<String, Object>> executeSelectQuery(String sqlQuery) {
        String cleanQuery = cleanAndSanitizeQuery(sqlQuery);
        System.out.println("[Executing Clean SQL]: " + cleanQuery); 

        if (!cleanQuery.toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("Safety Warning: Only SELECT statements can be executed here.");
        }
        return jdbcTemplate.queryForList(cleanQuery);
    }

    /**
     * Enhanced security for modifying queries with Role Validation and WHERE clause enforcement.
     */
    @Transactional
    public int executeModifyQuery(String sqlQuery, String currentUserRole) {
        // 1. Role-Based Access Control Guardrail
        if (!"MANAGER".equalsIgnoreCase(currentUserRole) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Access Denied: Your role (" + currentUserRole + ") does not have permission to modify data.");
        }

        String cleanQuery = cleanAndSanitizeQuery(sqlQuery);
        String upperQuery = cleanQuery.toUpperCase();

        // 2. Destructive Execution Guardrail (Enforce WHERE clause)
        if (upperQuery.startsWith("UPDATE") || upperQuery.startsWith("DELETE")) {
            if (!upperQuery.contains("WHERE")) {
                throw new IllegalArgumentException("CRITICAL SAFETY BLOCK: UPDATE or DELETE statements without a WHERE clause are restricted to prevent accidental database wiping.");
            }
        }

        System.out.println("[Executing Secure Modifying SQL]: " + cleanQuery);

        if (upperQuery.startsWith("UPDATE") || upperQuery.startsWith("DELETE") || upperQuery.startsWith("INSERT")) {
            return jdbcTemplate.update(cleanQuery);
        } else {
            throw new IllegalArgumentException("Safety Warning: Invalid modification statement.");
        }
    }
}