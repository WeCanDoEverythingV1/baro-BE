package wecandoeverything.ledgerly.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wecandoeverything.ledgerly.dto.EmailDraftDto;
import wecandoeverything.ledgerly.dto.SubscriptionDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailDraftService {

    private final GeminiClient geminiClient;
    private final SubscriptionWasteCalculator calculator;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String defaultLanguage = "Korean";

    public EmailDraftDto draft(SubscriptionDto sub, String action, String language) {
        int idle = calculator.idleSeats(sub);
        BigDecimal waste = calculator.monthlyWaste(sub);

        String prompt = """
        Write a professional vendor email from a finance operations team,
        requesting a subscription plan change. Structure it as 4-5 short
        paragraphs:
        1. Greeting and brief context (who you are, why you're writing)
        2. The specific usage data driving this request (seats, activity)
        3. The financial impact and the specific change requested
        4. A clear ask (confirm rate, effective date, any paperwork needed)
        5. Polite closing

        Formatting requirement: separate each paragraph with a blank line —
        insert two newline characters (\\n\\n) between paragraphs inside the
        "body" JSON string value. Do not write it as one continuous block
        of text.

        Action requested: %s
        Vendor: %s
        Current seats: %d, active seats: %d, idle seats: %d
        Estimated monthly waste: $%.2f
        Last used: %s

        %s
        """.formatted(action, sub.getName(), sub.getSeats(), sub.getActiveSeats(),
                idle, waste, sub.getLastUsed(), languageInstruction(language));

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "subject", Map.of("type", "STRING"),
                        "body", Map.of("type", "STRING")
                ),
                "required", List.of("subject", "body")
        );

        String json = geminiClient.generate(prompt, schema);
        try {
            return mapper.readValue(json, EmailDraftDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse email draft: " + json, e);
        }
    }

    private String languageInstruction(String language) {
        if (language == null || language.isBlank()) {
            language = defaultLanguage;
        }
        return """
                Write the entire email — both the subject line and the body — in %s.
                Use formal, polite business register appropriate for professional
                correspondence in that language.
            """.formatted(language);
    }
}