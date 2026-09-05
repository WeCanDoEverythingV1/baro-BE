package wecandoeverything.ledgerly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wecandoeverything.ledgerly.domain.PolicyRule;

import java.util.List;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, String> {
    List<PolicyRule> findByRulesetId(String rulesetId);
    void deleteByRulesetId(String rulesetId);
}