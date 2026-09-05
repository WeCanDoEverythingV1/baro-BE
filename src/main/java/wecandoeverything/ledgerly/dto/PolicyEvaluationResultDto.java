package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;
import wecandoeverything.ledgerly.domain.CitedClause;
import wecandoeverything.ledgerly.domain.ComplianceLevel;

import java.util.List;

@Getter
@Builder
public class PolicyEvaluationResultDto {
    private ComplianceLevel level;
    private String summary;
    private List<CitedClause> citedClauses;
    private List<String> matchedRuleIds;
    private Integer rulesetVersion;
    private boolean deterministic;
}