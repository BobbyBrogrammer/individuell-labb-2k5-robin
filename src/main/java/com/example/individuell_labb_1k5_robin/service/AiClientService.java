package com.example.individuell_labb_1k5_robin.service;

import com.example.individuell_labb_1k5_robin.dto.AiResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class AiClientService {

    @Value("${groq.api.key:}")
    private String apiKey;

    private RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("CRITICAL: API key is missing!");
        }
        // Step 2: Hantering av Timeouts och bygg RestClient ->
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(8000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.groq.com/openai/v1")
                //.baseUrl("http://localhost:8080")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // Step 3: Prompt Engineering & Deterministisk utdata ->
//    public String analyzeText(String userText) {
//        String systemPrompt = """
//                You are a sentiment analysis engine.
//                Analyze the sentiment of the user's text and respond ONLY a valid JSON object.
//                Do not include markdown formatting, code blocks, or conversational text.
//                The JSON must follow this exact schema:
//                {"sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
//                "score": 0.0-1.0,
//                "summary": "one sentence explanation"}
//                """;
//
//        String requestBody = """
//                {
//                    "model": "llama-3.1-8b-instant",
//                    "temperature": 0.1,
//                    "messages": [
//                        {"role": "system", "content": "%s"},
//                        {"role": "user", "content": "%s"}
//                        ]
//                }
//                """.formatted(systemPrompt.replace("\"", "\\\"")
//                .replace("\n", "\\n"), userText);
//
//        return restClient.post()
//                .uri("/chat/completions")
//                .body(requestBody)
//                .retrieve()
//                .body(String.class);
//    }

    //Step 4: Hantera Rate Limits med Exponential Backoff ->
//    public String analyzeText(String userText) {
//        String systemPrompt = """
//                You are a sentiment analysis engine.
//                Analyze the sentiment of the user's text and respond ONLY a valid JSON object.
//                Do not include markdown formatting, code blocks, or conversational text.
//                The JSON must follow this exact schema:
//                {"sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
//                "score": 0.0-1.0,
//                "summary": "one sentence explanation"}
//                """;
//
//        String requestBody = """
//                {
//                    "model": "llama-3.1-8b-instant",
//                    "temperature": 0.1,
//                    "messages": [
//                        {"role": "system", "content": "%s"},
//                        {"role": "user", "content": "%s"}
//                        ]
//                }
//                """.formatted(systemPrompt.replace("\"", "\\\"")
//                .replace("\n", "\\n"), userText);
//
//        int retries = 3;
//        long delay = 1000;
//
//        for (int i = 0; i < retries; i++) {
//            ResponseEntity<String> response = restClient.post()
//                    .uri("/chat/completions")
//                    .body(requestBody)
//                    .retrieve()
//                    .toEntity(String.class);
//
//            if (response.getStatusCode().is2xxSuccessful()) {
//                return response.getBody();
//            }
//
//            if (response.getStatusCode().value() == 429) {
//                log.warn("Rate limit hit (429), retrying in {} ms... (attempt {}/{})", delay, i + 1, retries);
//                try {
//                    Thread.sleep(delay);
//                } catch (InterruptedException ex) {
//                    Thread.currentThread().interrupt();
//                }
//                delay *= 2;
//            }
//        }
//        throw new RuntimeException("Failed after " + retries + " retries due to rate limiting.");
//    }

    // Step 5: Begränsa Hallucinationer & Validera Struktur
    public AiResponseDto analyzeText(String userText) {
        String systemPrompt = """
                You are a sentiment analysis engine.
                Analyze the sentiment of the user's text and respond ONLY a valid JSON object.
                Do not include markdown formatting, code blocks, or conversational text.
                The JSON must follow this exact schema:
                {"sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
                "score": 0.0-1.0,
                "summary": "one sentence explanation"}
                """;

        String requestBody = """
                {
                "model": "llama-3.1-8b-instant",
                "temperature": 0.1,
                "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
                ]
                }
                """.formatted(systemPrompt.replace("\"", "\\\"")
                .replace("\n", "\\n"), userText);

        int retries = 3;
        long delay = 1000;

        for (int i = 0; i < retries; i++) {
            ResponseEntity<String> response = restClient.post()
                    .uri("/chat/completions")
                    //.uri("/fake/analyze")
                    //.uri("/fake/hallucinate") //Step 6: Framtvinga Hallucination
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status ->
                            status.value() == 429, (req, res) -> {})
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                try {
                    String rawBody = response.getBody();
                    JsonNode root = objectMapper.readTree(rawBody);
                    String aiContent = root.path("choices").get(0)
                            .path("message").path("content").asText();
                    return objectMapper.readValue(aiContent, AiResponseDto.class);
                } catch (JsonProcessingException ex) {
                    log.warn("Failed to parse AI response, returning fallback: {}", ex.getMessage());
                    return AiResponseDto.fallback();
                }
            }
            if (response.getStatusCode().value() == 429) {
                log.warn("Rate limit hit (429), retrying in {} ms... (attempt {}/{})", delay, i + 1, retries);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                delay *= 2;
            }
        }
        throw new RuntimeException("Failed after " + retries + " retries due to rate limiting.");
    }
}
