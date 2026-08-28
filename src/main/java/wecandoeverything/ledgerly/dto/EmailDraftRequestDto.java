package wecandoeverything.ledgerly.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailDraftRequestDto {
    private SubscriptionDto subscription;
    private String action;
}
