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

    public EmailDraftDto draft(SubscriptionDto sub, String action) {
        int idle = calculator.idleSeats(sub);
        BigDecimal waste = calculator.monthlyWaste(sub);

        String prompt = """
                Write a professional vendor email from a finance operations team.
                Action requested: %s
                Vendor: %s
                Current seats: %d, active seats: %d, idle seats: %d
                Estimated monthly waste: $%.2f
                Last used: %s

                Keep it concise, polite, and specific to these numbers.
                """.formatted(action, sub.getName(), sub.getSeats(), sub.getActiveSeats(),
                idle, waste, sub.getLastUsed());

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
}