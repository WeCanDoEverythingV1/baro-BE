package wecandoeverything.ledgerly.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import wecandoeverything.ledgerly.dto.SubscriptionDto;
import wecandoeverything.ledgerly.dto.SubscriptionAnalysisDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionAnalysisService {

    private final SubscriptionWasteCalculator calculator;
    private final GeminiClient geminiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<SubscriptionAnalysisDto> analyze(@Valid List<SubscriptionDto> subscriptions) {
        // 1. Prompt built straight from the source objects — no intermediate map
        String lines = subscriptions.stream()
                .map(s -> "- id=%s, name=%s, idleSeats=%d, monthlyWaste=%s, severity=%s, lastUsed=%s"
                        .formatted(s.getId(), s.getName(), calculator.idleSeats(s),
                                calculator.monthlyWaste(s), calculator.severity(s), s.getLastUsed()))
                .collect(Collectors.joining("\n"));

        String prompt = """
            You are a SaaS spend analyst. For each subscription below, write ONE
            short, specific recommendation sentence (max 20 words) a finance
            team could act on immediately. Use the idle seat count and waste
            amount already provided — do not recalculate them.

            Subscriptions:
            %s
            """.formatted(lines);

        Map<String, Object> schema = Map.of(
                "type", "ARRAY",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "id", Map.of("type", "STRING"),
                                "recommendation", Map.of("type", "STRING")
                        ),
                        "required", List.of("id", "recommendation")
                )
        );

        Map<String, String> recommendations = parseList(geminiClient.generate(prompt, schema))
                .stream()
                .collect(Collectors.toMap(r -> r.get("id"), r -> r.get("recommendation")));

        // 2. Build the DTO once, directly from the source — no casts, no NPE risk
        return subscriptions.stream()
                .map(s -> SubscriptionAnalysisDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .idleSeats(calculator.idleSeats(s))
                        .monthlyWaste(calculator.monthlyWaste(s))
                        .severity(calculator.severity(s))
                        .recommendation(recommendations.getOrDefault(
                                s.getId(), "No recommendation available."))
                        .build())
                .toList();
    }

    private List<Map<String, String>> parseList(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini recommendations: " + json, e);
        }
    }
}