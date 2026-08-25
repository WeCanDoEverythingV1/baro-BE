package wecandoeverything.ledgerly.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SubscriptionDto {

    @NotBlank(message = "id is required")
    private String id;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "category is required")
    private String category;

    @NotNull(message = "monthlyCost is required")
    @PositiveOrZero(message = "monthlyCost cannot be negative")
    private BigDecimal monthlyCost;

    @NotNull(message = "seats is required")
    @Positive(message = "seats must be at least 1")
    private Integer seats;

    @NotNull(message = "activeSeats is required")
    @PositiveOrZero(message = "activeSeats cannot be negative")
    private Integer activeSeats;

    @NotBlank(message = "lastUsed is required")
    private String lastUsed;

    @AssertTrue(message = "activeSeats cannot exceed seats")
    public boolean isActiveSeatsWithinLimit() {
        if (seats == null || activeSeats == null) {
            return true; // let @NotNull report the real problem, not this one
        }
        return activeSeats <= seats;
    }
}