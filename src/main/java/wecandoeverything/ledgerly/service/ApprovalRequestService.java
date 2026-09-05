package wecandoeverything.ledgerly.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wecandoeverything.ledgerly.domain.ApprovalRequest;
import wecandoeverything.ledgerly.domain.ApprovalStatus;
import wecandoeverything.ledgerly.dto.ApprovalRequestCreateDto;
import wecandoeverything.ledgerly.dto.ApprovalRequestResponseDto;
import wecandoeverything.ledgerly.exception.ApprovalRequestNotFoundException;
import wecandoeverything.ledgerly.repository.ApprovalRequestRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestService {

    private final ApprovalRequestRepository repository;

    @Transactional
    public ApprovalRequestResponseDto create(ApprovalRequestCreateDto dto) {
        ApprovalRequest entity = ApprovalRequest.builder()
                .employeeName(dto.getEmployeeName())
                .merchant(dto.getMerchant())
                .date(dto.getDate())
                .amount(dto.getAmount())
                .itemName(dto.getItemName())
                .purpose(dto.getPurpose())
                .status(ApprovalStatus.PENDING)
                .category(dto.getExpenseCategory())
                .build();

        ApprovalRequest saved = repository.save(entity);
        return toResponseDto(saved);
    }

    public List<ApprovalRequestResponseDto> getPending() {
        return repository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<ApprovalRequestResponseDto> getByStatus(ApprovalStatus status) {
        List<ApprovalRequest> requests = (status == null)
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);

        return requests.stream().map(this::toResponseDto).toList();
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
        return toResponseDto(entity);
    }

    private ApprovalRequestResponseDto toResponseDto(ApprovalRequest entity) {
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
                .build();
    }
}