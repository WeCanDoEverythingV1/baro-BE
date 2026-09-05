package wecandoeverything.ledgerly.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wecandoeverything.ledgerly.domain.ComplianceLevel;
import wecandoeverything.ledgerly.domain.PolicyRule;
import wecandoeverything.ledgerly.domain.PolicyRuleset;
import wecandoeverything.ledgerly.dto.PolicyEvaluationInputDto;
import wecandoeverything.ledgerly.dto.PolicyEvaluationResultDto;
import wecandoeverything.ledgerly.repository.PolicyRuleRepository;
import wecandoeverything.ledgerly.repository.PolicyRulesetRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final PolicyRulesetRepository rulesetRepository;
    private final PolicyRuleRepository ruleRepository;
    private final RuleEngineService ruleEngine;
    private final GeminiClient geminiClient;
    private final ObjectMapper mapper;

    public Optional<PolicyEvaluationResultDto> evaluate(PolicyEvaluationInputDto input) {
        PolicyRuleset active = rulesetRepository.findByStatus(
                wecandoeverything.ledgerly.domain.PolicyRulesetStatus.ACTIVE).orElse(null);

        if (active == null) return Optional.empty(); // caller returns 409

        List<PolicyRule> rules = ruleRepository.findByRulesetId(active.getId());
        PolicyEvaluationResultDto deterministic = ruleEngine.evaluate(rules, input, active.getVersion());

        if (deterministic != null) {
            return Optional.of(deterministic);
        }

        // No rule covers this category — fall back to LLM judgment.
        return Optional.of(evaluateWithLlm(input, active.getVersion()));
    }

    private PolicyEvaluationResultDto evaluateWithLlm(PolicyEvaluationInputDto input, Integer rulesetVersion) {
        String prompt = """
                You are checking a company expense against internal policy. No
                explicit rule in the active policy covers this expense category,
                so use general reasonable business judgment.

                Category: %s
                Merchant: %s
                Item: %s
                Purpose: %s
                Amount: %s
                Date: %s

                Choose a compliance level: COMPLIANT, WARNING, or VIOLATION.
                Write one short Korean sentence explaining the reasoning.
                """.formatted(input.getCategory(), input.getMerchant(), input.getItemName(),
                        input.getPurpose(), input.getAmount(), input.getDate());

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "level", Map.of("type", "STRING", "enum", List.of("COMPLIANT", "WARNING", "VIOLATION")),
                        "summary", Map.of("type", "STRING")
                ),
                "required", List.of("level", "summary")
        );

        try {
            String json = geminiClient.generate(prompt, schema);
            Map<String, String> parsed = mapper.readValue(json, Map.class);
            return PolicyEvaluationResultDto.builder()
                    .level(ComplianceLevel.valueOf(parsed.get("level")))
                    .summary(parsed.get("summary"))
                    .citedClauses(List.of()) // nothing to cite — no rule covered this
                    .matchedRuleIds(List.of())
                    .rulesetVersion(rulesetVersion)
                    .deterministic(false)
                    .build();
        } catch (Exception e) {
            return PolicyEvaluationResultDto.builder()
                    .level(ComplianceLevel.COMPLIANT)
                    .summary("정책 판정을 수행할 수 없어 기본값으로 처리되었습니다")
                    .citedClauses(List.of())
                    .matchedRuleIds(List.of())
                    .rulesetVersion(rulesetVersion)
                    .deterministic(false)
                    .build();
        }
    }
}