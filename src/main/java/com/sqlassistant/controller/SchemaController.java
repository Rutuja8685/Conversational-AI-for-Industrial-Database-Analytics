package com.sqlassistant.controller;

import com.sqlassistant.model.dto.PromptSuggestion;
import com.sqlassistant.model.dto.SchemaSummary;
import com.sqlassistant.service.SchemaService;
import com.sqlassistant.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/schema")
@CrossOrigin(origins = "*")
public class SchemaController {

    private final SchemaService schemaService;
    private final SuggestionService suggestionService;
    private final JdbcTemplate jdbcTemplate;

    public SchemaController(SchemaService schemaService, SuggestionService suggestionService, JdbcTemplate jdbcTemplate) {
        this.schemaService = schemaService;
        this.suggestionService = suggestionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Context-aware prompt suggestions based on database schema tables
     * GET /api/schema/suggest
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<PromptSuggestion>> getSuggestions() {
        return ResponseEntity.ok(suggestionService.getPromptSuggestions());
    }

    /**
     * Live MySQL Database overview counts (Total Tables, Sensors, Logs, Depts, Users)
     * GET /api/schema/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<com.sqlassistant.model.dto.DatabaseOverviewDto> getOverview() {
        return ResponseEntity.ok(schemaService.getDatabaseOverview());
    }

    /**
     * Inspect active database schema dynamically using java.sql.DatabaseMetaData
     * GET /api/schema/inspect
     */
    @GetMapping("/inspect")
    public ResponseEntity<SchemaSummary> inspectSchema() {
        return ResponseEntity.ok(schemaService.getDynamicSchema());
    }

    /**
     * Utility to view raw contents of a database table for verification
     * GET /api/schema/tables/{tableName}/data
     */
    @GetMapping("/tables/{tableName}/data")
    public ResponseEntity<Map<String, Object>> getTableData(@PathVariable String tableName) {
        // Sanitize input to prevent SQL injection for table lookup
        String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + safeTableName + " LIMIT 50");
        List<String> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tableName", safeTableName);
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("rowCount", rows.size());

        return ResponseEntity.ok(result);
    }
}
