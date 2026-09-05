package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;
import wecandoeverything.ledgerly.domain.ApprovalStatus;
import wecandoeverything.ledgerly.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}