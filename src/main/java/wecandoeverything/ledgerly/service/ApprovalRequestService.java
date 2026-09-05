package wecandoeverything.ledgerly.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wecandoeverything.ledgerly.domain.ApprovalRequest;
import wecandoeverything.ledgerly.domain.ApprovalStatus;
import wecandoeverything.ledgerly.dto.ApprovalRequestCreateDto;
import wecandoeverything.ledgerly.dto.ApprovalRequestResponseDto;
import wecandoeverything.ledgerly.dto.PolicyEvaluationInputDto;
import wecandoeverything.ledgerly.dto.RiskAnalysisDto;
import wecandoeverything.ledgerly.exception.ApprovalRequestNotFoundException;
import wecandoeverything.ledgerly.repository.ApprovalRequestRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestService {

    private final ApprovalRequestRepository repository;
    private final RiskAnalysisService riskAnalysisService;
    private final PolicyEvaluationService policyEvaluationService;

    @Transactional
    public ApprovalRequestResponseDto create(ApprovalRequestCreateDto dto) {
        ApprovalRequest entity = ApprovalRequest.builder()
                .employeeName(dto.getEmployeeName())
                .merchant(dto.getMerchant())
                .date(dto.getDate())
                .amount(dto.getAmount())
                .itemName(dto.getItemName())
                .category(dto.getExpenseCategory())
                .purpose(dto.getPurpose())
                .status(ApprovalStatus.PENDING)
                .build();

        applyCompliance(entity);

        ApprovalRequest saved = repository.save(entity);
        return toResponseDto(saved, null);
    }

    private void applyCompliance(ApprovalRequest entity) {
        try {
            PolicyEvaluationInputDto input = new PolicyEvaluationInputDto();
            input.setMerchant(entity.getMerchant());
            input.setItemName(entity.getItemName());
            input.setPurpose(entity.getPurpose());
            input.setCategory(entity.getCategory());
            input.setAmount(entity.getAmount());
            input.setDate(entity.getDate());
            // attendeeCount/hour: not collected on this form yet — left null,
            // meaning PER_PERSON rules divide by 1 and time-of-day checks skip.

            policyEvaluationService.evaluate(input).ifPresent(result -> {
                entity.setComplianceLevel(result.getLevel());
                entity.setComplianceSummary(result.getSummary());
                entity.setCitedClauses(result.getCitedClauses());
                entity.setRulesetVersion(result.getRulesetVersion());
            });
            // Optional.empty() = no active ruleset → all 4 fields stay null, as specified
        } catch (Exception e) {
            // A submission must never fail just because policy evaluation broke —
            // fields simply stay null, same as "no active ruleset."
        }
    }

    public List<ApprovalRequestResponseDto> getPending() {
        return getByStatus(ApprovalStatus.PENDING);
    }

    public List<ApprovalRequestResponseDto> getByStatus(ApprovalStatus status) {
        List<ApprovalRequest> all = repository.findAllByOrderByCreatedAtDesc();
        List<ApprovalRequest> filtered = (status == null)
                ? all
                : all.stream().filter(r -> r.getStatus() == status).toList();

        Map<Long, RiskAnalysisDto> riskById = riskAnalysisService.analyzeAll(all);

        return filtered.stream()
                .map(r -> toResponseDto(r, riskById.get(r.getId())))
                .toList();
    }

    @Transactional
    public ApprovalRequestResponseDto approve(Long id) {
        return updateStatus(id, ApprovalStatus.APPROVED);
    }

    @Transactional
    public ApprovalRequestResponseDto reject(Long id) {
        return updateStatus(id, ApprovalStatus.REJECTED);
    }

    private ApprovalRequestResponseDto updateStatus(Long id, ApprovalStatus newStatus) {
        ApprovalRequest entity = repository.findById(id)
                .orElseThrow(() -> new ApprovalRequestNotFoundException(id));
        entity.setStatus(newStatus);
        return toResponseDto(entity, null);
    }

    private ApprovalRequestResponseDto toResponseDto(ApprovalRequest entity, RiskAnalysisDto risk) {
        return ApprovalRequestResponseDto.builder()
                .id(entity.getId())
                .employeeName(entity.getEmployeeName())
                .merchant(entity.getMerchant())
                .date(entity.getDate())
                .amount(entity.getAmount())
                .itemName(entity.getItemName())
                .purpose(entity.getPurpose())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .expenseCategory(entity.getCategory())
                .risk(risk)
                .build();
    }
}