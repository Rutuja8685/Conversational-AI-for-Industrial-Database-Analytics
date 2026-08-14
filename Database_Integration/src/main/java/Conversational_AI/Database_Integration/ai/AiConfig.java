package Conversational_AI.Database_Integration.ai;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public SqlGeneratorAgent sqlGeneratorAgent() {
        // Switch back to Gemini Cloud API (Lightweight on your laptop CPU!)
        GoogleAiGeminiChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey("AQ.Ab8RN6I4pU61xGWXB_aYL5smiClJn2hpA35-c4gV3PjWErdM3Q") // Replace with your actual AIzaSy key
                .modelName("gemini-3-flash-preview")     // Fast and highly accurate with structured text/SQL
                .temperature(0.0)
                .build();

        return AiServices.builder(SqlGeneratorAgent.class)
                .chatLanguageModel(chatModel)
                .build();
    }
}