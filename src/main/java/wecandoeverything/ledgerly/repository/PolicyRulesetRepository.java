package wecandoeverything.ledgerly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wecandoeverything.ledgerly.domain.PolicyRuleset;
import wecandoeverything.ledgerly.domain.PolicyRulesetStatus;

import java.util.List;
import java.util.Optional;

public interface PolicyRulesetRepository extends JpaRepository<PolicyRuleset, String> {
    List<PolicyRuleset> findAllByOrderByVersionDesc();
    Optional<PolicyRuleset> findByStatus(PolicyRulesetStatus status); // ACTIVE is unique per company
    Optional<PolicyRuleset> findBySourceFileHash(String hash);
}