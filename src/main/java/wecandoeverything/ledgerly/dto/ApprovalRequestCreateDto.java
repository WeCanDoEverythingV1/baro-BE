package wecandoeverything.ledgerly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import wecandoeverything.ledgerly.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ApprovalRequestCreateDto {

    @NotBlank(message = "employeeName is required")
    private String employeeName;

    @NotBlank(message = "merchant is required")
    private String merchant;

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotNull
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "itemName is required")
    private String itemName;

    @NotBlank(message = "purpose is required")
    private String purpose;

    @NotNull(message = "category is required")
    private ExpenseCategory expenseCategory;
}