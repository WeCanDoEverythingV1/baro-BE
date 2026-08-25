package wecandoeverything.ledgerly.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubscriptionAnalysisRequest {

    @NotEmpty(message = "subscriptions must not be empty")
    @Valid
    private List<SubscriptionDto> subscriptions;
}