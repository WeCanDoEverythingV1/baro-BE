package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailDraftRequestDto {
    private SubscriptionDto subscription;
    private String action;
}
