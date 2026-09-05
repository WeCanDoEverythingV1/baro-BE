package wecandoeverything.ledgerly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wecandoeverything.ledgerly.domain.ApprovalRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findAllByOrderByCreatedAtDesc();

    List<ApprovalRequest> findByMerchantIgnoreCaseAndAmount(String merchant, BigDecimal amount);

    boolean existsByEmployeeNameIgnoreCaseAndMerchantIgnoreCase(String employeeName, String merchant);

    long countByRulesetVersion(Integer rulesetVersion);
}