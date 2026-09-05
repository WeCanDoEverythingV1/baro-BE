package wecandoeverything.ledgerly.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateRulesRequestDto {
    @NotNull(message = "rules is required")
    @Valid
    private List<PolicyRuleDto> rules;
}