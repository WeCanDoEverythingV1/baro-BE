package wecandoeverything.ledgerly.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailDraftDto {
    private String subject;
    private String body;
}