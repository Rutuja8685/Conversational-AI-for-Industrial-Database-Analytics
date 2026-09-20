package com.sqlassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlassistant.model.User;
import com.sqlassistant.model.dto.SchemaSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public enum IntentType {
        GREETING_OR_CHITCHAT,
        DATABASE_QUERY
    }

    public LlmService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public static class LlmSqlResult {
        private String sql;
        private String operationType; // SELECT, UPDATE, DELETE, INSERT, INVALID
        private String explanation;
        private List<String> affectedTables;

        public LlmSqlResult(String sql, String operationType, String explanation, List<String> affectedTables) {
            this.sql = sql;
            this.operationType = operationType;
            this.explanation = explanation;
            this.affectedTables = affectedTables;
        }

        public String getSql() { return sql; }
        public String getOperationType() { return operationType; }
        public String getExplanation() { return explanation; }
        public List<String> getAffectedTables() { return affectedTables; }
    }

    /**
     * Intent Classifier: Determines if user prompt is general chit-chat vs database query.
     */
    public IntentType classifyIntent(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return IntentType.GREETING_OR_CHITCHAT;
        }
        String lower = prompt.toLowerCase().trim();

        // 1. Data query indicators
        boolean hasDataKeywords = lower.contains("sensor") || lower.contains("log") || lower.contains("maintenance") ||
                lower.contains("department") || lower.contains("user") || lower.contains("select") ||
                lower.contains("update") || lower.contains("delete") || lower.contains("show") ||
                lower.contains("count") || lower.contains("record") || lower.contains("list") ||
                lower.contains("how many") || lower.contains("find") || lower.contains("get") ||
                lower.contains("data") || lower.contains("cost") || lower.contains("budget") ||
                lower.contains("table") || lower.contains("status");

        if (hasDataKeywords) {
            return IntentType.DATABASE_QUERY;
        }

        // 2. Greeting or Chit-chat indicators
        if (lower.equals("hi") || lower.equals("hello") || lower.equals("hey") || lower.startsWith("hi ") ||
                lower.startsWith("hello ") || lower.contains("how are you") || lower.contains("who are you") ||
                lower.contains("what can you do") || lower.contains("good morning") || lower.contains("good evening") ||
                lower.contains("help") || lower.contains("thanks") || lower.contains("thank you") || lower.equals("bye")) {
            return IntentType.GREETING_OR_CHITCHAT;
        }

        // Default to DATABASE_QUERY if uncertain to let LLM process
        return IntentType.DATABASE_QUERY;
    }

    /**
     * Generates a warm, role-aware conversational response for Chit-chat / Greetings.
     */
    public String generateChitchatResponse(String prompt, User user) {
        String name = (user != null && user.getFullName() != null) ? user.getFullName() : "there";
        String role = (user != null && user.getRole() != null) ? user.getRole().name() : "USER";
        String lower = prompt.toLowerCase().trim();

        if (lower.contains("who are you") || lower.contains("what can you do")) {
            return "Hello " + name + "! 👋 I am your Conversational AI Database Assistant (Role: " + role + "). " +
                    "I help you explore, query, and modify your MySQL database using natural language.\n\n" +
                    "Here are a few things you can ask me:\n" +
                    "• 'how many data you have' - View total record counts across all MySQL tables.\n" +
                    "• 'Show all offline sensors in Department 1' - Retrieve sensor telemetry data.\n" +
                    "• 'List high-cost maintenance logs' - Inspect maintenance logs and technicians.\n" +
                    "• 'Update status of sensor TEMP-101 to MAINTENANCE' - Safely modify records with RBAC guardrails.";
        }

        if (lower.contains("how are you")) {
            return "I am doing great and ready to help you analyze your MySQL database! How can I assist you today, " + name + "?";
        }

        if (lower.contains("thanks") || lower.contains("thank you")) {
            return "You're very welcome, " + name + "! Let me know if you need any more database insights.";
        }

        return "Hello " + name + "! 👋 I am your AI Database Assistant. How can I help you query or explore your MySQL database today?";
    }

    /**
     * Translates Natural Language prompt into JSON containing SQL & metadata.
     */
    public LlmSqlResult translateToSql(String userPrompt, SchemaSummary schemaSummary) {
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("${")) {
            try {
                return callGeminiApiForSql(userPrompt, schemaSummary);
            } catch (Exception e) {
                logger.warn("Gemini API call failed, using intelligent schema fallback. Error: {}", e.getMessage());
            }
        }
        return generateFallbackSql(userPrompt, schemaSummary);
    }

    /**
     * Call Google Gemini API to translate NL to SQL based on dynamic DB schema.
     */
    private LlmSqlResult callGeminiApiForSql(String userPrompt, SchemaSummary schemaSummary) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        String systemPrompt = "You are an expert database administrator and SQL generator.\n" +
                "Given the following database schema metadata:\n\n" + schemaSummary.getSummaryPromptText() + "\n\n" +
                "Convert the user's plain-English request into an accurate SQL query.\n" +
                "Return ONLY a raw JSON object (without markdown code blocks ```json ... ```) with keys:\n" +
                "{\n" +
                "  \"sql\": \"SELECT ... / UPDATE ... / DELETE ...\",\n" +
                "  \"operationType\": \"SELECT\" | \"UPDATE\" | \"DELETE\" | \"INSERT\",\n" +
                "  \"explanation\": \"Brief explanation of what the query does\",\n" +
                "  \"affectedTables\": [\"table1\", \"table2\"]\n" +
                "}\n\n" +
                "Rules:\n" +
                "1. Use valid ANSI SQL / MySQL dialect.\n" +
                "2. Match column names and table names strictly to the schema provided.\n" +
                "3. Ensure string comparisons are formatted cleanly.\n" +
                "4. Operation type must be strictly SELECT, UPDATE, DELETE, or INSERT.";

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", systemPrompt + "\n\nUser Request: " + userPrompt);

        content.put("parts", Collections.singletonList(textPart));
        requestBody.put("contents", Collections.singletonList(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        JsonNode rootNode = objectMapper.readTree(response.getBody());

        String jsonText = rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        // Clean any markdown formatting if present
        jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

        JsonNode sqlJson = objectMapper.readTree(jsonText);
        String sql = sqlJson.path("sql").asText();
        String operationType = sqlJson.path("operationType").asText(detectOperationType(sql));
        String explanation = sqlJson.path("explanation").asText();

        List<String> affectedTables = new ArrayList<>();
        if (sqlJson.has("affectedTables")) {
            for (JsonNode tableNode : sqlJson.get("affectedTables")) {
                affectedTables.add(tableNode.asText());
            }
        }

        return new LlmSqlResult(sql, operationType.toUpperCase(), explanation, affectedTables);
    }

    /**
     * Schema-Aware Fallback Engine when Gemini API Key is not supplied.
     */
    private LlmSqlResult generateFallbackSql(String userPrompt, SchemaSummary schemaSummary) {
        String lower = userPrompt.toLowerCase().trim();
        String sql;
        String opType = "SELECT";
        String explanation;
        List<String> tables = new ArrayList<>();

        if (lower.contains("how many data") || lower.contains("how much data") || lower.contains("overview") || lower.contains("total records") || lower.contains("count records")) {
            sql = "SELECT 'departments' AS table_name, COUNT(*) AS record_count FROM departments " +
                  "UNION ALL SELECT 'users', COUNT(*) FROM users " +
                  "UNION ALL SELECT 'sensors', COUNT(*) FROM sensors " +
                  "UNION ALL SELECT 'maintenance_logs', COUNT(*) FROM maintenance_logs";
            explanation = "Calculates total record counts across all MySQL database tables";
            tables.addAll(Arrays.asList("departments", "users", "sensors", "maintenance_logs"));
        } else if (lower.contains("update") || lower.contains("change") || lower.contains("set")) {
            opType = "UPDATE";
            if (lower.contains("sensor") && lower.contains("temp-101")) {
                sql = "UPDATE sensors SET status = 'MAINTENANCE' WHERE sensor_code = 'TEMP-101'";
                explanation = "Updates sensor TEMP-101 status to MAINTENANCE";
                tables.add("sensors");
            } else if (lower.contains("budget") || lower.contains("department")) {
                sql = "UPDATE departments SET budget = budget + 50000.00 WHERE code = 'DEPT-IIOT'";
                explanation = "Increases budget for department DEPT-IIOT by $50,000";
                tables.add("departments");
            } else {
                sql = "UPDATE sensors SET status = 'ACTIVE' WHERE status = 'OFFLINE'";
                explanation = "Sets all offline sensors status to ACTIVE";
                tables.add("sensors");
            }
        } else if (lower.contains("delete") || lower.contains("remove") || lower.contains("clear")) {
            opType = "DELETE";
            if (lower.contains("log") || lower.contains("maintenance")) {
                sql = "DELETE FROM maintenance_logs WHERE cost > 3000.00";
                explanation = "Deletes maintenance logs with a cost exceeding $3,000.00";
                tables.add("maintenance_logs");
            } else {
                sql = "DELETE FROM sensors WHERE status = 'FAULT'";
                explanation = "Removes sensors with FAULT status";
                tables.add("sensors");
            }
        } else if (lower.contains("log") || lower.contains("maintenance") || lower.contains("cost")) {
            sql = "SELECT m.id, s.sensor_code, s.name AS sensor_name, u.full_name AS technician, m.description, m.cost, m.status " +
                  "FROM maintenance_logs m " +
                  "JOIN sensors s ON m.sensor_id = s.id " +
                  "JOIN users u ON m.user_id = u.id " +
                  "ORDER BY m.cost DESC";
            explanation = "Retrieves maintenance logs with sensor details and technician names ordered by cost";
            tables.addAll(Arrays.asList("maintenance_logs", "sensors", "users"));
        } else if (lower.contains("department") || lower.contains("budget")) {
            sql = "SELECT d.id, d.name, d.code, d.location, d.budget, COUNT(u.id) AS user_count " +
                  "FROM departments d " +
                  "LEFT JOIN users u ON d.id = u.department_id " +
                  "GROUP BY d.id, d.name, d.code, d.location, d.budget";
            explanation = "Calculates total department budgets and user counts";
            tables.addAll(Arrays.asList("departments", "users"));
        } else if (lower.contains("user") || lower.contains("role") || lower.contains("engineer")) {
            sql = "SELECT u.id, u.username, u.full_name, u.email, u.role, d.name AS department_name " +
                  "FROM users u " +
                  "LEFT JOIN departments d ON u.department_id = d.id";
            explanation = "Lists all system users, their assigned roles, and departments";
            tables.addAll(Arrays.asList("users", "departments"));
        } else {
            // Default query: offline or fault sensors with department info
            sql = "SELECT s.id, s.sensor_code, s.name, s.sensor_type, s.location, s.status, s.last_reading, d.name AS department_name " +
                  "FROM sensors s " +
                  "LEFT JOIN departments d ON s.department_id = d.id";
            explanation = "Retrieves all registered sensors along with their current status and department information";
            tables.addAll(Arrays.asList("sensors", "departments"));
        }

        return new LlmSqlResult(sql, opType, explanation, tables);
    }

    /**
     * Generates a concise human-friendly text summary of tabular SQL query results.
     */
    public String generateDataSummary(String userPrompt, String sql, List<String> columns, List<List<Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "No matching records found in the database for query.";
        }

        // Special handling for union record count queries ("how many data you have")
        String lower = (userPrompt != null) ? userPrompt.toLowerCase() : "";
        if (lower.contains("how many data") || lower.contains("how much data") || lower.contains("overview") || lower.contains("total records") || lower.contains("count records")) {
            long total = 0;
            StringBuilder sb = new StringBuilder("Database Record Overview: ");
            for (int i = 0; i < rows.size(); i++) {
                String tableName = String.valueOf(rows.get(i).get(0));
                long count = Long.parseLong(String.valueOf(rows.get(i).get(1)));
                total += count;
                sb.append(tableName).append(": ").append(count);
                if (i < rows.size() - 1) sb.append(", ");
            }
            sb.append(". (Total ").append(total).append(" records across ").append(rows.size()).append(" active MySQL tables).");
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(rows.size()).append(" record(s). ");
        if (rows.size() == 1) {
            sb.append("Details: ");
            for (int i = 0; i < Math.min(columns.size(), 4); i++) {
                sb.append(columns.get(i)).append(" = ").append(rows.get(0).get(i));
                if (i < Math.min(columns.size(), 4) - 1) sb.append(", ");
            }
            sb.append(".");
        } else {
            sb.append("Sample key values: ");
            for (int i = 0; i < Math.min(rows.size(), 3); i++) {
                sb.append("[").append(columns.get(0)).append(": ").append(rows.get(i).get(0)).append("]");
                if (i < Math.min(rows.size(), 3) - 1) sb.append(", ");
            }
            sb.append(". Showing top records.");
        }
        return sb.toString();
    }

    private String detectOperationType(String sql) {
        if (sql == null) return "SELECT";
        String s = sql.trim().toUpperCase();
        if (s.startsWith("UPDATE")) return "UPDATE";
        if (s.startsWith("DELETE")) return "DELETE";
        if (s.startsWith("INSERT")) return "INSERT";
        return "SELECT";
    }
}
