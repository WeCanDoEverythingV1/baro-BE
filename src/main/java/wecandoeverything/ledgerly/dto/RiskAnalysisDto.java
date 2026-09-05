package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;
import wecandoeverything.ledgerly.domain.RiskLevel;

@Getter
@Builder
public class RiskAnalysisDto {
    private RiskLevel level;
    private String label;
}