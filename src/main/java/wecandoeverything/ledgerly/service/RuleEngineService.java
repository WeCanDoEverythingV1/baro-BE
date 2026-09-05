package wecandoeverything.ledgerly.service;

import org.springframework.stereotype.Component;
import wecandoeverything.ledgerly.domain.*;
import wecandoeverything.ledgerly.dto.PolicyEvaluationInputDto;
import wecandoeverything.ledgerly.dto.PolicyEvaluationResultDto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Component
public class RuleEngineService {

    private static final BigDecimal WARNING_THRESHOLD_RATIO = new BigDecimal("0.8");

    /**
     * Returns a result only when the ruleset actually has enough to decide.
     * Returns null when no rule for this category exists at all — that's the
     * caller's signal to fall back to the LLM path, per the doc's design.
     */
    public PolicyEvaluationResultDto evaluate(List<PolicyRule> allRules, PolicyEvaluationInputDto input, Integer rulesetVersion) {
        // 1. Prohibited keywords — checked against every rule, regardless of category.
        String haystack = String.join(" ",
                nullToEmpty(input.getMerchant()), nullToEmpty(input.getItemName()), nullToEmpty(input.getPurpose())
        ).toLowerCase();

        for (PolicyRule rule : allRules) {
            if (rule.getProhibitions() == null) continue;
            for (String keyword : rule.getProhibitions()) {
                if (!keyword.isBlank() && haystack.contains(keyword.toLowerCase())) {
                    return result(ComplianceLevel.VIOLATION,
                            "%s — 금지 항목(\"%s\")이 감지되었습니다".formatted(rule.getClauseArticle(), keyword),
                            List.of(rule), rulesetVersion, true);
                }
            }
        }

        // 2. Only rules matching this expense's category are relevant from here on.
        List<PolicyRule> categoryRules = allRules.stream()
                .filter(r -> r.getExpenseCategory() == input.getCategory())
                .toList();

        if (categoryRules.isEmpty()) {
            return null; // signal: no coverage, caller should invoke the LLM fallback
        }

        List<PolicyRule> triggered = new ArrayList<>();
        ComplianceLevel worst = ComplianceLevel.COMPLIANT;
        List<String> summaries = new ArrayList<>();

        int attendees = (input.getAttendeeCount() != null && input.getAttendeeCount() > 0) ? input.getAttendeeCount() : 1;

        for (PolicyRule rule : categoryRules) {
            // Amount limit check
            if (rule.getLimitAmount() != null) {
                BigDecimal comparable = (rule.getScope() == RuleScope.PER_PERSON)
                        ? input.getAmount().divide(BigDecimal.valueOf(attendees), 2, java.math.RoundingMode.HALF_UP)
                        : input.getAmount();

                if (comparable.compareTo(rule.getLimitAmount()) > 0) {
                    triggered.add(rule);
                    worst = worse(worst, toComplianceLevel(rule.getSeverity()));
                    summaries.add("%s — %s 한도 ₩%s을 ₩%s 초과했습니다"
                            .formatted(rule.getClauseArticle(), scopeLabel(rule.getScope()),
                                    rule.getLimitAmount().toPlainString(), comparable.toPlainString()));
                } else if (comparable.compareTo(rule.getLimitAmount().multiply(WARNING_THRESHOLD_RATIO)) >= 0) {
                    triggered.add(rule);
                    worst = worse(worst, ComplianceLevel.WARNING);
                    summaries.add("%s — 한도의 80%% 이상 사용했습니다 (₩%s / ₩%s)"
                            .formatted(rule.getClauseArticle(), comparable.toPlainString(), rule.getLimitAmount().toPlainString()));
                }
            }

            // Condition checks
            RuleConditions cond = rule.getConditions();
            if (cond != null) {
                if (cond.latestHour() != null && input.getHour() != null && input.getHour() >= cond.latestHour()) {
                    triggered.add(rule);
                    worst = worse(worst, toComplianceLevel(rule.getSeverity()));
                    summaries.add("%s — %d시 이후 지출은 제한 조건이 있습니다".formatted(rule.getClauseArticle(), cond.latestHour()));
                }
                if (Boolean.FALSE.equals(cond.weekendAllowed()) && isWeekend(input.getDate())) {
                    triggered.add(rule);
                    worst = worse(worst, toComplianceLevel(rule.getSeverity()));
                    summaries.add("%s — 주말 지출은 허용되지 않습니다".formatted(rule.getClauseArticle()));
                }
                if (cond.minAttendees() != null && attendees < cond.minAttendees()) {
                    triggered.add(rule);
                    worst = worse(worst, toComplianceLevel(rule.getSeverity()));
                    summaries.add("%s — 최소 참석 인원(%d명) 미달입니다".formatted(rule.getClauseArticle(), cond.minAttendees()));
                }
            }
        }

        if (triggered.isEmpty()) {
            return result(ComplianceLevel.COMPLIANT, "적용되는 규정을 모두 준수했습니다", List.of(), rulesetVersion, true);
        }

        return result(worst, String.join(" / ", summaries), triggered, rulesetVersion, true);
    }

    private ComplianceLevel toComplianceLevel(RuleSeverity severity) {
        return severity == RuleSeverity.VIOLATION ? ComplianceLevel.VIOLATION : ComplianceLevel.WARNING;
    }

    private ComplianceLevel worse(ComplianceLevel a, ComplianceLevel b) {
        if (a == ComplianceLevel.VIOLATION || b == ComplianceLevel.VIOLATION) return ComplianceLevel.VIOLATION;
        if (a == ComplianceLevel.WARNING || b == ComplianceLevel.WARNING) return ComplianceLevel.WARNING;
        return ComplianceLevel.COMPLIANT;
    }

    private boolean isWeekend(java.time.LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String scopeLabel(RuleScope scope) {
        return switch (scope) {
            case PER_PERSON -> "1인당";
            case PER_RECEIPT -> "건당";
            case PER_MONTH -> "월별";
            case PER_TRIP -> "출장당";
        };
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private PolicyEvaluationResultDto result(ComplianceLevel level, String summary, List<PolicyRule> cited,
                                               Integer rulesetVersion, boolean deterministic) {
        return PolicyEvaluationResultDto.builder()
                .level(level)
                .summary(summary)
                .citedClauses(cited.stream()
                        .map(r -> new CitedClause(r.getClauseArticle(), r.getClauseText(), r.getClausePage()))
                        .toList())
                .matchedRuleIds(cited.stream().map(PolicyRule::getId).toList())
                .rulesetVersion(rulesetVersion)
                .deterministic(deterministic)
                .build();
    }
}