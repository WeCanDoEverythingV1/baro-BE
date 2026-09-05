package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;
import wecandoeverything.ledgerly.domain.ApprovalStatus;
import wecandoeverything.ledgerly.domain.CitedClause;
import wecandoeverything.ledgerly.domain.ComplianceLevel;
import wecandoeverything.ledgerly.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ApprovalRequestResponseDto {
    private Long id;
    private String employeeName;
    private String merchant;
    private LocalDate date;
    private BigDecimal amount;
    private String itemName;
    private String purpose;
    private ApprovalStatus status;
    private LocalDateTime createdAt;
    private ExpenseCategory expenseCategory;
    private RiskAnalysisDto risk;
    private ComplianceLevel complianceLevel; // null = no active ruleset when submitted
    private String complianceSummary;
    private List<CitedClause> citedClauses;
    private Integer rulesetVersion;
}