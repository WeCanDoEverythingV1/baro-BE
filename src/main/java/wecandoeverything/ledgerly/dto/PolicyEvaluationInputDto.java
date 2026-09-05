package wecandoeverything.ledgerly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import wecandoeverything.ledgerly.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PolicyEvaluationInputDto {
    private String merchant;
    private String itemName;
    private String purpose;

    @NotNull(message = "category is required")
    private ExpenseCategory category;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotNull(message = "date is required")
    private LocalDate date;

    private Integer attendeeCount; // null/absent treated as 1 for PER_PERSON scope
    private Integer hour;          // 0-23, optional — null means "no time info, skip that check"
}