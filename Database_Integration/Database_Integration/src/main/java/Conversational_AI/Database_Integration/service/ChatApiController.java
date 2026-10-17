package Conversational_AI.Database_Integration.service;

import Conversational_AI.Database_Integration.ai.SqlGeneratorAgent;
import Conversational_AI.Database_Integration.service.DatabaseExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Allows your frontend to connect smoothly
public class ChatApiController {

    @Autowired
    private SqlGeneratorAgent aiAgent;

    @Autowired
    private DatabaseExecutorService executorService;

    // Temporary session cache to hold queries awaiting confirmation
    private String pendingSql = "";
    private String activeRole = "OPERATOR"; // Default fallback role

    @PostMapping("/role")
    public Map<String, String> setRole(@RequestBody Map<String, String> request) {
        this.activeRole = request.getOrDefault("role", "OPERATOR").toUpperCase();
        return Collections.singletonMap("status", "Role set to " + this.activeRole);
    }

    @PostMapping("/message")
    public Map<String, Object> handleMessage(@RequestBody Map<String, String> request) {
        String userInput = request.get("message");
        Map<String, Object> response = new HashMap<>();

        try {
            // Handle active confirmation state loops
            if (!pendingSql.isEmpty()) {
                if ("yes".equalsIgnoreCase(userInput) || "y".equalsIgnoreCase(userInput)) {
                    int rows = executorService.executeModifyQuery(pendingSql, activeRole);
                    response.put("type", "SUCCESS");
                    response.put("text", "Database transaction completed! " + rows + " rows altered successfully.");
                } else {
                    response.put("type", "SYSTEM");
                    response.put("text", "Operation cancelled safely by user request.");
                }
                pendingSql = "";
                return response;
            }

            // Standard natural language translation phase
            String generatedSql = aiAgent.generateSqlQuery(userInput);
            response.put("sql", generatedSql);

            String cleanLookup = generatedSql.replaceAll("(?i)```sql", "").replaceAll("```", "").trim().toUpperCase();
            cleanLookup = cleanLookup.replaceAll("^[^A-Z]+", "");

            // Security screening for data modification strings
            if (cleanLookup.startsWith("UPDATE") || cleanLookup.startsWith("DELETE") || cleanLookup.startsWith("INSERT")) {
                if (!"MANAGER".equalsIgnoreCase(activeRole) && !"ADMIN".equalsIgnoreCase(activeRole)) {
                    response.put("type", "ERROR");
                    response.put("text", "Security Block: Your current role (" + activeRole + ") is restricted from executing modification requests.");
                    return response;
                }
                
                pendingSql = generatedSql;
                response.put("type", "CONFIRMATION");
                response.put("text", "⚠️ Warning: This action modifies persistent tables. Do you want to execute this statement? (Type 'yes' or 'no')");
            } else {
                // Execute standard read-only select queries
                List<Map<String, Object>> records = executorService.executeSelectQuery(generatedSql);
                response.put("type", "DATA");
                response.put("data", records);
            }

        } catch (Exception e) {
            response.put("type", "ERROR");
            response.put("text", e.getMessage());
            pendingSql = ""; 
        }

        return response;
    }
}
