package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailDraftDto {
    private String subject;
    private String body;
}