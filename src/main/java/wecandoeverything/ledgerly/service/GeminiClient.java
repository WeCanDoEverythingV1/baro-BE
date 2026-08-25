package wecandoeverything.ledgerly.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Sends a prompt, gets back raw text. Caller is responsible for
     * telling Gemini what shape to respond in (see responseSchema below).
     */
    public String generate(String prompt, Map<String, Object> responseSchema) {
        Map<String, Object> body = Map.of(
                "contents", java.util.List.of(
                        Map.of("parts", java.util.List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );

        String rawResponse = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractText(rawResponse);
    }

    private String extractText(String rawResponse) {
        try {
            JsonNode root = mapper.readTree(rawResponse);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini response: " + rawResponse, e);
        }
    }
}