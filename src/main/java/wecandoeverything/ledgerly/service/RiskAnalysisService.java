package wecandoeverything.ledgerly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wecandoeverything.ledgerly.domain.ApprovalRequest;
import wecandoeverything.ledgerly.domain.ExpenseCategory;
import wecandoeverything.ledgerly.domain.RiskLevel;
import wecandoeverything.ledgerly.dto.RiskAnalysisDto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
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
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<Long, RiskAnalysisDto> analyzeAll(List<ApprovalRequest> requests) {
        if (requests.isEmpty()) return Map.of();

        Map<String, Set<String>> merchantsByEmployee = new HashMap<>();
        for (ApprovalRequest r : requests) {
            merchantsByEmployee
                    .computeIfAbsent(r.getEmployeeName(), k -> new HashSet<>())
                    .add(r.getMerchant().toLowerCase());
        }

        String lines = requests.stream()
                .map(r -> describeWithFacts(r, requests, merchantsByEmployee))
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are reviewing company expense requests for policy risk. For each
                request, choose exactly ONE label that best fits — prioritizing in
                this order when multiple conditions could apply:

                1. "위험: 예산 초과" — flaggedOverBudget is true
                2. "위험: 정책 위반 품목" — the item or purpose describes something
                   that is NOT a legitimate business expense (alcohol, personal
                   entertainment, gifts to individuals, etc.) — use your judgment
                   on the actual text, not just the category label
                3. "주의: 카테고리 불일치" — the stated category clearly doesn't match
                   what the item/purpose actually describes
                4. "주의: 모호한 목적 설명" — the purpose text is too generic or vague
                   to justify the expense (e.g. "misc", "업무 관련", no real detail)
                5. "주의: 고액 지출" — flaggedHighAmount is true
                6. "주의: 신규 가맹점" — flaggedNewMerchant is true
                7. "주의: 주말 지출" — flaggedWeekend is true
                8. "규정 준수" — none of the above apply

                Only use labels 2-4 based on genuinely reading the item/purpose
                text — do not guess or apply them without real textual evidence.

                Requests:
                %s
                """.formatted(lines);

        Map<String, Object> schema = Map.of(
                "type", "ARRAY",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "id", Map.of("type", "STRING"),
                                "label", Map.of("type", "STRING",
                                        "enum", new ArrayList<>(LABEL_TO_LEVEL.keySet()))
                        ),
                        "required", List.of("id", "label")
                )
        );

        List<Map<String, String>> parsed;
        try {
            String json = geminiClient.generate(prompt, schema);
            parsed = mapper.readValue(json, List.class);
        } catch (Exception e) {
            return requests.stream().collect(Collectors.toMap(
                    ApprovalRequest::getId,
                    r -> RiskAnalysisDto.builder()
                            .level(RiskLevel.COMPLIANT)
                            .label("규정 준수")
                            .build()));
        }

        Map<String, String> idToLabel = parsed.stream()
                .collect(Collectors.toMap(m -> m.get("id"), m -> m.get("label")));

        return requests.stream().collect(Collectors.toMap(
                ApprovalRequest::getId,
                r -> {
                    String label = idToLabel.getOrDefault(r.getId().toString(), "규정 준수");
                    return RiskAnalysisDto.builder()
                            .level(LABEL_TO_LEVEL.getOrDefault(label, RiskLevel.COMPLIANT))
                            .label(label)
                            .build();
                }));
    }

    private String describeWithFacts(ApprovalRequest r, List<ApprovalRequest> all,
                                     Map<String, Set<String>> merchantsByEmployee) {
        boolean overBudget = exceedsBudget(r);
        boolean highAmount = !overBudget && isHighButUnderLimit(r);
        boolean weekend = isWeekend(r);
        boolean newMerchant = isNewMerchant(r, all, merchantsByEmployee);

        return """
                - id=%s, merchant=%s, category=%s, amount=%s, itemName="%s", purpose="%s", date=%s, \
                flaggedOverBudget=%b, flaggedHighAmount=%b, flaggedWeekend=%b, flaggedNewMerchant=%b\
                """.formatted(r.getId(), r.getMerchant(), r.getCategory(), r.getAmount(),
                r.getItemName(), r.getPurpose(), r.getDate(),
                overBudget, highAmount, weekend, newMerchant);
    }

    private boolean exceedsBudget(ApprovalRequest request) {
        BigDecimal limit = BUDGET_LIMITS.getOrDefault(
                request.getCategory(), BUDGET_LIMITS.get(ExpenseCategory.OTHER));
        return request.getAmount().compareTo(limit) > 0;
    }

    private boolean isHighButUnderLimit(ApprovalRequest r) {
        BigDecimal limit = BUDGET_LIMITS.getOrDefault(r.getCategory(), BUDGET_LIMITS.get(ExpenseCategory.OTHER));
        return r.getAmount().compareTo(limit.multiply(WARNING_RATIO)) > 0;
    }

    private boolean isWeekend(ApprovalRequest request) {
        DayOfWeek day = request.getDate().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isNewMerchant(ApprovalRequest r, List<ApprovalRequest> all,
                                  Map<String, Set<String>> merchantsByEmployee) {
        long priorCount = all.stream()
                .filter(other -> !other.getId().equals(r.getId()))
                .filter(other -> other.getEmployeeName().equals(r.getEmployeeName()))
                .filter(other -> other.getMerchant().equalsIgnoreCase(r.getMerchant()))
                .count();
        return priorCount == 0;
    }
}