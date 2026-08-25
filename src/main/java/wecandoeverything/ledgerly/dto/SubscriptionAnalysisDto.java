package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SubscriptionAnalysisDto {
    private String id;
    private String name;
    private Integer idleSeats;
    private BigDecimal monthlyWaste;
    private String severity;       // "high" | "medium" | "low" — computed, not AI
    private String recommendation; // AI-written sentence
}