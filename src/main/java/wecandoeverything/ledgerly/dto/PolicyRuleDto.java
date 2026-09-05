package wecandoeverything.ledgerly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wecandoeverything.ledgerly.domain.ExpenseCategory;
import wecandoeverything.ledgerly.domain.RuleConditions;
import wecandoeverything.ledgerly.domain.RuleScope;
import wecandoeverything.ledgerly.domain.RuleSeverity;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRuleDto {
    private String id; // null when the reviewer adds a brand-new rule client-side

    @NotNull(message = "expenseCategory is required")
    private ExpenseCategory expenseCategory;

    @NotNull(message = "scope is required")
    private RuleScope scope;

    private BigDecimal limitAmount; // null = no amount limit

    private RuleConditions conditions;
    private List<String> requiredEvidence;
    private List<String> prohibitions;

    @NotNull(message = "severity is required")
    private RuleSeverity severity;

    private String note;

    private String clauseArticle;

    @NotBlank(message = "clauseText is required — every rule must cite source text")
    private String clauseText;

    private Integer clausePage;
    private Double confidence;
}