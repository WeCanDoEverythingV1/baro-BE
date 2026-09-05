package wecandoeverything.ledgerly.dto;

import wecandoeverything.ledgerly.domain.PolicyRule;

import java.util.List;

public record PolicyExtractionResult(List<PolicyRule> rules, List<String> unmappedClauses, Integer pageCount) {}