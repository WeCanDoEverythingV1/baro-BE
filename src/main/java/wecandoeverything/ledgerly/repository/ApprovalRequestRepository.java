package wecandoeverything.ledgerly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wecandoeverything.ledgerly.domain.ApprovalRequest;
import wecandoeverything.ledgerly.domain.ApprovalStatus;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);

    List<ApprovalRequest> findAllByOrderByCreatedAtDesc();
}