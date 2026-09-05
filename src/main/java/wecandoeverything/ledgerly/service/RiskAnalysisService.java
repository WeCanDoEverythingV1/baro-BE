package wecandoeverything.ledgerly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wecandoeverything.ledgerly.domain.ApprovalRequest;
import wecandoeverything.ledgerly.domain.ExpenseCategory;
import wecandoeverything.ledgerly.domain.RiskLevel;
import wecandoeverything.ledgerly.dto.RiskAnalysisDto;
import wecandoeverything.ledgerly.repository.ApprovalRequestRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskAnalysisService {

    private static final Map<ExpenseCategory, BigDecimal> BUDGET_LIMITS = Map.of(
            ExpenseCategory.TRAVEL, new BigDecimal("1000000"),
            ExpenseCategory.LODGING, new BigDecimal("500000"),
            ExpenseCategory.MEALS, new BigDecimal("100000"),
            ExpenseCategory.SOFTWARE, new BigDecimal("200000"),
            ExpenseCategory.OFFICE_SUPPLIES, new BigDecimal("300000"),
            ExpenseCategory.OTHER, new BigDecimal("300000")
    );

    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.6");

    private static final Map<String, RiskLevel> LABEL_TO_LEVEL = new LinkedHashMap<>() {{
        put("위험: 예산 초과", RiskLevel.HIGH);
        put("위험: 정책 위반 품목", RiskLevel.HIGH);
        put("주의: 카테고리 불일치", RiskLevel.WARNING);
        put("주의: 모호한 목적 설명", RiskLevel.WARNING);
        put("주의: 고액 지출", RiskLevel.WARNING);
        put("주의: 신규 가맹점", RiskLevel.WARNING);
        put("주의: 주말 지출", RiskLevel.WARNING);
        put("규정 준수", RiskLevel.COMPLIANT);
    }};

    private final GeminiClient geminiClient;
    private final ApprovalRequestRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    // ---------- Approver's list view: batch, entities already persisted ----------

    public Map<Long, RiskAnalysisDto> analyzeAll(List<ApprovalRequest> requests) {
        if (requests.isEmpty()) return Map.of();

        String lines = requests.stream()
                .map(r -> describeLine(
                        r.getId().toString(), r.getMerchant(), r.getCategory(), r.getAmount(),
                        r.getItemName(), r.getPurpose(), r.getDate(),
                        isNewMerchantWithinList(r, requests)))
                .collect(Collectors.joining("\n"));

        Map<String, Object> schema = Map.of(
                "type", "ARRAY",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "id", Map.of("type", "STRING"),
                                "label", Map.of("type", "STRING", "enum", new ArrayList<>(LABEL_TO_LEVEL.keySet()))
                        ),
                        "required", List.of("id", "label")
                )
        );

        List<Map<String, String>> parsed;
        try {
            String json = geminiClient.generate(buildBatchPrompt(lines), schema);
            parsed = mapper.readValue(json, List.class);
        } catch (Exception e) {
            return requests.stream().collect(Collectors.toMap(ApprovalRequest::getId, r -> compliantFallback()));
        }

        Map<String, String> idToLabel = parsed.stream()
                .collect(Collectors.toMap(m -> m.get("id"), m -> m.get("label")));

        return requests.stream().collect(Collectors.toMap(
                ApprovalRequest::getId,
                r -> toDto(idToLabel.getOrDefault(r.getId().toString(), "규정 준수"))));
    }

    // ---------- Receipt scan: single not-yet-persisted draft ----------

    public RiskAnalysisDto analyzeDraft(String employeeName, String merchant, ExpenseCategory category,
                                        BigDecimal amount, String itemName, String purpose, LocalDate date) {
        boolean newMerchant = !repository.existsByEmployeeNameIgnoreCaseAndMerchantIgnoreCase(employeeName, merchant);
        String line = describeLine("draft", merchant, category, amount, itemName, purpose, date, newMerchant);

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "label", Map.of("type", "STRING", "enum", new ArrayList<>(LABEL_TO_LEVEL.keySet()))
                ),
                "required", List.of("label")
        );

        try {
            String json = geminiClient.generate(buildSinglePrompt(line), schema);
            Map<String, String> parsed = mapper.readValue(json, Map.class);
            return toDto(parsed.getOrDefault("label", "규정 준수"));
        } catch (Exception e) {
            return compliantFallback();
        }
    }

    // ---------- Shared fact computation ----------

    private boolean isNewMerchantWithinList(ApprovalRequest r, List<ApprovalRequest> all) {
        return all.stream()
                .filter(other -> !other.getId().equals(r.getId()))
                .noneMatch(other -> other.getEmployeeName().equals(r.getEmployeeName())
                        && other.getMerchant().equalsIgnoreCase(r.getMerchant()));
    }

    private boolean exceedsBudget(ExpenseCategory category, BigDecimal amount) {
        BigDecimal limit = BUDGET_LIMITS.getOrDefault(category, BUDGET_LIMITS.get(ExpenseCategory.OTHER));
        return amount.compareTo(limit) > 0;
    }

    private boolean isHighButUnderLimit(ExpenseCategory category, BigDecimal amount) {
        BigDecimal limit = BUDGET_LIMITS.getOrDefault(category, BUDGET_LIMITS.get(ExpenseCategory.OTHER));
        return !exceedsBudget(category, amount) && amount.compareTo(limit.multiply(WARNING_RATIO)) > 0;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String describeLine(String id, String merchant, ExpenseCategory category, BigDecimal amount,
                                String itemName, String purpose, LocalDate date, boolean newMerchant) {
        return "- id=%s, merchant=%s, category=%s, amount=%s, itemName=\"%s\", purpose=\"%s\", date=%s, flaggedOverBudget=%b, flaggedHighAmount=%b, flaggedWeekend=%b, flaggedNewMerchant=%b"
                .formatted(id, merchant, category, amount, itemName, purpose, date,
                        exceedsBudget(category, amount), isHighButUnderLimit(category, amount),
                        isWeekend(date), newMerchant);
    }

    private String buildBatchPrompt(String lines) {
        return "You are reviewing company expense requests for policy risk. For each request, choose exactly ONE label that best fits — prioritizing in this order when multiple conditions could apply:\n"
                + labelGuide() + "\n\nRequests:\n" + lines;
    }

    private String buildSinglePrompt(String line) {
        return "You are reviewing a single company expense request for policy risk. Choose exactly ONE label that best fits — prioritizing in this order when multiple conditions could apply:\n"
                + labelGuide() + "\n\nRequest:\n" + line;
    }

    private String labelGuide() {
        return """
                1. "위험: 예산 초과" — flaggedOverBudget is true
                2. "위험: 정책 위반 품목" — the item or purpose describes something
                   that is NOT a legitimate business expense — use your judgment
                   on the actual text, not just the category label
                3. "주의: 카테고리 불일치" — the stated category clearly doesn't match
                   what the item/purpose actually describes
                4. "주의: 모호한 목적 설명" — the purpose text is too generic or vague
                   to justify the expense (empty or missing purpose also counts)
                5. "주의: 고액 지출" — flaggedHighAmount is true
                6. "주의: 신규 가맹점" — flaggedNewMerchant is true
                7. "주의: 주말 지출" — flaggedWeekend is true
                8. "규정 준수" — none of the above apply

                Only use labels 2-4 based on genuinely reading the item/purpose
                text — do not guess without real textual evidence.
                """;
    }

    private RiskAnalysisDto toDto(String label) {
        return RiskAnalysisDto.builder()
                .level(LABEL_TO_LEVEL.getOrDefault(label, RiskLevel.COMPLIANT))
                .label(label)
                .build();
    }

    private RiskAnalysisDto compliantFallback() {
        return toDto("규정 준수");
    }
}