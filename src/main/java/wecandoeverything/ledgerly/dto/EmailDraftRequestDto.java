package wecandoeverything.ledgerly.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailDraftRequestDto {

    @NotNull(message = "subscription is required")
    @Valid
    private SubscriptionDto subscription;

    @NotBlank(message = "action is required")
    private String action;
}
