package Conversational_AI.Database_Integration.controller;

import Conversational_AI.Database_Integration.entity.ChatMessage;
import Conversational_AI.Database_Integration.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatApiController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/history/{username}")
public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String username) {
    List<ChatMessage> history = chatMessageRepository.findByUsernameOrderByTimestampAsc(username);
    return ResponseEntity.ok(history);
}

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> processChatMessage(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String username = payload.getOrDefault("username", "rutuja_admin");

        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Prompt cannot be empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> responseMap = new HashMap<>();

        try {
            // 1. Call Gemini API to classify input or generate SQL
            String aiOutput = callGeminiApi(prompt);

            // 2. Check if Gemini returned an SQL SELECT Query
            if (aiOutput.toUpperCase().startsWith("SELECT")) {
                String cleanSql = aiOutput.replaceAll("```sql", "").replaceAll("```", "").trim();

                List<Map<String, Object>> rows = jdbcTemplate.queryForList(cleanSql);

                responseMap.put("type", "DATA");
                responseMap.put("data", rows);
                responseMap.put("sql", cleanSql);
                responseMap.put("response", "Retrieved " + rows.size() + " database records.");

                chatMessageRepository.save(new ChatMessage(username, prompt, "Executed SQL: " + cleanSql, LocalDateTime.now()));
            } 
            // 3. Fallback to normal conversational text output
            else {
                responseMap.put("type", "TEXT");
                responseMap.put("text", aiOutput);
                responseMap.put("response", aiOutput);

                chatMessageRepository.save(new ChatMessage(username, prompt, aiOutput, LocalDateTime.now()));
            }

        } catch (Exception e) {
            responseMap.put("type", "ERROR");
            responseMap.put("text", "Error processing request: " + e.getMessage());
        }

        return ResponseEntity.ok(responseMap);
    }

    private String callGeminiApi(String userPrompt) {
    String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + geminiApiKey;

    String systemInstruction = "You are an AI assistant integrated with a MySQL database (conventionalai_industrialdb).\n" +
            "Database tables: departments, maintenance_logs, sensors, users, plants, productionline.\n" +
            "Rules:\n" +
            "1. If user asks for database info, records, sensors, or logs, respond ONLY with an executable MySQL query starting with 'SELECT' and ending with ';'. No markdown code formatting.\n" +
            "2. If input is conversational (e.g. greetings, general knowledge), reply naturally.\n\n" +
            "User Input: " + userPrompt;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> textPart = new HashMap<>();
    textPart.put("text", systemInstruction);

    Map<String, Object> partsMap = new HashMap<>();
    partsMap.put("parts", Collections.singletonList(textPart));

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("contents", Collections.singletonList(partsMap));

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

    if (response.getBody() != null && response.getBody().containsKey("candidates")) {
        List candidates = (List) response.getBody().get("candidates");
        if (!candidates.isEmpty()) {
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            return ((String) firstPart.get("text")).trim();
        }
    }

    return "Could not process AI response.";
}}