package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;
import wecandoeverything.ledgerly.domain.PolicyRulesetStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PolicyRulesetDto {
    private String id;
    private Long companyId;
    private Integer version;
    private PolicyRulesetStatus status;
    private String sourceFileName;
    private String sourceFileHash;
    private Integer pageCount;
    private List<String> unmappedClauses;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private List<PolicyRuleDto> rules;
}